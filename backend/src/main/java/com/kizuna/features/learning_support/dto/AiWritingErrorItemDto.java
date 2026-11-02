package com.kizuna.features.learning_support.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AiWritingErrorItemDto {
    private String errorType;         // Trợ từ, Chia thể, Từ vựng, Sắc thái kính ngữ
    private String originalFragment;  // Đoạn sai
    private String correctedFragment; // Đoạn sửa
    private String explanation;       // Giải thích tiếng Việt
}
