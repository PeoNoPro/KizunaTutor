package com.kizuna.features.auth.service;

import com.kizuna.features.auth.dto.request.FirstTimePasswordChangeRequest;
import com.kizuna.features.auth.dto.request.LoginRequest;
import com.kizuna.features.auth.dto.request.RegisterRequest;
import com.kizuna.features.auth.dto.response.LoginResponse;
import com.kizuna.features.auth.dto.response.UserSummaryDto;
import jakarta.servlet.http.HttpServletResponse;

import java.util.Map;

public interface AuthService {
    LoginResponse login(LoginRequest request, HttpServletResponse response);
    LoginResponse register(RegisterRequest request, HttpServletResponse response);
    UserSummaryDto getCurrentUser(String email);
    Map<String, Object> changePasswordFirstTime(FirstTimePasswordChangeRequest request, String userEmail);
    void logout(HttpServletResponse response);
}
