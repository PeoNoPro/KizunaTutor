package com.kizuna.operations;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.*;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import java.nio.file.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;

class StaffAccountManagementTest extends OperationsIntegrationBase {
    @Test void catalogShowsSixRolesButOnlyFourAreStaffAssignable() throws Exception {
        var roles=data(mvc.perform(get("/api/admin/operations/roles").with(admin())).andExpect(status().isOk()).andReturn());
        assertEquals(6,roles.size());int assignable=0;
        for(var role:roles){if(role.get("staffAssignable").asBoolean())assignable++;if(Set.of("Học viên","Gia sư").contains(role.get("label").asText()))assertFalse(role.get("staffAssignable").asBoolean());}
        assertEquals(4,assignable);
    }
    private final List<UUID> created=new ArrayList<>();
    private static final String PASSWORD="Test-only-password-2026";
    @BeforeEach void schema() throws Exception {
        jdbc.execute(Files.readString(Path.of("../database/seeds/admin_staff_schema.sql")));
    }
    @AfterEach void cleanup() {
        jdbc.execute("ALTER TABLE staff_account_audit_events DROP CONSTRAINT IF EXISTS staff_test_audit_failure");
        for(var id:created) {jdbc.update("DELETE FROM staff_account_audit_events WHERE target_user_id=?",id);jdbc.update("DELETE FROM users WHERE user_id=?",id);}
    }
    private String body(Object value) throws Exception {return json.writeValueAsString(value);}
    private Map<String,Object> creation(String email) {return Map.of("fullName","Nhân viên kiểm thử","email",email,"phone","0901234567","roleId",4,"initialPassword",PASSWORD);}
    private JsonNode create() throws Exception {
        var result=mvc.perform(post("/api/admin/operations/staff").with(admin()).with(csrf()).contentType("application/json").content(body(creation(UUID.randomUUID()+"@test.invalid"))))
            .andExpect(status().isCreated()).andReturn();var data=data(result);created.add(UUID.fromString(data.get("userId").asText()));return data;
    }
    private JsonNode command(JsonNode account,String path,Map<String,Object> fields) throws Exception {
        var payload=new HashMap<>(fields);payload.put("expectedUpdatedAt",account.get("updatedAt").asText());payload.put("reason","Kiểm tra thay đổi có kiểm soát");
        return data(mvc.perform(put("/api/admin/operations/staff/"+account.get("userId").asText()+path).with(admin()).with(csrf()).contentType("application/json").content(body(payload))).andExpect(status().isOk()).andReturn());
    }
    @Test void createEditAssignSuspendReactivateAndAuditUseDatabase() throws Exception {
        var original=create();UUID id=UUID.fromString(original.get("userId").asText());
        assertFalse(original.has("passwordHash"));assertFalse(original.has("initialPassword"));
        String hash=jdbc.queryForObject("SELECT password_hash FROM users WHERE user_id=?",String.class,id);assertNotEquals(PASSWORD,hash);assertTrue(new BCryptPasswordEncoder().matches(PASSWORD,hash));
        assertEquals(Boolean.TRUE,jdbc.queryForObject("SELECT must_change_password FROM users WHERE user_id=?",Boolean.class,id));
        var edited=command(original,"",Map.of("fullName","Tên đã cập nhật","email",original.get("email").asText(),"phone","0912345678"));
        assertEquals("Tên đã cập nhật",edited.get("fullName").asText());
        var role=command(edited,"/role",Map.of("roleId",6));assertEquals("Cán bộ học vụ",role.get("roleLabel").asText());
        var suspended=command(role,"/status",Map.of("status","SUSPENDED"));assertEquals("Tạm khóa",suspended.get("statusLabel").asText());
        var active=command(suspended,"/status",Map.of("status","ACTIVE"));assertEquals("ACTIVE",active.get("status").asText());
        assertEquals(5,jdbc.queryForObject("SELECT count(*) FROM staff_account_audit_events WHERE target_user_id=?",Integer.class,id));
        mvc.perform(get("/api/admin/operations/staff").param("q","Tên đã cập nhật").with(admin())).andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").doesNotExist()).andExpect(jsonPath("$.data.totalElements").value(1));
    }
    @Test void operationsRequireAdminAndCsrf() throws Exception {
        mvc.perform(get("/api/admin/operations/staff")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/admin/operations/roles").with(user("ROLE_LEARNER@test.invalid"))).andExpect(status().isForbidden());
        mvc.perform(post("/api/admin/operations/staff").with(admin()).contentType("application/json").content(body(creation("no-token@test.invalid")))).andExpect(status().isForbidden());
    }
    @Test void rejectsDuplicateEmailAndStaleUpdates() throws Exception {
        var account=create();
        mvc.perform(post("/api/admin/operations/staff").with(admin()).with(csrf()).contentType("application/json").content(body(creation(account.get("email").asText().toUpperCase(Locale.ROOT)))))
            .andExpect(status().isBadRequest());
        command(account,"/role",Map.of("roleId",6));
        var old=Map.of("expectedUpdatedAt",account.get("updatedAt").asText(),"status","SUSPENDED","reason","Dữ liệu đã cũ");
        mvc.perform(put("/api/admin/operations/staff/"+account.get("userId").asText()+"/status").with(admin()).with(csrf()).contentType("application/json").content(body(old))).andExpect(status().isConflict());
    }
    @Test void cannotReassignSelfOrMoveLearnerIntoStaff() throws Exception {
        String version=jdbc.queryForObject("SELECT updated_at::text FROM users WHERE user_id=?",String.class,ADMIN);
        String timestamp=jdbc.queryForObject("SELECT to_char(updated_at AT TIME ZONE 'UTC','YYYY-MM-DD\"T\"HH24:MI:SS.US\"Z\"') FROM users WHERE user_id=?",String.class,ADMIN);
        var payload=Map.of("expectedUpdatedAt",timestamp,"roleId",4,"reason","Kiểm tra tự đổi vai trò");
        mvc.perform(put("/api/admin/operations/staff/"+ADMIN+"/role").with(admin()).with(csrf()).contentType("application/json").content(body(payload))).andExpect(status().isBadRequest());
        var account=create();
        mvc.perform(put("/api/admin/operations/staff/"+account.get("userId").asText()+"/role").with(admin()).with(csrf()).contentType("application/json").content(body(Map.of("expectedUpdatedAt",account.get("updatedAt").asText(),"roleId",2,"reason","Vai trò không phải nhân sự")))).andExpect(status().isBadRequest());
        UUID learner=UUID.nameUUIDFromBytes("ROLE_LEARNER".getBytes(java.nio.charset.StandardCharsets.UTF_8));
        mvc.perform(put("/api/admin/operations/staff/"+learner+"/role").with(admin()).with(csrf()).contentType("application/json").content(body(payload))).andExpect(status().isNotFound());
    }
    @Test void auditFailureRollsBackAccountUpdate() throws Exception {
        var account=create();UUID id=UUID.fromString(account.get("userId").asText());
        jdbc.execute("ALTER TABLE staff_account_audit_events ADD CONSTRAINT staff_test_audit_failure CHECK (reason <> 'reject-test')");
        var payload=Map.of("fullName","Không được lưu","email",account.get("email").asText(),"phone","","expectedUpdatedAt",account.get("updatedAt").asText(),"reason","reject-test");
        mvc.perform(put("/api/admin/operations/staff/"+id).with(admin()).with(csrf()).contentType("application/json").content(body(payload))).andExpect(status().isServiceUnavailable());
        assertEquals(account.get("fullName").asText(),jdbc.queryForObject("SELECT full_name FROM users WHERE user_id=?",String.class,id));
    }
}
