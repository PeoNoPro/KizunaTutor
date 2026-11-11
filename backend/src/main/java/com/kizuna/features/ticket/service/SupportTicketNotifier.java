package com.kizuna.features.ticket.service;

import com.kizuna.features.notification.service.NotificationEvents;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * FE-10 → FE-12: thông báo tự động khi yêu cầu hỗ trợ thay đổi. Gọi trong cùng transaction với thay đổi đã lưu;
 * mã sự kiện gắn với phiên bản mới của yêu cầu nên xử lý lại không tạo thêm thông báo.
 * Thông báo cho người gửi chỉ nói có phản hồi/kết quả, không chép ghi chú nội bộ hay quyết định của Quản lý.
 */
@Service
@RequiredArgsConstructor
public class SupportTicketNotifier {
    private final NotificationEvents events;

    private static String subject(Map<String, Object> ticket) { return String.valueOf(ticket.get("subject")); }
    private static UUID id(Map<String, Object> ticket) { return (UUID) ticket.get("ticket_id"); }
    private static long version(Map<String, Object> ticket) { return ((Number) ticket.get("version")).longValue() + 1; }

    /** Đường dẫn mở yêu cầu theo vai trò người nhận (trang đích tự kiểm quyền). */
    private static String requesterPath(Map<String, Object> ticket) {
        return ("ROLE_TUTOR".equals(ticket.get("requester_role")) ? "/tutor" : "/learner") + "/support/tickets/" + id(ticket);
    }
    private static String csPath(Map<String, Object> ticket) { return "/cs/support/tickets/" + id(ticket); }
    private static String managerPath(Map<String, Object> ticket) { return "/manager/support/tickets/" + id(ticket); }

    private void send(Map<String, Object> ticket, String event, String title, String content, String path, UUID recipient) {
        if (recipient == null) return;
        events.publish("ticket:" + id(ticket) + ":v" + version(ticket) + ":" + event + ":" + recipient, "TICKET", title, content, path, List.of(recipient));
    }

    /** Có tin nhắn mới: nhân viên trả lời thì báo người gửi; người gửi bổ sung thì báo nhân viên đang phụ trách. */
    public void message(Map<String, Object> ticket, boolean fromRequester) {
        if (fromRequester) send(ticket, "message", "Người dùng bổ sung thông tin",
                "Người gửi vừa trả lời trong yêu cầu hỗ trợ \"" + subject(ticket) + "\".", csPath(ticket), (UUID) ticket.get("assigned_to"));
        else send(ticket, "message", "Có phản hồi mới cho yêu cầu hỗ trợ",
                "Nhân viên hỗ trợ đã trả lời yêu cầu \"" + subject(ticket) + "\" của bạn. Mở yêu cầu để xem chi tiết.", requesterPath(ticket), (UUID) ticket.get("user_id"));
    }

    /** Sau một thao tác xử lý (đã lưu, ticket là trạng thái trước khi tăng phiên bản). */
    public void command(Map<String, Object> ticket, String action, UUID previousManager) {
        switch (action) {
            case "resolve" -> send(ticket, action, "Yêu cầu hỗ trợ đã được giải quyết",
                    "Yêu cầu \"" + subject(ticket) + "\" đã có kết quả xử lý. Nếu vấn đề vẫn còn, bạn có thể trả lời để mở lại.", requesterPath(ticket), (UUID) ticket.get("user_id"));
            case "assignment" -> send(ticket, action, "Bạn được giao một yêu cầu hỗ trợ",
                    "Yêu cầu \"" + subject(ticket) + "\" vừa được chuyển cho bạn phụ trách.", csPath(ticket), (UUID) ticket.get("assigned_to"));
            case "escalate" -> send(ticket, action, "Có yêu cầu hỗ trợ được chuyển lên",
                    "Nhân viên hỗ trợ đã chuyển yêu cầu \"" + subject(ticket) + "\" cho bạn xem xét.", managerPath(ticket), (UUID) ticket.get("escalated_to"));
            case "recipient" -> {
                if (!Objects.equals(previousManager, ticket.get("escalated_to")))
                    send(ticket, action, "Có yêu cầu hỗ trợ được chuyển lên",
                            "Yêu cầu \"" + subject(ticket) + "\" vừa được chuyển cho bạn xem xét.", managerPath(ticket), (UUID) ticket.get("escalated_to"));
            }
            case "return" -> send(ticket, action, "Quản lý đã trả kết quả",
                    "Quản lý đã gửi phương án cho yêu cầu \"" + subject(ticket) + "\". Mở yêu cầu để tiếp tục xử lý với người gửi.", csPath(ticket), (UUID) ticket.get("assigned_to"));
            default -> { }
        }
    }

    /** Ca đang diễn ra chưa có nhân viên hỗ trợ nào mà có yêu cầu chờ: nhắc mọi Quản lý xếp ca (mỗi ca một lần). */
    public void missingShift(java.time.LocalDate date, String shiftCode, String shiftLabel, long waiting) {
        var day = date.format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy"));
        events.publishToRole("support-shift-missing:" + date + ":" + shiftCode, "TICKET", "Chưa có nhân viên hỗ trợ trong ca",
                shiftLabel + " " + day + " chưa xếp nhân viên hỗ trợ nào, đang có " + waiting + " yêu cầu chờ người nhận. Vui lòng xếp ca để hệ thống tự giao.",
                "/pages/support-shifts/", "ROLE_MANAGER");
    }

    /** Người phụ trách không còn hoạt động: báo mọi Quản lý để chọn người tiếp nhận (một lần cho mỗi yêu cầu và người cũ). */
    public void handoverNeeded(Map<String, Object> row) {
        events.publishToRole("ticket:" + row.get("ticket_id") + ":handover-needed:" + row.get("assigned_to"), "TICKET", "Yêu cầu hỗ trợ cần bàn giao",
                row.get("assignee_name") + " không còn là nhân viên hỗ trợ đang hoạt động nhưng đang phụ trách yêu cầu \"" + row.get("subject")
                        + "\". Vui lòng mở yêu cầu và chọn nhân viên tiếp nhận.",
                "/manager/support/tickets/" + row.get("ticket_id"), "ROLE_MANAGER");
    }

    /** Hệ thống tự giao yêu cầu cho nhân viên đang trong ca. */
    public void autoAssigned(Map<String, Object> ticket, UUID staff) {
        send(ticket, "auto-assign", "Bạn được giao một yêu cầu hỗ trợ mới",
                "Hệ thống vừa giao cho bạn yêu cầu \"" + subject(ticket) + "\" theo ca làm việc.", csPath(ticket), staff);
    }
}
