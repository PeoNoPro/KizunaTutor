package com.kizuna.dto.kanji;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * DTO dữ liệu nét vẽ học viên gửi lên máy chủ để chấm điểm topo vector và phân tích AI.
 */
@Getter
@Setter
@NoArgsConstructor
public class ScoreSubmission {
    private String kanji;                 // Chữ Hán cần chấm (ví dụ: "愛")
    private int userStrokes;              // Số nét thực tế học viên đã vẽ
    private String strokeCoordinatesJson; // Mảng tọa độ nét vẽ JSON [[{x, y, t}, ...], ...]
    private Integer canvasWidth;          // Chiều rộng logic Canvas (ví dụ: 218 hoặc 480)
    private Integer canvasHeight;         // Chiều cao logic Canvas
    private String apiKey;                // Khóa API (Gemini / OpenAI) do người dùng truyền từ giao diện
    private String provider;              // "GEMINI", "OPENAI", "AUTO"
    private String canvasImageData;       // Chuỗi Base64 Data URL (image/png) để AI phân tích thị giác
}
