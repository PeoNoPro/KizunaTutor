package com.kizuna.dto.grammar;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiGenerateSentenceRequest {
    private String pattern;
    private String level;
    private String meaning;
    private String context; // vd: "Quán ăn", "Công sở", "Hỏi đường", hoặc tự nhập
    private String apiKey;  // Tùy chọn nếu client có key riêng
}
