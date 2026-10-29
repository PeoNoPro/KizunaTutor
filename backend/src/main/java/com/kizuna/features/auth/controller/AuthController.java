package com.kizuna.features.auth.controller;


import com.kizuna.core.security.*;
import com.kizuna.features.auth.dto.*;
import com.kizuna.core.entity.*;
import com.kizuna.features.classroom.entity.*;
import com.kizuna.features.learning_support.entity.*;
import com.kizuna.features.payment.entity.*;
import com.kizuna.features.progress.entity.*;
import com.kizuna.features.verification.entity.*;
import com.kizuna.features.tutor_search.entity.*;
import com.kizuna.features.ticket.entity.*;
import com.kizuna.features.notification.entity.*;
import com.kizuna.features.booking.entity.*;
import com.kizuna.features.admin.entity.*;
import com.kizuna.core.exception.BadRequestException;
import com.kizuna.core.repository.LearnerRepository;
import com.kizuna.core.repository.RoleRepository;
import com.kizuna.core.repository.TutorRepository;
import com.kizuna.core.repository.UserRepository;
import com.kizuna.features.auth.service.IAuthService;
import com.kizuna.features.auth.service.IEmailService;
import com.kizuna.features.auth.service.IOtpService;
import com.kizuna.core.security.JwtTokenProvider;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * REST Controller tiếp nhận các yêu cầu xác thực tài khoản:
 * 1. Gửi mã OTP xác thực đăng ký qua Gmail (POST /api/auth/register/send-otp)
 * 2. Xác thực OTP và kích hoạt tài khoản chính thức (POST /api/auth/register/verify-otp)
 * 3. Gửi lại mã OTP (POST /api/auth/register/resend-otp)
 * 4. Đăng nhập truyền thống & Mock Google OAuth2
 * 5. Thông tin phiên hiện tại & Đăng xuất
 */
@Slf4j
@RestController
@RequiredArgsConstructor
public class AuthController {

    private final IAuthService authService;
    private final IOtpService otpService;
    private final IEmailService emailService;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final LearnerRepository learnerRepository;
    private final TutorRepository tutorRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final com.kizuna.core.security.SecurityContextUtil securityContextUtil;

    /**
     * BƯỚC 1: Tiếp nhận thông tin đăng ký, băm mật khẩu, sinh OTP 6 số và gửi Gmail bất đồng bộ.
     */
    @PostMapping("/api/auth/register/send-otp")
    public ResponseEntity<Map<String, Object>> sendRegistrationOtp(@Valid @RequestBody RegisterRequest request) {
        String cleanEmail = request.getEmail().trim().toLowerCase();

        // 1. Kiểm tra email đã tồn tại hay chưa
        if (userRepository.findByEmail(cleanEmail).isPresent()) {
            throw new BadRequestException("Địa chỉ email '" + cleanEmail + "' đã được đăng ký trong hệ thống. Vui lòng đăng nhập.");
        }

        // 2. Băm mật khẩu bằng BCrypt
        String passwordHash = passwordEncoder.encode(request.getPassword());

        // 3. Sinh mã OTP 6 số và lưu vào cache với TTL 300s (5 phút)
        String otp = otpService.generateAndSaveOtp(
                cleanEmail,
                request.getFullName(),
                passwordHash,
                request.getRole(),
                request.getTargetJlpt()
        );

        // 4. Gửi email bất đồng bộ qua SMTP Gmail (kizunatutor@gmail.com)
        emailService.sendRegistrationOtpEmail(cleanEmail, request.getFullName(), otp);

        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Mã xác thực OTP gồm 6 chữ số đã được gửi đến hộp thư Gmail của bạn.",
                "email", cleanEmail,
                "expiresInSeconds", 300
        ));
    }

    /**
     * BƯỚC 2: Xác thực mã OTP 6 chữ số, lưu người dùng vào DB với ACTIVE, cấp JWT và chuyển hướng.
     */
    @PostMapping("/api/auth/register/verify-otp")
    @Transactional
    public ResponseEntity<LoginResponse> verifyRegistrationOtp(@Valid @RequestBody VerifyOtpRequest request,
                                                               HttpServletResponse response) {
        String cleanEmail = request.getEmail().trim().toLowerCase();

        // 1. Xác thực OTP từ Cache (Kiểm tra TTL và số lần thử sai)
        PendingRegistration pending = otpService.verifyOtp(cleanEmail, request.getOtp());

        // 2. Đề phòng trùng lặp email phát sinh trong quá trình chờ
        if (userRepository.findByEmail(cleanEmail).isPresent()) {
            throw new BadRequestException("Tài khoản đã được đăng ký trước đó. Vui lòng đăng nhập.");
        }

        // 3. Xác định Role Entity
        String targetRoleName = (pending.getRole() != null && !pending.getRole().isBlank())
                ? pending.getRole()
                : RoleEnum.ROLE_LEARNER.name();

        Role role = roleRepository.findByRoleName(targetRoleName)
                .orElseGet(() -> roleRepository.findByRoleName(RoleEnum.ROLE_LEARNER.name())
                        .orElseThrow(() -> new IllegalStateException("Không tìm thấy vai trò " + targetRoleName)));

        // 4. Tạo User chính thức với trạng thái ACTIVE
        User user = User.builder()
                .email(cleanEmail)
                .fullName(pending.getFullName())
                .passwordHash(pending.getPasswordHash())
                .role(role)
                .accountStatus(UserAccountStatus.ACTIVE)
                .build();

        user = userRepository.save(user);

        // 5. Khởi tạo hồ sơ bổ trợ theo từng Actor (Học viên hoặc Gia sư)
        if (RoleEnum.ROLE_TUTOR.name().equals(role.getRoleName())) {
            Tutor tutor = Tutor.builder()
                    .user(user)
                    .headline("Gia sư tiếng Nhật Kizuna")
                    .bio("Gia sư tiếng Nhật đồng hành cùng học viên tại nền tảng KizunaTutor.")
                    .experienceYears((short) 1)
                    .hourlyRate(BigDecimal.valueOf(200000))
                    .highestJlptLevel("N2")
                    .avgRating(BigDecimal.valueOf(5.0))
                    .totalReviews(0)
                    .verificationStatus(TutorVerificationStatus.APPROVED)
                    .membershipStatus(TutorMembershipStatus.PREMIUM)
                    .isFeatured(false)
                    .updatedAt(OffsetDateTime.now())
                    .build();
            tutorRepository.save(tutor);
            log.info("[AuthController] Đã khởi tạo hồ sơ Gia sư cho [{}]", cleanEmail);
        } else {
            String targetLevel = (pending.getTargetJlpt() != null && !pending.getTargetJlpt().isBlank())
                    ? pending.getTargetJlpt()
                    : "N5";

            Learner learner = Learner.builder()
                    .user(user)
                    .currentLevel("N5")
                    .targetLevel(targetLevel)
                    .learningGoals("Chinh phục mục tiêu JLPT " + targetLevel + " cùng KizunaTutor")
                    .updatedAt(OffsetDateTime.now())
                    .build();
            learnerRepository.save(learner);
            log.info("[AuthController] Đã khởi tạo hồ sơ Học viên cho [{}]", cleanEmail);
        }

        // 6. Cấp phát JWT Access Token
        String token = jwtTokenProvider.generateToken(user);
        RoleEnum roleEnum = RoleEnum.fromString(role.getRoleName());
        String redirectUrl = roleEnum.getDashboardRoute();

        // 7. Cấu hình Cookie access_token bảo mật
        if (response != null) {
            Cookie cookie = new Cookie("access_token", token);
            cookie.setPath("/");
            cookie.setHttpOnly(true);
            cookie.setMaxAge(86400); // 24 giờ
            response.addCookie(cookie);
        }

        UserSummaryDto userSummary = UserSummaryDto.builder()
                .id(user.getUserId())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .role(roleEnum.getRoleName())
                .roleBadge(roleEnum.getBadgeText())
                .avatarUrl(user.getAvatarUrl())
                .accountStatus(user.getAccountStatus().name())
                .build();

        log.info("[AuthController] Xác thực OTP thành công, đã kích hoạt tài khoản [{}] vai trò [{}], điều hướng đến [{}]",
                cleanEmail, roleEnum.getRoleName(), redirectUrl);

        return ResponseEntity.ok(LoginResponse.builder()
                .token(token)
                .user(userSummary)
                .redirectUrl(redirectUrl)
                .message("Xác thực OTP thành công! Chào mừng bạn gia nhập KizunaTutor.")
                .build());
    }

    /**
     * BƯỚC 3: Gửi lại mã OTP mới nếu mã cũ thất lạc (Cooldown 60s).
     */
    @PostMapping("/api/auth/register/resend-otp")
    public ResponseEntity<Map<String, Object>> resendRegistrationOtp(@Valid @RequestBody ResendOtpRequest request) {
        String cleanEmail = request.getEmail().trim().toLowerCase();

        if (userRepository.findByEmail(cleanEmail).isPresent()) {
            throw new BadRequestException("Tài khoản này đã được kích hoạt thành công. Vui lòng chuyển sang trang đăng nhập.");
        }

        String newOtp = otpService.resendOtp(cleanEmail);
        PendingRegistration pending = otpService.getPendingRegistration(cleanEmail);
        String fullName = (pending != null) ? pending.getFullName() : "Quý học viên";

        // Gửi lại email qua SMTP Gmail
        emailService.sendRegistrationOtpEmail(cleanEmail, fullName, newOtp);

        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Mã xác thực OTP mới đã được gửi lại vào email của bạn.",
                "email", cleanEmail,
                "expiresInSeconds", 300
        ));
    }

    /**
     * Endpoint Đăng nhập truyền thống: Trả về Token JWT, thông tin người dùng và URL Dashboard đích.
     */
    @PostMapping({"/api/auth/login", "/api/v1/auth/login"})
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request,
                                               HttpServletResponse response) {
        LoginResponse loginResponse = authService.login(request, response);
        return ResponseEntity.ok(loginResponse);
    }

    /**
     * Endpoint Đăng ký trực tiếp (Dành cho API khách hoặc fallback): Tạo tài khoản mới, cấp JWT và chuyển hướng.
     */
    @PostMapping({"/api/auth/register", "/api/v1/auth/register"})
    public ResponseEntity<LoginResponse> register(@Valid @RequestBody RegisterRequest request,
                                                  HttpServletResponse response) {
        LoginResponse registerResponse = authService.register(request, response);
        return ResponseEntity.ok(registerResponse);
    }

    /**
     * Endpoint Mock Đăng nhập Google (Dành cho kiểm thử nhanh luồng OAuth2 không cần Google Cloud Console).
     */
    @GetMapping({"/api/auth/mock-google-login", "/api/v1/auth/mock-google-login"})
    public void mockGoogleLogin(@RequestParam(required = false, defaultValue = "google.learner@kizuna.vn") String email,
                                @RequestParam(required = false, defaultValue = "Học Viên Google Kizuna") String name,
                                @RequestParam(required = false, defaultValue = "ROLE_LEARNER") String role,
                                HttpServletResponse response) throws IOException {
        String cleanEmail = email.trim().toLowerCase();
        User user = userRepository.findByEmail(cleanEmail).orElse(null);

        if (user == null) {
            Role userRole = roleRepository.findByRoleName(role)
                    .orElseGet(() -> roleRepository.findByRoleName(RoleEnum.ROLE_LEARNER.name())
                            .orElseThrow(() -> new IllegalStateException("Không tìm thấy vai trò")));

            user = User.builder()
                    .email(cleanEmail)
                    .fullName(name)
                    .avatarUrl("https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=150")
                    .passwordHash(passwordEncoder.encode(UUID.randomUUID().toString()))
                    .role(userRole)
                    .accountStatus(UserAccountStatus.ACTIVE)
                    .build();

            user = userRepository.save(user);

            if (RoleEnum.ROLE_LEARNER.name().equals(role)) {
                Learner learner = Learner.builder()
                        .user(user)
                        .currentLevel("N5")
                        .targetLevel("N3")
                        .learningGoals("Luyện Kanji & Ngữ pháp cùng KizunaTutor qua Gmail")
                        .updatedAt(OffsetDateTime.now())
                        .build();
                learnerRepository.save(learner);
            }
        }

        String token = jwtTokenProvider.generateToken(user);
        String roleName = user.getRole().getRoleName();

        Cookie cookie = new Cookie("access_token", token);
        cookie.setPath("/");
        cookie.setHttpOnly(true);
        cookie.setMaxAge(86400);
        response.addCookie(cookie);

        String redirectUrl = UriComponentsBuilder.fromUriString("/oauth2/redirect")
                .queryParam("token", token)
                .queryParam("role", roleName)
                .queryParam("email", URLEncoder.encode(cleanEmail, StandardCharsets.UTF_8))
                .queryParam("name", URLEncoder.encode(user.getFullName(), StandardCharsets.UTF_8))
                .build().toUriString();

        response.sendRedirect(redirectUrl);
    }

    /**
     * Lấy thông tin tài khoản đang đăng nhập.
     */
    @GetMapping({"/api/auth/me", "/api/v1/auth/me"})
    public ResponseEntity<UserSummaryDto> getCurrentUser() {
        return ResponseEntity.ok(authService.getCurrentUser());
    }

    /**
     * Endpoint Đổi mật khẩu lần đầu:
     * Dành cho nhân sự được Admin cấp tài khoản với cờ mustChangePassword = true.
     */
    @PostMapping({"/api/auth/change-password-first-time", "/api/v1/auth/change-password-first-time"})
    public ResponseEntity<Map<String, Object>> changePasswordFirstTime(
            @Valid @RequestBody com.kizuna.features.auth.dto.FirstTimePasswordChangeRequest request,
            org.springframework.security.core.Authentication authentication) {

        String userEmail = securityContextUtil.getCurrentUser()
                .map(User::getEmail)
                .orElseGet(() -> {
                    if (authentication != null) {
                        Object principal = authentication.getPrincipal();
                        if (principal instanceof User) {
                            return ((User) principal).getEmail();
                        } else if (principal instanceof String && !((String) principal).startsWith("com.kizuna.")) {
                            return (String) principal;
                        }
                    }
                    return null;
                });

        if (userEmail == null) {
            throw new BadRequestException("Phiên làm việc không hợp lệ hoặc đã hết hạn. Vui lòng đăng nhập lại.");
        }

        Map<String, Object> result = authService.changePasswordFirstTime(request, userEmail);
        return ResponseEntity.ok(result);
    }

    /**
     * Endpoint Đăng xuất: Xóa cookie xác thực và hủy ngữ cảnh phiên.
     */
    @PostMapping({"/api/auth/logout", "/api/v1/auth/logout"})
    public ResponseEntity<Map<String, Object>> logout(HttpServletResponse response) {
        authService.logout(response);
        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Đăng xuất thành công",
                "redirectUrl", "/login"
        ));
    }
}
