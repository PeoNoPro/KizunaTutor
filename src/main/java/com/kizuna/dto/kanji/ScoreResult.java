package com.kizuna.dto.kanji;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

/**
 * Kết quả phân tích và chấm điểm nét chữ theo hình học Topo Vector kết hợp AI Sensei (Gemini 1.5 Flash).
 */
@Getter
@Setter
@NoArgsConstructor
public class ScoreResult {

    // === CÁC TRƯỜNG TỔNG QUAN ===
    private String kanji;
    private int userStrokes;
    private int standardStrokes;
    private int strokeDifference;
    private int strokeScorePercent;     // Điểm số nét chuẩn hóa (0-100%)
    private int shapeScorePercent;      // Điểm độ chuẩn nét vector (0-100%)
    private int totalScorePercent;      // Tổng kết điểm chuẩn (0-100 điểm)
    private String feedback;            // Nhận xét kỹ thuật tổng quan
    private String aiFeedback;          // Lời nhận xét thư pháp chuyên sâu từ AI Sensei (Gemini 1.5 Flash)
    private String aiProvider;          // "GEMINI", "OPENAI", "VECTOR_INTERNAL"

    private String grade;               // "Xuất sắc - Chuẩn mực thư pháp", "Giỏi - Nét đẹp & Đúng hướng", ...
    private String gradeBadgeClass;     // "badge-mastery", "badge-proficient", "badge-adequate", "badge-needs-work"

    // 3 Cột điểm thành phần
    private ScoreCriterion strokeCriterion;     // Tiêu chí 1: Số nét & Thứ tự nét (Tối đa 25 điểm)
    private ScoreCriterion balanceCriterion;    // Tiêu chí 2: Cấu trúc khung & Cân đối (Tối đa 35 điểm)
    private ScoreCriterion trajectoryCriterion; // Tiêu chí 3: Hướng đi & Độ chuẩn nét Vector (Tối đa 40 điểm)

    // Các chỉ số độ chuẩn topo vector độc lập
    private int directionAccuracy;      // Độ chuẩn hướng đi nét qua Cosine Similarity (0-100%)
    private int startEndAccuracy;       // Độ chuẩn vị trí điểm bắt đầu/kết thúc (0-100%)
    private int orderAccuracy;          // Độ chuẩn thứ tự bút thuận (0-100%)
    private int proportionAccuracy;     // Độ chuẩn tỷ lệ khung chữ (0-100%)

    // Danh sách các khoản điểm bị trừ minh bạch
    private List<ScoreDeduction> deductions = new ArrayList<>();

    // Danh sách các gợi ý sửa hành động cụ thể cho từng nét/bộ thủ
    private List<ActionableTip> actionableTips = new ArrayList<>();

    // Các vùng tọa độ gợi ý khoanh vùng lỗi trực quan trên ô lưới
    private List<VisualHighlightZone> visualHighlightZones = new ArrayList<>();

    // === DTOs LỒNG CHO TIÊU CHÍ, TRỪ ĐIỂM & GỢI Ý (JAVA RECORDS) ===
    public record ScoreCriterion(
            String key,             // "stroke_order", "proportion_balance", "trajectory_shape"
            String name,            // "Đúng số nét & Trình tự", "Cân đối & Trọng tâm", "Hướng Đi & Độ Chuẩn Nét"
            int score,              // Điểm đạt được
            int maxScore,           // Điểm tối đa (25, 35, 40)
            int percentage,         // Tỷ lệ % (0-100)
            String status,          // "Rất tốt", "Đạt chuẩn", "Cần sửa", "Chưa đạt"
            String colorClass       // "bar-green", "bar-yellow", "bar-blue", "bar-red"
    ) {}

    public record ScoreDeduction(
            String category,        // "Số nét", "Thứ tự nét", "Tỷ lệ bộ thủ", "Trọng tâm & Vị trí", "Hướng nét", "Độ vươn nét"
            int penalty,            // Số điểm bị trừ (ví dụ: -8, -5, -4)
            String title,           // Tiêu đề lỗi
            String description,     // Mô tả chi tiết nguyên nhân mất điểm
            String severity,        // "high" (đỏ), "medium" (cam), "low" (vàng)
            Integer strokeIndex     // Chỉ số nét bị lỗi (1-based), hoặc null nếu lỗi toàn chữ
    ) {}

    public record ActionableTip(
            String target,          // Đối tượng cần sửa (ví dụ: "Nét số 3 (nét ngang)", "Bộ bên phải", "Toàn bộ chữ")
            String tipText,         // Hướng dẫn hành động cụ thể để sửa
            String icon             // Icon minh họa: "✏️", "📐", "🎯", "⚡"
    ) {}

    public record VisualHighlightZone(
            double x, double y, double width, double height,
            String type,            // "skew", "too_short", "reverse_direction", "off_center"
            String label            // Nhãn hiển thị trên canvas
    ) {}
}
