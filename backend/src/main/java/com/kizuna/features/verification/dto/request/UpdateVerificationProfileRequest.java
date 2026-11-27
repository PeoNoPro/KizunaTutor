package com.kizuna.features.verification.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

/**
 * Lưu nháp thông tin xác minh (UC-23). Mọi trường đều tùy chọn khi lưu nháp;
 * kiểm tra bắt buộc đầy đủ chỉ thực hiện khi nộp hồ sơ (FR-005).
 * Chuỗi rỗng được hiểu là xóa giá trị.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateVerificationProfileRequest {

    @Size(max = 100, message = "Họ tên tối đa 100 ký tự.")
    private String fullName;

    @Pattern(regexp = "^$|^0\\d{9}$", message = "Số điện thoại gồm 10 chữ số và bắt đầu bằng số 0.")
    private String phone;

    @Past(message = "Ngày sinh phải là một ngày trong quá khứ.")
    private LocalDate dateOfBirth;

    @Size(max = 255, message = "Địa chỉ tối đa 255 ký tự.")
    private String address;

    @Size(max = 2000, message = "Phần giới thiệu tối đa 2.000 ký tự.")
    private String bio;

    @Min(value = 0, message = "Số năm kinh nghiệm không được âm.")
    @Max(value = 60, message = "Số năm kinh nghiệm tối đa là 60.")
    private Integer experienceYears;

    @Pattern(regexp = "^$|^N[1-5]$", message = "Vui lòng chọn trình độ JLPT từ N5 đến N1.")
    private String highestJlptLevel;
}
