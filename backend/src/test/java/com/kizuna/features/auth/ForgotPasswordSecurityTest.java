package com.kizuna.features.auth;

import com.kizuna.core.entity.Role;
import com.kizuna.core.entity.User;
import com.kizuna.core.entity.UserAccountStatus;
import com.kizuna.core.exception.BusinessException;
import com.kizuna.core.repository.LearnerRepository;
import com.kizuna.core.repository.RoleRepository;
import com.kizuna.core.repository.TutorRepository;
import com.kizuna.core.repository.UserRepository;
import com.kizuna.core.security.JwtTokenProvider;
import com.kizuna.features.auth.dto.request.ResetPasswordRequest;
import com.kizuna.features.auth.service.EmailService;
import com.kizuna.features.auth.service.OtpService;
import com.kizuna.features.auth.service.impl.AuthServiceImpl;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class ForgotPasswordSecurityTest {

    private UserRepository userRepository;
    private PasswordEncoder passwordEncoder;
    private JavaMailSender mailSender;
    private AuthServiceImpl authService;

    @BeforeEach
    void setUp() {
        userRepository = Mockito.mock(UserRepository.class);
        RoleRepository roleRepository = Mockito.mock(RoleRepository.class);
        LearnerRepository learnerRepository = Mockito.mock(LearnerRepository.class);
        TutorRepository tutorRepository = Mockito.mock(TutorRepository.class);
        passwordEncoder = Mockito.mock(PasswordEncoder.class);
        JwtTokenProvider tokenProvider = Mockito.mock(JwtTokenProvider.class);
        OtpService otpService = Mockito.mock(OtpService.class);
        EmailService emailService = Mockito.mock(EmailService.class);
        mailSender = Mockito.mock(JavaMailSender.class);

        authService = new AuthServiceImpl(
                userRepository,
                roleRepository,
                learnerRepository,
                tutorRepository,
                passwordEncoder,
                tokenProvider,
                otpService,
                emailService,
                mailSender
        );
        ReflectionTestUtils.setField(authService, "senderEmail", "kizunatutor@gmail.com");
        ReflectionTestUtils.setField(authService, "senderName", "KizunaTutor");
    }

    private User createUserWithRole(String email, String roleName) {
        return User.builder()
                .email(email)
                .fullName("Test User")
                .passwordHash("hashed-old")
                .role(Role.builder().roleName(roleName).build())
                .accountStatus(UserAccountStatus.ACTIVE)
                .build();
    }

    @ParameterizedTest
    @ValueSource(strings = {"ROLE_ADMIN", "ROLE_MANAGER", "ROLE_CS_STAFF", "ROLE_ACADEMIC_OFFICER"})
    @DisplayName("Chặn nhân sự nội bộ và quản trị viên thực hiện quên mật khẩu (HTTP 403)")
    void shouldBlockInternalRolesFromForgotAndResetPassword(String restrictedRole) {
        String email = "staff@kizuna.vn";
        User internalUser = createUserWithRole(email, restrictedRole);
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(internalUser));

        BusinessException step1Ex = assertThrows(BusinessException.class,
                () -> authService.resetPasswordAndSendEmail(email));
        assertEquals(403, step1Ex.status());

        ResetPasswordRequest step2Req = ResetPasswordRequest.builder()
                .email(email)
                .temporaryPassword("Temp123456")
                .newPassword("NewPass123")
                .confirmPassword("NewPass123")
                .build();
        BusinessException step2Ex = assertThrows(BusinessException.class,
                () -> authService.resetPasswordWithTemporary(step2Req));
        assertEquals(403, step2Ex.status());

        verify(userRepository, never()).save(any());
        verify(mailSender, never()).send(any(MimeMessage.class));
    }

    @ParameterizedTest
    @ValueSource(strings = {"ROLE_LEARNER", "ROLE_TUTOR"})
    @DisplayName("Cho phép Học viên và Gia sư cấp mật khẩu ngẫu nhiên và đổi mật khẩu mới")
    void shouldAllowLearnerAndTutorToResetPassword(String allowedRole) {
        String email = "user@gmail.com";
        User user = createUserWithRole(email, allowedRole);
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
        when(passwordEncoder.encode(anyString())).thenAnswer(inv -> "encoded-" + inv.getArgument(0));
        when(mailSender.createMimeMessage()).thenReturn(new MimeMessage(Session.getDefaultInstance(new Properties())));

        assertDoesNotThrow(() -> authService.resetPasswordAndSendEmail(email));
        verify(userRepository, times(1)).save(user);
        verify(mailSender, times(1)).send(any(MimeMessage.class));
    }

    @Test
    @DisplayName("Xác minh mật khẩu tạm và cập nhật mật khẩu mới cho Học viên")
    void shouldVerifyTempPasswordAndUpdateNewPasswordForLearner() {
        String email = "learner@gmail.com";
        User learner = createUserWithRole(email, "ROLE_LEARNER");
        learner.setPasswordHash("encoded-TempABC123");

        when(userRepository.findByEmail(email)).thenReturn(Optional.of(learner));
        when(passwordEncoder.matches("TempABC123", "encoded-TempABC123")).thenReturn(true);
        when(passwordEncoder.encode("MyNewPass99")).thenReturn("encoded-MyNewPass99");

        ResetPasswordRequest req = ResetPasswordRequest.builder()
                .email(email)
                .temporaryPassword("TempABC123")
                .newPassword("MyNewPass99")
                .confirmPassword("MyNewPass99")
                .build();

        assertDoesNotThrow(() -> authService.resetPasswordWithTemporary(req));
        assertEquals("encoded-MyNewPass99", learner.getPasswordHash());
        verify(userRepository, times(1)).save(learner);
    }

    @Test
    @DisplayName("Không tiết lộ email chưa đăng ký: trả về bình thường và không gửi mail")
    void shouldNotRevealUnregisteredEmailOnForgotPassword() {
        String unknownEmail = "unknown@gmail.com";
        when(userRepository.findByEmail(unknownEmail)).thenReturn(Optional.empty());

        assertDoesNotThrow(() -> authService.resetPasswordAndSendEmail(unknownEmail));
        verify(userRepository, never()).save(any());
        verify(mailSender, never()).send(any(MimeMessage.class));
    }

    @Test
    @DisplayName("Giới hạn tần suất quên mật khẩu: chặn lần thứ 4 liên tiếp (HTTP 429)")
    void shouldRateLimitRepeatedForgotPasswordRequests() {
        String email = "spam@gmail.com";
        when(userRepository.findByEmail(email)).thenReturn(Optional.empty());

        for (int i = 0; i < 3; i++) {
            assertDoesNotThrow(() -> authService.resetPasswordAndSendEmail(email));
        }

        BusinessException rateEx = assertThrows(BusinessException.class,
                () -> authService.resetPasswordAndSendEmail(email));
        assertEquals(429, rateEx.status());
    }
}
