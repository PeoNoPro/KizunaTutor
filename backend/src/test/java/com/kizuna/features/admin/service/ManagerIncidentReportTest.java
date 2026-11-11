package com.kizuna.features.admin.service;

import com.kizuna.core.exception.BusinessException;
import com.kizuna.features.admin.dto.IncidentCreateRequest;
import com.kizuna.features.admin.dto.OperationsActor;
import com.kizuna.features.admin.entity.TechnicalIncident;
import com.kizuna.features.admin.entity.TechnicalIncidentEvent;
import com.kizuna.features.admin.repository.TechnicalIncidentEventRepository;
import com.kizuna.features.admin.repository.TechnicalIncidentRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Clock;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/** Quản lý báo sự cố cho Admin: sự cố mới "Chưa xử lý", người ghi nhận là Quản lý, Quản lý không có thao tác xử lý. */
class ManagerIncidentReportTest {
    private static final UUID MANAGER = UUID.randomUUID();
    private final TechnicalIncidentRepository incidents = mock(TechnicalIncidentRepository.class);
    private final TechnicalIncidentEventRepository events = mock(TechnicalIncidentEventRepository.class);
    private final OperationsActorResolver actors = mock(OperationsActorResolver.class);
    private final OperationsCommandExecutor commands = mock(OperationsCommandExecutor.class);
    private final TechnicalIncidentService service = new TechnicalIncidentService(incidents, events, actors, commands, Clock.systemUTC());

    private static IncidentCreateRequest request(UUID recurrenceOf) {
        return new IncidentCreateRequest("Không tải được tệp đính kèm", "Từ yêu cầu hỗ trợ của gia sư.", "HIGH", Set.of("APPLICATION"), recurrenceOf, null);
    }

    @Test
    void managerReportLandsAsNewIncidentWithManagerAsReporterAndNoActions() {
        when(actors.requireManager()).thenReturn(new OperationsActor(MANAGER, "ROLE_MANAGER", "Quản lý", "manager@test.invalid", false));
        when(actors.names(any())).thenReturn(Map.of(MANAGER, "Quản lý"));

        var result = service.report(request(null));

        var saved = ArgumentCaptor.forClass(TechnicalIncident.class);
        verify(incidents).saveAndFlush(saved.capture());
        assertEquals("NEW", saved.getValue().getStatus());
        assertEquals(MANAGER, saved.getValue().getReporterId());
        assertNull(saved.getValue().getAssigneeId());
        var event = ArgumentCaptor.forClass(TechnicalIncidentEvent.class);
        verify(events).saveAndFlush(event.capture());
        assertEquals("CREATE", event.getValue().getAction());
        assertEquals(MANAGER, event.getValue().getActorId());
        assertEquals(List.of(), result.get("allowedActions"));
        assertEquals("Quản lý", result.get("reporterLabel"));
        verify(commands, never()).lockAdmin(any());
    }

    @Test
    void managerCannotReopenClosedIncidentAsRecurrence() {
        when(actors.requireManager()).thenReturn(new OperationsActor(MANAGER, "ROLE_MANAGER", "Quản lý", "manager@test.invalid", false));
        var error = assertThrows(BusinessException.class, () -> service.report(request(UUID.randomUUID())));
        assertEquals(400, error.status());
        verify(incidents, never()).saveAndFlush(any());
    }

    @Test
    void nonManagerIsRejectedBeforeAnythingIsSaved() {
        when(actors.requireManager()).thenThrow(new BusinessException(403, "Bạn không có quyền sử dụng chức năng này."));
        assertEquals(403, assertThrows(BusinessException.class, () -> service.report(request(null))).status());
        assertEquals(403, assertThrows(BusinessException.class, () -> service.reportedByMe(0, 20, null)).status());
        verify(incidents, never()).saveAndFlush(any());
    }
}
