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

    /** Yêu cầu mới vào hàng chờ: báo mọi nhân viên hỗ trợ đang hoạt động (bỏ phân ca 2026-11-16). */
    public void created(Map<String, Object> ticket) {
        events.publishToRole("ticket:" + id(ticket) + ":created", "TICKET", "Có yêu cầu hỗ trợ mới",
                "Yêu cầu \"" + subject(ticket) + "\" đang chờ nhân viên hỗ trợ nhận xử lý.", csPath(ticket), "ROLE_CS_STAFF");
    }

    /** Nhân viên hỗ trợ đã nhận xử lý: báo người gửi biết yêu cầu đã được tiếp nhận. */
    public void claimed(Map<String, Object> ticket) {
        send(ticket, "claim", "Yêu cầu hỗ trợ đã được tiếp nhận",
                "Nhân viên hỗ trợ đã nhận xử lý yêu cầu \"" + subject(ticket) + "\" của bạn và sẽ phản hồi sớm.", requesterPath(ticket), (UUID) ticket.get("user_id"));
    }
}
