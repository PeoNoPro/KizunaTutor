package com.kizuna.core.security;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Cấu hình Spring Security.
 * Cho phép truy cập công khai không cần đăng nhập đối với danh mục tìm kiếm gia sư và xác thực (Report 3 - Non-functional Security).
 * Các route của FE-03 (xác minh gia sư) yêu cầu đăng nhập bằng JWT và đúng vai trò.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

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
                                                   RestAuthErrorHandlers restAuthErrorHandlers) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .cors(cors -> {})
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint(restAuthErrorHandlers.authenticationEntryPoint())
                .accessDeniedHandler(restAuthErrorHandlers.accessDeniedHandler())
            )
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/tutor/verification/**").hasAuthority("ROLE_TUTOR")
                .requestMatchers("/api/academic/verifications/**").hasAuthority("ROLE_ACADEMIC_OFFICER")
                // FE-09: gói hội viên và thanh toán
                .requestMatchers("/api/payments/vnpay/ipn").permitAll()
                .requestMatchers("/api/tutor/memberships/**").hasAuthority("ROLE_TUTOR")
                .requestMatchers("/api/manager/membership-plans/**", "/api/manager/tutor-memberships/**").hasAuthority("ROLE_MANAGER")
                .requestMatchers("/api/admin/payment-transactions/**").hasAuthority("ROLE_ADMIN")
                .requestMatchers("/api/tutors/**").permitAll()
                .requestMatchers("/api/auth/**").permitAll()
                .anyRequest().permitAll()
            );

        return http.build();
    }
}
