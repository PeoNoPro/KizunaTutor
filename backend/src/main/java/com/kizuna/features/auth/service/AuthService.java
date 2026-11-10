package com.kizuna.features.auth.service;

import com.kizuna.features.auth.dto.request.FirstTimePasswordChangeRequest;
import com.kizuna.features.auth.dto.request.LoginRequest;
import com.kizuna.features.auth.dto.request.RegisterRequest;
import com.kizuna.features.auth.dto.request.ResetPasswordRequest;
import com.kizuna.features.auth.dto.request.VerifyOtpRequest;
import com.kizuna.features.auth.dto.response.LoginResponse;
import com.kizuna.features.auth.dto.response.UserSummaryDto;
import jakarta.servlet.http.HttpServletResponse;

import java.util.Map;

public interface AuthService {
    void initiateRegistration(RegisterRequest request);
    LoginResponse completeRegistration(VerifyOtpRequest request);
    LoginResponse login(LoginRequest request, HttpServletResponse response);
    LoginResponse register(RegisterRequest request, HttpServletResponse response);
    UserSummaryDto getCurrentUser(String email);
    Map<String, Object> changePasswordFirstTime(FirstTimePasswordChangeRequest request, String userEmail);
    void resetPasswordAndSendEmail(String email);
    void resetPasswordWithTemporary(ResetPasswordRequest request);
    void logout(HttpServletResponse response);
}