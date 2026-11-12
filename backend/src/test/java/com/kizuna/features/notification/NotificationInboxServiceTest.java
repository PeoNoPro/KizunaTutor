package com.kizuna.features.notification;

import com.kizuna.core.exception.BusinessException;
import com.kizuna.features.notification.repository.NotificationInboxRepository;
import com.kizuna.features.notification.service.NotificationInboxService;
import com.kizuna.features.notification.service.NotificationInboxService.PreferenceGroup;
import com.kizuna.features.notification.service.NotificationInboxService.PreferenceUpdate;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** FE-12 Story 1–2: chỉ chủ thông báo xem/đánh dấu/xóa được; tùy chọn có mặc định và chống ghi đè phiên bản cũ. */
class NotificationInboxServiceTest {
    private static final UUID OWNER = UUID.randomUUID();
    private final NotificationInboxRepository inbox = mock(NotificationInboxRepository.class);
    private final NotificationInboxService service = new NotificationInboxService(inbox);

    @BeforeEach
    void signIn() {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken("owner@test.invalid", null, List.of()));
        when(inbox.activeAccount("owner@test.invalid", null)).thenReturn(Optional.of(new NotificationInboxRepository.Account(OWNER, "ROLE_LEARNER", "Học viên", 0)));
    }

    @AfterEach
    void signOut() { SecurityContextHolder.clearContext(); }

    @Test
    void someoneElsesOrDeletedNotificationLooksMissing() {
        var other = UUID.randomUUID();
        when(inbox.find(OWNER, other)).thenReturn(Optional.empty());
        when(inbox.markRead(OWNER, other)).thenReturn(false);
        when(inbox.softDelete(OWNER, other)).thenReturn(false);
        assertEquals(404, assertThrows(BusinessException.class, () -> service.detail(other)).status());
        assertEquals(404, assertThrows(BusinessException.class, () -> service.markRead(other)).status());
        assertEquals(404, assertThrows(BusinessException.class, () -> service.delete(other)).status());
    }

    @Test
    void lockedOrUnknownAccountIsRejected() {
        when(inbox.activeAccount("owner@test.invalid", null)).thenReturn(Optional.empty());
        assertEquals(403, assertThrows(BusinessException.class, service::unreadCount).status());
    }

    @Test
    void listRejectsOutOfRangePagingAndUnknownFilters() {
        assertEquals(400, assertThrows(BusinessException.class, () -> service.list(null, null, null, null, 0, 51)).status());
        assertEquals(400, assertThrows(BusinessException.class, () -> service.list("old", null, null, null, 0, 20)).status());
        assertEquals(400, assertThrows(BusinessException.class, () -> service.list(null, "CHAT", null, null, 0, 20)).status());
        verify(inbox, never()).pageWithTotals(any(), anyBoolean(), any(), any(), any(), anyInt(), anyInt());
    }

    @Test
    void preferencesDefaultToInAppOnWithEmailOff() {
        when(inbox.preferences(OWNER)).thenReturn(Map.of());
        var view = service.preferences();
        @SuppressWarnings("unchecked") var groups = (List<Map<String, Object>>) view.get("groups");
        assertEquals(5, groups.size());
        groups.forEach(group -> { assertEquals(true, group.get("inAppEnabled")); assertEquals(false, group.get("emailEnabled")); });
        assertEquals(0L, view.get("version"));
    }

    private static List<PreferenceGroup> allGroups() {
        return NotificationInboxService.TYPES.keySet().stream().map(type -> new PreferenceGroup(type, true, true)).toList();
    }

    @Test
    void staleVersionIsRejectedWithoutSaving() {
        when(inbox.lockPreferenceVersion(OWNER)).thenReturn(3L);
        var error = assertThrows(BusinessException.class, () -> service.savePreferences(new PreferenceUpdate(2L, allGroups())));
        assertEquals(409, error.status());
        verify(inbox, never()).savePreferences(any(), any());
    }

    @Test
    void preferencesMustCoverEachGroupExactlyOnce() {
        var groups = new ArrayList<>(allGroups());
        groups.set(4, new PreferenceGroup("SYSTEM", true, false));
        assertEquals(400, assertThrows(BusinessException.class, () -> service.savePreferences(new PreferenceUpdate(0L, groups))).status());
        assertEquals(400, assertThrows(BusinessException.class, () -> service.savePreferences(new PreferenceUpdate(0L, allGroups().subList(0, 4)))).status());
        verify(inbox, never()).lockPreferenceVersion(any());
    }

    @Test
    void currentVersionSavesWholeMatrix() {
        when(inbox.lockPreferenceVersion(OWNER)).thenReturn(0L);
        when(inbox.preferences(OWNER)).thenReturn(Map.of());
        service.savePreferences(new PreferenceUpdate(0L, allGroups()));
        verify(inbox).savePreferences(eq(OWNER), argThat(saved -> saved.size() == 5));
    }
}
