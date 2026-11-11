package com.kizuna.features.profile;

import com.kizuna.core.entity.Role;
import com.kizuna.core.entity.User;
import com.kizuna.core.entity.UserAccountStatus;
import com.kizuna.core.exception.BusinessException;
import com.kizuna.core.repository.LearnerRepository;
import com.kizuna.core.repository.RoleRepository;
import com.kizuna.core.repository.TutorRepository;
import com.kizuna.core.repository.UserRepository;
import com.kizuna.core.security.JwtAuthenticationFilter;
import com.kizuna.core.security.JwtTokenProvider;
import com.kizuna.core.security.JwtTokenVerifier;
import com.kizuna.core.security.RestAuthErrorHandlers;
import com.kizuna.core.security.SecurityConfig;
import com.kizuna.features.admin.service.OperationsActorResolver;
import com.kizuna.features.auth.dto.request.LoginRequest;
import com.kizuna.features.auth.service.EmailService;
import com.kizuna.features.auth.service.OtpService;
import com.kizuna.features.auth.service.impl.AuthServiceImpl;
import com.kizuna.features.profile.controller.LearnerProfileController;
import com.kizuna.features.profile.service.LearnerProfileService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(LearnerProfileController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, RestAuthErrorHandlers.class})
class SuspendedUserSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private LearnerProfileService learnerProfileService;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private JwtTokenVerifier jwtTokenVerifier;

    @MockBean
    private UserRepository userRepository;

    @MockBean
    private OperationsActorResolver operationsActorResolver;

    private void givenBearerUser(String token, UserAccountStatus status, OffsetDateTime deletedAt) {
        User user = User.builder()
                .userId(UUID.randomUUID())
                .email("blocked@kizuna.vn")
                .fullName("Người dùng bị khóa")
                .role(Role.builder().roleId((short) 6).roleName("ROLE_LEARNER").build())
                .accountStatus(status)
                .deletedAt(deletedAt)
                .build();
        when(jwtTokenProvider.validateToken(token)).thenReturn(true);
        when(jwtTokenProvider.getEmailFromToken(token)).thenReturn("blocked@kizuna.vn");
        when(userRepository.findByEmail("blocked@kizuna.vn")).thenReturn(Optional.of(user));
    }

    @Test
    @DisplayName("Token của tài khoản SUSPENDED bị chặn ngay với HTTP 403 và thông báo tiếng Việt")
    void suspendedTokenRejectedWith403() throws Exception {
        givenBearerUser("tok-suspended", UserAccountStatus.SUSPENDED, null);

        mockMvc.perform(get("/api/profile/learner/me").header("Authorization", "Bearer tok-suspended"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value(JwtAuthenticationFilter.BLOCKED_ACCOUNT_MESSAGE));

        verify(learnerProfileService, never()).getMyProfile(any());
    }

    @Test
    @DisplayName("Token của tài khoản BANNED bị chặn ngay với HTTP 403 và thông báo tiếng Việt")
    void bannedTokenRejectedWith403() throws Exception {
        givenBearerUser("tok-banned", UserAccountStatus.BANNED, null);

        mockMvc.perform(get("/api/profile/learner/me").header("Authorization", "Bearer tok-banned"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value(JwtAuthenticationFilter.BLOCKED_ACCOUNT_MESSAGE));

        verify(learnerProfileService, never()).getMyProfile(any());
    }

    @Test
    @DisplayName("Token của tài khoản đã xóa mềm bị chặn ngay với HTTP 403")
    void softDeletedTokenRejectedWith403() throws Exception {
        givenBearerUser("tok-deleted", UserAccountStatus.ACTIVE, OffsetDateTime.now().minusDays(1));

        mockMvc.perform(get("/api/profile/learner/me").header("Authorization", "Bearer tok-deleted"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value(JwtAuthenticationFilter.BLOCKED_ACCOUNT_MESSAGE));

        verify(learnerProfileService, never()).getMyProfile(any());
    }

    @Test
    @DisplayName("Đăng nhập với tài khoản BANNED hoặc SUSPENDED bị từ chối với HTTP 403")
    void loginRejectsBlockedOrDeletedAccount() {
        UserRepository users = mock(UserRepository.class);
        PasswordEncoder encoder = mock(PasswordEncoder.class);
        AuthServiceImpl authService = new AuthServiceImpl(
                users,
                mock(RoleRepository.class),
                mock(LearnerRepository.class),
                mock(TutorRepository.class),
                encoder,
                mock(JwtTokenProvider.class),
                mock(OtpService.class),
                mock(EmailService.class),
                mock(JavaMailSender.class)
        );
        User suspended = User.builder()
                .userId(UUID.randomUUID())
                .email("blocked@kizuna.vn")
                .passwordHash("hashed")
                .accountStatus(UserAccountStatus.SUSPENDED)
                .build();
        when(users.findByEmail("blocked@kizuna.vn")).thenReturn(Optional.of(suspended));
        when(encoder.matches("MatKhau123", "hashed")).thenReturn(true);

        LoginRequest req = new LoginRequest();
        req.setEmail("blocked@kizuna.vn");
        req.setPassword("MatKhau123");

        assertThatThrownBy(() -> authService.login(req, null))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> {
                    BusinessException be = (BusinessException) ex;
                    assertThat(be.status()).isEqualTo(403);
                    assertThat(be.getMessage()).isEqualTo(JwtAuthenticationFilter.BLOCKED_ACCOUNT_MESSAGE);
                });
    }
}
