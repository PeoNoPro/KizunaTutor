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
 * Tuyệt đối không chuyển sang endpoint giả lập khi chưa cấu hình Client ID hợp lệ.
 */
@Slf4j
@Controller
public class GoogleOAuthController {

    private static final String PLACEHOLDER_CLIENT_ID = "1029384756-kizunatutor-google-client.apps.googleusercontent.com";

    @Value("${spring.security.oauth2.client.registration.google.client-id:}")
    private String googleClientId;

    @Value("${app.auth.google-redirect-uri:http://localhost:3000/login/oauth2/code/google}")
    private String redirectUri;

    @GetMapping("/oauth2/authorization/google")
    public void authorizeGoogle(HttpServletResponse response) throws IOException {
        if (googleClientId == null || googleClientId.isBlank() || PLACEHOLDER_CLIENT_ID.equals(googleClientId.trim())) {
            log.warn("[OAUTH2] Chưa cấu hình Google Client ID thật; từ chối chuyển hướng đăng nhập Google.");
            response.sendRedirect("/login?error=oauth2_not_configured");
            return;
        }

        String googleAuthUrl = UriComponentsBuilder.fromHttpUrl("https://accounts.google.com/o/oauth2/v2/auth")
                .queryParam("client_id", googleClientId.trim())
                .queryParam("redirect_uri", redirectUri)
                .queryParam("response_type", "code")
                .queryParam("scope", "openid email profile")
                .queryParam("access_type", "offline")
                .encode(StandardCharsets.UTF_8)
                .toUriString();

        response.sendRedirect(googleAuthUrl);
    }
}
