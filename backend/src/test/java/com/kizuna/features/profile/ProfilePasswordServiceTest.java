package com.kizuna.features.profile;

import com.kizuna.core.entity.Role;
import com.kizuna.core.entity.User;
import com.kizuna.core.exception.BadRequestException;
import com.kizuna.core.repository.UserRepository;
import com.kizuna.features.profile.dto.request.ChangePasswordRequest;
import com.kizuna.features.profile.service.impl.ProfilePasswordServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ProfilePasswordServiceTest {

    private static final String EMAIL = "tutor@kizuna.vn";
    private static final String CURRENT = "MatKhauCu123";

    private final PasswordEncoder encoder = new BCryptPasswordEncoder(4);
    private UserRepository userRepository;
    private ProfilePasswordServiceImpl service;
    private User user;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        service = new ProfilePasswordServiceImpl(userRepository, encoder);
        user = User.builder().userId(UUID.randomUUID()).email(EMAIL)
                .passwordHash(encoder.encode(CURRENT)).mustChangePassword(true)
                .role(Role.builder().roleName("ROLE_TUTOR").build()).build();
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
    }

    @Test
    @DisplayName("Đổi mật khẩu hợp lệ: lưu hash mới, bỏ cờ bắt buộc đổi mật khẩu")
    void changesPassword() {
        service.changeMyPassword(EMAIL, new ChangePasswordRequest(CURRENT, "MatKhauMoi456", "MatKhauMoi456"));

        assertThat(encoder.matches("MatKhauMoi456", user.getPasswordHash())).isTrue();
        assertThat(user.isMustChangePassword()).isFalse();
        verify(userRepository).save(user);
    }

    @Test
    @DisplayName("Sai mật khẩu hiện tại, xác nhận không khớp hoặc mật khẩu mới trùng cũ đều bị từ chối, không lưu")
    void rejectsInvalidChanges() {
        assertThatThrownBy(() -> service.changeMyPassword(EMAIL, new ChangePasswordRequest("SaiMatKhau1", "MatKhauMoi456", "MatKhauMoi456")))
                .isInstanceOf(BadRequestException.class).hasMessage("Mật khẩu hiện tại chưa đúng.");
        assertThatThrownBy(() -> service.changeMyPassword(EMAIL, new ChangePasswordRequest(CURRENT, "MatKhauMoi456", "KhacNhau789")))
                .isInstanceOf(BadRequestException.class).hasMessage("Mật khẩu xác nhận không khớp.");
        assertThatThrownBy(() -> service.changeMyPassword(EMAIL, new ChangePasswordRequest(CURRENT, CURRENT, CURRENT)))
                .isInstanceOf(BadRequestException.class).hasMessage("Mật khẩu mới cần khác mật khẩu hiện tại.");
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Tài khoản không có mật khẩu dạng BCrypt (ví dụ đăng nhập Google) báo sai mật khẩu, không lộ lỗi kỹ thuật")
    void treatsNonBcryptHashAsWrongPassword() {
        user.setPasswordHash("google-oauth-no-password");

        assertThatThrownBy(() -> service.changeMyPassword(EMAIL, new ChangePasswordRequest(CURRENT, "MatKhauMoi456", "MatKhauMoi456")))
                .isInstanceOf(BadRequestException.class).hasMessage("Mật khẩu hiện tại chưa đúng.");
    }

    @Test
    @DisplayName("Nhân sự nội bộ không đổi mật khẩu qua API hồ sơ học viên/gia sư")
    void rejectsStaffRole() {
        user.setRole(Role.builder().roleName("ROLE_MANAGER").build());

        assertThatThrownBy(() -> service.changeMyPassword(EMAIL, new ChangePasswordRequest(CURRENT, "MatKhauMoi456", "MatKhauMoi456")))
                .isInstanceOf(AccessDeniedException.class);
    }
}
