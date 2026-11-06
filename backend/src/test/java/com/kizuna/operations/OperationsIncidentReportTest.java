package com.kizuna.operations;
import com.kizuna.core.exception.BusinessException;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.junit.jupiter.api.Assertions.*;
import java.time.*;
import com.kizuna.features.admin.service.*;
import java.util.UUID;
class OperationsIncidentReportTest extends OperationsIntegrationBase {
    @Test void historicalStateDistinctResolutionsAndCarryOver() throws Exception {
        UUID id=UUID.randomUUID();
        jdbc.update("INSERT INTO technical_incidents(incident_id,subject,description,severity,status,reporter_id,assignee_id,created_at,updated_at,version) VALUES (?,'Lịch sử','Kiểm thử','HIGH','CLOSED',?,?,'2026-01-01T00:00:00Z','2026-01-10T00:00:00Z',4)",id,ADMIN,ADMIN);
        String[] actions={"CREATE","RESOLVE","RESUME","RESOLVE","CLOSE"};
        String[] states={"NEW","RESOLVED","IN_PROGRESS","RESOLVED","CLOSED"};
        String[] times={"2026-01-01T00:00:00Z","2026-01-03T00:00:00Z","2026-01-03T01:00:00Z","2026-01-03T02:00:00Z","2026-01-10T00:00:00Z"};
        for(int n=0;n<5;n++) jdbc.update("INSERT INTO technical_incident_events(event_id,incident_id,sequence_no,actor_id,occurred_at,action,new_state) VALUES (?,?,?,?,?::timestamptz,?,?::jsonb)",UUID.randomUUID(),id,n+1,ADMIN,times[n],actions[n],"{\"status\":\""+states[n]+"\",\"severity\":\"HIGH\",\"components\":[\"DATABASE\"]}");
        var response=mvc.perform(get("/api/admin/operations/reports/incidents?startDate=2026-01-03&endDate=2026-01-03&metric=open").with(admin())).andReturn();
        if(response.getResolvedException()!=null) throw response.getResolvedException();
        assertEquals(200,response.getResponse().getStatus()); var report=data(response);
        assertEquals(0,report.get("createdCount").asInt()); assertEquals(1,report.get("resolvedCount").asInt()); assertEquals(1,report.get("openAtCutoff").asInt());
        assertEquals(1,report.get("breakdowns").get("open").get("status").get("Đã giải quyết").asInt());
        assertEquals("Đã giải quyết",report.get("items").get(0).get("statusLabel").asText());
        var later=data(mvc.perform(get("/api/admin/operations/reports/incidents?startDate=2026-01-10&endDate=2026-01-10").with(admin())).andReturn()); assertEquals(0,later.get("openAtCutoff").asInt());
    }
    @Test void validatesVietnameseDateBoundaries() {
        var now=OffsetDateTime.parse("2026-03-01T18:00:00Z");
        var p=OperationsIncidentReportService.period(null,null,now);
        assertEquals(LocalDate.of(2026,3,2),p.endDate()); assertEquals(LocalDate.of(2026,2,24),p.start().toLocalDate());
        assertThrows(BusinessException.class,()->OperationsIncidentReportService.period(LocalDate.of(2024,1,1),LocalDate.of(2026,1,1),now));
        assertThrows(BusinessException.class,()->OperationsIncidentReportService.period(null,LocalDate.of(2026,1,1),now));
        assertDoesNotThrow(()->OperationsIncidentReportService.period(LocalDate.of(2025,3,2),LocalDate.of(2026,3,2),now));
        assertThrows(BusinessException.class,()->OperationsIncidentReportService.period(LocalDate.of(2025,3,1),LocalDate.of(2026,3,2),now));
        assertThrows(BusinessException.class,()->OperationsIncidentReportService.period(LocalDate.of(2026,3,3),LocalDate.of(2026,3,3),now));
    }
}
