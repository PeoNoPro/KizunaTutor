package com.kizuna.features.booking.service;

import com.kizuna.core.entity.Tutor;
import com.kizuna.core.entity.User;
import com.kizuna.core.exception.BadRequestException;
import com.kizuna.features.booking.entity.ConnectionRequest;

import java.time.OffsetDateTime;
import java.util.regex.Pattern;

/**
 * Chia sẻ liên hệ giữa học viên và gia sư (migration 027_connection_contact_share.sql).
 * <ul>
 *   <li>Học viên gửi yêu cầu: bắt buộc đồng ý gửi email + số điện thoại cho gia sư; lưu bản sao theo từng yêu cầu.</li>
 *   <li>Gia sư thấy liên hệ học viên khi yêu cầu chưa bị từ chối / hủy / hết hạn.</li>
 *   <li>Học viên thấy liên hệ gia sư khi gia sư đã đồng ý (khóa học đã kết thúc vẫn giữ).</li>
 * </ul>
 */
public final class ContactShare {

    private ContactShare() {
    }

    private static final Pattern VN_PHONE = Pattern.compile("^0\\d{9}$");
    private static final Pattern EMAIL = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");

    /** Liên hệ của một bên. */
    public record Contact(String email, String phone) {
        public static final Contact NONE = new Contact(null, null);
    }

    /** "+84 912 345 678", "0912.345.678" → "0912345678"; null nếu để trống. */
    public static String normalizePhone(String raw) {
        if (raw == null) return null;
        String digits = raw.replaceAll("[^0-9+]", "");
        if (digits.startsWith("+84")) digits = "0" + digits.substring(3);
        else if (digits.startsWith("84") && digits.length() == 11) digits = "0" + digits.substring(2);
        return digits.isEmpty() ? null : digits;
    }

    /**
     * Kiểm tra và ghi liên hệ học viên vào yêu cầu. Thiếu số điện thoại thì lấy trong hồ sơ; vẫn thiếu thì báo lỗi.
     * Email để trống thì dùng email tài khoản. Hồ sơ chưa có số điện thoại thì lưu luôn số vừa nhập.
     */
    public static void attachLearnerContact(ConnectionRequest connection, User learnerUser,
                                            String email, String phone, Boolean consent) {
        if (!Boolean.TRUE.equals(consent)) {
            throw new BadRequestException("Vui lòng đồng ý chia sẻ hồ sơ, email và số điện thoại với gia sư để gia sư liên hệ với bạn.");
        }
        String p = normalizePhone(phone);
        if (p == null) p = normalizePhone(learnerUser.getPhone());
        if (p == null || !VN_PHONE.matcher(p).matches()) {
            throw new BadRequestException("Số điện thoại không hợp lệ. Vui lòng nhập 10 số, bắt đầu bằng 0.");
        }
        String e = email == null || email.isBlank() ? learnerUser.getEmail() : email.trim();
        if (e == null || e.length() > 255 || !EMAIL.matcher(e).matches()) {
            throw new BadRequestException("Email liên hệ không hợp lệ.");
        }
        connection.setContactEmail(e);
        connection.setContactPhone(p);
        connection.setContactConsentAt(OffsetDateTime.now());
        if (learnerUser.getPhone() == null || learnerUser.getPhone().isBlank()) {
            learnerUser.setPhone(p);
        }
    }

    /** Liên hệ học viên đã gửi kèm yêu cầu (yêu cầu cũ chưa có thì lấy theo tài khoản). */
    public static Contact learnerContact(ConnectionRequest connection, User learnerUser) {
        if (connection != null && (connection.getContactEmail() != null || connection.getContactPhone() != null)) {
            return new Contact(connection.getContactEmail(), connection.getContactPhone());
        }
        return learnerUser == null ? Contact.NONE : new Contact(learnerUser.getEmail(), learnerUser.getPhone());
    }

    /** Liên hệ gia sư: số điện thoại trong hồ sơ gia sư, không có thì theo tài khoản. */
    public static Contact tutorContact(Tutor tutor) {
        if (tutor == null || tutor.getUser() == null) return Contact.NONE;
        String phone = tutor.getPhone() != null && !tutor.getPhone().isBlank() ? tutor.getPhone() : tutor.getUser().getPhone();
        return new Contact(tutor.getUser().getEmail(), phone);
    }
}
