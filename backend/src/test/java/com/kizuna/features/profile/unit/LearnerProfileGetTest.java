package com.kizuna.features.profile.unit;

import com.kizuna.core.entity.Learner;
import com.kizuna.core.entity.User;
import com.kizuna.core.exception.ResourceNotFoundException;
import com.kizuna.features.profile.dto.response.LearnerProfileResponseDto;
import com.kizuna.features.profile.service.impl.LearnerProfileServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;

import java.time.OffsetDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit test FE-02 / UC-02.01 View Learner Profile - LearnerProfileServiceImpl.getMyProfile(email).
 * Hồ sơ lấy theo email của phiên đăng nhập (hàm không nhận user ID).
 */
class LearnerProfileGetTest extends ProfileUnitTestBase {
    private final LearnerProfileServiceImpl service = new LearnerProfileServiceImpl(users, learners, avatars);

    @Test @DisplayName("UTCID01: Học viên xem hồ sơ -> trả đủ họ tên, email, SĐT, ảnh, trình độ, mục tiêu, tỉnh, địa chỉ")
    void utcid01_viewOwnProfile() {
        User u = loggedIn("ROLE_LEARNER");
        u.setPhone("0912345678");
        when(learners.findByUser_UserId(USER_ID)).thenReturn(Optional.of(Learner.builder().user(u).currentLevel("N5")
                .targetLevel("N3").learningGoals("Pass N3").province("Hà Nội").address("Cầu Giấy").build()));

        LearnerProfileResponseDto dto = service.getMyProfile(EMAIL);

        assertEquals(EMAIL, dto.email());
        assertEquals("Nguyen Van A", dto.fullName());
        assertEquals("0912345678", dto.phone());
        assertEquals(OLD_AVATAR, dto.avatarUrl());
        assertEquals("N5", dto.currentLevel());
        assertEquals("N3", dto.targetLevel());
        assertEquals("Pass N3", dto.learningGoals());
        assertEquals("Hà Nội", dto.province());
        assertEquals("Cầu Giấy", dto.address());
    }

    @Test @DisplayName("UTCID02: Học viên chưa có bản ghi learner -> vẫn trả thông tin tài khoản, phần học tập rỗng")
    void utcid02_noLearnerRow() {
        loggedIn("ROLE_LEARNER");
        when(learners.findByUser_UserId(USER_ID)).thenReturn(Optional.empty());

        LearnerProfileResponseDto dto = service.getMyProfile(EMAIL);

        assertEquals(EMAIL, dto.email());
        assertNull(dto.currentLevel());
    }

    @Test @DisplayName("UTCID03: Tài khoản không phải học viên -> 403 'Chỉ tài khoản học viên mới xem được hồ sơ này.'")
    void utcid03_notLearner() {
        loggedIn("ROLE_TUTOR");
        var ex = assertThrows(AccessDeniedException.class, () -> service.getMyProfile(EMAIL));
        assertEquals("Chỉ tài khoản học viên mới xem được hồ sơ này.", ex.getMessage());
    }

    @Test @DisplayName("UTCID04: Tài khoản đã bị xóa -> 404 'Không tìm thấy hồ sơ học viên.'")
    void utcid04_deletedAccount() {
        User u = loggedIn("ROLE_LEARNER");
        u.setDeletedAt(OffsetDateTime.now());
        var ex = assertThrows(ResourceNotFoundException.class, () -> service.getMyProfile(EMAIL));
        assertEquals("Không tìm thấy hồ sơ học viên.", ex.getMessage());
    }

    @Test @DisplayName("UTCID05: Email không tồn tại -> 404")
    void utcid05_unknownEmail() {
        when(users.findByEmail(EMAIL)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> service.getMyProfile(EMAIL));
    }
}
