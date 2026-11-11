package com.kizuna.features.profile.dto.request;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * Gia sư cập nhật hồ sơ cá nhân và giảng dạy (contracts/profile-api.md mục 2.2, GB-15).
 * Không chứa trường email: email chỉ đọc.
 */
public record UpdateTutorProfileRequest(
        @NotBlank(message = "Vui lòng nhập họ và tên.")
        @Size(max = 100, message = "Họ và tên tối đa 100 ký tự.")
        String fullName,

        @Pattern(regexp = "^((0|\\+84)[0-9]{9})?$", message = "Số điện thoại không đúng định dạng 10 chữ số.")
        String phone,

        @Size(max = 500, message = "Đường dẫn ảnh đại diện tối đa 500 ký tự.")
        @Pattern(regexp = "^(https?://\\S+|/api/profile/avatar/[A-Za-z0-9._-]+)?$",
                message = "Đường dẫn ảnh đại diện phải bắt đầu bằng http:// hoặc https://.")
        String avatarUrl,

        @NotBlank(message = "Vui lòng nhập tiêu đề giảng dạy.")
        @Size(max = 200, message = "Tiêu đề giảng dạy tối đa 200 ký tự.")
        String headline,

        @Size(max = 3000, message = "Giới thiệu bản thân tối đa 3000 ký tự.")
        String bio,

        @NotNull(message = "Vui lòng nhập số năm kinh nghiệm.")
        @Min(value = 0, message = "Số năm kinh nghiệm phải từ 0 đến 50 năm.")
        @Max(value = 50, message = "Số năm kinh nghiệm phải từ 0 đến 50 năm.")
        Integer experienceYears,

        @NotNull(message = "Vui lòng nhập học phí mỗi giờ.")
        @DecimalMin(value = "0", message = "Học phí không được nhỏ hơn 0 đồng.")
        @DecimalMax(value = "50000000", message = "Học phí vượt quá giới hạn cho phép.")
        BigDecimal hourlyRate,

        @NotBlank(message = "Vui lòng chọn trình độ JLPT cao nhất.")
        @Pattern(regexp = "^(N5|N4|N3|N2|N1)$", message = "Trình độ JLPT không hợp lệ.")
        String highestJlptLevel,

        @NotBlank(message = "Vui lòng chọn Tỉnh/Thành phố.")
        @Size(max = 100, message = "Tỉnh/Thành phố tối đa 100 ký tự.")
        String province,

        @Size(max = 150, message = "Quận/Huyện hoặc khu vực tối đa 150 ký tự.")
        String addressDetail) {
}
