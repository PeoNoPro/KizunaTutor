package com.kizuna.dto.tutor;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * DTO tiếp nhận dữ liệu lưu nháp hoặc nộp hồ sơ gia sư.
 * Phục vụ UC 9, 10, 11, 12, 13 (Cập nhật và nộp hồ sơ).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SaveTutorProfileRequest {

    private String fullName;

    private String phone;

    private LocalDate dateOfBirth;

    private String address;

    private String headline;

    private String bio;

    private String highestJlptLevel;

    private Short experienceYears;

    private BigDecimal hourlyRate;

    private String certificateUrls;
}
