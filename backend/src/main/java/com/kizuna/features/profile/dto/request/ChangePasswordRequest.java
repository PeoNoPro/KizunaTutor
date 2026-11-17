package com.kizuna.features.profile.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Học viên hoặc gia sư đổi mật khẩu của chính mình (contracts/profile-api.md mục 2A).
 * Quy tắc mật khẩu mới giống nhân sự nội bộ: 8–64 ký tự, có cả chữ và số.
 */
public record ChangePasswordRequest(
        @NotBlank(message = "Vui lòng nhập mật khẩu hiện tại.")
        String currentPassword,

        @NotBlank(message = "Vui lòng nhập mật khẩu mới.")
        @Size(min = 8, max = 64, message = "Mật khẩu mới dài 8–64 ký tự.")
        @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*[0-9]).+$", message = "Mật khẩu mới cần có cả chữ và số.")
        String newPassword,

        @NotBlank(message = "Vui lòng nhập lại mật khẩu mới.")
        String confirmPassword) {
}
