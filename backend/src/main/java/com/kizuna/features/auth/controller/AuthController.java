package com.kizuna.features.auth.controller;

import com.kizuna.core.entity.*;
import com.kizuna.core.exception.BadRequestException;
import com.kizuna.core.repository.LearnerRepository;
import com.kizuna.core.repository.RoleRepository;
import com.kizuna.core.repository.TutorRepository;
import com.kizuna.core.repository.UserRepository;
import com.kizuna.core.security.JwtTokenProvider;
import com.kizuna.features.auth.dto.request.*;
import com.kizuna.features.auth.dto.response.LoginResponse;
import com.kizuna.features.auth.dto.response.PendingRegistration;
import com.kizuna.features.auth.dto.response.UserSummaryDto;
import com.kizuna.features.auth.service.AuthService;
import com.kizuna.features.auth.service.EmailService;
import com.kizuna.features.auth.service.OtpService;
import com.kizuna.features.payment.entity.TutorMembershipStatus;
import com.kizuna.features.verification.entity.TutorVerificationStatus;
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

@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final OtpService otpService;
    private final EmailService emailService;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final LearnerRepository learnerRepository;
    private final TutorRepository tutorRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    @PostMapping("/register/send-otp")
    public ResponseEntity<Map<String, Object>> sendRegistrationOtp(@Valid @RequestBody RegisterRequest request) {
        String cleanEmail = request.getEmail().trim().toLowerCase();

        if (userRepository.findByEmail(cleanEmail).isPresent()) {
            throw new BadRequestException("Địa chỉ email này đã được đăng ký trong hệ thống. Vui lòng đăng nhập.");
        }

        String passwordHash = passwordEncoder.encode(request.getPassword());
        String otp = otpService.generateAndSaveOtp(
                cleanEmail,
                request.getFullName(),
                passwordHash,
                request.getRole(),
                request.getTargetJlpt()
        );

        emailService.sendRegistrationOtpEmail(cleanEmail, request.getFullName(), otp);

        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Mã xác thực OTP gồm 6 chữ số đã được gửi đến hộp thư Gmail của bạn.",
                "email", cleanEmail,
                "expiresInSeconds", 300
        ));
    }

    @PostMapping("/register/verify-otp")
    @Transactional
    public ResponseEntity<LoginResponse> verifyRegistrationOtp(@Valid @RequestBody VerifyOtpRequest request,
                                                               HttpServletResponse response) {
        String cleanEmail = request.getEmail().trim().toLowerCase();

        PendingRegistration pending = otpService.verifyOtp(cleanEmail, request.getOtp());

        if (userRepository.findByEmail(cleanEmail).isPresent()) {
            throw new BadRequestException("Tài khoản đã được đăng ký trước đó. Vui lòng đăng nhập.");
        }

        String targetRoleName = (pending.getRole() != null && !pending.getRole().isBlank())
                ? pending.getRole()
                : "ROLE_LEARNER";

        Role role = roleRepository.findByRoleName(targetRoleName)
                .orElseGet(() -> roleRepository.findByRoleName("ROLE_LEARNER")
                        .orElseThrow(() -> new IllegalStateException("Không tìm thấy vai trò " + targetRoleName)));

        User user = User.builder()
                .email(cleanEmail)
                .fullName(pending.getFullName())
                .passwordHash(pending.getPasswordHash())
                .role(role)
                .accountStatus(UserAccountStatus.ACTIVE)
                .build();

        user = userRepository.save(user);

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
        } else {
            Learner learner = Learner.builder()
                    .user(user)
                    .currentLevel("N5")
                    .targetLevel(pending.getTargetJlpt() != null ? pending.getTargetJlpt() : "N3")
                    .learningGoals("Chinh phục mục tiêu JLPT cùng KizunaTutor")
                    .updatedAt(OffsetDateTime.now())
                    .build();
            learnerRepository.save(learner);
        }

        String token = jwtTokenProvider.generateToken(user);
        RoleEnum roleEnum = RoleEnum.fromString(role.getRoleName());
        String redirectUrl = roleEnum.getDashboardRoute();

        if (response != null) {
            Cookie cookie = new Cookie("access_token", token);
            cookie.setPath("/");
            cookie.setHttpOnly(true);
            cookie.setMaxAge(86400);
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

        return ResponseEntity.ok(LoginResponse.builder()
                .token(token)
                .user(userSummary)
                .redirectUrl(redirectUrl)
                .message("Xác thực OTP thành công! Chào mừng bạn gia nhập KizunaTutor.")
                .build());
    }

    @PostMapping("/register/resend-otp")
    public ResponseEntity<Map<String, Object>> resendRegistrationOtp(@Valid @RequestBody ResendOtpRequest request) {
        String cleanEmail = request.getEmail().trim().toLowerCase();

        if (userRepository.findByEmail(cleanEmail).isPresent()) {
            throw new BadRequestException("Tài khoản này đã được kích hoạt thành công. Vui lòng chuyển sang trang đăng nhập.");
        }

        String newOtp = otpService.resendOtp(cleanEmail);
        PendingRegistration pending = otpService.getPendingRegistration(cleanEmail);
        String fullName = (pending != null) ? pending.getFullName() : "Quý học viên";

        emailService.sendRegistrationOtpEmail(cleanEmail, fullName, newOtp);

        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Mã xác thực OTP mới đã được gửi lại vào email của bạn.",
                "email", cleanEmail,
                "expiresInSeconds", 300
        ));
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request,
                                               HttpServletResponse response) {
        LoginResponse loginResponse = authService.login(request, response);
        return ResponseEntity.ok(loginResponse);
    }

    @PostMapping("/register")
    public ResponseEntity<LoginResponse> register(@Valid @RequestBody RegisterRequest request,
                                                  HttpServletResponse response) {
        LoginResponse registerResponse = authService.register(request, response);
        return ResponseEntity.ok(registerResponse);
    }

    @GetMapping("/mock-google-login")
    public void mockGoogleLogin(@RequestParam(required = false, defaultValue = "google.learner@kizuna.vn") String email,
                                @RequestParam(required = false, defaultValue = "Học Viên Google Kizuna") String name,
                                @RequestParam(required = false, defaultValue = "ROLE_LEARNER") String role,
                                HttpServletResponse response) throws IOException {
        String cleanEmail = email.trim().toLowerCase();
        User user = userRepository.findByEmail(cleanEmail).orElse(null);

        if (user == null) {
            Role userRole = roleRepository.findByRoleName(role)
                    .orElseGet(() -> roleRepository.findByRoleName("ROLE_LEARNER")
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

        String redirectUrl = UriComponentsBuilder.fromUriString("/login")
                .queryParam("token", token)
                .queryParam("role", roleName)
                .queryParam("email", URLEncoder.encode(cleanEmail, StandardCharsets.UTF_8))
                .queryParam("name", URLEncoder.encode(user.getFullName(), StandardCharsets.UTF_8))
                .build().toUriString();

        response.sendRedirect(redirectUrl);
    }

    @PostMapping("/change-password-first-time")
    public ResponseEntity<Map<String, Object>> changePasswordFirstTime(
            @Valid @RequestBody FirstTimePasswordChangeRequest request,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        String token = (authHeader != null && authHeader.startsWith("Bearer "))
                ? authHeader.substring(7)
                : null;

        if (token == null || !jwtTokenProvider.validateToken(token)) {
            throw new BadRequestException("Phiên làm việc không hợp lệ hoặc đã hết hạn. Vui lòng đăng nhập lại.");
        }

        String email = jwtTokenProvider.getEmailFromToken(token);
        Map<String, Object> result = authService.changePasswordFirstTime(request, email);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/logout")
    public ResponseEntity<Map<String, Object>> logout(HttpServletResponse response) {
        authService.logout(response);
        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Đăng xuất thành công",
                "redirectUrl", "/login"
        ));
    }
}
