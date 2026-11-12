package com.kizuna.features.booking.service;

import com.kizuna.features.booking.entity.TrialBookingStatus;

import java.util.List;

/**
 * Giới hạn chống spam yêu cầu học thử / kết nối trực tiếp.
 * Đặt cố định trong code; khi cần cho admin chỉnh thì chuyển sang chính sách đặt lịch.
 */
public final class BookingLimits {

    private BookingLimits() {
    }

    /** Trạng thái được coi là "đang mở": mỗi cặp học viên - gia sư chỉ có tối đa 1 buổi học thử ở các trạng thái này. */
    public static final List<TrialBookingStatus> OPEN_TRIAL_STATUSES =
            List.of(TrialBookingStatus.PENDING, TrialBookingStatus.CONFIRMED, TrialBookingStatus.RESCHEDULED);

    /** Yêu cầu học thử tự hết hạn nếu gia sư chưa phản hồi sau chừng này giờ (tính từ lần gửi/đổi lịch gần nhất), hoặc khi tới giờ học. */
    public static final int TRIAL_RESPONSE_HOURS = 48;

    /** Yêu cầu kết nối trực tiếp tự hết hạn nếu gia sư chưa phản hồi sau chừng này ngày. */
    public static final int DIRECT_RESPONSE_DAYS = 5;

    /** Học viên được xem lịch rảnh và chọn ngày bắt đầu học trong chừng này tuần (tính cả tuần hiện tại). */
    public static final int DIRECT_SCHEDULE_WEEKS = 4;

    /** Bị gia sư từ chối thì phải chờ chừng này giờ mới được gửi lại cho đúng gia sư đó. */
    public static final int DECLINE_COOLDOWN_HOURS = 24;
}
