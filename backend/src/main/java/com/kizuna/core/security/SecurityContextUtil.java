package com.kizuna.core.security;

import com.kizuna.core.entity.RoleEnum;
import com.kizuna.core.entity.User;
import com.kizuna.core.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Tiện ích truy xuất thông tin người dùng hiện tại từ Spring Security Context.
 */
@Component
@RequiredArgsConstructor
public class SecurityContextUtil {

    private final UserRepository userRepository;

    public Optional<User> getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated()) {
            if (auth.getPrincipal() instanceof User) {
                return Optional.of((User) auth.getPrincipal());
            }
            String email = auth.getName();
            if (email != null && !email.isBlank() && !"anonymousUser".equals(email)) {
                return userRepository.findByEmail(email);
            }
        }
        return Optional.empty();
    }

    public String getCurrentUserName(RoleEnum fallbackRole) {
        return getCurrentUser()
                .map(User::getFullName)
                .orElse(fallbackRole.getBadgeText());
    }

    public String getCurrentUserEmail(RoleEnum fallbackRole) {
        return getCurrentUser()
                .map(User::getEmail)
                .orElse("user@kizuna.vn");
    }

    public String getCurrentUserAvatarText(String fullName) {
        if (fullName == null || fullName.trim().isEmpty()) {
            return "KZ";
        }
        String[] parts = fullName.trim().split("\\s+");
        if (parts.length >= 2) {
            String first = parts[0].substring(0, 1).toUpperCase();
            String last = parts[parts.length - 1].substring(0, 1).toUpperCase();
            return first + last;
        }
        return fullName.substring(0, Math.min(2, fullName.length())).toUpperCase();
    }
}
