package com.kizuna.core.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kizuna.features.admin.config.OperationsDevLoginSettings;
import org.springframework.core.env.Environment;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Cấu hình Spring Security.
 * Cho phép truy cập công khai không cần đăng nhập đối với danh mục tìm kiếm gia sư và xác thực (Report 3 - Non-functional Security).
 * Khu vực vận hành hệ thống (FE-11) và chính sách đặt lịch yêu cầu đăng nhập bằng JWT.
 * Các route của FE-03 (xác minh gia sư) yêu cầu đăng nhập bằng JWT và đúng vai trò.
 */
@Slf4j
@Configuration
@EnableWebSecurity
@org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity
public class SecurityConfig {

    @Bean
    @Order(1)
    public SecurityFilterChain operationsSecurityFilterChain(HttpSecurity http, JwtTokenVerifier verifier,
            @Value("${app.frontend-origins:http://localhost:3000,http://127.0.0.1:3000}") String origins,
            // TẠM THỜI khi chưa có feature auth: email tài khoản thật dùng thay cho đăng nhập. Mặc định tắt.
            @Value("${app.dev-login.email:${DEV_LOGIN_EMAIL:}}") String devLoginEmail,
            Environment environment) throws Exception {
        String localDevEmail = OperationsDevLoginSettings.validatedEmail(environment, devLoginEmail);
        if (localDevEmail != null) {
            log.warn("Đang bật phiên quản trị phát triển trên máy cục bộ và cơ sở dữ liệu riêng.");
        }
        http
            .securityMatcher("/api/admin/operations/**", "/api/policies", "/api/policies/**", "/api/v1/tickets", "/api/v1/tickets/**", "/api/v1/notifications", "/api/v1/notifications/**")
            .cors(cors -> cors.configurationSource(corsConfigurationSource(origins)))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                .anyRequest().authenticated()
            )
            .exceptionHandling(errors -> errors
                .authenticationEntryPoint((request, response, error) ->
                    writeError(response, 401, "Vui lòng đăng nhập để tiếp tục."))
                .accessDeniedHandler((request, response, error) ->
                    writeError(response, 403, "Bạn không có quyền thực hiện thao tác này hoặc phiên xác nhận đã hết hạn."))
            )
            .addFilterBefore(new JwtAuthenticationAdapter(verifier, localDevEmail), UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * Filter JWT chỉ chạy trong chuỗi bảo mật, không đăng ký thêm lần nữa như servlet filter thường.
     */
    @Bean
    public FilterRegistrationBean<JwtAuthenticationFilter> jwtAuthenticationFilterRegistration(JwtAuthenticationFilter filter) {
        FilterRegistrationBean<JwtAuthenticationFilter> registration = new FilterRegistrationBean<>(filter);
        registration.setEnabled(false);
        return registration;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                   JwtAuthenticationFilter jwtAuthenticationFilter,
                                                   RestAuthErrorHandlers restAuthErrorHandlers,
                                                   @Value("${app.frontend-origins:http://localhost:3000,http://127.0.0.1:3000}") String origins) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .cors(cors -> cors.configurationSource(corsConfigurationSource(origins)))
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint(restAuthErrorHandlers.authenticationEntryPoint())
                .accessDeniedHandler(restAuthErrorHandlers.accessDeniedHandler())
            )
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/tutor/verification/**").hasAuthority("ROLE_TUTOR")
                .requestMatchers("/api/academic/verifications/**", "/api/academic/reviews/**", "/api/academic/reviews",
                        "/api/academic/profile/**", "/api/academic/profile",
                        "/api/academic/posts/**", "/api/academic/posts",
                        "/api/academic/content-imports/**").hasAuthority("ROLE_ACADEMIC_OFFICER")
                // Bài đăng gia sư (sau khi mua gói)
                .requestMatchers("/api/tutor/post/**", "/api/tutor/post").hasAuthority("ROLE_TUTOR")
                // FE-05: Đặt lịch học thử & Quản lý lịch rảnh gia sư
                .requestMatchers(HttpMethod.POST, "/api/v1/trial-bookings", "/api/trial-bookings").hasAuthority("ROLE_LEARNER")
                .requestMatchers("/api/v1/trial-bookings/*/respond", "/api/trial-bookings/*/respond").hasAuthority("ROLE_TUTOR")
                .requestMatchers("/api/v1/tutor/availability/**", "/api/tutor/availability/**").hasAuthority("ROLE_TUTOR")
                // Kết nối gia sư trực tiếp (không cần học thử)
                .requestMatchers(HttpMethod.POST, "/api/v1/connection-requests", "/api/connection-requests").hasAuthority("ROLE_LEARNER")
                .requestMatchers("/api/v1/connection-requests/*/respond", "/api/connection-requests/*/respond").hasAuthority("ROLE_TUTOR")
                .requestMatchers("/api/v1/connection-requests/*/cancel", "/api/connection-requests/*/cancel").hasAuthority("ROLE_LEARNER")
                .requestMatchers("/api/v1/connection-requests/**", "/api/connection-requests/**").authenticated()
                // FE-09: gói hội viên và thanh toán
                .requestMatchers("/api/payments/vnpay/ipn").permitAll()
                .requestMatchers("/api/tutor/memberships/**").hasAuthority("ROLE_TUTOR")
                // Nhóm thống nhất: Quản trị viên tạo/sửa/mở/đóng gói, Quản lý chỉ xem danh sách gói.
                .requestMatchers(org.springframework.http.HttpMethod.GET, "/api/manager/membership-plans/**").hasAnyAuthority("ROLE_MANAGER", "ROLE_ADMIN")
                .requestMatchers("/api/manager/membership-plans/**").hasAuthority("ROLE_ADMIN")
                .requestMatchers("/api/manager/tutor-memberships/**").hasAuthority("ROLE_MANAGER")
                .requestMatchers("/api/admin/payment-transactions/**").hasAuthority("ROLE_ADMIN")
                .requestMatchers("/api/tutors/**").permitAll()
                .requestMatchers("/api/auth/**").permitAll()
                .anyRequest().permitAll()
            );

        return http.build();
    }

    private CorsConfigurationSource corsConfigurationSource(String origins) {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(Arrays.stream(origins.split(",")).map(String::trim).toList());
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("Content-Type", "Authorization", "X-CSRF-TOKEN"));
        config.setAllowCredentials(true);
        config.setMaxAge(600L);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    private void writeError(HttpServletResponse response, int status, String message) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json;charset=UTF-8");
        new ObjectMapper().writeValue(response.getWriter(),
            Map.of("success", false, "message", message, "timestamp", OffsetDateTime.now().toString()));
    }
}
