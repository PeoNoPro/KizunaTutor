package com.kizuna.service.kanji;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kizuna.dto.kanji.ScoreResult;
import com.kizuna.dto.kanji.ScoreResult.*;
import com.kizuna.dto.kanji.ScoreSubmission;
import com.kizuna.entity.KanjiDictionary;
import com.kizuna.repository.KanjiDictionaryRepository;
import com.kizuna.service.ai.AiStrokeAnalysisService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Hệ thống Đánh Giá Luyện Viết Chữ Hán Dựa Trên Topo Hình Học Vector (Vector Geometry / Stroke Topology)
 * kết hợp với AI Sensei (Google Gemini 1.5 Flash).
 *
 * 3 Trụ Cột Đánh Giá Chuẩn Mực:
 * 1. Tính Toàn Vẹn Số Nét & Thứ Tự Bút Thuận (25%): Số nét chuẩn + Trình tự trên-dưới, trái-phải.
 * 2. Cấu Trúc Khung Chữ & Cân Đối (35%): Trọng tâm ô Mễ + Tỷ lệ khung chữ vuông vắn chuẩn hóa.
 * 3. Hướng Đi & Độ Chuẩn Nét Vector (40%): Cosine Similarity đánh giá góc phương vị, điểm khởi bút, độ vươn nét.
 */
@Service
@Slf4j
public class GradingService {

    private final KanjiDictionaryRepository kanjiRepository;
    private final AiStrokeAnalysisService aiStrokeAnalysisService;
    private final ObjectMapper objectMapper;

    public GradingService(
            KanjiDictionaryRepository kanjiRepository,
            AiStrokeAnalysisService aiStrokeAnalysisService,
            ObjectMapper objectMapper) {
        this.kanjiRepository = kanjiRepository;
        this.aiStrokeAnalysisService = aiStrokeAnalysisService;
        this.objectMapper = objectMapper;
    }

    public ScoreResult evaluate(ScoreSubmission submission) {
        KanjiDictionary kanji = null;
        if (submission.getKanji() != null && !submission.getKanji().isBlank()) {
            kanji = kanjiRepository.findByKanjiCharacter(submission.getKanji().trim()).orElse(null);
        }

        int standardStrokes = (kanji != null && kanji.getStrokeCount() != null && kanji.getStrokeCount() > 0)
                ? kanji.getStrokeCount().intValue() : 5;
        int userStrokes = Math.max(1, submission.getUserStrokes());

        double canvasW = (submission.getCanvasWidth() != null && submission.getCanvasWidth() > 0)
                ? submission.getCanvasWidth() : 218.0;
        double canvasH = (submission.getCanvasHeight() != null && submission.getCanvasHeight() > 0)
                ? submission.getCanvasHeight() : 218.0;

        // 1. Phân tích vector nét vẽ từ Canvas của người học
        // 1. Phân tích vector nét vẽ từ Canvas của người học (kèm mốc thời gian t)
        List<List<StrokeSensoryAnalyzer.StrokePoint>> strokesWithTime = parseStrokePointsWithTime(submission.getStrokeCoordinatesJson());
        List<List<Point2D>> rawUserStrokes = strokesWithTime.stream()
                .map(stroke -> stroke.stream().map(p -> new Point2D(p.x(), p.y())).toList())
                .toList();
        if (rawUserStrokes.isEmpty()) {
            rawUserStrokes = parseStrokeCoordinates(submission.getStrokeCoordinatesJson());
        }

        // 2. Trích xuất vector nét chuẩn tiêu chuẩn từ KanjiVG SVG
        List<StandardStroke> stdStrokes = (kanji != null && kanji.getSvgData() != null)
                ? parseStandardSvgStrokes(kanji.getSvgData()) : Collections.emptyList();

        List<ScoreDeduction> deductions = new ArrayList<>();
        List<ActionableTip> actionableTips = new ArrayList<>();
        List<VisualHighlightZone> highlightZones = new ArrayList<>();

        // =========================================================================
        // TIÊU CHÍ 1: TÍNH TOÀN VẸN SỐ NÉT & THỨ TỰ BÚT THUẬN (TỐI ĐA 25 ĐIỂM)
        // =========================================================================
        int strokeIntegrityScore = 0;
        int strokeCountScore = 0;
        int strokeOrderScore = 0;

        int diff = Math.abs(userStrokes - standardStrokes);

        // a) Đúng số lượng nét (Tối đa 15 điểm)
        if (diff == 0) {
            strokeCountScore = 15;
        } else if (diff == 1) {
            strokeCountScore = 8;
            int penalty = -7;
            if (userStrokes > standardStrokes) {
                deductions.add(new ScoreDeduction(
                        "Số nét", penalty, "Thừa 1 nét bút",
                        "Bạn viết " + userStrokes + " nét thay vì " + standardStrokes + " nét chuẩn. Các nét gập/móc (như 𠃍, 亅) nên viết liền tay một hơi.",
                        "medium", null
                ));
                actionableTips.add(new ActionableTip(
                        "Số lượng nét",
                        "Nét gập/móc cần viết liền mạch một hơi, tránh nhấc bút chia làm 2 nét riêng biệt.",
                        "✏️"
                ));
            } else {
                deductions.add(new ScoreDeduction(
                        "Số nét", penalty, "Thiếu 1 nét bút",
                        "Bạn viết " + userStrokes + " nét (chuẩn " + standardStrokes + " nét). Hãy chú ý không bỏ sót nét chấm (丶) hoặc gộp nét.",
                        "medium", null
                ));
                actionableTips.add(new ActionableTip(
                        "Số lượng nét",
                        "Kiểm tra lại thứ tự các nét trên hình mẫu KanjiVG để không bỏ sót các nét nhỏ.",
                        "✏️"
                ));
            }
        } else if (diff == 2) {
            strokeCountScore = 4;
            deductions.add(new ScoreDeduction(
                    "Số nét", -11, "Chênh lệch 2 nét",
                    "Số nét viết (" + userStrokes + ") lệch 2 nét so với quy chuẩn (" + standardStrokes + " nét).",
                    "high", null
            ));
            actionableTips.add(new ActionableTip(
                    "Số lượng nét",
                    "Quan sát kỹ thứ tự nét mẫu (KanjiVG) trước khi viết để đếm đúng số nét.",
                    "✏️"
            ));
        } else {
            strokeCountScore = 0;
            deductions.add(new ScoreDeduction(
                    "Số nét", -15, "Sai lệch số nét nghiêm trọng",
                    "Số nét viết thực tế (" + userStrokes + ") khác biệt quá lớn so với tiêu chuẩn (" + standardStrokes + " nét).",
                    "high", null
            ));
        }

        // b) Đúng thứ tự bút thuận (Tối đa 10 điểm)
        int orderInversions = detectOrderInversions(rawUserStrokes, canvasH, canvasW);
        if (orderInversions == 0) {
            strokeOrderScore = 10;
        } else if (orderInversions == 1) {
            strokeOrderScore = 6;
            deductions.add(new ScoreDeduction(
                    "Thứ tự nét", -4, "Chưa đúng thứ tự hạ bút",
                    "Có nét được viết nhảy cóc hoặc viết phần dưới trước khi hoàn tất phần trên.",
                    "medium", null
            ));
            actionableTips.add(new ActionableTip(
                    "Thứ tự nét",
                    "Tuân thủ quy tắc bút thuận cốt lõi: 'Từ trên xuống dưới, từ trái qua phải'.",
                    "📐"
            ));
        } else {
            strokeOrderScore = 2;
            deductions.add(new ScoreDeduction(
                    "Thứ tự nét", -8, "Đảo lộn quy tắc bút thuận",
                    "Các nét bị hạ bút ngẫu nhiên không theo trình tự chuẩn mực của thư pháp chữ Hán.",
                    "high", null
            ));
            actionableTips.add(new ActionableTip(
                    "Thứ tự nét",
                    "Hãy nhìn số thứ tự nét 1, 2, 3... trên chữ mẫu tiêu chuẩn bên cạnh để tập lại.",
                    "📐"
            ));
        }

        strokeIntegrityScore = Math.min(25, strokeCountScore + strokeOrderScore);
        int orderAccuracy = (int) Math.round((strokeIntegrityScore / 25.0) * 100);
        String strokeStatus = strokeIntegrityScore >= 22 ? "Rất tốt" : (strokeIntegrityScore >= 16 ? "Đạt chuẩn" : "Cần sửa");
        String strokeColor = strokeIntegrityScore >= 22 ? "bar-green" : (strokeIntegrityScore >= 16 ? "bar-blue" : "bar-orange");

        ScoreCriterion strokeCriterion = new ScoreCriterion(
                "stroke_order", "Đúng Số Nét & Trình Tự",
                strokeIntegrityScore, 25,
                orderAccuracy, strokeStatus, strokeColor
        );

        // =========================================================================
        // TIÊU CHÍ 2: CẤU TRÚC KHUNG CHỮ & CÂN ĐỐI (TỐI ĐA 35 ĐIỂM)
        // =========================================================================
        int balanceScore = 0;
        int centeringScore = 0;
        int proportionScore = 0;

        BoundingBox bbox = calculateBoundingBox(rawUserStrokes, canvasW, canvasH);

        // a) Trọng tâm & Vị trí ô Mễ (Tối đa 15 điểm) - Dung sai rộng 20%
        double gridCenterX = canvasW / 2.0;
        double gridCenterY = canvasH / 2.0;
        double dx = (bbox.centerX - gridCenterX) / canvasW;
        double dy = (bbox.centerY - gridCenterY) / canvasH;
        double centerDistance = Math.sqrt(dx * dx + dy * dy);

        if (centerDistance <= 0.08) {
            centeringScore = 15;
        } else if (centerDistance <= 0.15) {
            centeringScore = 11;
            deductions.add(new ScoreDeduction(
                    "Trọng tâm & Vị trí", -4, "Chữ lệch tâm nhẹ",
                    "Toàn bộ khối chữ hơi nghiêng lệch so với giao điểm tâm ô Mễ (米字格).",
                    "low", null
            ));
        } else {
            centeringScore = 6;
            String direction = "";
            if (dy < -0.06) direction += "góc trên ";
            else if (dy > 0.06) direction += "góc dưới ";
            if (dx < -0.06) direction += "bên trái";
            else if (dx > 0.06) direction += "bên phải";
            if (direction.isBlank()) direction = "một phía ngoài";

            deductions.add(new ScoreDeduction(
                    "Trọng tâm & Vị trí", -9, "Lệch trọng tâm ô chữ",
                    "Toàn bộ khối chữ bị viết lệch hẳn về " + direction.trim() + " của ô chữ. Hãy căn chỉnh trọng tâm vào điểm giao chữ thập chính giữa.",
                    "high", null
            ));
            actionableTips.add(new ActionableTip(
                    "Trọng tâm ô Mễ",
                    "Căn chữ nằm cân giữa điểm giao nhau của 4 đường lưới ô Mễ, tránh viết dồn về một phía.",
                    "🎯"
            ));

            highlightZones.add(new VisualHighlightZone(
                    bbox.minX, bbox.minY, bbox.width, bbox.height,
                    "off_center", "Lệch trọng tâm (" + direction.trim() + ")"
            ));
        }

        // b) Tỷ lệ khung chữ & Các bộ thủ (Tối đa 20 điểm) - Chuẩn hóa khung
        double aspectRatio = bbox.width / Math.max(1.0, bbox.height);
        boolean hasLeftRightImbalance = checkLeftRightImbalance(rawUserStrokes, bbox);

        if (hasLeftRightImbalance) {
            proportionScore = 12;
            deductions.add(new ScoreDeduction(
                    "Tỷ lệ bộ thủ", -8, "Lệch tỷ lệ các bộ",
                    "Nửa bên trái viết quá to lấn sang nửa bên phải, làm mất đi tỷ lệ cân đối hài hòa giữa các bộ thủ.",
                    "high", null
            ));
            actionableTips.add(new ActionableTip(
                    "Tỷ lệ bộ thủ",
                    "Bộ bên trái thường đóng vai trò trợ lực hoặc biểu nghĩa, hãy thu gọn bề ngang khoảng 15% để bộ bên phải đủ không gian vươn nét.",
                    "📐"
            ));
        } else if (aspectRatio > 1.40) {
            proportionScore = 13;
            deductions.add(new ScoreDeduction(
                    "Tỷ lệ bộ thủ", -7, "Khung chữ bị bè ngang",
                    "Chiều ngang quá rộng so với chiều dọc (tỷ lệ " + String.format("%.2f", aspectRatio) + "), các nét ngang bị kéo quá dài làm chữ bị bẹp.",
                    "medium", null
            ));
            actionableTips.add(new ActionableTip(
                    "Khung chữ",
                    "Hãy thu hẹp độ vươn của các nét ngang khoảng 15% để đạt tỷ lệ khung chữ vuông vắn 1:1 chuẩn mực.",
                    "📐"
            ));
        } else if (aspectRatio < 0.68) {
            proportionScore = 13;
            deductions.add(new ScoreDeduction(
                    "Tỷ lệ bộ thủ", -7, "Khung chữ quá hẹp",
                    "Chiều ngang chữ quá hẹp so với chiều dọc, các nét bị ép sát vào nhau.",
                    "medium", null
            ));
            actionableTips.add(new ActionableTip(
                    "Khung chữ",
                    "Hãy mở rộng độ vươn của các nét ngang và nét phẩy/mác sang hai bên để chữ thông thoáng hơn.",
                    "📐"
            ));
        } else {
            proportionScore = 20;
        }

        balanceScore = Math.min(35, centeringScore + proportionScore);
        int proportionAccuracy = (int) Math.round((balanceScore / 35.0) * 100);
        String balanceStatus = balanceScore >= 30 ? "Rất tốt" : (balanceScore >= 22 ? "Đạt chuẩn" : "Cần sửa");
        String balanceColor = balanceScore >= 30 ? "bar-green" : (balanceScore >= 22 ? "bar-blue" : "bar-orange");

        ScoreCriterion balanceCriterion = new ScoreCriterion(
                "proportion_balance", "Cân Đối & Trọng Tâm",
                balanceScore, 35,
                proportionAccuracy, balanceStatus, balanceColor
        );

        // =========================================================================
        // TIÊU CHÍ 3: HƯỚNG ĐI & ĐỘ CHUẨN NÉT VECTOR (TỐI ĐA 40 ĐIỂM)
        // 100% THUẬT TOÁN VECTOR & COSINE SIMILARITY
        // =========================================================================
        VectorTopologyResult vectorResult = evaluateVectorTopology(
                rawUserStrokes, stdStrokes, bbox, canvasW, canvasH,
                deductions, actionableTips, highlightZones
        );

        int trajectoryScore = vectorResult.score;
        int directionAccuracy = vectorResult.directionAccuracy;
        int startEndAccuracy = vectorResult.startEndAccuracy;

        String trajStatus = trajectoryScore >= 34 ? "Rất tốt" : (trajectoryScore >= 26 ? "Đạt chuẩn" : "Cần sửa");
        String trajColor = trajectoryScore >= 34 ? "bar-green" : (trajectoryScore >= 26 ? "bar-blue" : "bar-orange");

        ScoreCriterion trajectoryCriterion = new ScoreCriterion(
                "trajectory_shape", "Hướng Đi & Độ Chuẩn Nét",
                trajectoryScore, 40,
                (int) Math.round((trajectoryScore / 40.0) * 100),
                trajStatus, trajColor
        );

        // =========================================================================
        // TỔNG KẾT ĐIỂM (100 ĐIỂM) & XẾP HẠNG
        // =========================================================================
        int totalScore = strokeIntegrityScore + balanceScore + trajectoryScore;
        totalScore = Math.max(0, Math.min(100, totalScore));

        String grade;
        String gradeBadgeClass;
        if (totalScore >= 90) {
            grade = "Xuất sắc - Chuẩn mực thư pháp";
            gradeBadgeClass = "badge-mastery";
        } else if (totalScore >= 75) {
            grade = "Giỏi - Nét đẹp & Đúng hướng";
            gradeBadgeClass = "badge-proficient";
        } else if (totalScore >= 60) {
            grade = "Khá - Cần điều chỉnh thêm";
            gradeBadgeClass = "badge-adequate";
        } else {
            grade = "Cần luyện tập thêm";
            gradeBadgeClass = "badge-needs-work";
        }

        String overallFeedback;
        if (totalScore >= 90) {
            overallFeedback = "🎉 Xuất sắc! Nét viết thanh thoát, hướng bút chuẩn xác và các bộ thủ cân đối hài hòa trong ô Mễ.";
        } else if (totalScore >= 75) {
            overallFeedback = "🌿 Giỏi! Bạn đã nắm vững hướng đi của nét và số lượng nét. Hãy chú ý thêm các gợi ý chi tiết bên dưới để nét chữ đẹp hơn.";
        } else if (totalScore >= 60) {
            overallFeedback = "⚖️ Khá! Chữ viết đã đúng dáng nhưng còn một số điểm trừ về hướng bút hoặc độ cân đối. Hãy đọc kỹ các mục bên dưới để sửa.";
        } else {
            overallFeedback = "✍️ Bạn cần luyện tập thêm. Hãy chú ý kéo đúng chiều của từng nét theo hướng mũi tên chỉ dẫn.";
        }

        if (actionableTips.isEmpty()) {
            actionableTips.add(new ActionableTip(
                    "Thư pháp chuẩn",
                    "Hãy duy trì tốc độ lia bút đều tay, nhấn nhẹ đầu nét và nhấc dứt khoát ở đuôi nét.",
                    "⚡"
            ));
        }

        ScoreResult result = new ScoreResult();
        result.setKanji(kanji != null ? kanji.getKanjiCharacter() : submission.getKanji());
        result.setUserStrokes(userStrokes);
        result.setStandardStrokes(standardStrokes);
        result.setStrokeDifference(diff);
        result.setStrokeScorePercent((int) Math.round((strokeIntegrityScore / 25.0) * 100));
        result.setShapeScorePercent(directionAccuracy);
        result.setTotalScorePercent(totalScore);
        result.setFeedback(overallFeedback);

        result.setGrade(grade);
        result.setGradeBadgeClass(gradeBadgeClass);
        result.setStrokeCriterion(strokeCriterion);
        result.setBalanceCriterion(balanceCriterion);
        result.setTrajectoryCriterion(trajectoryCriterion);

        result.setDirectionAccuracy(directionAccuracy);
        result.setStartEndAccuracy(startEndAccuracy);
        result.setOrderAccuracy(orderAccuracy);
        result.setProportionAccuracy(proportionAccuracy);

        result.setDeductions(deductions);
        result.setActionableTips(actionableTips);
        result.setVisualHighlightZones(highlightZones);

        // Phân tích cảm quan động lực học (Kinematics & Sensory) từ chuỗi tọa độ thời gian (x, y, t)
        StrokeSensoryAnalyzer.CharacterSensoryReport sensoryReport =
                StrokeSensoryAnalyzer.analyzeCharacter(strokesWithTime, canvasW);
        result.setSensoryAssessment(sensoryReport.overallAssessment());
        result.setAverageVelocity(sensoryReport.overallAvgVelocity());
        result.setJitterRate(sensoryReport.overallJitterRate());
        result.setStabilityScore(sensoryReport.stabilityScore());

        if (sensoryReport.overallJitterRate() > 0.22) {
            actionableTips.add(new ActionableTip(
                    "Độ vững tay",
                    "Thả lỏng cổ tay và lia nét liền mạch, tránh ngập ngừng dừng giữa chừng làm nét bị run.",
                    "⚡"
            ));
        } else if (sensoryReport.overallAvgVelocity() > 1.35) {
            actionableTips.add(new ActionableTip(
                    "Tốc độ nét",
                    "Hãm bớt tốc độ kéo bút để nét có độ đầm, tránh lướt quá nhanh làm nét bị mỏng lực.",
                    "🎯"
            ));
        }

        // =========================================================================
        // BƯỚC 4: KẾT NỐI AI SENSEI (GOOGLE GEMINI 1.5 FLASH / OPENAI)
        // =========================================================================
        try {
            String aiFeedback = aiStrokeAnalysisService.generateAiSenseiFeedback(
                    kanji,
                    result,
                    submission.getApiKey(),
                    submission.getProvider(),
                    null // Chế độ Text-Only siêu tiết kiệm token (không gửi ảnh Canvas Base64)
            );
            result.setAiFeedback(aiFeedback);
            result.setAiProvider(submission.getProvider() != null ? submission.getProvider().toUpperCase() : "GEMINI");
        } catch (Exception ex) {
            log.warn("Không thể sinh nhận xét từ AI Sensei: {}", ex.getMessage());
            result.setAiFeedback("Hệ thống đã hoàn tất chấm điểm nét vẽ Topo Vector.");
            result.setAiProvider("VECTOR_INTERNAL");
        }

        return result;
    }

    private VectorTopologyResult evaluateVectorTopology(
            List<List<Point2D>> userStrokes,
            List<StandardStroke> stdStrokes,
            BoundingBox bbox,
            double canvasW, double canvasH,
            List<ScoreDeduction> deductions,
            List<ActionableTip> actionableTips,
            List<VisualHighlightZone> highlightZones) {

        if (userStrokes == null || userStrokes.isEmpty()) {
            return new VectorTopologyResult(20, 50, 50);
        }

        int score = 40;
        double totalCosSim = 0;
        double totalStartDist = 0;
        int evaluatedStrokes = 0;

        for (int i = 0; i < userStrokes.size(); i++) {
            List<Point2D> rawStroke = userStrokes.get(i);
            if (rawStroke.size() < 2) continue;

            List<Point2D> resampled = resampleStroke(rawStroke, 16);
            Point2D uStart = resampled.get(0);
            Point2D uEnd = resampled.get(resampled.size() - 1);

            double uDx = uEnd.x - uStart.x;
            double uDy = uEnd.y - uStart.y;
            double uLen = Math.sqrt(uDx * uDx + uDy * uDy);

            if (uLen < 6.0) continue;

            double uDirX = uDx / uLen;
            double uDirY = uDy / uLen;

            StandardStroke std = (i < stdStrokes.size()) ? stdStrokes.get(i) : null;

            double stdDirX;
            double stdDirY;
            double stdLenNorm = 0.5;

            if (std != null && std.length > 2.0) {
                stdDirX = std.dx / std.length;
                stdDirY = std.dy / std.length;
                stdLenNorm = std.length / 109.0;
            } else {
                if (Math.abs(uDx) >= Math.abs(uDy)) {
                    stdDirX = 1.0; stdDirY = 0.0;
                } else {
                    stdDirX = 0.0; stdDirY = 1.0;
                }
            }

            double cosSim = (uDirX * stdDirX) + (uDirY * stdDirY);
            totalCosSim += Math.max(-1.0, Math.min(1.0, cosSim));
            evaluatedStrokes++;

            // a) Ngược hướng nét (cosSim < -0.35)
            if (cosSim < -0.35) {
                score -= 8;
                String desc;
                if (stdDirY > 0.4) {
                    desc = "Nét số " + (i + 1) + " phải kéo từ trên xuống dưới, bạn đang kéo ngược từ dưới lên.";
                } else if (stdDirX > 0.4) {
                    desc = "Nét số " + (i + 1) + " phải kéo từ trái sang phải, bạn đang kéo ngược từ phải sang trái.";
                } else {
                    desc = "Nét số " + (i + 1) + " bị kéo ngược hướng so với quy chuẩn bút thuận.";
                }

                deductions.add(new ScoreDeduction(
                        "Hướng nét", -8, "Ngược hướng nét (Nét " + (i + 1) + ")",
                        desc, "high", i + 1
                ));
                actionableTips.add(new ActionableTip(
                        "Nét số " + (i + 1),
                        desc + " Hãy chú ý hướng xuất phát của ngòi bút.",
                        "✏️"
                ));

                highlightZones.add(new VisualHighlightZone(
                        Math.min(uStart.x, uEnd.x), Math.min(uStart.y, uEnd.y),
                        Math.max(24, Math.abs(uDx)), Math.max(24, Math.abs(uDy)),
                        "reverse_direction", "Nét " + (i + 1) + " ngược hướng"
                ));
            }
            // b) Lệch góc nghiêng (-0.35 <= cosSim < 0.65)
            else if (cosSim < 0.65) {
                score -= 5;
                String desc;
                if (std != null && std.isSlantedLeft()) {
                    desc = "Nét phẩy số " + (i + 1) + " hơi thẳng, hãy vuốt chéo nhẹ sang trái theo chiều nét.";
                } else if (std != null && std.isSlantedRight()) {
                    desc = "Nét mác số " + (i + 1) + " hơi dốc, hãy vươn chéo sang phải theo chiều nét mác.";
                } else if (std != null && std.isHorizontal()) {
                    desc = "Nét ngang số " + (i + 1) + " bị nghiêng dốc, hãy giữ cổ tay thẳng và kéo nằm ngang hơn.";
                } else {
                    desc = "Nét số " + (i + 1) + " bị lệch góc nghiêng so với nét chuẩn.";
                }

                deductions.add(new ScoreDeduction(
                        "Hướng nét", -5, "Lệch góc nét (Nét " + (i + 1) + ")",
                        desc, "medium", i + 1
                ));
                actionableTips.add(new ActionableTip(
                        "Nét số " + (i + 1),
                        desc, "✏️"
                ));
            }

            // c) Điểm khởi bút
            if (std != null) {
                double uNormSx = (uStart.x - bbox.minX) / bbox.width;
                double uNormSy = (uStart.y - bbox.minY) / bbox.height;
                double stdNormSx = std.startX / 109.0;
                double stdNormSy = std.startY / 109.0;

                double startDist = Math.sqrt(Math.pow(uNormSx - stdNormSx, 2) + Math.pow(uNormSy - stdNormSy, 2));
                totalStartDist += startDist;

                if (startDist > 0.32) {
                    score -= 4;
                    deductions.add(new ScoreDeduction(
                            "Hướng nét", -4, "Vị trí khởi bút lệch vùng (Nét " + (i + 1) + ")",
                            "Nét số " + (i + 1) + " bắt đầu bị lệch khỏi khu vực tiêu chuẩn của chữ.",
                            "medium", i + 1
                    ));
                }

                double uLenNorm = uLen / Math.max(bbox.width, bbox.height);
                if (stdLenNorm > 0.42 && uLenNorm < stdLenNorm * 0.48) {
                    score -= 4;
                    deductions.add(new ScoreDeduction(
                            "Độ vươn nét", -4, "Nét viết hơi ngắn (Nét " + (i + 1) + ")",
                            "Nét số " + (i + 1) + " hơi ngắn so với tổng thể chữ, hãy kéo dài sang phải một chút để cân bằng.",
                            "medium", i + 1
                    ));
                }
            }
        }

        score = Math.max(14, Math.min(40, score));

        double avgCos = evaluatedStrokes > 0 ? (totalCosSim / evaluatedStrokes) : 0.85;
        int dirAcc = (int) Math.round(Math.max(0.0, Math.min(1.0, (avgCos + 1.0) / 2.0)) * 100);

        double avgDist = evaluatedStrokes > 0 ? (totalStartDist / evaluatedStrokes) : 0.15;
        int startAcc = (int) Math.round(Math.max(0.0, Math.min(1.0, 1.0 - (avgDist / 0.5))) * 100);

        return new VectorTopologyResult(score, dirAcc, startAcc);
    }

    private List<Point2D> resampleStroke(List<Point2D> points, int targetCount) {
        if (points == null || points.size() < 2) return points;
        if (targetCount < 2) targetCount = 2;

        double totalDist = 0;
        double[] dists = new double[points.size()];
        dists[0] = 0;
        for (int i = 1; i < points.size(); i++) {
            double dx = points.get(i).x - points.get(i - 1).x;
            double dy = points.get(i).y - points.get(i - 1).y;
            totalDist += Math.sqrt(dx * dx + dy * dy);
            dists[i] = totalDist;
        }

        if (totalDist < 0.001) {
            List<Point2D> dup = new ArrayList<>();
            for (int k = 0; k < targetCount; k++) dup.add(points.get(0));
            return dup;
        }

        List<Point2D> result = new ArrayList<>();
        result.add(points.get(0));

        double step = totalDist / (targetCount - 1);
        int currentSegment = 0;

        for (int i = 1; i < targetCount - 1; i++) {
            double targetD = i * step;
            while (currentSegment < points.size() - 2 && dists[currentSegment + 1] < targetD) {
                currentSegment++;
            }
            double segStartD = dists[currentSegment];
            double segEndD = dists[currentSegment + 1];
            double segLen = segEndD - segStartD;
            double t = (segLen > 0.0001) ? (targetD - segStartD) / segLen : 0;

            Point2D p1 = points.get(currentSegment);
            Point2D p2 = points.get(currentSegment + 1);
            result.add(new Point2D(p1.x + t * (p2.x - p1.x), p1.y + t * (p2.y - p1.y)));
        }

        result.add(points.get(points.size() - 1));
        return result;
    }

    private List<StandardStroke> parseStandardSvgStrokes(String svgXml) {
        List<StandardStroke> list = new ArrayList<>();
        if (svgXml == null || svgXml.isBlank()) return list;

        try {
            Matcher pathMatcher = Pattern.compile("<path[^>]*id=[\"']([^\"']*-s([0-9]+))[\"'][^>]*d=[\"']([^\"']+)[\"'][^>]*>").matcher(svgXml);
            while (pathMatcher.find()) {
                int strokeIdx = Integer.parseInt(pathMatcher.group(2));
                String d = pathMatcher.group(3);

                StandardStroke stroke = parseSinglePath(strokeIdx, d);
                if (stroke != null) {
                    list.add(stroke);
                }
            }
        } catch (Exception e) {
            log.warn("Không thể bóc tách vector SVG chuẩn: {}", e.getMessage());
        }
        return list;
    }

    private StandardStroke parseSinglePath(int index, String d) {
        if (d == null || d.isBlank()) return null;
        try {
            Matcher mStart = Pattern.compile("(?i)M\\s*([0-9.-]+)[,\\s]+([0-9.-]+)").matcher(d);
            if (!mStart.find()) return null;
            double startX = Double.parseDouble(mStart.group(1));
            double startY = Double.parseDouble(mStart.group(2));

            double curX = startX;
            double curY = startY;

            Matcher cmdMatcher = Pattern.compile("([a-zA-Z])([^a-zA-Z]*)").matcher(d);
            while (cmdMatcher.find()) {
                char cmd = cmdMatcher.group(1).charAt(0);
                String argsStr = cmdMatcher.group(2).trim();
                if (argsStr.isEmpty()) continue;

                Matcher numMatcher = Pattern.compile("[-+]?[0-9]*\\.?[0-9]+(?:[eE][-+]?[0-9]+)?").matcher(argsStr);
                List<Double> nums = new ArrayList<>();
                while (numMatcher.find()) {
                    nums.add(Double.parseDouble(numMatcher.group()));
                }

                if (cmd == 'c') {
                    for (int i = 0; i + 5 < nums.size(); i += 6) {
                        curX += nums.get(i + 4);
                        curY += nums.get(i + 5);
                    }
                } else if (cmd == 'C') {
                    for (int i = 0; i + 5 < nums.size(); i += 6) {
                        curX = nums.get(i + 4);
                        curY = nums.get(i + 5);
                    }
                } else if (cmd == 's' || cmd == 'S') {
                    for (int i = 0; i + 3 < nums.size(); i += 4) {
                        curX = (cmd == 's') ? curX + nums.get(i + 2) : nums.get(i + 2);
                        curY = (cmd == 's') ? curY + nums.get(i + 3) : nums.get(i + 3);
                    }
                } else if (cmd == 'l' || cmd == 'L') {
                    for (int i = 0; i + 1 < nums.size(); i += 2) {
                        curX = (cmd == 'l') ? curX + nums.get(i) : nums.get(i);
                        curY = (cmd == 'l') ? curY + nums.get(i + 1) : nums.get(i + 1);
                    }
                }
            }

            double dx = curX - startX;
            double dy = curY - startY;
            double length = Math.sqrt(dx * dx + dy * dy);
            double angle = Math.toDegrees(Math.atan2(dy, dx));

            return new StandardStroke(index, startX, startY, curX, curY, dx, dy, length, angle);
        } catch (Exception e) {
            return null;
        }
    }

    private int detectOrderInversions(List<List<Point2D>> strokes, double canvasH, double canvasW) {
        if (strokes == null || strokes.size() < 2) return 0;
        int inversions = 0;
        for (int i = 0; i < strokes.size() - 1; i++) {
            List<Point2D> s1 = strokes.get(i);
            List<Point2D> s2 = strokes.get(i + 1);
            if (s1.isEmpty() || s2.isEmpty()) continue;

            double cy1 = strokeCenterY(s1);
            double cy2 = strokeCenterY(s2);

            if (cy2 < cy1 - (canvasH * 0.32)) {
                inversions++;
            }
        }
        return inversions;
    }

    private boolean checkLeftRightImbalance(List<List<Point2D>> strokes, BoundingBox bbox) {
        if (strokes == null || strokes.size() < 3) return false;
        double midX = bbox.centerX;
        double leftWeight = 0;
        double rightWeight = 0;

        for (List<Point2D> stroke : strokes) {
            for (Point2D p : stroke) {
                if (p.x < midX) leftWeight++;
                else rightWeight++;
            }
        }

        double total = leftWeight + rightWeight;
        if (total < 30) return false;
        return (leftWeight / total) > 0.74;
    }

    private BoundingBox calculateBoundingBox(List<List<Point2D>> strokes, double defaultW, double defaultH) {
        if (strokes == null || strokes.isEmpty()) {
            return new BoundingBox(defaultW * 0.15, defaultH * 0.15, defaultW * 0.85, defaultH * 0.85);
        }

        double minX = Double.MAX_VALUE;
        double maxX = Double.MIN_VALUE;
        double minY = Double.MAX_VALUE;
        double maxY = Double.MIN_VALUE;
        boolean hasPoints = false;

        for (List<Point2D> stroke : strokes) {
            for (Point2D p : stroke) {
                hasPoints = true;
                if (p.x < minX) minX = p.x;
                if (p.x > maxX) maxX = p.x;
                if (p.y < minY) minY = p.y;
                if (p.y > maxY) maxY = p.y;
            }
        }

        if (!hasPoints) {
            return new BoundingBox(defaultW * 0.15, defaultH * 0.15, defaultW * 0.85, defaultH * 0.85);
        }
        return new BoundingBox(minX, minY, maxX, maxY);
    }

    private double strokeCenterY(List<Point2D> stroke) {
        double sum = 0;
        for (Point2D p : stroke) sum += p.y;
        return sum / stroke.size();
    }

    private List<List<StrokeSensoryAnalyzer.StrokePoint>> parseStrokePointsWithTime(String json) {
        if (json == null || json.isBlank()) return Collections.emptyList();
        try {
            JsonNode root = objectMapper.readTree(json);
            if (!root.isArray()) return Collections.emptyList();

            List<List<StrokeSensoryAnalyzer.StrokePoint>> result = new ArrayList<>();
            for (JsonNode strokeNode : root) {
                JsonNode pointsArray = strokeNode.isArray() ? strokeNode :
                        (strokeNode.has("value") ? strokeNode.get("value") :
                        (strokeNode.has("points") ? strokeNode.get("points") : null));

                if (pointsArray != null && pointsArray.isArray()) {
                    List<StrokeSensoryAnalyzer.StrokePoint> points = new ArrayList<>();
                    for (JsonNode pt : pointsArray) {
                        if (pt.has("x") && pt.has("y")) {
                            double x = pt.get("x").asDouble();
                            double y = pt.get("y").asDouble();
                            long t = pt.has("t") ? pt.get("t").asLong() : 0L;
                            points.add(new StrokeSensoryAnalyzer.StrokePoint(x, y, t));
                        }
                    }
                    if (!points.isEmpty()) {
                        result.add(points);
                    }
                }
            }
            return result;
        } catch (Exception e) {
            log.warn("Không thể parse strokePointsWithTime: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    private List<List<Point2D>> parseStrokeCoordinates(String json) {
        if (json == null || json.isBlank()) return Collections.emptyList();
        try {
            JsonNode root = objectMapper.readTree(json);
            if (!root.isArray()) return Collections.emptyList();

            List<List<Point2D>> result = new ArrayList<>();
            for (JsonNode strokeNode : root) {
                JsonNode pointsArray = strokeNode.isArray() ? strokeNode :
                        (strokeNode.has("value") ? strokeNode.get("value") :
                        (strokeNode.has("points") ? strokeNode.get("points") : null));

                if (pointsArray != null && pointsArray.isArray()) {
                    List<Point2D> points = new ArrayList<>();
                    for (JsonNode pt : pointsArray) {
                        if (pt.has("x") && pt.has("y")) {
                            double x = pt.get("x").asDouble();
                            double y = pt.get("y").asDouble();
                            points.add(new Point2D(x, y));
                        }
                    }
                    if (!points.isEmpty()) {
                        result.add(points);
                    }
                }
            }
            return result;
        } catch (Exception e) {
            log.warn("Không thể parse strokeCoordinatesJson: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    private record Point2D(double x, double y) {}

    private record StandardStroke(
            int strokeIndex,
            double startX, double startY,
            double endX, double endY,
            double dx, double dy,
            double length, double angle
    ) {
        boolean isHorizontal() {
            return Math.abs(dx) > Math.abs(dy) * 1.5;
        }
        boolean isVertical() {
            return Math.abs(dy) > Math.abs(dx) * 1.5;
        }
        boolean isSlantedLeft() {
            return dx < -5 && dy > 5;
        }
        boolean isSlantedRight() {
            return dx > 5 && dy > 5;
        }
    }

    private record VectorTopologyResult(
            int score,
            int directionAccuracy,
            int startEndAccuracy
    ) {}

    private static class BoundingBox {
        final double minX, minY, maxX, maxY;
        final double width, height, centerX, centerY;

        BoundingBox(double minX, double minY, double maxX, double maxY) {
            this.minX = minX;
            this.minY = minY;
            this.maxX = maxX;
            this.maxY = maxY;
            this.width = Math.max(1.0, maxX - minX);
            this.height = Math.max(1.0, maxY - minY);
            this.centerX = (minX + maxX) / 2.0;
            this.centerY = (minY + maxY) / 2.0;
        }
    }
}
