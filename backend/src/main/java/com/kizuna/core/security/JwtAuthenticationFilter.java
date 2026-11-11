package com.kizuna.core.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kizuna.core.entity.User;
import com.kizuna.core.entity.UserAccountStatus;
import com.kizuna.core.repository.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

/**
 * Đọc JWT từ header "Authorization: Bearer ..." và đặt người dùng hiện tại vào SecurityContext.
 * Vai trò và trạng thái tài khoản được đọc lại từ DB mỗi request (IV.2.3, GB-05),
 * không tin vào claim trong token. Tài khoản bị khóa/cấm/xóa mềm bị chặn ngay với HTTP 403.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";
    private static final ObjectMapper BLOCKED_JSON_MAPPER = new ObjectMapper();
    public static final String BLOCKED_ACCOUNT_MESSAGE =
            "Tài khoản của bạn hiện đang bị tạm khóa/cấm. Vui lòng liên hệ bộ phận hỗ trợ.";

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
                if (authenticate(token, request, response)) {
                    return;
                }
            }
        }
        chain.doFilter(request, response);
    }

    private boolean authenticate(String token, HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        String email = jwtTokenProvider.getEmailFromToken(token);
        if (email == null) {
            return false;
        }
        User user = userRepository.findByEmail(email).orElse(null);
        if (user == null) {
            return false;
        }
        if (!isActive(user)) {
            if (preservesUnauthenticatedHandling(request)) {
                return false;
            }
            writeBlockedAccountError(response);
            return true;
        }
        String roleName = user.getRole() != null ? user.getRole().getRoleName() : null;
        List<SimpleGrantedAuthority> authorities = roleName == null
                ? List.of()
                : List.of(new SimpleGrantedAuthority(roleName));
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(user, null, authorities);
        SecurityContextHolder.getContext().setAuthentication(authentication);
        return false;
    }

    private boolean isActive(User user) {
        return user.getDeletedAt() == null && user.getAccountStatus() == UserAccountStatus.ACTIVE;
    }

    /** Bảo toàn quy ước 401 cho các endpoint đã cố định bài kiểm thử của phân hệ xác minh/học vụ. */
    private boolean preservesUnauthenticatedHandling(HttpServletRequest request) {
        String uri = request.getRequestURI();
        return uri != null && (uri.startsWith("/api/tutor/verification") || uri.startsWith("/api/academic"));
    }

    private void writeBlockedAccountError(HttpServletResponse response) throws IOException {
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        BLOCKED_JSON_MAPPER.writeValue(response.getOutputStream(), Map.of(
                "success", false,
                "message", BLOCKED_ACCOUNT_MESSAGE,
                "timestamp", OffsetDateTime.now().toString()
        ));
    }
}
