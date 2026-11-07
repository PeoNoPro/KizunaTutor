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
    @Test void catalogShowsSixRolesButOnlyThreeAreAssignableAtCreation() throws Exception {
        var roles=data(mvc.perform(get("/api/admin/operations/roles").with(admin())).andExpect(status().isOk()).andReturn());
        assertEquals(6,roles.size());int assignable=0;
        for(var role:roles){if(role.get("staffAssignable").asBoolean())assignable++;if(Set.of("Học viên","Gia sư").contains(role.get("label").asText()))assertFalse(role.get("staffAssignable").asBoolean());}
        assertEquals(3,assignable);
    }
    private final List<UUID> created=new ArrayList<>();
    @org.springframework.beans.factory.annotation.Autowired org.springframework.mail.javamail.JavaMailSender mail;
    private String emailedPassword(String to) throws Exception {
        var captor=org.mockito.ArgumentCaptor.forClass(jakarta.mail.internet.MimeMessage.class);
        org.mockito.Mockito.verify(mail,org.mockito.Mockito.timeout(3000).atLeastOnce()).send(captor.capture());
        for(var message:captor.getAllValues())if(message.getAllRecipients()[0].toString().equals(to)){
            var matcher=java.util.regex.Pattern.compile("data-temporary-password>([^<]+)<").matcher(message.getContent().toString());
            if(matcher.find())return org.springframework.web.util.HtmlUtils.htmlUnescape(matcher.group(1));
        }
        throw new AssertionError("Không có thư gửi tới "+to);
    }
    @BeforeEach void schema() throws Exception {
        jdbc.execute(Files.readString(Path.of("../database/seeds/fe11_operations_schema.sql")));
    }
    @AfterEach void cleanup() {
        jdbc.execute("ALTER TABLE staff_account_audit_events DROP CONSTRAINT IF EXISTS staff_test_audit_failure");
        for(var id:created) {jdbc.update("DELETE FROM staff_account_audit_events WHERE target_user_id=?",id);jdbc.update("DELETE FROM users WHERE user_id=?",id);}
    }
    private String body(Object value) throws Exception {return json.writeValueAsString(value);}
    private Map<String,Object> creation(String email) {return Map.of("fullName","Nhân viên kiểm thử","email",email,"phone","0901234567","roleId",4,"appointmentReason","Quyết định bổ nhiệm");}
    private JsonNode create() throws Exception {
        var result=mvc.perform(post("/api/admin/operations/staff").with(admin()).with(csrf()).contentType("application/json").content(body(creation(UUID.randomUUID()+"@test.invalid"))))
            .andExpect(status().isCreated()).andReturn();var data=data(result);created.add(UUID.fromString(data.get("userId").asText()));return data;
    }
    private JsonNode command(JsonNode account,String path,Map<String,Object> fields) throws Exception {
        var payload=new HashMap<>(fields);payload.put("expectedUpdatedAt",account.get("updatedAt").asText());payload.put("reason","Kiểm tra thay đổi có kiểm soát");
        return data(mvc.perform(put("/api/admin/operations/staff/"+account.get("userId").asText()+path).with(admin()).with(csrf()).contentType("application/json").content(body(payload))).andExpect(status().isOk()).andReturn());
    }
    @Test void createEditSuspendReactivateAndAuditUseDatabase() throws Exception {
        var original=create();UUID id=UUID.fromString(original.get("userId").asText());
        assertFalse(original.has("passwordHash"));assertFalse(original.has("initialPassword"));
        String password=emailedPassword(original.get("email").asText());assertFalse(original.toString().contains(password),"API không được trả mật khẩu");
        String hash=jdbc.queryForObject("SELECT password_hash FROM users WHERE user_id=?",String.class,id);assertTrue(new BCryptPasswordEncoder().matches(password,hash));
        assertFalse(jdbc.queryForObject("SELECT string_agg(reason,' ') FROM staff_account_audit_events WHERE target_user_id=?",String.class,id).contains(password));
        assertEquals(Boolean.TRUE,jdbc.queryForObject("SELECT must_change_password FROM users WHERE user_id=?",Boolean.class,id));
        var edited=command(original,"",Map.of("fullName","Tên đã cập nhật","email",original.get("email").asText(),"phone","0912345678"));
        assertEquals("Tên đã cập nhật",edited.get("fullName").asText());
        var suspended=command(edited,"/status",Map.of("status","SUSPENDED"));assertEquals("Tạm khóa",suspended.get("statusLabel").asText());
        var active=command(suspended,"/status",Map.of("status","ACTIVE"));assertEquals("ACTIVE",active.get("status").asText());
        assertEquals(4,jdbc.queryForObject("SELECT count(*) FROM staff_account_audit_events WHERE target_user_id=?",Integer.class,id));
        mvc.perform(get("/api/admin/operations/staff").param("q","Tên đã cập nhật").with(admin())).andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").doesNotExist()).andExpect(jsonPath("$.data.totalElements").value(1));
    }
    @Test void resendReplacesTemporaryPasswordOnlyBeforeFirstChange() throws Exception {
        var account=create();UUID id=UUID.fromString(account.get("userId").asText());String path="/api/admin/operations/staff/"+id+"/resend-credentials";
        String first=emailedPassword(account.get("email").asText());
        org.mockito.Mockito.clearInvocations(mail);
        mvc.perform(post(path).with(user("ROLE_MANAGER@test.invalid")).with(csrf()).contentType("application/json").content(body(Map.of("expectedUpdatedAt",account.get("updatedAt").asText())))).andExpect(status().isForbidden());
        var updated=data(mvc.perform(post(path).with(admin()).with(csrf()).contentType("application/json").content(body(Map.of("expectedUpdatedAt",account.get("updatedAt").asText())))).andExpect(status().isOk()).andReturn());
        String second=emailedPassword(account.get("email").asText());assertNotEquals(first,second);
        String hash=jdbc.queryForObject("SELECT password_hash FROM users WHERE user_id=?",String.class,id);
        assertTrue(new BCryptPasswordEncoder().matches(second,hash));assertFalse(new BCryptPasswordEncoder().matches(first,hash));
        jdbc.update("UPDATE users SET must_change_password=false WHERE user_id=?",id);
        String current=jdbc.queryForObject("SELECT to_char(updated_at AT TIME ZONE 'UTC','YYYY-MM-DD\"T\"HH24:MI:SS.US\"Z\"') FROM users WHERE user_id=?",String.class,id);
        mvc.perform(post(path).with(admin()).with(csrf()).contentType("application/json").content(body(Map.of("expectedUpdatedAt",current)))).andExpect(status().isConflict());
        assertEquals(updated.get("userId").asText(),id.toString());
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
        command(account,"",Map.of("fullName","Đã đổi tên","email",account.get("email").asText(),"phone",""));
        var old=Map.of("expectedUpdatedAt",account.get("updatedAt").asText(),"status","SUSPENDED","reason","Dữ liệu đã cũ");
        mvc.perform(put("/api/admin/operations/staff/"+account.get("userId").asText()+"/status").with(admin()).with(csrf()).contentType("application/json").content(body(old))).andExpect(status().isConflict());
    }
    @Test void auditFailureRollsBackAccountUpdate() throws Exception {
        var account=create();UUID id=UUID.fromString(account.get("userId").asText());
        jdbc.execute("ALTER TABLE staff_account_audit_events ADD CONSTRAINT staff_test_audit_failure CHECK (reason <> 'reject-test')");
        var payload=Map.of("fullName","Không được lưu","email",account.get("email").asText(),"phone","","expectedUpdatedAt",account.get("updatedAt").asText(),"reason","reject-test");
        mvc.perform(put("/api/admin/operations/staff/"+id).with(admin()).with(csrf()).contentType("application/json").content(body(payload))).andExpect(status().isServiceUnavailable());
        assertEquals(account.get("fullName").asText(),jdbc.queryForObject("SELECT full_name FROM users WHERE user_id=?",String.class,id));
    }
}
