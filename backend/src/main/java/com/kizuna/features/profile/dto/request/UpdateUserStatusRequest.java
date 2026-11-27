package com.kizuna.features.profile.dto.request;

import com.kizuna.core.entity.UserAccountStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Yêu cầu thay đổi trạng thái tài khoản người dùng kèm lý do hành chính (contracts/profile-api.md mục 4.3).
 */
public record UpdateUserStatusRequest(
        @NotNull(message = "Vui lòng chọn trạng thái tài khoản mới.")
        UserAccountStatus status,

        @NotBlank(message = "Vui lòng nhập lý do thay đổi trạng thái.")
        @Size(min = 5, max = 500, message = "Lý do thay đổi trạng thái phải từ 5 đến 500 ký tự.")
        String reason) {
}
