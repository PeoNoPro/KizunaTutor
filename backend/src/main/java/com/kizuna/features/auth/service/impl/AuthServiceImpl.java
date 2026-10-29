package com.kizuna.features.auth.service.impl;

import com.kizuna.features.auth.dto.LoginRequest;
import com.kizuna.features.auth.dto.LoginResponse;
import com.kizuna.features.auth.dto.RegisterRequest;
import com.kizuna.features.auth.dto.UserSummaryDto;
import com.kizuna.features.auth.dto.FirstTimePasswordChangeRequest;
import java.util.Map;
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
import com.kizuna.core.exception.ResourceNotFoundException;
import com.kizuna.core.repository.LearnerRepository;
import com.kizuna.core.repository.RoleRepository;
import com.kizuna.core.repository.UserRepository;
import com.kizuna.features.auth.service.IAuthService;
import com.kizuna.core.security.JwtTokenProvider;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

/**
 * Triển khai nghiệp vụ xác thực người dùng, đăng ký, cấp phát JWT và điều phối luồng chuyển hướng.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements IAuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final LearnerRepository learnerRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;

    @Override
    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request, HttpServletResponse response) {
        String email = request.getEmail().trim().toLowerCase();

        // 1. Tìm người dùng theo email
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BadRequestException("Tài khoản hoặc mật khẩu không chính xác"));

        // 2. Kiểm tra mật khẩu mã hóa BCrypt
        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new BadRequestException("Tài khoản hoặc mật khẩu không chính xác");
        }

        // 3. Kiểm tra trạng thái tài khoản
        if (user.getAccountStatus() != UserAccountStatus.ACTIVE) {
            String statusMsg;
            if (user.getAccountStatus() == UserAccountStatus.PENDING) {
                statusMsg = "Tài khoản của bạn đang chờ xác thực hoặc phê duyệt. Vui lòng kiểm tra email để hoàn tất.";
            } else if (user.getAccountStatus() == UserAccountStatus.SUSPENDED) {
                statusMsg = "Tài khoản của bạn đang tạm thời bị khóa. Vui lòng liên hệ bộ phận hỗ trợ.";
            } else if (user.getAccountStatus() == UserAccountStatus.BANNED) {
                statusMsg = "Tài khoản của bạn đã bị ngừng quyền truy cập. Vui lòng liên hệ ban quản trị.";
            } else {
                statusMsg = "Tài khoản chưa được kích hoạt. Vui lòng liên hệ bộ phận hỗ trợ.";
            }
            throw new BadRequestException(statusMsg);
        }

        // 4. Tạo JWT Token
        String token = tokenProvider.generateToken(user);

        // 5. Xác định Vai trò & Route điều hướng đích
        String roleName = (user.getRole() != null) ? user.getRole().getRoleName() : "ROLE_LEARNER";
        RoleEnum roleEnum = RoleEnum.fromString(roleName);
        boolean mustChange = user.isMustChangePassword();
        String redirectUrl = mustChange ? "/change-password-first-time" : roleEnum.getDashboardRoute();

        // 6. Cấu hình Cookie access_token để phục vụ trình duyệt điều hướng mượt mà
        if (response != null) {
            Cookie jwtCookie = new Cookie("access_token", token);
            jwtCookie.setHttpOnly(true);
            jwtCookie.setPath("/");
            jwtCookie.setMaxAge(24 * 60 * 60); // 24 giờ
            response.addCookie(jwtCookie);
        }

        // 7. Tạo đối tượng tóm tắt thông tin người dùng
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

        log.info("Xác thực thành công cho user [{}] với vai trò [{}], mustChangePassword=[{}], điều hướng đến [{}]",
                user.getEmail(), roleEnum.getRoleName(), mustChange, redirectUrl);

        return LoginResponse.builder()
                .token(token)
                .user(userSummary)
                .redirectUrl(redirectUrl)
                .mustChangePassword(mustChange)
                .message("Đăng nhập thành công! Đang chuyển hướng...")
                .build();
    }

    @Override
    @Transactional
    public LoginResponse register(RegisterRequest request, HttpServletResponse response) {
        String email = request.getEmail().trim().toLowerCase();

        // 1. Kiểm tra xem email đã tồn tại hay chưa
        if (userRepository.findByEmail(email).isPresent()) {
            throw new BadRequestException("Địa chỉ email '" + email + "' đã được sử dụng. Vui lòng đăng nhập hoặc chọn email khác.");
        }

        // 2. Xác định vai trò (Mặc định ROLE_LEARNER)
        String targetRoleName = (request.getRole() != null && !request.getRole().isBlank())
                ? request.getRole()
                : RoleEnum.ROLE_LEARNER.name();

        Role role = roleRepository.findByRoleName(targetRoleName)
                .orElseGet(() -> roleRepository.findByRoleName(RoleEnum.ROLE_LEARNER.name())
                        .orElseThrow(() -> new IllegalStateException("Không tìm thấy vai trò " + targetRoleName)));

        // 3. Khởi tạo thực thể User
        User user = User.builder()
                .email(email)
                .fullName(request.getFullName().trim())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .role(role)
                .accountStatus(UserAccountStatus.ACTIVE)
                .build();

        user = userRepository.save(user);

        // 4. Nếu là Học Viên, khởi tạo hồ sơ Learner liên kết
        if (RoleEnum.ROLE_LEARNER.name().equals(role.getRoleName())) {
            String targetLevel = (request.getTargetJlpt() != null && !request.getTargetJlpt().isBlank())
                    ? request.getTargetJlpt()
                    : "N5";

            Learner learner = Learner.builder()
                    .user(user)
                    .currentLevel("N5")
                    .targetLevel(targetLevel)
                    .learningGoals("Chinh phục mục tiêu JLPT " + targetLevel + " cùng KizunaTutor")
                    .updatedAt(OffsetDateTime.now())
                    .build();

            learnerRepository.save(learner);
        }

        // 5. Cấp phát JWT Token
        String token = tokenProvider.generateToken(user);
        RoleEnum roleEnum = RoleEnum.fromString(role.getRoleName());
        String redirectUrl = roleEnum.getDashboardRoute();

        // 6. Cấu hình Cookie access_token
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

        log.info("Đăng ký tài khoản thành công cho [{}] với vai trò [{}], chuyển hướng [{}]",
                email, roleEnum.getRoleName(), redirectUrl);

        return LoginResponse.builder()
                .token(token)
                .user(userSummary)
                .redirectUrl(redirectUrl)
                .message("Đăng ký tài khoản thành công! Đang chuyển hướng...")
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public UserSummaryDto getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new BadRequestException("Chưa đăng nhập");
        }

        String email;
        if (auth.getPrincipal() instanceof User) {
            email = ((User) auth.getPrincipal()).getEmail();
        } else {
            email = auth.getName();
        }

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy thông tin người dùng"));

        String roleName = (user.getRole() != null) ? user.getRole().getRoleName() : "ROLE_LEARNER";
        RoleEnum roleEnum = RoleEnum.fromString(roleName);

        return UserSummaryDto.builder()
                .id(user.getUserId())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .role(roleEnum.getRoleName())
                .roleBadge(roleEnum.getBadgeText())
                .avatarUrl(user.getAvatarUrl())
                .accountStatus(user.getAccountStatus().name())
                .build();
    }

    @Override
    public void logout(HttpServletResponse response) {
        if (response != null) {
            Cookie jwtCookie = new Cookie("access_token", null);
            jwtCookie.setHttpOnly(true);
            jwtCookie.setPath("/");
            jwtCookie.setMaxAge(0);
            response.addCookie(jwtCookie);
        }
        SecurityContextHolder.clearContext();
        log.info("Đã xóa phiên đăng nhập người dùng");
    }

    @Override
    @Transactional
    public Map<String, Object> changePasswordFirstTime(FirstTimePasswordChangeRequest request, String userEmail) {
        if (userEmail == null || userEmail.isBlank()) {
            throw new BadRequestException("Không tìm thấy thông tin phiên đăng nhập người dùng.");
        }

        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy tài khoản người dùng: " + userEmail));

        // 1. Kiểm tra mật khẩu tạm thời hiện tại
        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPasswordHash())) {
            throw new BadRequestException("Mật khẩu tạm thời hiện tại không chính xác.");
        }

        // 2. Kiểm tra mật khẩu mới và xác nhận mật khẩu
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new BadRequestException("Mật khẩu mới và mật khẩu xác nhận không trùng khớp.");
        }

        // 3. Mật khẩu mới không được trùng mật khẩu tạm thời
        if (request.getNewPassword().equals(request.getCurrentPassword()) || passwordEncoder.matches(request.getNewPassword(), user.getPasswordHash())) {
            throw new BadRequestException("Mật khẩu mới không được trùng với mật khẩu tạm thời.");
        }

        // 4. Kiểm tra độ mạnh mật khẩu (ít nhất 8 ký tự, có chữ hoa, số hoặc ký tự đặc biệt)
        String newPass = request.getNewPassword();
        boolean hasUppercase = newPass.matches(".*[A-Z].*");
        boolean hasDigitOrSpecial = newPass.matches(".*[0-9!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>\\/?].*");

        if (newPass.length() < 8 || !hasUppercase || !hasDigitOrSpecial) {
            throw new BadRequestException("Mật khẩu mới phải có tối thiểu 8 ký tự, bao gồm ít nhất 1 chữ hoa, và ít nhất 1 chữ số hoặc ký tự đặc biệt.");
        }

        // 5. Cập nhật mật khẩu mới và xóa cờ mustChangePassword
        user.setPasswordHash(passwordEncoder.encode(newPass));
        user.setMustChangePassword(false);
        userRepository.save(user);

        // 6. Xác định Route Dashboard theo Role
        String roleName = (user.getRole() != null) ? user.getRole().getRoleName() : "ROLE_LEARNER";
        RoleEnum roleEnum = RoleEnum.fromString(roleName);
        String dashboardRoute = roleEnum.getDashboardRoute();

        log.info("[AUTH] Người dùng [{}] đã hoàn tất đổi mật khẩu lần đầu, điều hướng đến [{}]", user.getEmail(), dashboardRoute);

        return Map.of(
                "success", true,
                "message", "Thiết lập mật khẩu mới thành công! Chào mừng bạn gia nhập KizunaTutor.",
                "redirectUrl", dashboardRoute,
                "role", roleName
        );
    }
}
