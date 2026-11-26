package com.kizuna.features.profile.unit;

import com.kizuna.core.entity.Tutor;
import com.kizuna.core.entity.User;
import com.kizuna.core.exception.ResourceNotFoundException;
import com.kizuna.features.profile.dto.response.TutorProfileDetailDto;
import com.kizuna.features.profile.service.impl.TutorProfileServiceImpl;
import com.kizuna.features.tutor_search.service.TutorSearchService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit test FE-02 / UC-02.03 View Tutor Profile - TutorProfileServiceImpl.getMyProfile(email).
 */
class TutorProfileGetTest extends ProfileUnitTestBase {
    private final TutorSearchService search = mock(TutorSearchService.class);
    private final TutorProfileServiceImpl service = new TutorProfileServiceImpl(users, tutors, avatars, search);

    @Test @DisplayName("UTCID01: Gia sư xem hồ sơ -> trả mã gia sư, tiêu đề, kinh nghiệm, học phí, JLPT, tỉnh, khu vực")
    void utcid01_viewOwnProfile() {
        User u = loggedIn("ROLE_TUTOR");
        when(tutors.findByUser_UserId(USER_ID)).thenReturn(Optional.of(Tutor.builder().user(u).tutorCode(10001L)
                .headline("JLPT N1 teacher").experienceYears((short) 3).hourlyRate(new BigDecimal("200000"))
                .highestJlptLevel("N1").address("Hà Nội - Cầu Giấy").build()));

        TutorProfileDetailDto dto = service.getMyProfile(EMAIL);

        assertEquals(10001L, dto.tutorCode());
        assertEquals(EMAIL, dto.email());
        assertEquals("JLPT N1 teacher", dto.headline());
        assertEquals(3, dto.experienceYears());
        assertEquals(0, new BigDecimal("200000").compareTo(dto.hourlyRate()));
        assertEquals("N1", dto.highestJlptLevel());
        assertEquals("Hà Nội", dto.province());
        assertEquals("Cầu Giấy", dto.addressDetail());
    }

    @Test @DisplayName("UTCID02: Tài khoản không phải gia sư -> 403 'Chỉ tài khoản gia sư mới xem được hồ sơ này.'")
    void utcid02_notTutor() {
        loggedIn("ROLE_LEARNER");
        var ex = assertThrows(AccessDeniedException.class, () -> service.getMyProfile(EMAIL));
        assertEquals("Chỉ tài khoản gia sư mới xem được hồ sơ này.", ex.getMessage());
    }

    @Test @DisplayName("UTCID03: Tài khoản gia sư đã bị xóa -> 404 'Không tìm thấy hồ sơ gia sư.'")
    void utcid03_deleted() {
        User u = loggedIn("ROLE_TUTOR");
        u.setDeletedAt(OffsetDateTime.now());
        var ex = assertThrows(ResourceNotFoundException.class, () -> service.getMyProfile(EMAIL));
        assertEquals("Không tìm thấy hồ sơ gia sư.", ex.getMessage());
    }
}
