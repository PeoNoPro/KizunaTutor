package com.kizuna.features.auth.security;

import com.kizuna.core.entity.RoleEnum;
import com.kizuna.core.exception.BusinessException;
import com.kizuna.features.auth.dto.response.LoginResponse;
import com.kizuna.features.auth.service.AuthService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * Xử lý callback sau khi Google OAuth2 xác thực thành công:
 * tìm/tạo tài khoản, cấp JWT + cookie access_token và chuyển hướng tương đối về /login?token=...
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class GoogleOAuth2SuccessHandler implements AuthenticationSuccessHandler {

    private final AuthService authService;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) {
        String selectedRole = consumeSelectedRole(request);

        Object principal = authentication != null ? authentication.getPrincipal() : null;
        if (!(principal instanceof OAuth2User oauth2User)) {
            redirectRelative(response, "/login?error=oauth2_process_error");
            return;
        }

        String email = oauth2User.getAttribute("email");
        Object emailVerifiedAttr = oauth2User.getAttribute("email_verified");
        boolean emailUnverified = Boolean.FALSE.equals(emailVerifiedAttr)
                || "false".equalsIgnoreCase(String.valueOf(emailVerifiedAttr));
        if (email == null || email.isBlank() || (emailVerifiedAttr != null && emailUnverified)) {
            redirectRelative(response, "/login?error=email_not_provided");
            return;
        }

        String fullName = oauth2User.getAttribute("name");
        String picture = oauth2User.getAttribute("picture");

        try {
            LoginResponse loginResponse = authService.authenticateWithGoogle(email, fullName, picture, selectedRole);
            String token = loginResponse.getToken();

            Cookie cookie = new Cookie("access_token", token);
            cookie.setPath("/");
            cookie.setHttpOnly(true);
            cookie.setMaxAge(86400);
            response.addCookie(cookie);

            String role = loginResponse.getUser() != null ? loginResponse.getUser().getRole() : RoleEnum.ROLE_LEARNER.name();
            String resolvedName = loginResponse.getUser() != null ? loginResponse.getUser().getFullName() : "Học Viên Kizuna";
            String resolvedEmail = loginResponse.getUser() != null ? loginResponse.getUser().getEmail() : email.trim().toLowerCase();

            String redirectUrl = UriComponentsBuilder.fromUriString("/login")
                    .queryParam("token", token)
                    .queryParam("role", role)
                    .queryParam("email", URLEncoder.encode(resolvedEmail, StandardCharsets.UTF_8))
                    .queryParam("name", URLEncoder.encode(resolvedName, StandardCharsets.UTF_8))
                    .build().toUriString();

            redirectRelative(response, redirectUrl);
        } catch (BusinessException ex) {
            log.warn("[OAUTH2] Từ chối đăng nhập Google cho {}: {}", email, ex.getMessage());
            redirectRelative(response, "/login?error=account_blocked");
        } catch (Exception ex) {
            log.error("[OAUTH2] Lỗi xử lý đăng nhập Google cho {}: {}", email, ex.getMessage(), ex);
            redirectRelative(response, "/login?error=oauth2_process_error");
        }
    }

    private String consumeSelectedRole(HttpServletRequest request) {
        if (request == null) {
            return RoleEnum.ROLE_LEARNER.name();
        }
        HttpSession session = request.getSession(false);
        if (session == null) {
            return RoleEnum.ROLE_LEARNER.name();
        }
        Object raw = session.getAttribute(GoogleOAuth2AuthorizationRequestResolver.SESSION_SELECTED_ROLE);
        session.removeAttribute(GoogleOAuth2AuthorizationRequestResolver.SESSION_SELECTED_ROLE);
        return RoleEnum.ROLE_TUTOR.name().equals(raw)
                ? RoleEnum.ROLE_TUTOR.name()
                : RoleEnum.ROLE_LEARNER.name();
    }

    private void redirectRelative(HttpServletResponse response, String location) {
        response.setStatus(HttpServletResponse.SC_FOUND);
        response.setHeader("Location", location);
    }
}
