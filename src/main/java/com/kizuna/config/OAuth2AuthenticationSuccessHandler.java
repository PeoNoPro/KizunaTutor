package com.kizuna.config;

import com.kizuna.entity.Learner;
import com.kizuna.entity.Role;
import com.kizuna.entity.RoleEnum;
import com.kizuna.entity.User;
import com.kizuna.entity.UserAccountStatus;
import com.kizuna.repository.LearnerRepository;
import com.kizuna.repository.RoleRepository;
import com.kizuna.repository.UserRepository;
import com.kizuna.util.JwtTokenProvider;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Xử lý xác thực thành công qua Google OAuth2:
 * 1. Trích xuất email, name, avatar từ Google Profile.
 * 2. Tự động tạo mới tài khoản (ROLE_LEARNER) nếu chưa có, hoặc cập nhật thông tin nếu đã tồn tại.
 * 3. Ký JWT Token, set Cookie và chuyển hướng an toàn về /oauth2/redirect với các tham số cần thiết.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OAuth2AuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final JwtTokenProvider jwtTokenProvider;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final LearnerRepository learnerRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {
        try {
            OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();

            String email = oAuth2User.getAttribute("email");
            String name = oAuth2User.getAttribute("name");
            String picture = oAuth2User.getAttribute("picture");

            if (email == null || email.isBlank()) {
                log.error("[OAuth2] Không tìm thấy email trong thông tin Google Profile");
                response.sendRedirect("/login?error=email_not_provided");
                return;
            }

            if (name == null || name.isBlank()) {
                name = email.split("@")[0];
            }

            log.info("[OAuth2] Đăng nhập thành công từ Google: email={}, name={}", email, name);

            // Kiểm tra bảng users
            User user = userRepository.findByEmail(email).orElse(null);

            if (user == null) {
                // Tự động tạo user mới với role ROLE_LEARNER
                Role learnerRole = roleRepository.findByRoleName(RoleEnum.ROLE_LEARNER.name())
                        .orElseThrow(() -> new IllegalStateException("Role ROLE_LEARNER không tồn tại trong hệ thống"));

                user = User.builder()
                        .email(email)
                        .fullName(name)
                        .avatarUrl(picture)
                        .passwordHash(passwordEncoder.encode(UUID.randomUUID().toString()))
                        .role(learnerRole)
                        .accountStatus(UserAccountStatus.ACTIVE)
                        .build();

                user = userRepository.save(user);

                // Tạo thực thể Learner tương ứng
                Learner learner = Learner.builder()
                        .user(user)
                        .currentLevel("N5")
                        .targetLevel("N3")
                        .learningGoals("Học tiếng Nhật & luyện chữ Hán tương tác cùng KizunaTutor")
                        .updatedAt(OffsetDateTime.now())
                        .build();

                learnerRepository.save(learner);
                log.info("[OAuth2] Đã tự động tạo tài khoản Học Viên mới cho: {}", email);
            } else {
                // Cập nhật avatar nếu có
                if (picture != null && !picture.isBlank()) {
                    user.setAvatarUrl(picture);
                    userRepository.save(user);
                }
            }

            // Sinh JWT Token
            String token = jwtTokenProvider.generateToken(user);
            String roleName = user.getRole().getRoleName();

            // Thiết lập Cookie an toàn (cho phép cả localhost HTTP lẫn HTTPS)
            Cookie cookie = new Cookie("access_token", token);
            cookie.setPath("/");
            cookie.setHttpOnly(true);
            cookie.setMaxAge(86400); // 1 ngày
            response.addCookie(cookie);

            // Redirect về URL tiếp nhận callback ở Frontend kèm token, role, email, name
            String redirectUrl = UriComponentsBuilder.fromUriString("/oauth2/redirect")
                    .queryParam("token", token)
                    .queryParam("role", roleName)
                    .queryParam("email", URLEncoder.encode(email, StandardCharsets.UTF_8))
                    .queryParam("name", URLEncoder.encode(name, StandardCharsets.UTF_8))
                    .build().toUriString();

            getRedirectStrategy().sendRedirect(request, response, redirectUrl);

        } catch (Exception e) {
            log.error("[OAuth2] Lỗi trong quá trình xử lý authentication success:", e);
            response.sendRedirect("/login?error=oauth2_process_error");
        }
    }
}
