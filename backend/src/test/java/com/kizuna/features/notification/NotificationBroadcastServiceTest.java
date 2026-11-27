package com.kizuna.features.notification;

import com.kizuna.core.exception.BusinessException;
import com.kizuna.features.admin.dto.OperationsActor;
import com.kizuna.features.admin.service.OperationsActorResolver;
import com.kizuna.features.notification.repository.NotificationDispatchRepository;
import com.kizuna.features.notification.repository.NotificationDispatchRepository.Dispatch;
import com.kizuna.features.notification.service.NotificationBroadcastService;
import com.kizuna.features.notification.service.NotificationBroadcastService.BroadcastRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.*;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/** FE-12 Story 3–4: kiểm tra trước khi gửi, gửi lặp không tạo thêm đợt, khẩn toàn hệ thống luôn có bản trong ứng dụng. */
class NotificationBroadcastServiceTest {
    private static final UUID ADMIN = UUID.randomUUID();
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-11-09T03:00:00Z"), ZoneId.of("Asia/Ho_Chi_Minh"));
    private final NotificationDispatchRepository dispatches = mock(NotificationDispatchRepository.class);
    private final OperationsActorResolver actors = mock(OperationsActorResolver.class);
    private final NotificationBroadcastService service = new NotificationBroadcastService(dispatches, actors, CLOCK);

    @BeforeEach
    void admin() { when(actors.requireBroadcaster()).thenReturn(new OperationsActor(ADMIN, "ROLE_ADMIN", "Admin", "admin@test.invalid", false)); }

    private static BroadcastRequest roles(List<String> roles, List<String> channels, OffsetDateTime at, UUID key) {
        return new BroadcastRequest("Bảo trì hệ thống", "Hệ thống bảo trì lúc 22 giờ.", "SYSTEM", "ROLES", roles, channels, false, at, key);
    }

    private static BroadcastRequest urgent() {
        return new BroadcastRequest("Sự cố đăng nhập", "Đang khắc phục.", "SYSTEM", "ALL", null, List.of("EMAIL"), true, null, UUID.randomUUID());
    }

    private static int status(Runnable action) { return assertThrows(BusinessException.class, action::run).status(); }

    @Test
    void rejectsMissingRecipientsChannelsAndPastSchedule() {
        assertEquals(400, status(() -> service.preview(roles(List.of(), List.of("IN_APP"), null, null))));
        assertEquals(400, status(() -> service.preview(roles(List.of("ROLE_TUTOR"), List.of(), null, null))));
        assertEquals(400, status(() -> service.preview(roles(List.of("ROLE_GUEST"), List.of("IN_APP"), null, null))));
        assertEquals(400, status(() -> service.submit(roles(List.of("ROLE_TUTOR"), List.of("IN_APP"), OffsetDateTime.now(CLOCK).minusMinutes(1), UUID.randomUUID()))));
        verify(dispatches, never()).insert(any());
    }

    @Test
    void allUsersOnlyForUrgentAndUrgentCannotBeScheduled() {
        var notUrgent = new BroadcastRequest("A", "B", "SYSTEM", "ALL", null, List.of("IN_APP"), false, null, UUID.randomUUID());
        assertEquals(400, status(() -> service.preview(notUrgent)));
        var scheduledUrgent = new BroadcastRequest("A", "B", "SYSTEM", "ALL", null, List.of("IN_APP"), true, OffsetDateTime.now(CLOCK).plusHours(1), UUID.randomUUID());
        assertEquals(400, status(() -> service.preview(scheduledUrgent)));
    }

    @Test
    void urgentAlwaysIncludesInAppAndAsksForConfirmation() {
        when(dispatches.estimate(any())).thenReturn(7);
        var preview = service.preview(urgent());
        assertEquals(List.of("Trong ứng dụng", "Email"), preview.get("channelLabels"));
        assertEquals(true, preview.get("requiresConfirmation"));
        assertEquals(7, preview.get("estimatedRecipients"));
    }

    private static Dispatch stored(UUID key, String title, OffsetDateTime at) {
        return new Dispatch(UUID.randomUUID(), ADMIN, "Admin", "SYSTEM", title, "Hệ thống bảo trì lúc 22 giờ.", false, "ROLES",
                List.of("ROLE_TUTOR"), List.of("IN_APP"), at, OffsetDateTime.now(CLOCK), null, 3, null, at == null ? "QUEUED" : "SCHEDULED");
    }

    @Test
    void repeatedSubmitReturnsSameDispatchWithoutInsertingAgain() {
        var key = UUID.randomUUID();
        var at = OffsetDateTime.now(CLOCK).plusDays(1);
        when(dispatches.bySubmission(ADMIN, key)).thenReturn(Optional.of(stored(key, "Bảo trì hệ thống", at)));
        var result = service.submit(roles(List.of("ROLE_TUTOR"), List.of("IN_APP"), at, key));
        assertEquals("Đã lên lịch", result.get("statusLabel"));
        verify(dispatches, never()).insert(any());
        verify(dispatches, never()).freeze(any());
    }

    @Test
    void sameKeyWithDifferentContentIsConflict() {
        var key = UUID.randomUUID();
        when(dispatches.bySubmission(ADMIN, key)).thenReturn(Optional.of(stored(key, "Tiêu đề khác", null)));
        assertEquals(409, status(() -> service.submit(roles(List.of("ROLE_TUTOR"), List.of("IN_APP"), null, key))));
    }

    @Test
    void scheduledSubmitIsNotFrozenUntilDue() {
        var key = UUID.randomUUID();
        var at = OffsetDateTime.now(CLOCK).plusHours(2);
        var saved = stored(key, "Bảo trì hệ thống", at);
        when(dispatches.bySubmission(ADMIN, key)).thenReturn(Optional.empty(), Optional.of(saved));
        service.submit(roles(List.of("ROLE_TUTOR"), List.of("IN_APP"), at, key));
        verify(dispatches).insert(argThat(d -> "SCHEDULED".equals(d.status())));
        verify(dispatches, never()).freeze(any());
    }

    // ----- Góp ý review PR #21 -----

    @Test
    void titleMustBeOneLineBecauseItIsAlsoTheEmailSubject() {
        var request = new BroadcastRequest("Bảo trì\nBcc: ai@do.invalid","Nội dung", "SYSTEM", "ROLES", List.of("ROLE_TUTOR"), List.of("EMAIL"), false, null, UUID.randomUUID());
        assertEquals(400, status(() -> service.submit(request)));
        verify(dispatches, never()).insert(any());
    }

    private static Dispatch scheduled(UUID id, UUID creator, String status, OffsetDateTime frozenAt) {
        return new Dispatch(id, creator, "Người tạo", "SYSTEM", "Bảo trì", "Nội dung", false, "ROLES", List.of("ROLE_TUTOR"), List.of("IN_APP"),
                OffsetDateTime.now(CLOCK).plusDays(1), OffsetDateTime.now(CLOCK), frozenAt, 3, null, status);
    }

    @Test
    void creatorOrAdminCanCancelAScheduledBroadcastBeforeItIsSent() {
        var id = UUID.randomUUID();
        when(dispatches.find(id)).thenReturn(Optional.of(scheduled(id, UUID.randomUUID(), "SCHEDULED", null)));
        when(dispatches.cancel(id, ADMIN)).thenReturn(true);
        service.cancel(id);
        verify(dispatches).cancel(id, ADMIN);

        var manager = UUID.randomUUID();
        when(actors.requireBroadcaster()).thenReturn(new OperationsActor(manager, "ROLE_MANAGER", "Quản lý", "ql@test.invalid", false));
        assertEquals(403, status(() -> service.cancel(id)));
        verify(dispatches, never()).cancel(id, manager);
    }

    @Test
    void sentOrAlreadyCancelledBroadcastCannotBeCancelled() {
        var sent = UUID.randomUUID(); var raced = UUID.randomUUID();
        when(dispatches.find(sent)).thenReturn(Optional.of(scheduled(sent, ADMIN, "COMPLETED", OffsetDateTime.now(CLOCK))));
        assertEquals(409, status(() -> service.cancel(sent)));
        // Tác vụ nền vừa chốt người nhận đúng lúc bấm hủy: câu UPDATE không đổi dòng nào → 409.
        when(dispatches.find(raced)).thenReturn(Optional.of(scheduled(raced, ADMIN, "SCHEDULED", null)));
        when(dispatches.cancel(raced, ADMIN)).thenReturn(false);
        assertEquals(409, status(() -> service.cancel(raced)));
    }
}
