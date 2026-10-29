package com.kizuna.features.auth.dto;


import com.kizuna.core.entity.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO tiếp nhận yêu cầu đổi mật khẩu lần đầu cho nhân sự được Admin cấp tài khoản.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FirstTimePasswordChangeRequest {

    @NotBlank(message = "Vui lòng nhập mật khẩu tạm thời hiện tại")
    private String currentPassword;

    @NotBlank(message = "Vui lòng nhập mật khẩu mới")
    @Size(min = 8, message = "Mật khẩu mới phải có tối thiểu 8 ký tự")
    private String newPassword;

    @NotBlank(message = "Vui lòng xác nhận lại mật khẩu mới")
    private String confirmPassword;
}
