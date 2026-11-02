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
            throw new BadRequestException("Tài khoản chưa được kích hoạt hoặc đang bị tạm khóa. Vui lòng liên hệ hỗ trợ.");
        }

        String token = tokenProvider.generateToken(user);
        String roleName = (user.getRole() != null) ? user.getRole().getRoleName() : "ROLE_LEARNER";
        RoleEnum roleEnum = RoleEnum.fromString(roleName);
        boolean mustChange = user.isMustChangePassword();
        String redirectUrl = mustChange ? "/change-password-first-time" : roleEnum.getDashboardRoute();

        if (response != null) {
            Cookie jwtCookie = new Cookie("access_token", token);
            jwtCookie.setHttpOnly(true);
            jwtCookie.setPath("/");
            jwtCookie.setMaxAge(24 * 60 * 60);
            response.addCookie(jwtCookie);
        }

        UserSummaryDto userSummary = UserSummaryDto.builder()
                .id(user.getUserId())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .role(roleEnum.getRoleName())
                .roleBadge(roleEnum.getBadgeText())
                .avatarUrl(user.getAvatarUrl())
                .accountStatus(user.getAccountStatus().name())
                .mustChangePassword(mustChange)
                .build();

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

        String targetRoleName = (request.getRole() != null && !request.getRole().isBlank())
                ? request.getRole()
                : "ROLE_LEARNER";

        Role role = roleRepository.findByRoleName(targetRoleName)
                .orElseGet(() -> roleRepository.findByRoleName("ROLE_LEARNER")
                        .orElseThrow(() -> new IllegalStateException("Không tìm thấy vai trò mặc định")));

        User user = User.builder()
                .email(email)
                .fullName(request.getFullName().trim())
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
                .message("Đăng ký tài khoản thành công")
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public UserSummaryDto getCurrentUser(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy thông tin tài khoản"));

        RoleEnum roleEnum = RoleEnum.fromString(user.getRole().getRoleName());
        return UserSummaryDto.builder()
                .id(user.getUserId())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .role(roleEnum.getRoleName())
                .roleBadge(roleEnum.getBadgeText())
                .avatarUrl(user.getAvatarUrl())
                .accountStatus(user.getAccountStatus().name())
                .mustChangePassword(user.isMustChangePassword())
                .build();
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
