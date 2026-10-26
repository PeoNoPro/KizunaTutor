package com.kizuna.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .authorizeHttpRequests(auth -> auth
                // Cho phep tai nguyen tinh va trang cong khai
                .requestMatchers(
                    "/", "/index", "/about", "/contact",
                    "/login", "/register", "/verify-otp",
                    "/tutors", "/tutors/**",
                    "/kanji", "/kanji/**",
                    "/css/**", "/js/**", "/images/**", "/static/**",
                    "/api/auth/**", "/api/public/**", "/api/test/**", "/api/kanji/**", "/api/ai/**",
                    "/api/user/**",
                    "/error"
                ).permitAll()
                .anyRequest().authenticated()
            );
        return http.build();
    }
}
