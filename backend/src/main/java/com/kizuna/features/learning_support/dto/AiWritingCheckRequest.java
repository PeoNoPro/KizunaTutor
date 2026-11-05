package com.kizuna.features.learning_support.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AiWritingCheckRequest {

    @NotBlank(message = "Nội dung câu viết không được để trống")
    @Size(max = 2000, message = "Bài viết không được vượt quá 2000 ký tự")
    private String text;

    private String jlptLevel; // N5, N4, N3, N2, N1

    private String formality; // POLITE (Lịch sự), PLAIN (Thể thường), KEIGO (Kính ngữ)

    private String grammarPattern; // Mẫu ngữ pháp mục tiêu (tùy chọn)
}
