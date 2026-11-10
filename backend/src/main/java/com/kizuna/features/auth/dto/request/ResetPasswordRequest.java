package com.kizuna.features.auth.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO xác minh mật khẩu tạm được cấp qua email và thiết lập mật khẩu mới.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResetPasswordRequest {

    @NotBlank(message = "Thiếu thông tin email tài khoản.")
    @Email(message = "Địa chỉ email không đúng định dạng.")
    private String email;

    @NotBlank(message = "Vui lòng nhập mật khẩu được cấp từ email.")
    private String temporaryPassword;

    @NotBlank(message = "Vui lòng nhập mật khẩu mới.")
    @Size(min = 6, message = "Mật khẩu mới phải có ít nhất 6 ký tự.")
    private String newPassword;

    @NotBlank(message = "Vui lòng nhập lại để xác minh mật khẩu mới.")
    private String confirmPassword;
}
