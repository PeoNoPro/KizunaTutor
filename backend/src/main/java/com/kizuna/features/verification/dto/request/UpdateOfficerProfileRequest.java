package com.kizuna.features.verification.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Cán bộ học vụ cập nhật thông tin cá nhân: chỉ họ tên và số điện thoại (email do quản trị cấp). */
@Getter
@Setter
@NoArgsConstructor
public class UpdateOfficerProfileRequest {
    private String fullName;
    private String phone;
}
