package com.kizuna.features.ticket.service;

import java.time.Duration;
import java.time.OffsetDateTime;

/**
 * Mức ưu tiên ban đầu của yêu cầu hỗ trợ (bổ sung 2026-11-09). Người gửi không tự chọn mức ưu tiên;
 * hệ thống đặt theo nhóm yêu cầu và thông tin liên quan để tự giao đúng thứ tự, CS phụ trách vẫn sửa được sau.
 * <ul>
 *   <li>Gắn buổi học thử bắt đầu trong vòng 24 giờ tới: Khẩn cấp (sắp tới giờ học).</li>
 *   <li>Thanh toán, Sự cố kỹ thuật: Cao (liên quan tiền hoặc không dùng được hệ thống).</li>
 *   <li>Nội dung khác: Thấp.</li>
 *   <li>Còn lại (Tài khoản, Kết nối gia sư, Đặt học thử, Hội viên): Bình thường.</li>
 * </ul>
 */
public final class SupportTicketPriorityRule {
    static final Duration SOON = Duration.ofHours(24);

    private SupportTicketPriorityRule() {}

    public static String initial(String category, OffsetDateTime bookingStart, OffsetDateTime now) {
        if (bookingStart != null && !bookingStart.isBefore(now) && bookingStart.isBefore(now.plus(SOON))) return "URGENT";
        return switch (category) {
            case "BILLING", "TECHNICAL" -> "HIGH";
            case "OTHER" -> "LOW";
            default -> "MEDIUM";
        };
    }
}
