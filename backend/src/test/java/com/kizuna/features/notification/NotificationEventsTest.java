package com.kizuna.features.notification;

import com.kizuna.features.notification.repository.NotificationDispatchRepository;
import com.kizuna.features.notification.service.NotificationEvents;
import com.kizuna.features.ticket.service.SupportTicketNotifier;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** FE-12 giai đoạn 3: thông báo tự động đúng người, không trùng khi xử lý lại, lỗi không làm hỏng nghiệp vụ. */
class NotificationEventsTest {
    private final NotificationDispatchRepository dispatches = mock(NotificationDispatchRepository.class);
    private final NotificationEvents events = new NotificationEvents(new NotificationEvents.Writer(dispatches,
            mock(javax.sql.DataSource.class), mock(org.springframework.transaction.PlatformTransactionManager.class)));
    private final UUID learner = UUID.randomUUID(), cs = UUID.randomUUID(), manager = UUID.randomUUID();

    @Test
    void newEventIsFrozenForExactlyTheGivenRecipients() {
        when(dispatches.insert(any())).thenReturn(true);
        events.publish("ticket:1:v2:resolve", "TICKET", "Đã giải quyết", "Nội dung", "/learner/support/tickets/1", List.of(learner, learner));
        verify(dispatches).insert(argThat(d -> "EVENT".equals(d.origin()) && "EXPLICIT".equals(d.targetMode()) && "ticket:1:v2:resolve".equals(d.eventKey())
                && "/learner/support/tickets/1".equals(d.actionPath()) && d.channels().equals(List.of("IN_APP", "EMAIL"))));
        verify(dispatches).freeze(any(), eq(List.of(learner)));
    }

    @Test
    void sameEventAgainDoesNotCreateAnotherNotification() {
        when(dispatches.insert(any())).thenReturn(false);
        events.publish("ticket:1:v2:resolve", "TICKET", "Đã giải quyết", "Nội dung", null, List.of(learner));
        verify(dispatches, never()).freeze(any(), any());
    }

    @Test
    void noRecipientsOrStorageErrorNeverBreaksTheBusinessAction() {
        events.publish("ticket:1:v3:assignment", "TICKET", "A", "B", null, Collections.singletonList(null));
        verify(dispatches, never()).insert(any());
        when(dispatches.insert(any())).thenThrow(new org.springframework.dao.DataAccessResourceFailureException("db"));
        assertDoesNotThrow(() -> events.publish("ticket:1:v4:resolve", "TICKET", "A", "B", null, List.of(learner)));
    }

    @Test
    void externalLinksAndUnknownGroupsAreRejected() {
        assertThrows(IllegalArgumentException.class, () -> events.publish("k", "TICKET", "A", "B", "https://evil.example", List.of(learner)));
        assertThrows(IllegalArgumentException.class, () -> events.publish("k", "CHAT", "A", "B", null, List.of(learner)));
        // Góp ý review PR #21: "/\evil.com" được trình duyệt hiểu là "//evil.com"; xuống dòng cũng bị chặn.
        for (var path : List.of("//evil.example", "/\\evil.example", "/a\nb", "/a b", "javascript:alert(1)"))
            assertThrows(IllegalArgumentException.class, () -> events.publish("k", "TICKET", "A", "B", path, List.of(learner)), path);
        assertTrue(NotificationEvents.safePath("/pages/support-ticket/?role=manager&tab=internal&id=00000000-0000-0000-0000-000000000001"));
    }

    private Map<String, Object> ticket() {
        var t = new HashMap<String, Object>();
        t.put("ticket_id", UUID.fromString("00000000-0000-0000-0000-000000000001")); t.put("subject", "Không vào được lớp");
        t.put("user_id", learner); t.put("requester_role", "ROLE_LEARNER"); t.put("assigned_to", cs); t.put("escalated_to", manager); t.put("version", 4L);
        return t;
    }

    @Test
    void ticketEventsReachTheRightPersonWithTheRightLink() {
        var sent = mock(NotificationEvents.class);
        var notifier = new SupportTicketNotifier(sent);
        notifier.message(ticket(), false);
        verify(sent).publish(startsWith("ticket:00000000-0000-0000-0000-000000000001:v5:message"), eq("TICKET"), anyString(), anyString(),
                eq("/learner/support/tickets/00000000-0000-0000-0000-000000000001"), eq(List.of(learner)));
        notifier.message(ticket(), true);
        verify(sent).publish(anyString(), eq("TICKET"), anyString(), anyString(), eq("/cs/support/tickets/00000000-0000-0000-0000-000000000001"), eq(List.of(cs)));
        notifier.command(ticket(), "escalate", null);
        verify(sent).publish(anyString(), eq("TICKET"), anyString(), anyString(), eq("/manager/support/tickets/00000000-0000-0000-0000-000000000001"), eq(List.of(manager)));
        notifier.command(ticket(), "claim", null);
        notifier.command(ticket(), "recipient", manager);
        verify(sent, times(3)).publish(anyString(), anyString(), anyString(), anyString(), anyString(), anyCollection());
    }
}
