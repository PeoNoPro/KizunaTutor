package com.kizuna.features.auth.service.impl;

import com.kizuna.core.entity.*;
import com.kizuna.core.exception.BadRequestException;
import com.kizuna.core.exception.BusinessException;
import com.kizuna.core.exception.ResourceNotFoundException;
import com.kizuna.core.exception.ServiceUnavailableException;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.EnumSet;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.time.Duration;
import java.time.Instant;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import java.io.UnsupportedEncodingException;
import java.security.SecureRandom;
import com.kizuna.core.repository.LearnerRepository;
import com.kizuna.core.repository.RoleRepository;
import com.kizuna.core.repository.TutorRepository;
import com.kizuna.core.repository.UserRepository;
import com.kizuna.core.security.JwtTokenProvider;
import com.kizuna.features.auth.dto.request.FirstTimePasswordChangeRequest;
import com.kizuna.features.auth.dto.request.LoginRequest;
import com.kizuna.features.auth.dto.request.RegisterRequest;
import com.kizuna.features.auth.dto.response.LoginResponse;
import com.kizuna.features.auth.dto.response.UserSummaryDto;
import com.kizuna.features.auth.service.AuthService;
import com.kizuna.features.auth.service.OtpService;
import com.kizuna.features.auth.service.EmailService;
import com.kizuna.features.auth.dto.request.ResetPasswordRequest;
import com.kizuna.features.auth.dto.request.VerifyOtpRequest;
import com.kizuna.features.auth.dto.response.PendingRegistration;
import com.kizuna.features.payment.entity.TutorMembershipStatus;
import com.kizuna.features.verification.entity.TutorVerificationStatus;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final LearnerRepository learnerRepository;
    private final TutorRepository tutorRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;
    private final OtpService otpService;
    private final EmailService emailService;
    private final JavaMailSender mailSender;

    @Value("${app.mail.sender-email:${spring.mail.username:kizunatutor@gmail.com}}")
    private String senderEmail;

    @Value("${app.mail.sender-name:KizunaTutor - Nền Tảng Gia Sư & Học Tiếng Nhật}")
    private String senderName;

    private static final String PASSWORD_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789";
    private static final SecureRandom PASSWORD_RANDOM = new SecureRandom();
    private static final int TEMP_PASSWORD_LENGTH = 10;
    private static final int FORGOT_RATE_LIMIT_MAX_ATTEMPTS = 3;
    private static final Duration FORGOT_RATE_LIMIT_WINDOW = Duration.ofMinutes(15);

    private final Map<String, Deque<Instant>> forgotPasswordAttempts = new ConcurrentHashMap<>();

    private void enforceForgotPasswordRateLimit(String cleanEmail) {
        Instant now = Instant.now();
        Instant windowStart = now.minus(FORGOT_RATE_LIMIT_WINDOW);

        if (forgotPasswordAttempts.size() > 5000) {
            forgotPasswordAttempts.entrySet().removeIf(e -> {
                synchronized (e.getValue()) {
                    while (!e.getValue().isEmpty() && e.getValue().peekFirst().isBefore(windowStart)) {
                        e.getValue().pollFirst();
                    }
                    return e.getValue().isEmpty();
                }
            });
        }

        Deque<Instant> timestamps = forgotPasswordAttempts.computeIfAbsent(cleanEmail, k -> new ArrayDeque<>());
        synchronized (timestamps) {
            while (!timestamps.isEmpty() && timestamps.peekFirst().isBefore(windowStart)) {
                timestamps.pollFirst();
            }
            if (timestamps.size() >= FORGOT_RATE_LIMIT_MAX_ATTEMPTS) {
                log.warn("[SECURITY] Vượt giới hạn tần suất yêu cầu Quên mật khẩu cho: {}", cleanEmail);
                throw new BusinessException(429,
                        "Bạn đã yêu cầu khôi phục mật khẩu quá nhiều lần. Vui lòng thử lại sau ít phút.");
            }
            timestamps.addLast(now);
        }
    }

    private static final Set<RoleEnum> FORGOT_PASSWORD_RESTRICTED_ROLES = EnumSet.of(
            RoleEnum.ROLE_ADMIN,
            RoleEnum.ROLE_MANAGER,
            RoleEnum.ROLE_CS_STAFF,
            RoleEnum.ROLE_ACADEMIC_OFFICER
    );

    private void ensureForgotPasswordAllowed(User user) {
        if (user.getRole() == null) return;
        RoleEnum roleEnum = RoleEnum.fromString(user.getRole().getRoleName());
        if (FORGOT_PASSWORD_RESTRICTED_ROLES.contains(roleEnum)) {
            log.warn("[SECURITY] Chặn thao tác Quên mật khẩu đối với tài khoản nội bộ: {} ({})",
                    user.getEmail(), roleEnum.getRoleName());
            throw new BusinessException(403,
                    "Tài khoản quản trị và nhân sự nội bộ (Admin, Manager, CS Staff, Academic Officer) không thể tự thực hiện quên mật khẩu. Vui lòng liên hệ Quản trị viên để được hỗ trợ.");
        }
    }

    private String generateRandomPassword(int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(PASSWORD_CHARS.charAt(PASSWORD_RANDOM.nextInt(PASSWORD_CHARS.length())));
        }
        return sb.toString();
    }

    @Override
    @Transactional
    public void resetPasswordAndSendEmail(String email) {
        String cleanEmail = email.trim().toLowerCase();

        // 1. Giới hạn tần suất yêu cầu nhằm chống spam và dò tìm tài khoản
        enforceForgotPasswordRateLimit(cleanEmail);

        // 2. Tránh tiết lộ email có tồn tại hay không: nếu không tìm thấy tài khoản vẫn kết thúc bình thường để trả thông báo chung
        Optional<User> userOpt = userRepository.findByEmail(cleanEmail);
        if (userOpt.isEmpty()) {
            log.debug("[FORGOT PASSWORD] Yêu cầu với email chưa đăng ký (trả thông báo chung): {}", cleanEmail);
            return;
        }
        User user = userOpt.get();

        // 3. Bảo mật: Cấm nhân sự nội bộ / quản trị viên tự khôi phục mật khẩu
        ensureForgotPasswordAllowed(user);

        // 2. Sinh mật khẩu ngẫu nhiên và cập nhật mã hóa vào CSDL
        // (@Transactional sẽ tự động rollback nếu bước gửi email bên dưới gặp lỗi)
        String randomPassword = generateRandomPassword(TEMP_PASSWORD_LENGTH);
        user.setPasswordHash(passwordEncoder.encode(randomPassword));
        userRepository.save(user);

        // 3. Gửi email chứa mật khẩu ngẫu nhiên qua JavaMailSender (không nuốt lỗi)
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(senderEmail, senderName);
            helper.setTo(cleanEmail);
            helper.setSubject("[KizunaTutor] Mật khẩu đăng nhập mới của bạn");

            String displayName = (user.getFullName() != null && !user.getFullName().isBlank())
                    ? user.getFullName()
                    : "Học viên";

            String htmlContent = """
                    <div style="font-family: Arial, sans-serif; max-width: 560px; margin: 0 auto; padding: 24px; background-color: #FFE5EC; border: 3px solid #0F172A; border-radius: 12px;">
                      <h2 style="color: #0F172A; margin-top: 0;">Cấp lại mật khẩu - KizunaTutor</h2>
                      <p style="color: #0F172A;">Xin chào <strong>%s</strong>,</p>
                      <p style="color: #0F172A;">Hệ thống đã tạo một mật khẩu ngẫu nhiên mới cho tài khoản của bạn. Bạn có thể dùng mật khẩu bên dưới để đăng nhập trực tiếp vào hệ thống:</p>
                      <div style="background: #FFFFFF; border: 2px solid #0F172A; padding: 14px 20px; text-align: center; font-size: 24px; font-weight: 800; letter-spacing: 3px; color: #E0399A; margin: 20px 0; font-family: monospace;">
                        %s
                      </div>
                      <p style="color: #0F172A; font-size: 13px;">
                        Khuyến nghị: Sau khi đăng nhập thành công, hãy đổi lại mật khẩu cá nhân để đảm bảo an toàn tài khoản.
                      </p>
                    </div>
                    """.formatted(displayName, randomPassword);

            helper.setText(htmlContent, true);
            mailSender.send(message);

            log.info("[FORGOT PASSWORD] Đã cấp mật khẩu mới và gửi email tới: {}", cleanEmail);
        } catch (MessagingException | MailException | UnsupportedEncodingException ex) {
            log.error("[FORGOT PASSWORD] Gửi email mật khẩu mới thất bại tới {}: {}", cleanEmail, ex.getMessage(), ex);
            throw new ServiceUnavailableException(
                    "Không thể gửi email chứa mật khẩu mới lúc này. Vui lòng thử lại sau.", ex);
        }
    }

    @Override
    @Transactional
    public void resetPasswordWithTemporary(ResetPasswordRequest request) {
        String cleanEmail = request.getEmail().trim().toLowerCase();

        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new BadRequestException("Mật khẩu mới và mật khẩu xác minh không trùng khớp.");
        }

        User user = userRepository.findByEmail(cleanEmail)
                .orElseThrow(() -> new BadRequestException(
                        "Email hoặc mật khẩu được cấp không chính xác. Vui lòng kiểm tra lại."));

        ensureForgotPasswordAllowed(user);

        if (!passwordEncoder.matches(request.getTemporaryPassword(), user.getPasswordHash())) {
            throw new BadRequestException("Email hoặc mật khẩu được cấp không chính xác. Vui lòng kiểm tra lại.");
        }

        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        user.setMustChangePassword(false);
        userRepository.save(user);

        log.info("[RESET PASSWORD] Đã xác minh mật khẩu tạm và cập nhật mật khẩu mới cho: {}", cleanEmail);
    }

    @Override
    public void initiateRegistration(RegisterRequest request) {
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
    }

    @Override
    @Transactional
    public LoginResponse completeRegistration(VerifyOtpRequest request) {
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
                    // Gia sư mới chưa nộp hồ sơ xác minh và chưa mua gói hội viên
                    .verificationStatus(TutorVerificationStatus.UNVERIFIED)
                    .membershipStatus(TutorMembershipStatus.FREE)
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

        String token = tokenProvider.generateToken(user);
        RoleEnum roleEnum = RoleEnum.fromString(role.getRoleName());
        String redirectUrl = roleEnum.getDashboardRoute();

        UserSummaryDto userSummary = UserSummaryDto.builder()
                .id(user.getUserId())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .role(roleEnum.getRoleName())
                .roleBadge(roleEnum.getBadgeText())
                .avatarUrl(user.getAvatarUrl())
                .accountStatus(user.getAccountStatus().name())
                .build();

        return LoginResponse.builder()
                .token(token)
                .user(userSummary)
                .redirectUrl(redirectUrl)
                .message("Xác thực OTP thành công! Chào mừng bạn gia nhập KizunaTutor.")
                .build();
    }
    @Override
    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request, HttpServletResponse response) {
        String email = request.getEmail().trim().toLowerCase();

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BadRequestException("Tài khoản hoặc mật khẩu không chính xác"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new BadRequestException("Tài khoản hoặc mật khẩu không chính xác");
        }

        if (user.getAccountStatus() != UserAccountStatus.ACTIVE) {
            throw new BadRequestException("Tài khoản đã bị tạm khóa hoặc ngừng hoạt động. Vui lòng liên hệ ban quản trị.");
        }

        String token = tokenProvider.generateToken(user);
        RoleEnum roleEnum = RoleEnum.fromString(user.getRole().getRoleName());
        String redirectUrl = roleEnum.getDashboardRoute();
        boolean mustChange = user.isMustChangePassword();

        if (response != null) {
            Cookie jwtCookie = new Cookie("access_token", token);
            jwtCookie.setHttpOnly(true);
            jwtCookie.setPath("/");
            jwtCookie.setMaxAge(24 * 60 * 60);
            response.addCookie(jwtCookie);
        }

        UserSummaryDto userSummary = buildUserSummaryDto(user, roleEnum, mustChange);

        return LoginResponse.builder()
                .token(token)
                .user(userSummary)
                .redirectUrl(redirectUrl)
                .message("Đăng nhập thành công")
                .mustChangePassword(mustChange)
                .build();
    }

    @Override
    @Transactional
    public LoginResponse register(RegisterRequest request, HttpServletResponse response) {
        String email = request.getEmail().trim().toLowerCase();

        if (userRepository.findByEmail(email).isPresent()) {
            throw new BadRequestException("Địa chỉ email này đã được sử dụng. Vui lòng đăng nhập.");
        }

        Role role = roleRepository.findByRoleName(request.getRole())
                .orElseGet(() -> roleRepository.findByRoleName("ROLE_LEARNER")
                        .orElseThrow(() -> new IllegalStateException("Không tìm thấy vai trò")));

        User user = User.builder()
                .email(email)
                .fullName(request.getFullName())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
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
                    // Gia sư mới chưa nộp hồ sơ xác minh và chưa mua gói hội viên
                    .verificationStatus(TutorVerificationStatus.UNVERIFIED)
                    .membershipStatus(TutorMembershipStatus.FREE)
                    .isFeatured(false)
                    .updatedAt(OffsetDateTime.now())
                    .build();
            tutorRepository.save(tutor);
        } else {
            Learner learner = Learner.builder()
                    .user(user)
                    .currentLevel("N5")
                    .targetLevel(request.getTargetJlpt() != null ? request.getTargetJlpt() : "N3")
                    .learningGoals("Chinh phục mục tiêu JLPT cùng KizunaTutor")
                    .updatedAt(OffsetDateTime.now())
                    .build();
            learnerRepository.save(learner);
        }

        String token = tokenProvider.generateToken(user);
        RoleEnum roleEnum = RoleEnum.fromString(role.getRoleName());
        String redirectUrl = roleEnum.getDashboardRoute();

        if (response != null) {
            Cookie jwtCookie = new Cookie("access_token", token);
            jwtCookie.setHttpOnly(true);
            jwtCookie.setPath("/");
            jwtCookie.setMaxAge(24 * 60 * 60);
            response.addCookie(jwtCookie);
        }

        UserSummaryDto userSummary = buildUserSummaryDto(user, roleEnum, false);

        return LoginResponse.builder()
                .token(token)
                .user(userSummary)
                .redirectUrl(redirectUrl)
                .message("Đăng ký tài khoản thành công")
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public UserSummaryDto getCurrentUser(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy thông tin tài khoản"));

        RoleEnum roleEnum = RoleEnum.fromString(user.getRole().getRoleName());
        return buildUserSummaryDto(user, roleEnum, user.isMustChangePassword());
    }

    private UserSummaryDto buildUserSummaryDto(User user, RoleEnum roleEnum, boolean mustChangePassword) {
        String subtitle = resolveSubtitle(user, roleEnum);
        String initialLetters = calculateInitialLetters(user.getFullName());

        return UserSummaryDto.builder()
                .id(user.getUserId())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .role(roleEnum.getRoleName())
                .roleBadge(roleEnum.getBadgeText())
                .subtitle(subtitle)
                .initialLetters(initialLetters)
                .avatarUrl(user.getAvatarUrl())
                .accountStatus(user.getAccountStatus().name())
                .mustChangePassword(mustChangePassword)
                .build();
    }

    private String resolveSubtitle(User user, RoleEnum roleEnum) {
        if (roleEnum == RoleEnum.ROLE_TUTOR) {
            return tutorRepository.findByUser_UserId(user.getUserId())
                    .map(t -> {
                        String level = (t.getHighestJlptLevel() != null && !t.getHighestJlptLevel().isBlank())
                                ? " • JLPT " + t.getHighestJlptLevel()
                                : "";
                        return "Gia sư" + level;
                    })
                    .orElse("Gia sư Kizuna");
        } else if (roleEnum == RoleEnum.ROLE_LEARNER) {
            return "Học viên";
        }
        return roleEnum.getBadgeText();
    }

    private String calculateInitialLetters(String fullName) {
        if (fullName == null || fullName.isBlank()) return "KZ";
        String[] parts = fullName.trim().split("\\s+");
        if (parts.length == 1) {
            return parts[0].substring(0, Math.min(2, parts[0].length())).toUpperCase();
        }
        String first = parts[0].substring(0, 1);
        String last = parts[parts.length - 1].substring(0, 1);
        return (first + last).toUpperCase();
    }

    @Override
    @Transactional
    public Map<String, Object> changePasswordFirstTime(FirstTimePasswordChangeRequest request, String userEmail) {
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new BadRequestException("Mật khẩu mới và mật khẩu xác nhận không trùng khớp");
        }

        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy tài khoản người dùng"));

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPasswordHash())) {
            throw new BadRequestException("Mật khẩu tạm thời hiện tại không chính xác");
        }

        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        user.setMustChangePassword(false);
        userRepository.save(user);

        return Map.of(
                "success", true,
                "message", "Cập nhật mật khẩu mới thành công"
        );
    }

    @Override
    public void logout(HttpServletResponse response) {
        if (response != null) {
            Cookie expiredCookie = new Cookie("access_token", "");
            expiredCookie.setHttpOnly(true);
            expiredCookie.setPath("/");
            expiredCookie.setMaxAge(0);
            response.addCookie(expiredCookie);
        }
    }
}
