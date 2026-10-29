package com.kizuna.config;

import com.kizuna.entity.RoleEnum;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Cấu hình Spring Security:
 * - Bảo vệ phân quyền 6 tiền tố Route tương ứng với 6 vai trò Actor.
 * - Tích hợp JwtAuthenticationFilter xác thực cả Bearer Token lẫn Cookie.
 * - Tích hợp Google OAuth2 Login với OAuth2AuthenticationSuccessHandler.
 * - Điều hướng thông minh: Chặn truy cập chéo và tự động chuyển về đúng Dashboard của người dùng.
 */
@Configuration
@EnableWebSecurity
@org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final OAuth2AuthenticationSuccessHandler oAuth2AuthenticationSuccessHandler;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .authorizeHttpRequests(auth -> auth
                // 1. Tài nguyên tĩnh và các trang công khai
                .requestMatchers(
                    "/", "/index", "/about", "/contact",
                    "/login", "/register", "/verify-otp", "/logout",
                    "/oauth2/**", "/login/oauth2/**", "/oauth2/redirect",
                    "/tutors", "/tutors/**",
                    "/kanji", "/kanji/**",
                    "/kana-quiz", "/kana-quiz/**", "/api/kana/**",
                    "/css/**", "/js/**", "/images/**", "/static/**", "/webjars/**",
                    "/api/auth/**", "/api/v1/auth/**", "/api/public/**", "/api/test/**",
                    "/api/kanji/**", "/api/ai/**", "/api/user/**",
                    "/api/learner/decks/**", "/api/learner/flashcard-decks/**", "/api/learner/kanji/**",
                    "/error"
                ).permitAll()

                // 2. Bảo vệ 6 Route Dashboard & API tương ứng với từng Actor (Zero Crossing)
                .requestMatchers("/admin", "/admin/**", "/api/admin/**").hasAuthority("ROLE_ADMIN")
                .requestMatchers("/manager", "/manager/**", "/api/manager/**").hasAuthority("ROLE_MANAGER")
                .requestMatchers("/cs", "/cs/**", "/api/cs/**").hasAuthority("ROLE_CS_STAFF")
                .requestMatchers("/academic", "/academic/**", "/api/academic/**").hasAuthority("ROLE_ACADEMIC_OFFICER")
                .requestMatchers("/tutor", "/tutor/**", "/api/tutor/**").hasAuthority("ROLE_TUTOR")
                .requestMatchers("/learner", "/learner/**", "/api/learner/**").hasAuthority("ROLE_LEARNER")

                // 3. Mọi yêu cầu còn lại bắt buộc phải xác thực
                .anyRequest().authenticated()
            )
            .oauth2Login(oauth2 -> oauth2
                .loginPage("/login")
                .successHandler(oAuth2AuthenticationSuccessHandler)
                .failureHandler((request, response, exception) -> {
                    response.sendRedirect("/login?error=oauth2_failure");
                })
            )
            .exceptionHandling(exceptions -> exceptions
                // Xử lý khi chưa đăng nhập (Unauthenticated)
                .authenticationEntryPoint((request, response, authException) -> {
                    String uri = request.getRequestURI();
                    if (uri.startsWith("/api/")) {
                        response.setContentType("application/json;charset=UTF-8");
                        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                        response.getWriter().write("{\"error\":\"UNAUTHORIZED\",\"message\":\"Vui lòng đăng nhập để tiếp tục\",\"redirectUrl\":\"/login\"}");
                    } else {
                        response.sendRedirect("/login");
                    }
                })
                // Xử lý khi cố ý truy cập chéo vai trò không được phép (Access Denied / Forbidden)
                .accessDeniedHandler((request, response, accessDeniedException) -> {
                    String uri = request.getRequestURI();
                    if (uri.startsWith("/api/")) {
                        response.setContentType("application/json;charset=UTF-8");
                        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                        response.getWriter().write("{\"error\":\"FORBIDDEN\",\"message\":\"Bạn không có quyền truy cập chức năng này\"}");
                    } else {
                        // Tự động điều hướng về đúng Dashboard tương ứng của Actor đang đăng nhập
                        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
                        if (auth != null && auth.isAuthenticated()) {
                            for (GrantedAuthority ga : auth.getAuthorities()) {
                                RoleEnum roleEnum = RoleEnum.fromString(ga.getAuthority());
                                if (roleEnum != null) {
                                    response.sendRedirect(roleEnum.getDashboardRoute());
                                    return;
                                }
                            }
                        }
                        response.sendRedirect("/login");
                    }
                })
            )
            // Thêm Filter JWT trước UsernamePasswordAuthenticationFilter
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
