package com.kizuna.features.auth.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;

/**
 * Chuyển hướng về trang đăng nhập kèm mã lỗi chuẩn khi xác thực Google thất bại hoặc bị hủy.
 */
@Slf4j
@Component
public class GoogleOAuth2FailureHandler implements AuthenticationFailureHandler {

    @Override
    public void onAuthenticationFailure(HttpServletRequest request,
                                        HttpServletResponse response,
                                        AuthenticationException exception) {
        if (request != null) {
            HttpSession session = request.getSession(false);
            if (session != null) {
                session.removeAttribute(GoogleOAuth2AuthorizationRequestResolver.SESSION_SELECTED_ROLE);
            }
        }
        log.warn("[OAUTH2] Đăng nhập Google thất bại: {}", exception != null ? exception.getMessage() : "unknown");
        response.setStatus(HttpServletResponse.SC_FOUND);
        response.setHeader("Location", "/login?error=oauth2_failure");
    }
}
