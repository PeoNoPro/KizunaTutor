package com.kizuna.features.auth.controller;

import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Điều hướng luồng Google OAuth2 (/oauth2/authorization/google).
 * - Nếu đã cấu hình Google Client ID thật -> chuyển sang trang đăng nhập Google OAuth2.
 * - Nếu đang dùng cấu hình dev/placeholder -> chuyển sang luồng dev /api/auth/mock-google-login để tránh lỗi 404.
 */
@Slf4j
@Controller
public class GoogleOAuthController {

    private static final String PLACEHOLDER_CLIENT_ID = "1029384756-kizunatutor-google-client.apps.googleusercontent.com";

    @Value("${spring.security.oauth2.client.registration.google.client-id:}")
    private String googleClientId;

    @GetMapping("/oauth2/authorization/google")
    public void authorizeGoogle(HttpServletResponse response) throws IOException {
        if (googleClientId == null || googleClientId.isBlank() || PLACEHOLDER_CLIENT_ID.equals(googleClientId.trim())) {
            log.info("[OAUTH2 DEV] Google Client ID đang ở cấu hình mẫu, chuyển hướng sang /api/auth/mock-google-login");
            response.sendRedirect("/api/auth/mock-google-login");
            return;
        }

        String googleAuthUrl = UriComponentsBuilder.fromHttpUrl("https://accounts.google.com/o/oauth2/v2/auth")
                .queryParam("client_id", googleClientId.trim())
                .queryParam("redirect_uri", "http://localhost:3000/login/oauth2/code/google")
                .queryParam("response_type", "code")
                .queryParam("scope", "openid email profile")
                .queryParam("access_type", "offline")
                .encode(StandardCharsets.UTF_8)
                .toUriString();

        response.sendRedirect(googleAuthUrl);
    }
}
