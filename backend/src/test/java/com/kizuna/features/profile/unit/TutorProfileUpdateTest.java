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

    @Test @DisplayName("UTCID01: Cập nhật hợp lệ -> lưu user và tutor, địa chỉ = 'Tỉnh - Khu vực'")
    void utcid01_validUpdate() {
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

    @Test @DisplayName("UTCID02: Tỉnh không thuộc danh sách hỗ trợ -> 400 'Tỉnh/Thành phố không nằm trong danh sách hỗ trợ.'")
    void utcid02_unsupportedProvince() {
        loggedIn("ROLE_TUTOR");
        var ex = assertThrows(BadRequestException.class,
                () -> service.updateMyProfile(EMAIL, req("H", null, 1, "1", "N1", "Paris", null)));
        assertEquals("Tỉnh/Thành phố không nằm trong danh sách hỗ trợ.", ex.getMessage());
        verify(search).getAvailableLocations();
        verify(users, never()).saveAndFlush(any());
    }

    @Test @DisplayName("UTCID03: Khu vực có số nhà và tên đường '12 Nguyễn Trãi' -> 400 địa chỉ quá chi tiết")
    void utcid03_streetAddress() {
        loggedIn("ROLE_TUTOR");
        var ex = assertThrows(BadRequestException.class,
                () -> service.updateMyProfile(EMAIL, req("H", null, 1, "1", "N1", "Hà Nội", "12 Nguyễn Trãi")));
        assertEquals(COARSE, ex.getMessage());
        verify(users, never()).saveAndFlush(any());
    }

    @Test @DisplayName("UTCID04: Khu vực là tọa độ GPS -> 400 địa chỉ quá chi tiết")
    void utcid04_gps() {
        loggedIn("ROLE_TUTOR");
        var ex = assertThrows(BadRequestException.class,
                () -> service.updateMyProfile(EMAIL, req("H", null, 1, "1", "N1", "Hà Nội", "21.0285, 105.8542")));
        assertEquals(COARSE, ex.getMessage());
    }

    @Test @DisplayName("UTCID05: SĐT đã thuộc tài khoản khác -> 409, không lưu")
    void utcid05_phoneTaken() {
        loggedIn("ROLE_TUTOR");
        phoneTaken(true);
        var ex = assertThrows(ConflictException.class, () -> service.updateMyProfile(EMAIL, valid()));
        assertEquals("Số điện thoại này đã được liên kết với một tài khoản khác trong hệ thống.", ex.getMessage());
        verify(tutors, never()).saveAndFlush(any());
    }

    @Test @DisplayName("UTCID06: Đổi ảnh đại diện -> sau khi lưu gọi xóa ảnh cũ")
    void utcid06_deleteOldAvatar() {
        User u = loggedIn("ROLE_TUTOR");
        existingTutor(u);
        phoneTaken(false);

        service.updateMyProfile(EMAIL, valid());

        verify(avatars).requireOwnAvatar(NEW_AVATAR, OLD_AVATAR, USER_ID);
        verify(avatars).deleteReplaced(OLD_AVATAR, NEW_AVATAR);
    }

    // ---- Ràng buộc dữ liệu (Bean Validation) ----

    @Test @DisplayName("UTCID07: Dữ liệu hợp lệ -> không có lỗi")
    void utcid07_validRequestHasNoError() {
        assertTrue(violations(valid()).isEmpty());
    }

    @Test @DisplayName("UTCID08: Tiêu đề trống -> 'Vui lòng nhập tiêu đề giảng dạy.'")
    void utcid08_emptyHeadline() {
        assertTrue(hasError(req(" ", null, 1, "1", "N1", "Hà Nội", null), "headline", "Vui lòng nhập tiêu đề giảng dạy."));
    }

    @Test @DisplayName("UTCID09: Tiêu đề 200 ký tự (biên) -> hợp lệ")
    void utcid09_headline200() {
        assertTrue(violations(req(chars(200), null, 1, "1", "N1", "Hà Nội", null)).isEmpty());
    }

    @Test @DisplayName("UTCID10: Tiêu đề 201 ký tự -> 'Tiêu đề giảng dạy tối đa 200 ký tự.'")
    void utcid10_headline201() {
        assertTrue(hasError(req(chars(201), null, 1, "1", "N1", "Hà Nội", null), "headline", "Tiêu đề giảng dạy tối đa 200 ký tự."));
    }

    @Test @DisplayName("UTCID11: Giới thiệu 3000 ký tự (biên) -> hợp lệ")
    void utcid11_bio3000() {
        assertTrue(violations(req("H", chars(3000), 1, "1", "N1", "Hà Nội", null)).isEmpty());
    }

    @Test @DisplayName("UTCID12: Giới thiệu 3001 ký tự -> 'Giới thiệu bản thân tối đa 3000 ký tự.'")
    void utcid12_bio3001() {
        assertTrue(hasError(req("H", chars(3001), 1, "1", "N1", "Hà Nội", null), "bio", "Giới thiệu bản thân tối đa 3000 ký tự."));
    }

    @Test @DisplayName("UTCID13: Kinh nghiệm 0 năm (biên dưới) -> hợp lệ")
    void utcid13_experience0() {
        assertTrue(violations(req("H", null, 0, "1", "N1", "Hà Nội", null)).isEmpty());
    }

    @Test @DisplayName("UTCID14: Kinh nghiệm 50 năm (biên trên) -> hợp lệ")
    void utcid14_experience50() {
        assertTrue(violations(req("H", null, 50, "1", "N1", "Hà Nội", null)).isEmpty());
    }

    @Test @DisplayName("UTCID15: Kinh nghiệm -1 năm -> 'Số năm kinh nghiệm phải từ 0 đến 50 năm.'")
    void utcid15_experienceMinus1() {
        assertTrue(hasError(req("H", null, -1, "1", "N1", "Hà Nội", null), "experienceYears", "Số năm kinh nghiệm phải từ 0 đến 50 năm."));
    }

    @Test @DisplayName("UTCID16: Kinh nghiệm 51 năm -> 'Số năm kinh nghiệm phải từ 0 đến 50 năm.'")
    void utcid16_experience51() {
        assertTrue(hasError(req("H", null, 51, "1", "N1", "Hà Nội", null), "experienceYears", "Số năm kinh nghiệm phải từ 0 đến 50 năm."));
    }

    @Test @DisplayName("UTCID17: Học phí 0 VND (biên dưới) -> hợp lệ")
    void utcid17_rate0() {
        assertTrue(violations(req("H", null, 1, "0", "N1", "Hà Nội", null)).isEmpty());
    }

    @Test @DisplayName("UTCID18: Học phí 50.000.000 VND (biên trên) -> hợp lệ")
    void utcid18_rate50m() {
        assertTrue(violations(req("H", null, 1, "50000000", "N1", "Hà Nội", null)).isEmpty());
    }

    @Test @DisplayName("UTCID19: Học phí 50.000.001 VND -> 'Học phí vượt quá giới hạn cho phép.'")
    void utcid19_rateTooHigh() {
        assertTrue(hasError(req("H", null, 1, "50000001", "N1", "Hà Nội", null), "hourlyRate", "Học phí vượt quá giới hạn cho phép."));
    }

    @Test @DisplayName("UTCID20: Chưa chọn trình độ JLPT -> 'Vui lòng chọn trình độ JLPT cao nhất.'")
    void utcid20_missingJlpt() {
        assertTrue(hasError(req("H", null, 1, "1", "", "Hà Nội", null), "highestJlptLevel", "Vui lòng chọn trình độ JLPT cao nhất."));
    }

    @Test @DisplayName("UTCID21: Khu vực 151 ký tự -> 'Quận/Huyện hoặc khu vực tối đa 150 ký tự.'")
    void utcid21_area151() {
        assertTrue(hasError(req("H", null, 1, "1", "N1", "Hà Nội", chars(151)), "addressDetail", "Quận/Huyện hoặc khu vực tối đa 150 ký tự."));
    }
}
