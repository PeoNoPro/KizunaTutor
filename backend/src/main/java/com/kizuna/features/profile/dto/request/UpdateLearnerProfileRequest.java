package com.kizuna.features.profile.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Học viên cập nhật hồ sơ (contracts/profile-api.md mục 1.2, data-model.md mục 2.1).
 * Không có trường email: email chỉ đọc (FR-015). Trường tùy chọn để trống được lưu là rỗng.
 */
public record UpdateLearnerProfileRequest(
        @NotBlank(message = "Vui lòng nhập họ và tên.")
        @Size(max = 100, message = "Họ và tên tối đa 100 ký tự.")
        String fullName,

        @Pattern(regexp = "^((0|\\+84)[0-9]{9})?$", message = "Số điện thoại không đúng định dạng 10 chữ số.")
        String phone,

        @Size(max = 500, message = "Đường dẫn ảnh đại diện tối đa 500 ký tự.")
        @Pattern(regexp = "^(https?://\\S+|/api/profile/avatar/[A-Za-z0-9._-]+)?$",
                message = "Đường dẫn ảnh đại diện phải bắt đầu bằng http:// hoặc https://.")
        String avatarUrl,

        @Pattern(regexp = "^(BEGINNER|N5|N4|N3|N2|N1)$", message = "Trình độ hiện tại không hợp lệ.")
        String currentLevel,

        @Pattern(regexp = "^(N5|N4|N3|N2|N1)$", message = "Trình độ mục tiêu không hợp lệ.")
        String targetLevel,

        @Size(max = 2000, message = "Mục tiêu học tập tối đa 2000 ký tự.")
        String learningGoals,

        @Size(max = 100, message = "Tỉnh/Thành phố tối đa 100 ký tự.")
        String province,

        @Size(max = 255, message = "Địa chỉ chi tiết tối đa 255 ký tự.")
        String address) {
}
