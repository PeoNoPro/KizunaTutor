package com.kizuna.features.auth.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO tiếp nhận yêu cầu Quên mật khẩu và gửi mã OTP.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ForgotPasswordRequest {

    @NotBlank(message = "Vui lòng nhập địa chỉ email.")
    @Email(message = "Địa chỉ email không đúng định dạng.")
    private String email;
}
