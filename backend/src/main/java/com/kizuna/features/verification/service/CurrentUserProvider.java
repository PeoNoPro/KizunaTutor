package com.kizuna.features.verification.service;

import com.kizuna.core.entity.User;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * Lấy người dùng đã được JwtAuthenticationFilter xác thực (đã đọc lại vai trò, trạng thái từ DB).
 */
@Component
public class CurrentUserProvider {

    public User requireCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof User user)) {
            throw VerificationException.unauthenticated();
        }
        return user;
    }
}
