package com.kizuna.config;

import com.kizuna.entity.User;
import com.kizuna.repository.UserRepository;
import com.kizuna.util.JwtTokenProvider;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.Optional;

/**
 * Filter chặn mọi HTTP request để kiểm tra tính hợp lệ của JWT Token.
 * Hỗ trợ nhận diện Token qua cả 2 kênh:
 * 1. HTTP Header: 'Authorization: Bearer <token>' (Dành cho REST API call)
 * 2. HTTP Cookie: 'access_token' (Dành cho các trang điều hướng SSR / Web Navigation)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider tokenProvider;
    private final UserRepository userRepository;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        try {
            String jwt = resolveToken(request);

            if (StringUtils.hasText(jwt) && tokenProvider.validateToken(jwt)) {
                String email = tokenProvider.getEmailFromToken(jwt);
                String role = tokenProvider.getRoleFromToken(jwt);

                if (email != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                    Optional<User> userOpt = userRepository.findByEmail(email);

                    // Đảm bảo role có tiền tố ROLE_
                    String grantedAuthority = role;
                    if (grantedAuthority != null && !grantedAuthority.startsWith("ROLE_")) {
                        grantedAuthority = "ROLE_" + grantedAuthority;
                    }

                    SimpleGrantedAuthority authority = new SimpleGrantedAuthority(
                            grantedAuthority != null ? grantedAuthority : "ROLE_LEARNER"
                    );

                    Object principal = userOpt.isPresent() ? userOpt.get() : email;

                    UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(
                                    principal,
                                    null,
                                    Collections.singletonList(authority)
                            );

                    authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                }
            }
        } catch (Exception ex) {
            log.error("Không thể thiết lập xác thực người dùng trong Security Context: {}", ex.getMessage());
        }

        filterChain.doFilter(request, response);
    }

    /**
     * Trích xuất JWT từ Header hoặc Cookie của request.
     */
    private String resolveToken(HttpServletRequest request) {
        // 1. Kiểm tra Authorization Header
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }

        // 2. Kiểm tra Cookie (thuận tiện cho việc điều hướng SSR của trình duyệt)
        if (request.getCookies() != null) {
            for (Cookie cookie : request.getCookies()) {
                if ("access_token".equals(cookie.getName()) || "jwt_token".equals(cookie.getName())) {
                    if (StringUtils.hasText(cookie.getValue())) {
                        return cookie.getValue();
                    }
                }
            }
        }

        return null;
    }
}
