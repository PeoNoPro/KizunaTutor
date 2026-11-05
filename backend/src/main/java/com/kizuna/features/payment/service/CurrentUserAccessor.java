package com.kizuna.features.payment.service;

import com.kizuna.core.entity.User;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * Người dùng đã được JwtAuthenticationFilter xác thực (vai trò và trạng thái đã đọc lại từ DB).
 */
@Component
public class CurrentUserAccessor {

    public User requireUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof User user)) {
            throw new AccessDeniedException("Bạn không có quyền thực hiện thao tác này.");
        }
        return user;
    }
}
