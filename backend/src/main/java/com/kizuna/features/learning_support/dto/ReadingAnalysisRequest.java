package com.kizuna.features.learning_support.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReadingAnalysisRequest {

    @NotBlank(message = "Văn bản tiếng Nhật không được để trống")
    @Size(max = 3000, message = "Đoạn văn tối đa 3000 ký tự")
    private String text;

    /** Cấp độ JLPT kỳ vọng (N5, N4, N3, N2, N1 hoặc ALL) */
    @Builder.Default
    private String targetJlpt = "ALL";

    /** Có yêu cầu tạo mã Ruby HTML cho Furigana hay không */
    @Builder.Default
    private Boolean includeFurigana = true;
}
