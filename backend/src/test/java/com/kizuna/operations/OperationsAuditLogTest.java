package com.kizuna.operations;
import org.junit.jupiter.api.*;
import org.springframework.http.MediaType;
import com.fasterxml.jackson.databind.JsonNode;
import java.nio.file.*;
import java.util.UUID;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.junit.jupiter.api.Assertions.*;
class OperationsAuditLogTest extends OperationsIntegrationBase {
    static final String LOG="/api/admin/operations/audit-log";
    @BeforeEach void schema() throws Exception { jdbc.execute(Files.readString(Path.of("../database/seeds/admin_staff_schema.sql"))); }
    JsonNode read(String query) throws Exception { return data(mvc.perform(get(LOG+query).with(admin())).andExpect(status().isOk()).andReturn()); }
    JsonNode find(JsonNode page,String reason) { for(var item:page.get("items")) if(reason.equals(item.path("reason").asText())) return item; return null; }
    @Test void combinesAllSourcesWithFiltersAndWithoutInvestigationText() throws Exception {
        String marker="log-"+UUID.randomUUID();
        var incident=data(mvc.perform(post("/api/admin/operations/incidents").with(admin()).with(csrf()).contentType(MediaType.APPLICATION_JSON)
            .content("{\"subject\":\"Sự cố nhật ký\",\"description\":\"Mô tả nội bộ không vào nhật ký\",\"severity\":\"LOW\",\"components\":[\"DATABASE\"]}")).andExpect(status().isCreated()).andReturn());
        String path="/api/admin/operations/incidents/"+incident.get("incidentId").asText();
        var claimed=data(mvc.perform(post(path+"/claim").with(admin()).with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{\"expectedVersion\":"+incident.get("version").asLong()+"}")).andExpect(status().isOk()).andReturn());
        mvc.perform(post(path+"/release").with(admin()).with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{\"expectedVersion\":"+claimed.get("version").asLong()+",\"reason\":\""+marker+"\"}")).andExpect(status().isOk());
        jdbc.update("INSERT INTO staff_account_audit_events(event_id,actor_user_id,target_user_id,action,reason) VALUES (?,?,?,'STATUS',?)",UUID.randomUUID(),ADMIN,ADMIN2,marker+"-staff");
        long version=data(mvc.perform(get("/api/admin/operations/booking-policy").with(admin())).andExpect(status().isOk()).andReturn()).get("version").asLong();
        mvc.perform(put("/api/admin/operations/booking-policy").with(admin()).with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{\"expectedVersion\":"+version+",\"durationMinutes\":60,\"minAdvanceHours\":24,\"reason\":\""+marker+"-config\"}")).andExpect(status().isOk());

        var all=read("?size=50");
        var reassign=find(all,marker);assertEquals("Sự cố",reassign.get("sourceLabel").asText());assertEquals("Trả lại danh sách chờ",reassign.get("actionLabel").asText());assertEquals("Sự cố nhật ký",reassign.get("targetLabel").asText());assertEquals("Mới ghi nhận",reassign.get("resultLabel").asText());assertEquals(incident.get("incidentId").asText(),reassign.get("incidentId").asText());
        var staff=find(all,marker+"-staff");assertEquals("Đổi trạng thái tài khoản",staff.get("actionLabel").asText());assertEquals("Nhân sự",staff.get("sourceLabel").asText());
        var config=find(all,marker+"-config");assertEquals("Cấu hình",config.get("sourceLabel").asText());assertTrue(config.get("resultLabel").asText().contains("→ 60 phút"));
        assertFalse(all.toString().contains("Mô tả nội bộ"));assertFalse(all.toString().contains("ROLE_"));

        var onlyStaff=read("?source=STAFF&size=50");assertNotNull(find(onlyStaff,marker+"-staff"));assertNull(find(onlyStaff,marker));
        assertNull(find(read("?actor="+ADMIN2+"&size=50"),marker));assertNotNull(find(read("?actor="+ADMIN+"&size=50"),marker));
        assertEquals(0,read("?toDate=2000-01-01").get("totalElements").asInt());
    }
    @Test void onlyActiveAdministratorsWithValidFilters() throws Exception {
        mvc.perform(get(LOG)).andExpect(status().isUnauthorized());
        mvc.perform(get(LOG).with(user("ROLE_MANAGER@test.invalid"))).andExpect(status().isForbidden());
        for(String bad:new String[]{"?source=OTHER","?size=51","?page=-1","?fromDate=2026-02-02&toDate=2026-02-01","?actor=abc"})
            mvc.perform(get(LOG+bad).with(admin())).andExpect(status().isBadRequest());
    }
}
