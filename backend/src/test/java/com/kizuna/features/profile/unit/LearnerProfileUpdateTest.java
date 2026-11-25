package com.kizuna.features.profile.unit;

import com.kizuna.core.entity.Learner;
import com.kizuna.core.entity.User;
import com.kizuna.core.exception.BadRequestException;
import com.kizuna.core.exception.ConflictException;
import com.kizuna.features.profile.dto.request.UpdateLearnerProfileRequest;
import com.kizuna.features.profile.service.impl.LearnerProfileServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit test FE-02 / UC-02.02 Update Learner Profile - LearnerProfileServiceImpl.updateMyProfile(email, request)
 * và ràng buộc dữ liệu trên UpdateLearnerProfileRequest (Bean Validation, chạy trước khi vào service).
 */
class LearnerProfileUpdateTest extends ProfileUnitTestBase {
    private final LearnerProfileServiceImpl service = new LearnerProfileServiceImpl(users, learners, avatars);

    private static UpdateLearnerProfileRequest req(String fullName, String phone, String avatar, String current,
                                                   String target, String goals, String province, String address) {
        return new UpdateLearnerProfileRequest(fullName, phone, avatar, current, target, goals, province, address);
    }

    private static UpdateLearnerProfileRequest valid() {
        return req("Nguyen Van B", "0912345678", NEW_AVATAR, "N5", "N3", "Pass N3", "Hà Nội", "Cầu Giấy");
    }

    private Learner existingLearner(User u) {
        Learner l = Learner.builder().user(u).currentLevel("BEGINNER").build();
        when(learners.findByUser_UserId(USER_ID)).thenReturn(Optional.of(l));
        when(learners.saveAndFlush(any(Learner.class))).thenAnswer(inv -> inv.getArgument(0));
        return l;
    }

    // ---- Logic trong service ----

    @Test @DisplayName("TC-02-005: Cập nhật đủ trường hợp lệ -> lưu user và learner, trả hồ sơ mới")
    void tc02_005_validUpdate() {
        User u = loggedIn("ROLE_LEARNER");
        Learner l = existingLearner(u);
        phoneTaken(false);

        var dto = service.updateMyProfile(EMAIL, valid());

        assertEquals("Nguyen Van B", u.getFullName());
        assertEquals("0912345678", u.getPhone());
        assertEquals(NEW_AVATAR, u.getAvatarUrl());
        assertEquals("N5", l.getCurrentLevel());
        assertEquals("N3", l.getTargetLevel());
        assertEquals("Hà Nội", l.getProvince());
        verify(users).saveAndFlush(u);
        verify(learners).saveAndFlush(l);
        assertEquals("Nguyen Van B", dto.fullName());
    }

    @Test @DisplayName("TC-02-011: SĐT để trống -> lưu SĐT rỗng, không kiểm tra trùng")
    void tc02_011_emptyPhone() {
        User u = loggedIn("ROLE_LEARNER");
        existingLearner(u);

        service.updateMyProfile(EMAIL, req("Nguyen Van B", "", null, null, null, null, null, null));

        assertNull(u.getPhone());
        verify(users, never()).exists(any(org.springframework.data.jpa.domain.Specification.class));
    }

    @Test @DisplayName("TC-02-020: SĐT đã thuộc tài khoản khác -> 409, không lưu")
    void tc02_020_phoneTaken() {
        loggedIn("ROLE_LEARNER");
        phoneTaken(true);

        var ex = assertThrows(ConflictException.class, () -> service.updateMyProfile(EMAIL, valid()));
        assertEquals("Số điện thoại này đã được liên kết với một tài khoản khác trong hệ thống.", ex.getMessage());
        verify(users, never()).saveAndFlush(any());
    }

    @Test @DisplayName("TC-02-021: Ảnh đại diện không do học viên tải lên -> 400, không lưu")
    void tc02_021_avatarNotOwned() {
        loggedIn("ROLE_LEARNER");
        phoneTaken(false);
        doThrow(new BadRequestException("Ảnh đại diện chỉ nhận ảnh tải lên từ máy của bạn. Vui lòng chọn ảnh và tải lên lại."))
                .when(avatars).requireOwnAvatar(NEW_AVATAR, OLD_AVATAR, USER_ID);

        assertThrows(BadRequestException.class, () -> service.updateMyProfile(EMAIL, valid()));
        verify(users, never()).saveAndFlush(any());
        verify(avatars, never()).deleteReplaced(any(), any());
    }

    @Test @DisplayName("TC-02-022: Đổi ảnh đại diện -> sau khi lưu gọi xóa ảnh cũ (cũ, mới)")
    void tc02_022_deleteOldAvatar() {
        User u = loggedIn("ROLE_LEARNER");
        existingLearner(u);
        phoneTaken(false);

        service.updateMyProfile(EMAIL, valid());

        verify(avatars).deleteReplaced(OLD_AVATAR, NEW_AVATAR);
    }

    @Test @DisplayName("TC-02-023: Email không đổi được qua cập nhật hồ sơ (request không có trường email)")
    void tc02_023_emailUnchanged() {
        User u = loggedIn("ROLE_LEARNER");
        existingLearner(u);
        phoneTaken(false);

        var dto = service.updateMyProfile(EMAIL, valid());

        assertEquals(EMAIL, u.getEmail());
        assertEquals(EMAIL, dto.email());
    }

    @Test @DisplayName("TC-02-005: Học viên chưa có bản ghi learner -> tạo mới khi lưu")
    void tc02_005_createsLearnerRow() {
        loggedIn("ROLE_LEARNER");
        when(learners.findByUser_UserId(USER_ID)).thenReturn(Optional.empty());
        when(learners.saveAndFlush(any(Learner.class))).thenAnswer(inv -> inv.getArgument(0));
        phoneTaken(false);

        service.updateMyProfile(EMAIL, valid());

        verify(learners).saveAndFlush(argThat(l -> "N3".equals(l.getTargetLevel())));
    }

    // ---- Ràng buộc dữ liệu (Bean Validation) ----

    @Test @DisplayName("TC-02-005: Dữ liệu hợp lệ -> không có lỗi")
    void tc02_005_validRequestHasNoError() {
        assertTrue(violations(valid()).isEmpty());
    }

    @Test @DisplayName("TC-02-007: Họ tên trống -> 'Vui lòng nhập họ và tên.'")
    void tc02_007_emptyName() {
        assertTrue(hasError(req(" ", null, null, null, null, null, null, null), "fullName", "Vui lòng nhập họ và tên."));
    }

    @Test @DisplayName("TC-02-008: Họ tên đúng 100 ký tự (biên) -> hợp lệ")
    void tc02_008_name100() {
        assertTrue(violations(req(chars(100), null, null, null, null, null, null, null)).isEmpty());
    }

    @Test @DisplayName("TC-02-009: Họ tên 101 ký tự (vượt biên) -> 'Họ và tên tối đa 100 ký tự.'")
    void tc02_009_name101() {
        assertTrue(hasError(req(chars(101), null, null, null, null, null, null, null), "fullName", "Họ và tên tối đa 100 ký tự."));
    }

    @Test @DisplayName("TC-02-010: SĐT +84912345678 -> hợp lệ")
    void tc02_010_phonePlus84() {
        assertTrue(violations(req("A", "+84912345678", null, null, null, null, null, null)).isEmpty());
    }

    @Test @DisplayName("TC-02-011: SĐT trống -> hợp lệ")
    void tc02_011_phoneEmptyValid() {
        assertTrue(violations(req("A", "", null, null, null, null, null, null)).isEmpty());
    }

    @Test @DisplayName("TC-02-012: SĐT 9 chữ số -> 'Số điện thoại không đúng định dạng 10 chữ số.'")
    void tc02_012_phone9Digits() {
        assertTrue(hasError(req("A", "091234567", null, null, null, null, null, null), "phone", "Số điện thoại không đúng định dạng 10 chữ số."));
    }

    @Test @DisplayName("TC-02-013: SĐT có chữ -> lỗi định dạng")
    void tc02_013_phoneLetters() {
        assertTrue(hasError(req("A", "09abc45678", null, null, null, null, null, null), "phone", "Số điện thoại không đúng định dạng 10 chữ số."));
    }

    @Test @DisplayName("TC-02-014: Trình độ hiện tại N6 -> 'Trình độ hiện tại không hợp lệ.'")
    void tc02_014_currentLevelInvalid() {
        assertTrue(hasError(req("A", null, null, "N6", null, null, null, null), "currentLevel", "Trình độ hiện tại không hợp lệ."));
    }

    @Test @DisplayName("TC-02-015: Trình độ mục tiêu BEGINNER -> 'Trình độ mục tiêu không hợp lệ.'")
    void tc02_015_targetBeginner() {
        assertTrue(hasError(req("A", null, null, null, "BEGINNER", null, null, null), "targetLevel", "Trình độ mục tiêu không hợp lệ."));
    }

    @Test @DisplayName("TC-02-016: Mục tiêu học 2000 ký tự (biên) -> hợp lệ")
    void tc02_016_goals2000() {
        assertTrue(violations(req("A", null, null, null, null, chars(2000), null, null)).isEmpty());
    }

    @Test @DisplayName("TC-02-017: Mục tiêu học 2001 ký tự -> 'Mục tiêu học tập tối đa 2000 ký tự.'")
    void tc02_017_goals2001() {
        assertTrue(hasError(req("A", null, null, null, null, chars(2001), null, null), "learningGoals", "Mục tiêu học tập tối đa 2000 ký tự."));
    }

    @Test @DisplayName("TC-02-018: Tỉnh/Thành 101 ký tự -> 'Tỉnh/Thành phố tối đa 100 ký tự.'")
    void tc02_018_province101() {
        assertTrue(hasError(req("A", null, null, null, null, null, chars(101), null), "province", "Tỉnh/Thành phố tối đa 100 ký tự."));
    }

    @Test @DisplayName("TC-02-019: Địa chỉ 256 ký tự -> 'Địa chỉ chi tiết tối đa 255 ký tự.'")
    void tc02_019_address256() {
        assertTrue(hasError(req("A", null, null, null, null, null, null, chars(256)), "address", "Địa chỉ chi tiết tối đa 255 ký tự."));
    }
}
