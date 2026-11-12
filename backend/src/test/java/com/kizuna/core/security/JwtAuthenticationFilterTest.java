package com.kizuna.core.security;

import com.kizuna.core.entity.User;
import com.kizuna.core.entity.UserAccountStatus;
import com.kizuna.core.repository.UserRepository;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.OffsetDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Góp ý review PR #21: nhân viên bị xóa/khóa phải bị từ chối ngay ở request kế tiếp, không chờ token hết hạn.
 * Bộ lọc đọc lại trạng thái tài khoản từ DB ở mỗi request nên token cũ không còn tác dụng.
 */
class JwtAuthenticationFilterTest {
    private final JwtTokenProvider tokens = mock(JwtTokenProvider.class);
    private final UserRepository users = mock(UserRepository.class);
    private final JwtAuthenticationFilter filter = new JwtAuthenticationFilter(tokens, users);

    @AfterEach
    void clear() { SecurityContextHolder.clearContext(); }

    private boolean authenticatedWith(User user) throws Exception {
        SecurityContextHolder.clearContext();
        when(tokens.validateToken("token-cu")).thenReturn(true);
        when(tokens.getEmailFromToken("token-cu")).thenReturn("nhanvien@test.invalid");
        when(users.findByEmail("nhanvien@test.invalid")).thenReturn(Optional.of(user));
        var request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer token-cu");
        var chain = mock(FilterChain.class);
        filter.doFilter(request, new MockHttpServletResponse(), chain);
        verify(chain).doFilter(any(), any());
        return SecurityContextHolder.getContext().getAuthentication() != null;
    }

    @Test
    void deletedOrSuspendedStaffIsRejectedEvenWithAnUnexpiredToken() throws Exception {
        var deleted = User.builder().email("nhanvien@test.invalid").accountStatus(UserAccountStatus.SUSPENDED).build();
        deleted.setDeletedAt(OffsetDateTime.now());
        assertFalse(authenticatedWith(deleted));

        var suspended = User.builder().email("nhanvien@test.invalid").accountStatus(UserAccountStatus.SUSPENDED).build();
        assertFalse(authenticatedWith(suspended));
    }

    @Test
    void activeStaffIsStillAuthenticated() throws Exception {
        assertTrue(authenticatedWith(User.builder().email("nhanvien@test.invalid").accountStatus(UserAccountStatus.ACTIVE).build()));
    }
}
