package com.kizuna.features.auth.dto;


import com.kizuna.core.entity.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Đối tượng lưu trữ thông tin đăng ký tạm thời trong bộ nhớ đệm (Cache) chờ người dùng xác thực OTP.
 * Thời hạn sống (TTL): 5 phút (300 giây).
 * Số lần thử tối đa (Max Attempts): 5 lần (Chống Brute-force).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PendingRegistration {

    private String email;
    private String fullName;
    private String passwordHash;
    private String role;
    private String targetJlpt;
    private String otp;
    private Instant expiresAt;
    @Builder.Default
    private int attempts = 0;
    private Instant lastSentAt;
}
