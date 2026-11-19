package com.kizuna.features.ticket.unit;

import com.kizuna.core.exception.BusinessException;
import com.kizuna.features.ticket.dto.SupportTicketActor;
import com.kizuna.features.ticket.dto.SupportTicketRequests.Command;
import com.kizuna.features.ticket.repository.*;
import com.kizuna.features.ticket.service.*;
import org.junit.jupiter.api.function.Executable;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Dữ liệu và mock dùng chung cho unit test FE-10 (yêu cầu hỗ trợ).
 * Repository, người đang đăng nhập, thông tin liên quan và gửi thông báo là mock nên không chạm DB thật;
 * quy tắc quyền (SupportTicketAccessPolicy) dùng bản thật vì đó là nghiệp vụ cần kiểm tra.
 */
abstract class TicketUnitTestBase {
    static final UUID TICKET = UUID.fromString("00000000-0000-0000-0000-0000000000a1");
    static final UUID OWNER = UUID.fromString("00000000-0000-0000-0000-000000000001");
    static final UUID OTHER_LEARNER = UUID.fromString("00000000-0000-0000-0000-000000000002");
    static final UUID CS = UUID.fromString("00000000-0000-0000-0000-000000000003");
    static final UUID MANAGER = UUID.fromString("00000000-0000-0000-0000-000000000004");
    static final UUID OTHER_MANAGER = UUID.fromString("00000000-0000-0000-0000-000000000005");
    static final UUID ESCALATION = UUID.fromString("00000000-0000-0000-0000-0000000000e1");
    static final long VERSION = 3L;

    static final SupportTicketActor LEARNER_ACTOR = new SupportTicketActor(OWNER, "ROLE_LEARNER", "Học viên A");
    static final SupportTicketActor TUTOR_ACTOR = new SupportTicketActor(OWNER, "ROLE_TUTOR", "Gia sư A");
    static final SupportTicketActor OTHER_LEARNER_ACTOR = new SupportTicketActor(OTHER_LEARNER, "ROLE_LEARNER", "Học viên B");
    static final SupportTicketActor CS_ACTOR = new SupportTicketActor(CS, "ROLE_CS_STAFF", "Nhân viên hỗ trợ");
    static final SupportTicketActor MANAGER_ACTOR = new SupportTicketActor(MANAGER, "ROLE_MANAGER", "Quản lý A");
    static final SupportTicketActor OTHER_MANAGER_ACTOR = new SupportTicketActor(OTHER_MANAGER, "ROLE_MANAGER", "Quản lý B");

    final SupportTicketRepository tickets = mock(SupportTicketRepository.class);
    final TicketMessageRepository messages = mock(TicketMessageRepository.class);
    final TicketAttachmentRepository attachments = mock(TicketAttachmentRepository.class);
    final TicketAuditEventRepository audit = mock(TicketAuditEventRepository.class);
    final SupportTicketActorResolver actors = mock(SupportTicketActorResolver.class);
    final SupportTicketRelatedContextService contexts = mock(SupportTicketRelatedContextService.class);
    final SupportTicketNotifier notifier = mock(SupportTicketNotifier.class);
    final SupportTicketAccessPolicy policy = new SupportTicketAccessPolicy();
    final SupportTicketCommandExecutor executor =
            new SupportTicketCommandExecutor(tickets, messages, attachments, audit, actors, policy, contexts, notifier);

    {
        // Repository thật trả null khi không có yêu cầu (Mockito mặc định trả Map rỗng nên phải đặt rõ).
        when(tickets.find(any(), anyBoolean())).thenReturn(null);
    }

    /** Đăng nhập bằng tài khoản này. */
    void as(SupportTicketActor actor) { when(actors.current(anyBoolean())).thenReturn(actor); }

    /** Chưa đăng nhập: bộ đọc người dùng trả 401 như bản thật. */
    void notLoggedIn() { when(actors.current(anyBoolean())).thenThrow(new BusinessException(401, "Vui lòng đăng nhập để tiếp tục.")); }

    /** Yêu cầu hỗ trợ của OWNER, đang ở trạng thái status, phiên bản VERSION. */
    Map<String, Object> ticket(String status, UUID assignedTo, UUID escalatedTo) {
        var t = new HashMap<String, Object>();
        t.put("ticket_id", TICKET); t.put("user_id", OWNER); t.put("status", status); t.put("priority", "MEDIUM");
        t.put("assigned_to", assignedTo); t.put("escalated_to", escalatedTo); t.put("is_escalated", escalatedTo != null);
        t.put("version", VERSION); t.put("subject", "Không vào được lớp học");
        when(tickets.find(eq(TICKET), anyBoolean())).thenReturn(t);
        return t;
    }

    /** Lượt chuyển cấp đang chờ quản lý xử lý. */
    Map<String, Object> pendingEscalation(UUID currentManager) {
        var e = new HashMap<String, Object>();
        e.put("escalation_id", ESCALATION); e.put("ticket_id", TICKET); e.put("current_manager_id", currentManager);
        e.put("pending", true); e.put("recipient_revision", 1); e.put("decision_text", null); e.put("decision_by", null);
        when(tickets.escalation(eq(TICKET), anyBoolean())).thenReturn(List.of(e));
        return e;
    }

    static Command cmd(Long version) { return new Command(version, null, null, null, null, null, null, null); }
    static Command priority(String value) { return new Command(VERSION, null, null, value, null, null, null, null); }
    static Command resolve(String text) { return new Command(VERSION, null, null, null, null, null, null, text); }
    static Command escalate(UUID manager, String reason, String summary) { return new Command(VERSION, null, manager, null, reason, summary, null, null); }
    static Command decision(String text) { return new Command(VERSION, null, null, null, null, null, text, null); }

    static String chars(int n) { return "a".repeat(n); }

    static BusinessException fails(int status, Executable call) {
        var error = assertThrows(BusinessException.class, call);
        assertEquals(status, error.status(), error.getMessage());
        return error;
    }

    /** Không lưu gì: không đổi phiếu, không ghi lịch sử, không gửi thông báo. */
    void nothingSaved() {
        verify(tickets, never()).save(any());
        verify(audit, never()).append(any(), any(), any(), anyLong(), any(), any(), any());
        verifyNoInteractions(notifier);
    }

    static final String INVALID_TEXT = "Vui lòng nhập nội dung hợp lệ và đúng độ dài cho phép.";
    static final String NO_PERMISSION = "Bạn không có quyền thực hiện thao tác này.";
    static final String STATE_CHANGED = "Trạng thái yêu cầu đã thay đổi. Vui lòng tải lại trước khi thao tác.";
    static final String STALE = "Yêu cầu đã thay đổi hoặc chưa đủ điều kiện xử lý. Vui lòng tải lại trước khi gửi.";
    static final String NOT_FOUND = "Không tìm thấy yêu cầu hỗ trợ hoặc bạn không có quyền truy cập.";
    static final String RELOAD = "Vui lòng tải lại yêu cầu trước khi thực hiện thao tác.";
}
