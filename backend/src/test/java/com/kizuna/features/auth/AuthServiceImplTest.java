package com.kizuna.features.auth;

import com.kizuna.core.entity.*;
import com.kizuna.core.exception.BadRequestException;
import com.kizuna.core.exception.BusinessException;
import com.kizuna.core.exception.ResourceNotFoundException;
import com.kizuna.core.repository.LearnerRepository;
import com.kizuna.core.repository.RoleRepository;
import com.kizuna.core.repository.TutorRepository;
import com.kizuna.core.repository.UserRepository;
import com.kizuna.core.security.JwtTokenProvider;
import com.kizuna.features.auth.dto.request.FirstTimePasswordChangeRequest;
import com.kizuna.features.auth.dto.request.LoginRequest;
import com.kizuna.features.auth.dto.request.RegisterRequest;
import com.kizuna.features.auth.dto.request.ResetPasswordRequest;
import com.kizuna.features.auth.dto.request.VerifyOtpRequest;
import com.kizuna.features.auth.dto.response.LoginResponse;
import com.kizuna.features.auth.dto.response.PendingRegistration;
import com.kizuna.features.auth.dto.response.UserSummaryDto;
import com.kizuna.features.auth.service.EmailService;
import com.kizuna.features.auth.service.OtpService;
import com.kizuna.features.auth.service.impl.AuthServiceImpl;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit Test Suite for AuthServiceImpl.
 * Covers FEATURE 1: AUTHENTICATION & AUTHORIZATION (UC-01, UC-02, UC-02.OTP, UC-03, UC-04, UC-05).
 * Follows AAA (Arrange - Act - Assert) and Clean Code principles with 100% mocked dependencies.
 */
class AuthServiceImplTest {

    private UserRepository userRepository;
    private RoleRepository roleRepository;
    private LearnerRepository learnerRepository;
    private TutorRepository tutorRepository;
    private PasswordEncoder passwordEncoder;
    private JwtTokenProvider tokenProvider;
    private OtpService otpService;
    private EmailService emailService;
    private JavaMailSender mailSender;
    private HttpServletResponse httpServletResponse;

    private AuthServiceImpl authService;

    @BeforeEach
    void setUp() {
        userRepository = Mockito.mock(UserRepository.class);
        roleRepository = Mockito.mock(RoleRepository.class);
        learnerRepository = Mockito.mock(LearnerRepository.class);
        tutorRepository = Mockito.mock(TutorRepository.class);
        passwordEncoder = Mockito.mock(PasswordEncoder.class);
        tokenProvider = Mockito.mock(JwtTokenProvider.class);
        otpService = Mockito.mock(OtpService.class);
        emailService = Mockito.mock(EmailService.class);
        mailSender = Mockito.mock(JavaMailSender.class);
        httpServletResponse = Mockito.mock(HttpServletResponse.class);

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

    private User buildUser(String email, String roleName, UserAccountStatus status, boolean mustChangePassword) {
        Role role = Role.builder().roleName(roleName).build();
        return User.builder()
                .userId(java.util.UUID.randomUUID())
                .email(email)
                .fullName("Nguyen Van A")
                .passwordHash("$2a$10$hashedPassword")
                .role(role)
                .accountStatus(status)
                .mustChangePassword(mustChangePassword)
                .avatarUrl("https://kizuna.vn/avatar.png")
                .build();
    }

    // =========================================================================
    // UC-01: ĐĂNG NHẬP HỆ THỐNG (LOG IN)
    // =========================================================================
    @Nested
    @DisplayName("UC-01: Đăng nhập hệ thống (login)")
    class LoginTests {

        @ParameterizedTest(name = "Role {0} should redirect to {1}")
        @CsvSource({
                "ROLE_ADMIN, /admin/dashboard",
                "ROLE_MANAGER, /manager/dashboard",
                "ROLE_CS_STAFF, /cs/dashboard",
                "ROLE_ACADEMIC_OFFICER, /academic/dashboard",
                "ROLE_TUTOR, /tutor/dashboard",
                "ROLE_LEARNER, /pages/home/"
        })
        @DisplayName("Happy Path: Đăng nhập thành công với 6 vai trò và điều hướng Dashboard chính xác")
        void login_happyPath_shouldSucceedAndRedirectCorrectDashboard(String roleName, String expectedDashboard) {
            // [Arrange] Given a valid active user with role and matching password
            String email = "user." + roleName.toLowerCase() + "@kizuna.vn";
            String rawPassword = "Password123@";
            User user = buildUser(email, roleName, UserAccountStatus.ACTIVE, false);

            LoginRequest request = LoginRequest.builder()
                    .email(email)
                    .password(rawPassword)
                    .build();

            when(userRepository.findByEmail(email.toLowerCase())).thenReturn(Optional.of(user));
            when(passwordEncoder.matches(rawPassword, user.getPasswordHash())).thenReturn(true);
            when(tokenProvider.generateToken(user)).thenReturn("mocked.jwt.token." + roleName);

            // [Act] When calling login
            LoginResponse response = authService.login(request, httpServletResponse);

            // [Assert] Then login succeeds, returns JWT, sets HTTP-only cookie, and redirects to role dashboard
            assertNotNull(response);
            assertEquals("mocked.jwt.token." + roleName, response.getToken());
            assertEquals(expectedDashboard, response.getRedirectUrl());
            assertFalse(response.isMustChangePassword());
            assertEquals(email, response.getUser().getEmail());

            ArgumentCaptor<Cookie> cookieCaptor = ArgumentCaptor.forClass(Cookie.class);
            verify(httpServletResponse, times(1)).addCookie(cookieCaptor.capture());
            Cookie cookie = cookieCaptor.getValue();
            assertEquals("access_token", cookie.getName());
            assertEquals("mocked.jwt.token." + roleName, cookie.getValue());
            assertTrue(cookie.isHttpOnly());
            assertEquals("/", cookie.getPath());
        }

        @Test
        @DisplayName("Edge Case: Người dùng đăng nhập có cờ mustChangePassword = true (Kích hoạt flow đổi mật khẩu bắt buộc)")
        void login_whenMustChangePasswordIsTrue_shouldReturnFlagInResponse() {
            // [Arrange] Given user with mustChangePassword = true
            String email = "firsttime@kizuna.vn";
            User user = buildUser(email, "ROLE_LEARNER", UserAccountStatus.ACTIVE, true);
            LoginRequest request = LoginRequest.builder().email(email).password("Pass123").build();

            when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
            when(passwordEncoder.matches("Pass123", user.getPasswordHash())).thenReturn(true);
            when(tokenProvider.generateToken(user)).thenReturn("token.mustchange");

            // [Act] When calling login
            LoginResponse response = authService.login(request, httpServletResponse);

            // [Assert] Then mustChangePassword in response should be true
            assertNotNull(response);
            assertTrue(response.isMustChangePassword());
            assertTrue(response.getUser().isMustChangePassword());
        }

        @Test
        @DisplayName("Abnormal Case: Email không tồn tại trong hệ thống -> Ném BadRequestException")
        void login_whenEmailNotFound_shouldThrowBadRequestException() {
            // [Arrange] Given email not registered
            LoginRequest request = LoginRequest.builder()
                    .email("unknown@kizuna.vn")
                    .password("Password123")
                    .build();
            when(userRepository.findByEmail("unknown@kizuna.vn")).thenReturn(Optional.empty());

            // [Act & Assert] When calling login -> Throws BadRequestException
            BadRequestException ex = assertThrows(BadRequestException.class,
                    () -> authService.login(request, httpServletResponse));
            assertEquals("Tài khoản hoặc mật khẩu không chính xác", ex.getMessage());
            verify(passwordEncoder, never()).matches(anyString(), anyString());
        }

        @Test
        @DisplayName("Abnormal Case: Mật khẩu nhập sai -> Ném BadRequestException")
        void login_whenPasswordMismatch_shouldThrowBadRequestException() {
            // [Arrange] Given valid user but incorrect password
            String email = "learner@kizuna.vn";
            User user = buildUser(email, "ROLE_LEARNER", UserAccountStatus.ACTIVE, false);
            LoginRequest request = LoginRequest.builder()
                    .email(email)
                    .password("WrongPassword999")
                    .build();

            when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
            when(passwordEncoder.matches("WrongPassword999", user.getPasswordHash())).thenReturn(false);

            // [Act & Assert] When calling login -> Throws BadRequestException
            BadRequestException ex = assertThrows(BadRequestException.class,
                    () -> authService.login(request, httpServletResponse));
            assertEquals("Tài khoản hoặc mật khẩu không chính xác", ex.getMessage());
            verify(tokenProvider, never()).generateToken(any());
        }

        @Test
        @DisplayName("Abnormal Case: Tài khoản bị khóa (BANNED/INACTIVE) -> Chặn đăng nhập với thông báo rõ ràng")
        void login_whenAccountNotActive_shouldThrowBadRequestException() {
            // [Arrange] Given user with status BANNED
            String email = "banned@kizuna.vn";
            User user = buildUser(email, "ROLE_LEARNER", UserAccountStatus.BANNED, false);
            LoginRequest request = LoginRequest.builder().email(email).password("Pass123").build();

            when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
            when(passwordEncoder.matches("Pass123", user.getPasswordHash())).thenReturn(true);

            // [Act & Assert] When calling login -> Throws BadRequestException
            BadRequestException ex = assertThrows(BadRequestException.class,
                    () -> authService.login(request, httpServletResponse));
            assertThat(ex.getMessage()).contains("Tài khoản đã bị tạm khóa");
            verify(tokenProvider, never()).generateToken(any());
        }
    }

    // =========================================================================
    // UC-02: ĐĂNG KÝ TÀI KHOẢN (GỬI OTP)
    // =========================================================================
    @Nested
    @DisplayName("UC-02: Đăng ký tài khoản - Gửi OTP (initiateRegistration)")
    class RegistrationInitiateTests {

        @Test
        @DisplayName("Happy Path: Gửi OTP thành công cho email chưa đăng ký")
        void initiateRegistration_happyPath_shouldGenerateOtpAndSendEmail() {
            // [Arrange] Given valid RegisterRequest for new user
            RegisterRequest request = RegisterRequest.builder()
                    .fullName("Yamada Taro")
                    .email("taro@gmail.com")
                    .password("SecurePass123")
                    .role("ROLE_LEARNER")
                    .targetJlpt("N3")
                    .build();

            when(userRepository.findByEmail("taro@gmail.com")).thenReturn(Optional.empty());
            when(passwordEncoder.encode("SecurePass123")).thenReturn("hashed-pwd-taro");
            when(otpService.generateAndSaveOtp(eq("taro@gmail.com"), eq("Yamada Taro"), eq("hashed-pwd-taro"), eq("ROLE_LEARNER"), eq("N3")))
                    .thenReturn("654321");

            // [Act] When calling initiateRegistration
            assertDoesNotThrow(() -> authService.initiateRegistration(request));

            // [Assert] Then OTP is generated and emailService is called
            verify(emailService, times(1)).sendRegistrationOtpEmail("taro@gmail.com", "Yamada Taro", "654321");
        }

        @Test
        @DisplayName("Abnormal Case: Email đã được sử dụng trong hệ thống -> Báo lỗi BadRequestException")
        void initiateRegistration_whenEmailAlreadyExists_shouldThrowBadRequestException() {
            // [Arrange] Given email already exists
            RegisterRequest request = RegisterRequest.builder()
                    .fullName("Duplicate User")
                    .email("exist@gmail.com")
                    .password("Pass1234")
                    .role("ROLE_LEARNER")
                    .build();

            when(userRepository.findByEmail("exist@gmail.com")).thenReturn(Optional.of(buildUser("exist@gmail.com", "ROLE_LEARNER", UserAccountStatus.ACTIVE, false)));

            // [Act & Assert] When calling initiateRegistration -> Throws BadRequestException
            BadRequestException ex = assertThrows(BadRequestException.class,
                    () -> authService.initiateRegistration(request));
            assertThat(ex.getMessage()).contains("Địa chỉ email này đã được đăng ký");
            verify(otpService, never()).generateAndSaveOtp(anyString(), anyString(), anyString(), anyString(), anyString());
            verify(emailService, never()).sendRegistrationOtpEmail(anyString(), anyString(), anyString());
        }
    }

    // =========================================================================
    // UC-02.OTP: XÁC THỰC OTP & KÍCH HOẠT TÀI KHOẢN (completeRegistration)
    // =========================================================================
    @Nested
    @DisplayName("UC-02.OTP: Xác thực OTP & Kích hoạt tài khoản (completeRegistration)")
    class CompleteRegistrationTests {

        @Test
        @DisplayName("Happy Path: Xác thực OTP thành công cho vai trò ROLE_LEARNER -> Tạo User & Learner profile")
        void completeRegistration_whenLearnerRole_shouldCreateUserAndLearnerProfile() {
            // [Arrange] Given valid VerifyOtpRequest and valid pending registration
            VerifyOtpRequest request = VerifyOtpRequest.builder()
                    .email("learner@gmail.com")
                    .otp("123456")
                    .build();

            PendingRegistration pending = PendingRegistration.builder()
                    .email("learner@gmail.com")
                    .fullName("Tran Van Hoc")
                    .passwordHash("hashed-pw-123")
                    .role("ROLE_LEARNER")
                    .targetJlpt("N2")
                    .build();

            Role learnerRole = Role.builder().roleName("ROLE_LEARNER").build();

            when(otpService.verifyOtp("learner@gmail.com", "123456")).thenReturn(pending);
            when(userRepository.findByEmail("learner@gmail.com")).thenReturn(Optional.empty());
            when(roleRepository.findByRoleName("ROLE_LEARNER")).thenReturn(Optional.of(learnerRole));
            when(userRepository.save(any(User.class))).thenAnswer(inv -> {
                User u = inv.getArgument(0);
                u.setUserId(java.util.UUID.randomUUID());
                return u;
            });
            when(tokenProvider.generateToken(any(User.class))).thenReturn("jwt.token.learner");

            // [Act] When completeRegistration is executed
            LoginResponse response = authService.completeRegistration(request);

            // [Assert] Then user is saved, Learner entity created, JWT token generated
            assertNotNull(response);
            assertEquals("jwt.token.learner", response.getToken());
            assertEquals("/pages/home/", response.getRedirectUrl());
            verify(learnerRepository, times(1)).save(any(Learner.class));
            verify(tutorRepository, never()).save(any(Tutor.class));
        }

        @Test
        @DisplayName("Happy Path: Xác thực OTP thành công cho vai trò ROLE_TUTOR -> Tạo User & Tutor profile APPROVED")
        void completeRegistration_whenTutorRole_shouldCreateUserAndTutorProfile() {
            // [Arrange] Given valid VerifyOtpRequest for ROLE_TUTOR
            VerifyOtpRequest request = VerifyOtpRequest.builder()
                    .email("tutor@gmail.com")
                    .otp("888999")
                    .build();

            PendingRegistration pending = PendingRegistration.builder()
                    .email("tutor@gmail.com")
                    .fullName("Le Sensei")
                    .passwordHash("hashed-pw-sensei")
                    .role("ROLE_TUTOR")
                    .build();

            Role tutorRole = Role.builder().roleName("ROLE_TUTOR").build();

            when(otpService.verifyOtp("tutor@gmail.com", "888999")).thenReturn(pending);
            when(userRepository.findByEmail("tutor@gmail.com")).thenReturn(Optional.empty());
            when(roleRepository.findByRoleName("ROLE_TUTOR")).thenReturn(Optional.of(tutorRole));
            when(userRepository.save(any(User.class))).thenAnswer(inv -> {
                User u = inv.getArgument(0);
                u.setUserId(java.util.UUID.randomUUID());
                return u;
            });
            when(tokenProvider.generateToken(any(User.class))).thenReturn("jwt.token.tutor");

            // [Act] When completeRegistration is called
            LoginResponse response = authService.completeRegistration(request);

            // [Assert] Then Tutor entity is created and saved
            assertNotNull(response);
            assertEquals("jwt.token.tutor", response.getToken());
            assertEquals("/tutor/dashboard", response.getRedirectUrl());
            verify(tutorRepository, times(1)).save(any(Tutor.class));
            verify(learnerRepository, never()).save(any(Learner.class));
        }

        @Test
        @DisplayName("Abnormal Case: Email đã được kích hoạt trước khi xác thực OTP -> Báo lỗi BadRequestException")
        void completeRegistration_whenEmailAlreadyRegistered_shouldThrowBadRequestException() {
            // [Arrange] Given user already present in DB
            VerifyOtpRequest request = VerifyOtpRequest.builder()
                    .email("existing@kizuna.vn")
                    .otp("112233")
                    .build();

            PendingRegistration pending = PendingRegistration.builder()
                    .email("existing@kizuna.vn")
                    .fullName("Existing User")
                    .role("ROLE_LEARNER")
                    .build();

            when(otpService.verifyOtp("existing@kizuna.vn", "112233")).thenReturn(pending);
            when(userRepository.findByEmail("existing@kizuna.vn")).thenReturn(Optional.of(buildUser("existing@kizuna.vn", "ROLE_LEARNER", UserAccountStatus.ACTIVE, false)));

            // [Act & Assert] When calling completeRegistration -> Throws BadRequestException
            BadRequestException ex = assertThrows(BadRequestException.class,
                    () -> authService.completeRegistration(request));
            assertThat(ex.getMessage()).contains("Tài khoản đã được đăng ký trước đó");
            verify(userRepository, never()).save(any());
        }
    }

    // =========================================================================
    // UC-03: ĐĂNG XUẤT (LOG OUT)
    // =========================================================================
    @Nested
    @DisplayName("UC-03: Đăng xuất hệ thống (logout)")
    class LogoutTests {

        @Test
        @DisplayName("Happy Path: Xóa Cookie access_token với maxAge = 0")
        void logout_happyPath_shouldClearAccessTokenCookie() {
            // [Arrange] HttpServletResponse is provided

            // [Act] When calling logout
            authService.logout(httpServletResponse);

            // [Assert] Then access_token cookie is expired with maxAge 0
            ArgumentCaptor<Cookie> cookieCaptor = ArgumentCaptor.forClass(Cookie.class);
            verify(httpServletResponse, times(1)).addCookie(cookieCaptor.capture());
            Cookie expiredCookie = cookieCaptor.getValue();
            assertEquals("access_token", expiredCookie.getName());
            assertEquals("", expiredCookie.getValue());
            assertEquals(0, expiredCookie.getMaxAge());
            assertEquals("/", expiredCookie.getPath());
        }

        @Test
        @DisplayName("Edge Case: Gọi logout với HttpServletResponse = null không gây NullPointerException")
        void logout_whenResponseIsNull_shouldNotThrow() {
            assertDoesNotThrow(() -> authService.logout(null));
        }
    }

    // =========================================================================
    // UC-04: ĐỔI MẬT KHẨU (CHANGE PASSWORD)
    // =========================================================================
    @Nested
    @DisplayName("UC-04: Đổi mật khẩu lần đầu (changePasswordFirstTime)")
    class ChangePasswordFirstTimeTests {

        @Test
        @DisplayName("Happy Path: Đổi mật khẩu lần đầu thành công -> Cập nhật hash mới và tắt mustChangePassword")
        void changePasswordFirstTime_happyPath_shouldUpdateHashAndClearFlag() {
            // [Arrange] Given user with mustChangePassword = true
            String email = "firsttime@kizuna.vn";
            User user = buildUser(email, "ROLE_LEARNER", UserAccountStatus.ACTIVE, true);

            FirstTimePasswordChangeRequest request = FirstTimePasswordChangeRequest.builder()
                    .currentPassword("TempPass123")
                    .newPassword("MyNewSecret999@")
                    .confirmPassword("MyNewSecret999@")
                    .build();

            when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
            when(passwordEncoder.matches("TempPass123", user.getPasswordHash())).thenReturn(true);
            when(passwordEncoder.encode("MyNewSecret999@")).thenReturn("hashed-new-secret");

            // [Act] When calling changePasswordFirstTime
            Map<String, Object> result = authService.changePasswordFirstTime(request, email);

            // [Assert] Then user mustChangePassword is false and passwordHash is updated
            assertNotNull(result);
            assertTrue((Boolean) result.get("success"));
            assertFalse(user.isMustChangePassword());
            assertEquals("hashed-new-secret", user.getPasswordHash());
            verify(userRepository, times(1)).save(user);
        }

        @Test
        @DisplayName("Abnormal Case: Mật khẩu mới và xác nhận mật khẩu không khớp -> Báo lỗi BadRequestException")
        void changePasswordFirstTime_whenNewPasswordMismatch_shouldThrowBadRequestException() {
            // [Arrange] Given request with mismatched confirm password
            FirstTimePasswordChangeRequest request = FirstTimePasswordChangeRequest.builder()
                    .currentPassword("TempPass123")
                    .newPassword("NewPass123")
                    .confirmPassword("MismatchPass456")
                    .build();

            // [Act & Assert] When calling changePasswordFirstTime -> Throws BadRequestException
            BadRequestException ex = assertThrows(BadRequestException.class,
                    () -> authService.changePasswordFirstTime(request, "user@kizuna.vn"));
            assertThat(ex.getMessage()).contains("Mật khẩu mới và mật khẩu xác nhận không trùng khớp");
            verify(userRepository, never()).save(any());
        }

        @Test
        @DisplayName("Abnormal Case: Mật khẩu tạm thời hiện tại nhập không chính xác -> Báo lỗi BadRequestException")
        void changePasswordFirstTime_whenCurrentPasswordWrong_shouldThrowBadRequestException() {
            // [Arrange] Given user and invalid current password
            String email = "learner@kizuna.vn";
            User user = buildUser(email, "ROLE_LEARNER", UserAccountStatus.ACTIVE, true);

            FirstTimePasswordChangeRequest request = FirstTimePasswordChangeRequest.builder()
                    .currentPassword("WrongTemp123")
                    .newPassword("ValidNewPass123")
                    .confirmPassword("ValidNewPass123")
                    .build();

            when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
            when(passwordEncoder.matches("WrongTemp123", user.getPasswordHash())).thenReturn(false);

            // [Act & Assert] When calling changePasswordFirstTime -> Throws BadRequestException
            BadRequestException ex = assertThrows(BadRequestException.class,
                    () -> authService.changePasswordFirstTime(request, email));
            assertThat(ex.getMessage()).contains("Mật khẩu tạm thời hiện tại không chính xác");
            verify(userRepository, never()).save(any());
        }
    }

    // =========================================================================
    // UC-05: ĐẶT LẠI MẬT KHẨU QUA MẬT KHẨU TẠM (resetPasswordWithTemporary)
    // =========================================================================
    @Nested
    @DisplayName("UC-05: Đặt lại mật khẩu với mật khẩu tạm (resetPasswordWithTemporary)")
    class ResetPasswordWithTemporaryTests {

        @Test
        @DisplayName("Happy Path: Khôi phục mật khẩu thành công bằng mật khẩu tạm cho Học viên/Gia sư")
        void resetPasswordWithTemporary_happyPath_shouldSucceed() {
            // [Arrange] Given valid request for learner
            String email = "learner@kizuna.vn";
            User user = buildUser(email, "ROLE_LEARNER", UserAccountStatus.ACTIVE, false);
            user.setPasswordHash("hashed-temp-pass");

            ResetPasswordRequest request = ResetPasswordRequest.builder()
                    .email(email)
                    .temporaryPassword("TempPass777")
                    .newPassword("BrandNewPass999")
                    .confirmPassword("BrandNewPass999")
                    .build();

            when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
            when(passwordEncoder.matches("TempPass777", "hashed-temp-pass")).thenReturn(true);
            when(passwordEncoder.encode("BrandNewPass999")).thenReturn("hashed-brand-new");

            // [Act] When calling resetPasswordWithTemporary
            assertDoesNotThrow(() -> authService.resetPasswordWithTemporary(request));

            // [Assert] Then user passwordHash is updated to hashed-brand-new
            assertEquals("hashed-brand-new", user.getPasswordHash());
            assertFalse(user.isMustChangePassword());
            verify(userRepository, times(1)).save(user);
        }

        @Test
        @DisplayName("Abnormal Case: Mật khẩu mới và mật khẩu xác nhận không khớp -> Ném BadRequestException")
        void resetPasswordWithTemporary_whenMismatchedPasswords_shouldThrowBadRequestException() {
            // [Arrange] Given mismatched new password
            ResetPasswordRequest request = ResetPasswordRequest.builder()
                    .email("learner@kizuna.vn")
                    .temporaryPassword("TempPass777")
                    .newPassword("PassA123")
                    .confirmPassword("PassB456")
                    .build();

            // [Act & Assert] When calling resetPasswordWithTemporary -> Throws BadRequestException
            BadRequestException ex = assertThrows(BadRequestException.class,
                    () -> authService.resetPasswordWithTemporary(request));
            assertThat(ex.getMessage()).contains("Mật khẩu mới và mật khẩu xác minh không trùng khớp");
            verify(userRepository, never()).save(any());
        }

        @Test
        @DisplayName("Abnormal Case: Mật khẩu tạm thời không khớp với hash lưu trong CSDL -> Ném BadRequestException")
        void resetPasswordWithTemporary_whenTemporaryPasswordIncorrect_shouldThrowBadRequestException() {
            // [Arrange] Given wrong temporary password
            String email = "learner@kizuna.vn";
            User user = buildUser(email, "ROLE_LEARNER", UserAccountStatus.ACTIVE, false);

            ResetPasswordRequest request = ResetPasswordRequest.builder()
                    .email(email)
                    .temporaryPassword("WrongTemp")
                    .newPassword("ValidNewPass123")
                    .confirmPassword("ValidNewPass123")
                    .build();

            when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
            when(passwordEncoder.matches("WrongTemp", user.getPasswordHash())).thenReturn(false);

            // [Act & Assert] When calling resetPasswordWithTemporary -> Throws BadRequestException
            BadRequestException ex = assertThrows(BadRequestException.class,
                    () -> authService.resetPasswordWithTemporary(request));
            assertThat(ex.getMessage()).contains("Email hoặc mật khẩu được cấp không chính xác");
            verify(userRepository, never()).save(any());
        }
    }
}
