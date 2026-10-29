package com.kizuna.features.learning_support.service.kanji;


import com.kizuna.core.entity.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Thuật toán phân tích cảm quan động lực học nét vẽ (Kinematics & Sensory Analysis)
 * dựa trên chuỗi tọa độ thời gian (x, y, t).
 *
 * Tính toán các chỉ số:
 * 1. Vận tốc tức thời & suy diễn áp lực bút (Simulated Pressure)
 * 2. Kỹ thuật kết thúc nét Thư pháp: Dừng bút (Tome - 止め) vs Vuốt/Móc (Harai/Hane - 払い/跳ね)
 * 3. Độ ổn định cổ tay & vi chấn (Jitter / Shaky Hand Rate)
 */
public class StrokeSensoryAnalyzer {

    public record StrokePoint(double x, double y, long t) {}

    public record StrokeSensoryResult(
            double avgVelocity,       // Vận tốc trung bình (px/ms đã chuẩn hóa)
            double endVelocity,       // Vận tốc đoạn cuối nét (15% cuối)
            double jitterRate,        // Tỷ lệ phân đoạn bị bẻ góc đột ngột (0.0 -> 1.0)
            String dynamics,          // "TOME", "HARAI", "BALANCED"
            String note               // Nhận xét riêng cho nét này
    ) {}

    public record CharacterSensoryReport(
            double overallAvgVelocity,
            double overallJitterRate,
            int stabilityScore,       // Điểm vững tay (0 - 100%)
            String overallAssessment, // Nhận xét tổng quan cảm quan
            List<String> kineticBullets
    ) {}

    /**
     * Phân tích toàn bộ các nét của chữ Hán
     */
    public static CharacterSensoryReport analyzeCharacter(List<List<StrokePoint>> strokes, double canvasWidth) {
        if (strokes == null || strokes.isEmpty()) {
            return new CharacterSensoryReport(0.0, 0.0, 100, "Chưa ghi nhận nét vẽ hợp lệ.", Collections.emptyList());
        }

        double totalVelocity = 0.0;
        double totalJitter = 0.0;
        int validStrokesCount = 0;
        List<String> bullets = new ArrayList<>();

        for (int i = 0; i < strokes.size(); i++) {
            List<StrokePoint> stroke = strokes.get(i);
            if (stroke == null || stroke.size() < 2) continue;

            StrokeSensoryResult res = analyzeSingleStroke(stroke, canvasWidth, i + 1);
            totalVelocity += res.avgVelocity();
            totalJitter += res.jitterRate();
            validStrokesCount++;

            if (res.note() != null && !res.note().isBlank()) {
                bullets.add(res.note());
            }
        }

        if (validStrokesCount == 0) {
            return new CharacterSensoryReport(0.0, 0.0, 100, "Chưa đủ dữ liệu điểm nét vẽ.", Collections.emptyList());
        }

        double avgV = Math.round((totalVelocity / validStrokesCount) * 100.0) / 100.0;
        double jitterRate = Math.round((totalJitter / validStrokesCount) * 1000.0) / 1000.0;
        int stability = Math.max(40, Math.min(100, (int) Math.round((1.0 - (jitterRate * 1.8)) * 100)));

        String overallAssessment;
        if (jitterRate > 0.22) {
            overallAssessment = "Nét còn run rẩy do chần chừ hoặc kéo nét chưa dứt khoát.";
        } else if (avgV > 1.35) {
            overallAssessment = "Lướt bút khá nhanh, nét hơi nông lực; cần hãm bớt tốc độ để tăng độ đầm.";
        } else if (avgV < 0.20 && avgV > 0) {
            overallAssessment = "Tốc độ lia bút chậm, nên tự tin tăng nhịp kéo nét đều hơn.";
        } else {
            overallAssessment = "Lực bút đầm tay, tốc độ lia bút nhịp nhàng và ổn định.";
        }

        return new CharacterSensoryReport(avgV, jitterRate, stability, overallAssessment, bullets);
    }

    /**
     * Phân tích động lực học của một nét đơn
     */
    public static StrokeSensoryResult analyzeSingleStroke(List<StrokePoint> rawPoints, double canvasWidth, int strokeIndex) {
        if (rawPoints == null || rawPoints.size() < 3) {
            return new StrokeSensoryResult(0.5, 0.5, 0.0, "BALANCED", "");
        }

        // Chuẩn hóa tỷ lệ kích thước canvas (chuẩn cơ sở 300px)
        double scale = 300.0 / Math.max(100.0, canvasWidth);

        // Đảm bảo timestamp hợp lệ (nếu client không gửi timestamp thì giả lập khoảng 16ms/point)
        List<StrokePoint> points = normalizeTimestamps(rawPoints);

        double totalDist = 0.0;
        long totalTime = Math.max(1, points.get(points.size() - 1).t() - points.get(0).t());

        int jitterCount = 0;
        int validAngleSegments = 0;

        // Phân đoạn 15% cuối nét để kiểm tra Tome / Harai
        int lastSegmentIndex = Math.max(1, (int) (points.size() * 0.85));
        double endDist = 0.0;
        long endTime = Math.max(1, points.get(points.size() - 1).t() - points.get(lastSegmentIndex).t());

        for (int i = 1; i < points.size(); i++) {
            StrokePoint p1 = points.get(i - 1);
            StrokePoint p2 = points.get(i);

            double d = Math.hypot((p2.x() - p1.x()) * scale, (p2.y() - p1.y()) * scale);
            totalDist += d;

            if (i >= lastSegmentIndex) {
                endDist += d;
            }

            // Đo độ run nét: bỏ qua các đoạn dịch chuyển < 4.0px để lọc vi chấn bàn tay/chuột
            if (i < points.size() - 1) {
                StrokePoint p3 = points.get(i + 1);
                double v1x = p2.x() - p1.x(), v1y = p2.y() - p1.y();
                double v2x = p3.x() - p2.x(), v2y = p3.y() - p2.y();
                double l1 = Math.hypot(v1x, v1y);
                double l2 = Math.hypot(v2x, v2y);

                if (l1 >= 4.0 && l2 >= 4.0) {
                    double cosVal = (v1x * v2x + v1y * v2y) / (l1 * l2);
                    cosVal = Math.max(-1.0, Math.min(1.0, cosVal));
                    double angleDeg = Math.toDegrees(Math.acos(cosVal));

                    // Góc bẻ hướng đột ngột > 28 độ trong đoạn ngắn chứng tỏ nét bị rung
                    if (angleDeg > 28.0) {
                        jitterCount++;
                    }
                    validAngleSegments++;
                }
            }
        }

        double avgV = totalDist / totalTime;
        double endV = endDist / endTime;
        double jitterRate = validAngleSegments > 0 ? (double) jitterCount / validAngleSegments : 0.0;

        String dynamics = "BALANCED";
        String note = "";

        if (endV < avgV * 0.38 && endTime >= 60) {
            dynamics = "TOME";
            note = String.format("Nét %d: Kỹ thuật dừng bút (tome) chuẩn mực, lực đọng vững vàng.", strokeIndex);
        } else if (endV > avgV * 1.55) {
            dynamics = "HARAI";
            note = String.format("Nét %d: Vuốt bút (harai/hane) thanh thoát, phóng bút dứt khoát.", strokeIndex);
        } else if (jitterRate > 0.25) {
            note = String.format("Nét %d: Bị rung tay hoặc kéo nét chần chừ, nét thiếu liền mạch.", strokeIndex);
        }

        return new StrokeSensoryResult(avgV, endV, jitterRate, dynamics, note);
    }

    private static List<StrokePoint> normalizeTimestamps(List<StrokePoint> raw) {
        if (raw == null || raw.isEmpty()) return Collections.emptyList();
        boolean hasValidTimes = false;
        long firstT = raw.get(0).t();

        for (int i = 1; i < raw.size(); i++) {
            if (raw.get(i).t() > firstT) {
                hasValidTimes = true;
                break;
            }
        }

        if (hasValidTimes) {
            return raw;
        }

        // Nếu client không gửi timestamp hợp lý, giả lập 16.6ms / frame (tương đương 60 FPS)
        List<StrokePoint> generated = new ArrayList<>(raw.size());
        long simulatedTime = 0;
        for (int i = 0; i < raw.size(); i++) {
            StrokePoint p = raw.get(i);
            generated.add(new StrokePoint(p.x(), p.y(), simulatedTime));
            simulatedTime += 16;
        }
        return generated;
    }
}
