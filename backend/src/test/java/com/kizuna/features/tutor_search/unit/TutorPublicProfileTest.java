package com.kizuna.features.tutor_search.unit;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit test FE-05 / UC-05.05 View Tutor Public Profile - TutorSearchServiceImpl.getTutorDetail(tutorId).
 */
class TutorPublicProfileTest extends TutorSearchUnitTestBase {

    @Test @DisplayName("UTCID01: Mở hồ sơ công khai -> trả thông tin gia sư kèm chi tiết (lịch, đánh giá)")
    void utcid01_detail() {
        when(repo.findById(TUTOR_ID)).thenReturn(Optional.of(tutor(TUTOR_ID, "Nguyen Van A", "Hà Nội - Cầu Giấy")));

        var dto = service.getTutorDetail(TUTOR_ID);

        assertEquals(TUTOR_ID, dto.getTutorId());
        assertEquals("Nguyen Van A", dto.getFullName());
        assertEquals("N1", dto.getHighestJlptLevel());
        assertNotNull(dto.getReviews());
    }

    @Test @DisplayName("UTCID02: Địa chỉ có số nhà, tên đường -> hồ sơ công khai chỉ hiện Tỉnh - Quận")
    void utcid02_hidesStreet() {
        when(repo.findById(TUTOR_ID)).thenReturn(Optional.of(tutor(TUTOR_ID, "A", "Hà Nội - 12 Nguyễn Trãi")));

        String location = service.getTutorDetail(TUTOR_ID).getLocation();

        assertFalse(location.contains("12"), "không được lộ số nhà: " + location);
        assertFalse(location.contains("Nguyễn Trãi"), "không được lộ tên đường: " + location);
        assertTrue(location.startsWith("Hà Nội"));
    }

    @Test @DisplayName("UTCID03: Địa chỉ chỉ có Tỉnh - Quận -> giữ nguyên")
    void utcid03_keepsDistrict() {
        when(repo.findById(TUTOR_ID)).thenReturn(Optional.of(tutor(TUTOR_ID, "A", "Hà Nội - Cầu Giấy")));
        assertEquals("Hà Nội - Cầu Giấy", service.getTutorDetail(TUTOR_ID).getLocation());
    }

    @Test @DisplayName("UTCID04: Gia sư không tồn tại -> lỗi 'Không tìm thấy thông tin gia sư với mã yêu cầu'")
    void utcid04_notFound() {
        UUID random = UUID.randomUUID();
        when(repo.findById(random)).thenReturn(Optional.empty());
        var ex = assertThrows(IllegalArgumentException.class, () -> service.getTutorDetail(random));
        assertEquals("Không tìm thấy thông tin gia sư với mã yêu cầu", ex.getMessage());
    }
}
