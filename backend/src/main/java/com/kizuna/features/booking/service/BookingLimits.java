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
    public static final int TRIAL_RESPONSE_HOURS = 24;

    /**
     * Yêu cầu kết nối trực tiếp tự hết hạn nếu gia sư chưa phản hồi sau chừng này giờ.
     * Trong thời gian chờ, các buổi của yêu cầu GIỮ CHỖ nên không để quá lâu.
     */
    public static final int DIRECT_RESPONSE_HOURS = 24;

    /** Học viên được xem lịch rảnh và chọn ngày bắt đầu học trong chừng này tuần (tính cả tuần hiện tại). */
    public static final int DIRECT_SCHEDULE_WEEKS = 4;

    /** Số buổi tối đa của một yêu cầu kết nối trực tiếp. */
    public static final int MAX_CONNECTION_SESSIONS = 12;

    /** Số buổi mặc định khi học viên không chọn. */
    public static final int DEFAULT_CONNECTION_SESSIONS = 8;

    /** Số lần tối đa một học viên đề xuất học thử NGOÀI lịch rảnh với cùng một gia sư trong 7 ngày. */
    public static final int OFF_SCHEDULE_PROPOSALS_PER_WEEK = 2;

    /** Bị gia sư từ chối thì phải chờ chừng này giờ mới được gửi lại cho đúng gia sư đó. */
    public static final int DECLINE_COOLDOWN_HOURS = 24;
}
