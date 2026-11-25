package com.kizuna.features.profile.unit;

import com.kizuna.core.entity.Tutor;
import com.kizuna.core.entity.User;
import com.kizuna.core.exception.BadRequestException;
import com.kizuna.core.exception.ConflictException;
import com.kizuna.features.profile.dto.request.UpdateTutorProfileRequest;
import com.kizuna.features.profile.service.impl.TutorProfileServiceImpl;
import com.kizuna.features.tutor_search.service.TutorSearchService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit test FE-02 / UC-02.04 Update Tutor Profile - TutorProfileServiceImpl.updateMyProfile(email, request)
 * và ràng buộc dữ liệu trên UpdateTutorProfileRequest.
 */
class TutorProfileUpdateTest extends ProfileUnitTestBase {
    private static final String COARSE = "Địa chỉ công khai chỉ nên ghi Quận/Huyện hoặc khu vực chung, không ghi số nhà, tên đường hay tọa độ chi tiết.";
    private final TutorSearchService search = mock(TutorSearchService.class);
    private final TutorProfileServiceImpl service = new TutorProfileServiceImpl(users, tutors, avatars, search);

    {
        when(search.getAvailableLocations()).thenReturn(List.of("Hà Nội", "TP. Hồ Chí Minh"));
        when(tutors.saveAndFlush(any(Tutor.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    private static UpdateTutorProfileRequest req(String headline, String bio, Integer years, String rate,
                                                 String jlpt, String province, String area) {
        return new UpdateTutorProfileRequest("Tran Thi C", "0912345678", NEW_AVATAR, headline, bio, years,
                rate == null ? null : new BigDecimal(rate), jlpt, province, area);
    }

    private static UpdateTutorProfileRequest valid() {
        return req("JLPT N1 teacher", "Bio", 3, "200000", "N1", "Hà Nội", "Cầu Giấy");
    }

    private Tutor existingTutor(User u) {
        Tutor t = Tutor.builder().user(u).build();
        when(tutors.findByUser_UserId(USER_ID)).thenReturn(Optional.of(t));
        return t;
    }

    // ---- Logic trong service ----

    @Test @DisplayName("TC-02-027: Cập nhật hợp lệ -> lưu user và tutor, địa chỉ = 'Tỉnh - Khu vực'")
    void tc02_027_validUpdate() {
        User u = loggedIn("ROLE_TUTOR");
        Tutor t = existingTutor(u);
        phoneTaken(false);

        var dto = service.updateMyProfile(EMAIL, valid());

        assertEquals("Tran Thi C", u.getFullName());
        assertEquals("JLPT N1 teacher", t.getHeadline());
        assertEquals((short) 3, t.getExperienceYears());
        assertEquals(0, new BigDecimal("200000").compareTo(t.getHourlyRate()));
        assertEquals("N1", t.getHighestJlptLevel());
        assertEquals("Hà Nội - Cầu Giấy", t.getAddress());
        verify(users).saveAndFlush(u);
        verify(tutors).saveAndFlush(t);
        assertEquals("Hà Nội", dto.province());
    }

    @Test @DisplayName("TC-02-028/038: Tỉnh không thuộc danh sách hỗ trợ -> 400 'Tỉnh/Thành phố không nằm trong danh sách hỗ trợ.'")
    void tc02_038_unsupportedProvince() {
        loggedIn("ROLE_TUTOR");
        var ex = assertThrows(BadRequestException.class,
                () -> service.updateMyProfile(EMAIL, req("H", null, 1, "1", "N1", "Paris", null)));
        assertEquals("Tỉnh/Thành phố không nằm trong danh sách hỗ trợ.", ex.getMessage());
        verify(search).getAvailableLocations();
        verify(users, never()).saveAndFlush(any());
    }

    @Test @DisplayName("TC-02-039: Khu vực có số nhà và tên đường '12 Nguyễn Trãi' -> 400 địa chỉ quá chi tiết")
    void tc02_039_streetAddress() {
        loggedIn("ROLE_TUTOR");
        var ex = assertThrows(BadRequestException.class,
                () -> service.updateMyProfile(EMAIL, req("H", null, 1, "1", "N1", "Hà Nội", "12 Nguyễn Trãi")));
        assertEquals(COARSE, ex.getMessage());
        verify(users, never()).saveAndFlush(any());
    }

    @Test @DisplayName("TC-02-040: Khu vực là tọa độ GPS -> 400 địa chỉ quá chi tiết")
    void tc02_040_gps() {
        loggedIn("ROLE_TUTOR");
        var ex = assertThrows(BadRequestException.class,
                () -> service.updateMyProfile(EMAIL, req("H", null, 1, "1", "N1", "Hà Nội", "21.0285, 105.8542")));
        assertEquals(COARSE, ex.getMessage());
    }

    @Test @DisplayName("TC-02-042: SĐT đã thuộc tài khoản khác -> 409, không lưu")
    void tc02_042_phoneTaken() {
        loggedIn("ROLE_TUTOR");
        phoneTaken(true);
        var ex = assertThrows(ConflictException.class, () -> service.updateMyProfile(EMAIL, valid()));
        assertEquals("Số điện thoại này đã được liên kết với một tài khoản khác trong hệ thống.", ex.getMessage());
        verify(tutors, never()).saveAndFlush(any());
    }

    @Test @DisplayName("TC-02-043: Đổi ảnh đại diện -> sau khi lưu gọi xóa ảnh cũ")
    void tc02_043_deleteOldAvatar() {
        User u = loggedIn("ROLE_TUTOR");
        existingTutor(u);
        phoneTaken(false);

        service.updateMyProfile(EMAIL, valid());

        verify(avatars).requireOwnAvatar(NEW_AVATAR, OLD_AVATAR, USER_ID);
        verify(avatars).deleteReplaced(OLD_AVATAR, NEW_AVATAR);
    }

    // ---- Ràng buộc dữ liệu (Bean Validation) ----

    @Test @DisplayName("TC-02-027: Dữ liệu hợp lệ -> không có lỗi")
    void tc02_027_validRequestHasNoError() {
        assertTrue(violations(valid()).isEmpty());
    }

    @Test @DisplayName("TC-02-030: Tiêu đề trống -> 'Vui lòng nhập tiêu đề giảng dạy.'")
    void tc02_030_emptyHeadline() {
        assertTrue(hasError(req(" ", null, 1, "1", "N1", "Hà Nội", null), "headline", "Vui lòng nhập tiêu đề giảng dạy."));
    }

    @Test @DisplayName("TC-02-031: Tiêu đề 200 ký tự hợp lệ, 201 ký tự -> lỗi")
    void tc02_031_headlineBoundary() {
        assertTrue(violations(req(chars(200), null, 1, "1", "N1", "Hà Nội", null)).isEmpty());
        assertTrue(hasError(req(chars(201), null, 1, "1", "N1", "Hà Nội", null), "headline", "Tiêu đề giảng dạy tối đa 200 ký tự."));
    }

    @Test @DisplayName("TC-02-032: Giới thiệu 3000 ký tự hợp lệ, 3001 ký tự -> lỗi")
    void tc02_032_bioBoundary() {
        assertTrue(violations(req("H", chars(3000), 1, "1", "N1", "Hà Nội", null)).isEmpty());
        assertTrue(hasError(req("H", chars(3001), 1, "1", "N1", "Hà Nội", null), "bio", "Giới thiệu bản thân tối đa 3000 ký tự."));
    }

    @Test @DisplayName("TC-02-033: Kinh nghiệm 0 và 50 năm (biên) -> hợp lệ")
    void tc02_033_experienceValidBoundary() {
        assertTrue(violations(req("H", null, 0, "1", "N1", "Hà Nội", null)).isEmpty());
        assertTrue(violations(req("H", null, 50, "1", "N1", "Hà Nội", null)).isEmpty());
    }

    @Test @DisplayName("TC-02-034: Kinh nghiệm -1 và 51 năm -> 'Số năm kinh nghiệm phải từ 0 đến 50 năm.'")
    void tc02_034_experienceOutOfRange() {
        String msg = "Số năm kinh nghiệm phải từ 0 đến 50 năm.";
        assertTrue(hasError(req("H", null, -1, "1", "N1", "Hà Nội", null), "experienceYears", msg));
        assertTrue(hasError(req("H", null, 51, "1", "N1", "Hà Nội", null), "experienceYears", msg));
    }

    @Test @DisplayName("TC-02-035: Học phí 0 và 50.000.000 VND (biên) -> hợp lệ")
    void tc02_035_rateValidBoundary() {
        assertTrue(violations(req("H", null, 1, "0", "N1", "Hà Nội", null)).isEmpty());
        assertTrue(violations(req("H", null, 1, "50000000", "N1", "Hà Nội", null)).isEmpty());
    }

    @Test @DisplayName("TC-02-036: Học phí 50.000.001 VND -> 'Học phí vượt quá giới hạn cho phép.'")
    void tc02_036_rateTooHigh() {
        assertTrue(hasError(req("H", null, 1, "50000001", "N1", "Hà Nội", null), "hourlyRate", "Học phí vượt quá giới hạn cho phép."));
    }

    @Test @DisplayName("TC-02-037: Chưa chọn trình độ JLPT -> 'Vui lòng chọn trình độ JLPT cao nhất.'")
    void tc02_037_missingJlpt() {
        assertTrue(hasError(req("H", null, 1, "1", "", "Hà Nội", null), "highestJlptLevel", "Vui lòng chọn trình độ JLPT cao nhất."));
    }

    @Test @DisplayName("TC-02-041: Khu vực 151 ký tự -> 'Quận/Huyện hoặc khu vực tối đa 150 ký tự.'")
    void tc02_041_area151() {
        assertTrue(hasError(req("H", null, 1, "1", "N1", "Hà Nội", chars(151)), "addressDetail", "Quận/Huyện hoặc khu vực tối đa 150 ký tự."));
    }
}
