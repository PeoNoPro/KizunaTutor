package com.kizuna.core.security;

import com.kizuna.core.entity.User;
import com.kizuna.core.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * Trích xuất danh tính email người dùng đã xác thực từ SecurityContext hoặc JWT (Header / Cookie).
 */
@Component
@RequiredArgsConstructor
public class AuthenticatedUserHelper {

    private final JwtTokenProvider jwtTokenProvider;

    public String requireAuthenticatedEmail(String authHeader, String cookieToken) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !(auth instanceof AnonymousAuthenticationToken)) {
            Object principal = auth.getPrincipal();
            if (principal instanceof User u && u.getEmail() != null && !u.getEmail().isBlank()) {
                return u.getEmail().trim().toLowerCase();
            }
            if (principal instanceof String s && !s.isBlank() && !"anonymousUser".equalsIgnoreCase(s)) {
                return s.trim().toLowerCase();
            }
        }

        String token = (authHeader != null && authHeader.startsWith("Bearer "))
                ? authHeader.substring(7).trim()
                : (cookieToken != null ? cookieToken.trim() : null);

        if (token != null && !token.isBlank() && jwtTokenProvider.validateToken(token)) {
            String email = jwtTokenProvider.getEmailFromToken(token);
            if (email != null && !email.isBlank()) {
                return email.trim().toLowerCase();
            }
        }

        throw new BusinessException(401, "Vui lòng đăng nhập để sử dụng dữ liệu cá nhân này.");
    }
}
