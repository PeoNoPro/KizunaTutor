package com.kizuna.features.auth.service.impl;

import com.kizuna.core.entity.*;
import com.kizuna.core.exception.BadRequestException;
import com.kizuna.core.exception.ResourceNotFoundException;
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
            return learnerRepository.findByUser_UserId(user.getUserId())
                    .map(l -> {
                        String target = (l.getTargetLevel() != null && !l.getTargetLevel().isBlank())
                                ? " • Mục tiêu " + l.getTargetLevel()
                                : "";
                        return "Học viên" + target;
                    })
                    .orElse("Học viên Kizuna");
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
