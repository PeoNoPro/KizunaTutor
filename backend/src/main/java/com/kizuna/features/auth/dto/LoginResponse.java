package com.kizuna.features.auth.dto;


import com.kizuna.core.entity.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO trả về kết quả xác thực đăng nhập thành công.
 * Bao gồm Token JWT, Dữ liệu User và URL Dashboard điều hướng đích.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginResponse {
    private String token;
    private UserSummaryDto user;
    private String redirectUrl;
    private String message;
    private boolean mustChangePassword;
}
