package com.kizuna.features.user_management.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Đổi trạng thái tài khoản (UC-12). expectedStatus: trạng thái lúc Quản lý mở biểu mẫu, để không ghi đè thay đổi của người khác. */
public record UpdateUserStatusRequest(
        @NotBlank(message = "Vui lòng chọn trạng thái mới.")
        String status,

        @NotBlank(message = "Vui lòng tải lại thông tin tài khoản trước khi cập nhật.")
        String expectedStatus,

        @NotBlank(message = "Vui lòng nhập lý do nội bộ.")
        @Size(max = 1000, message = "Lý do nội bộ tối đa 1.000 ký tự.")
        String reason,

        @Size(max = 1000, message = "Lời nhắn gửi người dùng tối đa 1.000 ký tự.")
        String message) {
}
