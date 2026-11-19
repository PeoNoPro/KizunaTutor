package com.kizuna.features.booking.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

/** Gia sư đồng ý (ACCEPT) hoặc từ chối (DECLINE) yêu cầu kết nối trực tiếp. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RespondConnectionRequestDTO {

    @NotBlank(message = "Vui lòng chọn đồng ý hoặc từ chối yêu cầu kết nối")
    private String action;

    /** Lời nhắn khi đồng ý, hoặc lý do khi từ chối. */
    @Size(max = 1000, message = "Lời nhắn tối đa 1000 ký tự")
    private String message;
}
