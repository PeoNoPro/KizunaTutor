package com.kizuna.features.auth.service;

import com.kizuna.features.auth.dto.response.PendingRegistration;

public interface OtpService {
    String generateAndSaveOtp(String email, String fullName, String passwordHash, String role, String targetJlpt);
    PendingRegistration verifyOtp(String email, String otpInput);
    String resendOtp(String email);
    PendingRegistration getPendingRegistration(String email);
}
