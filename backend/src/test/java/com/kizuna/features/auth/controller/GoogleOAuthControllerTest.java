package com.kizuna.features.auth.controller;

import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

@DisplayName("Google OAuth Controller Unit Tests (3 Test Cases)")
class GoogleOAuthControllerTest {

    private GoogleOAuthController controller;
    private HttpServletResponse response;

    @BeforeEach
    void setUp() {
        controller = new GoogleOAuthController();
        response = mock(HttpServletResponse.class);
        ReflectionTestUtils.setField(controller, "redirectUri", "http://localhost:3000/login/oauth2/code/google");
    }

    @Test
    @DisplayName("UTCID01 (Normal): Google Client ID thật hợp lệ -> Chuyển hướng tới trang xác thực Google Consent")
    void authorizeGoogle_whenValidClientId_shouldRedirectToGoogleAuthUrl() throws IOException {
        String realClientId = "123456789-myrealapp.apps.googleusercontent.com";
        ReflectionTestUtils.setField(controller, "googleClientId", realClientId);

        controller.authorizeGoogle(response);

        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(response, times(1)).sendRedirect(captor.capture());
        String redirectedUrl = captor.getValue();

        assertTrue(redirectedUrl.startsWith("https://accounts.google.com/o/oauth2/v2/auth"));
        assertTrue(redirectedUrl.contains("client_id=" + realClientId));
        assertTrue(redirectedUrl.contains("scope=openid+email+profile") || redirectedUrl.contains("scope=openid%20email%20profile"));
        assertTrue(redirectedUrl.contains("response_type=code"));
    }

    @Test
    @DisplayName("UTCID02 (Abnormal): Google Client ID rỗng hoặc null -> Chặn chuyển hướng và báo lỗi oauth2_not_configured")
    void authorizeGoogle_whenClientIdBlankOrNull_shouldRedirectToLoginWithError() throws IOException {
        ReflectionTestUtils.setField(controller, "googleClientId", "");

        controller.authorizeGoogle(response);

        verify(response, times(1)).sendRedirect("/login?error=oauth2_not_configured");

        reset(response);
        ReflectionTestUtils.setField(controller, "googleClientId", null);

        controller.authorizeGoogle(response);

        verify(response, times(1)).sendRedirect("/login?error=oauth2_not_configured");
    }

    @Test
    @DisplayName("UTCID03 (Abnormal): Google Client ID là placeholder mặc định -> Chặn chuyển hướng và báo lỗi")
    void authorizeGoogle_whenClientIdIsPlaceholder_shouldRedirectToLoginWithError() throws IOException {
        String placeholder = "1029384756-kizunatutor-google-client.apps.googleusercontent.com";
        ReflectionTestUtils.setField(controller, "googleClientId", placeholder);

        controller.authorizeGoogle(response);

        verify(response, times(1)).sendRedirect("/login?error=oauth2_not_configured");
    }
}
