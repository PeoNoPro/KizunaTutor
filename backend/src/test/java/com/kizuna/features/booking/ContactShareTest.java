package com.kizuna.features.booking;

import com.kizuna.core.entity.Tutor;
import com.kizuna.core.entity.User;
import com.kizuna.core.exception.BadRequestException;
import com.kizuna.features.booking.entity.ConnectionRequest;
import com.kizuna.features.booking.service.ContactShare;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** Học viên gửi hồ sơ + email + số điện thoại cho gia sư khi gửi yêu cầu (học thử hoặc kết nối khóa học). */
class ContactShareTest {

    private static User learner(String phone) {
        return User.builder().email("hv@test.com").phone(phone).build();
    }

    @Test
    @DisplayName("Bắt buộc đồng ý chia sẻ liên hệ")
    void consentRequired() {
        ConnectionRequest cr = new ConnectionRequest();
        BadRequestException ex = assertThrows(BadRequestException.class,
                () -> ContactShare.attachLearnerContact(cr, learner(null), null, "0912345678", null));
        assertTrue(ex.getMessage().contains("đồng ý chia sẻ"));
        assertThrows(BadRequestException.class,
                () -> ContactShare.attachLearnerContact(cr, learner(null), null, "0912345678", false));
    }

    @Test
    @DisplayName("Chuẩn hóa số điện thoại: +84 / dấu chấm / khoảng trắng → 10 số bắt đầu bằng 0")
    void normalizesPhone() {
        assertEquals("0912345678", ContactShare.normalizePhone("+84 912 345 678"));
        assertEquals("0912345678", ContactShare.normalizePhone("0912.345.678"));
        assertEquals("0912345678", ContactShare.normalizePhone("84912345678"));
        assertNull(ContactShare.normalizePhone("  "));
    }

    @Test
    @DisplayName("Số điện thoại sai định dạng hoặc thiếu (hồ sơ cũng không có) → báo lỗi")
    void invalidOrMissingPhone() {
        ConnectionRequest cr = new ConnectionRequest();
        assertThrows(BadRequestException.class, () -> ContactShare.attachLearnerContact(cr, learner(null), null, "12345", true));
        BadRequestException ex = assertThrows(BadRequestException.class,
                () -> ContactShare.attachLearnerContact(cr, learner(null), null, null, true));
        assertTrue(ex.getMessage().contains("10 số"));
    }

    @Test
    @DisplayName("Lưu bản sao liên hệ vào yêu cầu; email trống dùng email tài khoản; hồ sơ chưa có số thì lưu luôn")
    void attachesSnapshotAndFillsProfile() {
        ConnectionRequest cr = new ConnectionRequest();
        User user = learner(null);
        ContactShare.attachLearnerContact(cr, user, " ", "+84912345678", true);
        assertEquals("hv@test.com", cr.getContactEmail());
        assertEquals("0912345678", cr.getContactPhone());
        assertNotNull(cr.getContactConsentAt());
        assertEquals("0912345678", user.getPhone());
    }

    @Test
    @DisplayName("Không nhập số thì dùng số trong hồ sơ; hồ sơ đã có số thì không ghi đè")
    void fallsBackToProfilePhone() {
        ConnectionRequest cr = new ConnectionRequest();
        User user = learner("0987654321");
        ContactShare.attachLearnerContact(cr, user, "khac@test.com", null, true);
        assertEquals("0987654321", cr.getContactPhone());
        assertEquals("khac@test.com", cr.getContactEmail());

        ConnectionRequest other = new ConnectionRequest();
        ContactShare.attachLearnerContact(other, user, null, "0911111111", true);
        assertEquals("0987654321", user.getPhone());
    }

    @Test
    @DisplayName("Liên hệ gia sư: ưu tiên số trong hồ sơ gia sư, không có thì theo tài khoản")
    void tutorContact() {
        User u = User.builder().email("gs@test.com").phone("0900000000").build();
        assertEquals("0911222333", ContactShare.tutorContact(Tutor.builder().user(u).phone("0911222333").build()).phone());
        assertEquals("0900000000", ContactShare.tutorContact(Tutor.builder().user(u).build()).phone());
        assertEquals("gs@test.com", ContactShare.tutorContact(Tutor.builder().user(u).build()).email());
    }
}
