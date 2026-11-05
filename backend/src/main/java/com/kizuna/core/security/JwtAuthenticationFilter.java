package com.kizuna.core.security;

import com.kizuna.core.entity.User;
import com.kizuna.core.entity.UserAccountStatus;
import com.kizuna.core.repository.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Đọc JWT từ header "Authorization: Bearer ..." và đặt người dùng hiện tại vào SecurityContext.
 * Vai trò và trạng thái tài khoản được đọc lại từ DB mỗi request (IV.2.3, GB-05),
 * không tin vào claim trong token. Thiếu token hoặc token không hợp lệ thì cho request đi tiếp
 * ở trạng thái chưa xác thực; việc chặn do SecurityConfig quyết định theo từng route.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtTokenProvider jwtTokenProvider;
    private final UserRepository userRepository;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith(BEARER_PREFIX)
                && SecurityContextHolder.getContext().getAuthentication() == null) {
            String token = header.substring(BEARER_PREFIX.length()).trim();
            if (!token.isEmpty() && jwtTokenProvider.validateToken(token)) {
                authenticate(token, request);
            }
        }
        chain.doFilter(request, response);
    }

    private void authenticate(String token, HttpServletRequest request) {
        String email = jwtTokenProvider.getEmailFromToken(token);
        if (email == null) {
            return;
        }
        userRepository.findByEmail(email)
                .filter(this::isActive)
                .ifPresent(user -> {
                    String roleName = user.getRole() != null ? user.getRole().getRoleName() : null;
                    List<SimpleGrantedAuthority> authorities = roleName == null
                            ? List.of()
                            : List.of(new SimpleGrantedAuthority(roleName));
                    UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(user, null, authorities);
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                });
    }

    private boolean isActive(User user) {
        return user.getDeletedAt() == null && user.getAccountStatus() == UserAccountStatus.ACTIVE;
    }
}
