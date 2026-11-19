package com.kizuna.features.ticket.service;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Map;

/**
 * Thời hạn phản hồi (SLA) của yêu cầu hỗ trợ, bổ sung 2026-11-12. Dùng chung cho Tổng quan Admin, CS Dashboard và danh sách yêu cầu.
 * <ul>
 *   <li><b>Đang chờ phản hồi từ lúc</b>: tin nhắn đầu tiên của người gửi mà nhân viên chưa trả lời (sau tin trả lời gần nhất của nhân viên).
 *       Nhân viên đã trả lời sau tin cuối của người gửi thì không còn chờ.</li>
 *   <li><b>Thời hạn</b> theo mức ưu tiên: Khẩn cấp 1 giờ, Cao 4 giờ, Bình thường 8 giờ, Thấp 24 giờ.</li>
 *   <li><b>Quá hạn</b>: yêu cầu còn mở (Chờ tiếp nhận, Đang xử lý, Đã chuyển Quản lý) mà thời gian chờ vượt thời hạn.</li>
 * </ul>
 * Ghi chú nội bộ nằm ở bảng riêng ticket_internal_notes nên không bao giờ được tính là trả lời người gửi.
 */
public final class SupportTicketSla {
    private SupportTicketSla() {}

    public static final Map<String, Duration> TARGETS = Map.of(
            "URGENT", Duration.ofHours(1), "HIGH", Duration.ofHours(4), "MEDIUM", Duration.ofHours(8), "LOW", Duration.ofHours(24));

    /** Trạng thái còn tính thời gian chờ. */
    public static final String ACTIVE_STATUSES = "('OPEN','IN_PROGRESS','ESCALATED')";

    /** Điều kiện thêm cho "tin trả lời của nhân viên" (bí danh s). Hiện để trống: ghi chú nội bộ không nằm trong ticket_messages. */
    public static final String STAFF_REPLY_FILTER = "";

    /** Biểu thức SQL (bảng support_tickets đặt bí danh t): thời điểm bắt đầu chờ, NULL nếu không chờ. */
    public static final String WAITING_SINCE_SQL = "(SELECT min(m.sent_at) FROM ticket_messages m WHERE m.ticket_id=t.ticket_id AND m.sender_id=t.user_id"
            + " AND m.sent_at>coalesce((SELECT max(s.sent_at) FROM ticket_messages s WHERE s.ticket_id=t.ticket_id AND s.sender_id<>t.user_id"
            + STAFF_REPLY_FILTER + "),'-infinity'::timestamptz))";

    /** Biểu thức SQL: số phút thời hạn theo mức ưu tiên của t. */
    public static final String TARGET_MINUTES_SQL = "(CASE t.priority::text WHEN 'URGENT' THEN 60 WHEN 'HIGH' THEN 240 WHEN 'MEDIUM' THEN 480 ELSE 1440 END)";

    /** Điều kiện SQL: yêu cầu đang quá hạn phản hồi. */
    public static final String OVERDUE_SQL = "(t.status::text IN " + ACTIVE_STATUSES + " AND " + WAITING_SINCE_SQL + "+" + TARGET_MINUTES_SQL
            + "*interval '1 minute'<now())";

    public static Duration target(String priority) { return TARGETS.getOrDefault(priority, Duration.ofHours(24)); }

    /** Hạn phản hồi, hoặc null khi không có tin nào đang chờ. */
    public static OffsetDateTime dueAt(String priority, OffsetDateTime waitingSince) {
        return waitingSince == null ? null : waitingSince.plus(target(priority));
    }
}
