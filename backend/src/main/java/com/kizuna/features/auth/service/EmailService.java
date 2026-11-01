package com.kizuna.features.auth.service;

public interface EmailService {
    void sendRegistrationOtpEmail(String toEmail, String fullName, String otp);
}
