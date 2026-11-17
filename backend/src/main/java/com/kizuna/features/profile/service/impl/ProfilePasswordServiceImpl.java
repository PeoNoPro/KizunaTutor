package com.kizuna.features.profile.service.impl;

import com.kizuna.core.entity.User;
import com.kizuna.core.exception.BadRequestException;
import com.kizuna.core.exception.ResourceNotFoundException;
import com.kizuna.core.repository.UserRepository;
import com.kizuna.features.profile.dto.request.ChangePasswordRequest;
import com.kizuna.features.profile.service.ProfilePasswordService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProfilePasswordServiceImpl implements ProfilePasswordService {

    private static final Set<String> ALLOWED_ROLES = Set.of("ROLE_LEARNER", "ROLE_TUTOR");

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void changeMyPassword(String email, ChangePasswordRequest request) {
        User user = userRepository.findByEmail(email)
                .filter(u -> u.getDeletedAt() == null)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy tài khoản của bạn."));
        if (user.getRole() == null || !ALLOWED_ROLES.contains(user.getRole().getRoleName())) {
            throw new AccessDeniedException("Chỉ học viên và gia sư đổi mật khẩu tại đây.");
        }
        if (!request.newPassword().equals(request.confirmPassword())) {
            throw new BadRequestException("Mật khẩu xác nhận không khớp.");
        }
        if (!matchesCurrent(request.currentPassword(), user.getPasswordHash())) {
            throw new BadRequestException("Mật khẩu hiện tại chưa đúng.");
        }
        if (request.currentPassword().equals(request.newPassword())) {
            throw new BadRequestException("Mật khẩu mới cần khác mật khẩu hiện tại.");
        }
        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        user.setMustChangePassword(false);
        userRepository.save(user);
        log.info("Người dùng {} đã đổi mật khẩu từ trang hồ sơ.", user.getUserId());
    }

    /** Hash rỗng hoặc không đúng định dạng (ví dụ tài khoản tạo qua Google) coi như sai mật khẩu, không ném lỗi kỹ thuật. */
    private boolean matchesCurrent(String raw, String hash) {
        if (hash == null || hash.isBlank()) {
            return false;
        }
        try {
            return passwordEncoder.matches(raw, hash);
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}
