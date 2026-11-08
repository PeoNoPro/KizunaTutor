package com.kizuna.operations;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.*;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import java.nio.file.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;

class StaffRoleChangeTest extends OperationsIntegrationBase {
    private static final String BASE="/api/admin/operations/role-changes";
    private static final int MANAGER_ROLE=4,CS_ROLE=5,ACADEMIC_ROLE=6;
    private final RequestPostProcessor manager=user("ROLE_MANAGER@test.invalid");
    private final List<UUID> created=new ArrayList<>();
    @BeforeEach void schema() throws Exception {
        jdbc.execute(Files.readString(Path.of("../database/seeds/fe11_operations_schema.sql")));
        jdbc.execute("DELETE FROM staff_role_change_requests");
    }
    @AfterEach void cleanup() {
        jdbc.execute("ALTER TABLE staff_account_audit_events DROP CONSTRAINT IF EXISTS staff_test_audit_failure");
        jdbc.execute("DELETE FROM staff_role_change_requests");
        for(var id:created){jdbc.update("DELETE FROM staff_account_audit_events WHERE target_user_id=? OR actor_user_id=?",id,id);jdbc.update("DELETE FROM users WHERE user_id=?",id);}
    }
    private String body(Object value) throws Exception {return json.writeValueAsString(value);}
    private JsonNode staff(int role) throws Exception {
        var request=Map.of("fullName","Nhân viên "+UUID.randomUUID(),"email",UUID.randomUUID()+"@test.invalid","phone","0901234567","roleId",role,"appointmentReason","Quyết định bổ nhiệm");
        var data=data(mvc.perform(post("/api/admin/operations/staff").with(admin()).with(csrf()).contentType("application/json").content(body(request))).andExpect(status().isCreated()).andReturn());
        created.add(UUID.fromString(data.get("userId").asText()));return data;
    }
    private int roleOf(JsonNode account) {return jdbc.queryForObject("SELECT role_id FROM users WHERE user_id=?::uuid",Integer.class,account.get("userId").asText());}
    private Map<String,Object> moveBody(JsonNode account,String reason) {return Map.of("expectedUpdatedAt",account.get("updatedAt").asText(),"reason",reason);}
    private org.springframework.test.web.servlet.ResultActions propose(JsonNode account,RequestPostProcessor actor) throws Exception {
        return mvc.perform(post(BASE+"/promotions").with(actor).with(csrf()).contentType("application/json").content(body(Map.of("targetUserId",account.get("userId").asText(),"expectedUpdatedAt",account.get("updatedAt").asText(),"reason","Đã hoàn thành tốt nhiệm vụ"))));
    }
    private String proposed(JsonNode account) throws Exception {return data(propose(account,manager).andExpect(status().isCreated()).andReturn()).get("requestId").asText();}
    private org.springframework.test.web.servlet.ResultActions decide(String id,String action,RequestPostProcessor actor,String note) throws Exception {
        return mvc.perform(post(BASE+"/promotions/"+id+"/"+action).with(actor).with(csrf()).contentType("application/json").content(note==null?"{}":body(Map.of("note",note))));
    }

    @Test void adminCannotChangeRoleDirectlyOrGrantAdmin() throws Exception {
        var account=staff(CS_ROLE);
        mvc.perform(put("/api/admin/operations/staff/"+account.get("userId").asText()+"/role").with(admin()).with(csrf()).contentType("application/json").content(body(Map.of("expectedUpdatedAt",account.get("updatedAt").asText(),"roleId",ACADEMIC_ROLE,"reason","Không được phép")))).andExpect(status().is4xxClientError());
        assertEquals(CS_ROLE,roleOf(account));
        mvc.perform(put(BASE+"/staff/"+account.get("userId").asText()+"/role").with(admin()).with(csrf()).contentType("application/json").content(body(moveBody(account,"Admin không đổi")))).andExpect(status().isForbidden());
        assertEquals(CS_ROLE,roleOf(account));
        var request=Map.of("fullName","Không tạo","email",UUID.randomUUID()+"@test.invalid","roleId",1);
        mvc.perform(post("/api/admin/operations/staff").with(admin()).with(csrf()).contentType("application/json").content(body(request))).andExpect(status().isBadRequest());
        assertEquals(MANAGER_ROLE,roleOf(staff(MANAGER_ROLE)),"Admin vẫn gán được Manager khi tạo tài khoản");
    }
    @Test void managerHasNoAccountAdministrationRights() throws Exception {
        var account=staff(CS_ROLE);String path="/api/admin/operations/staff/"+account.get("userId").asText();
        mvc.perform(get("/api/admin/operations/staff").with(manager)).andExpect(status().isForbidden());
        mvc.perform(get("/api/admin/operations/csrf").with(manager)).andExpect(status().isForbidden());
        mvc.perform(put(path+"/status").with(manager).with(csrf()).contentType("application/json").content(body(Map.of("status","SUSPENDED","expectedUpdatedAt",account.get("updatedAt").asText(),"reason","Không được phép")))).andExpect(status().isForbidden());
        mvc.perform(post("/api/admin/operations/staff").with(manager).with(csrf()).contentType("application/json").content(body(Map.of("fullName","Không tạo","email",UUID.randomUUID()+"@test.invalid","roleId",CS_ROLE)))).andExpect(status().isForbidden());
        mvc.perform(get(BASE+"/staff").with(admin())).andExpect(status().isForbidden());
        mvc.perform(get(BASE+"/staff")).andExpect(status().isUnauthorized());
        mvc.perform(get(BASE+"/promotions").with(user("ROLE_CS_STAFF@test.invalid"))).andExpect(status().isForbidden());
    }
    @Test void managerSeesOnlyMovableStaffAndMovesBothWaysWithAudit() throws Exception {
        var account=staff(CS_ROLE);var other=staff(MANAGER_ROLE);String id=account.get("userId").asText();
        var list=mvc.perform(get(BASE+"/staff").param("q",account.get("fullName").asText()).with(manager)).andExpect(status().isOk()).andExpect(jsonPath("$.data.totalElements").value(1))
            .andExpect(jsonPath("$.data.items[0].moveToLabel").value("Cán bộ học vụ")).andExpect(jsonPath("$.data.items[0].phone").doesNotExist()).andReturn();
        mvc.perform(get(BASE+"/staff").param("q",other.get("fullName").asText()).with(manager)).andExpect(jsonPath("$.data.totalElements").value(0));
        var row=data(list).get("items").get(0);
        mvc.perform(put(BASE+"/staff/"+id+"/role").with(manager).contentType("application/json").content(body(moveBody(account,"Thiếu CSRF")))).andExpect(status().isForbidden());
        mvc.perform(put(BASE+"/staff/"+id+"/role").with(manager).with(csrf()).contentType("application/json").content(body(moveBody(row," ")))).andExpect(status().isBadRequest());
        mvc.perform(put(BASE+"/staff/"+id+"/role").with(manager).with(csrf()).contentType("application/json").content(body(moveBody(row,"Điều chuyển nhóm")))).andExpect(status().isOk());
        assertEquals(ACADEMIC_ROLE,roleOf(account));
        String audit=jdbc.queryForObject("SELECT reason FROM staff_account_audit_events WHERE target_user_id=?::uuid AND action='ROLE' AND actor_user_id=(SELECT user_id FROM users WHERE email='ROLE_MANAGER@test.invalid')",String.class,id);
        assertEquals("Chăm sóc khách hàng → Cán bộ học vụ. Điều chuyển nhóm",audit);
        mvc.perform(put(BASE+"/staff/"+id+"/role").with(manager).with(csrf()).contentType("application/json").content(body(moveBody(row,"Dữ liệu đã cũ")))).andExpect(status().isConflict());
    }
    @Test void managerCannotMoveManagersAdminsLearnersOrSuspendedStaff() throws Exception {
        var other=staff(MANAGER_ROLE);
        mvc.perform(put(BASE+"/staff/"+other.get("userId").asText()+"/role").with(manager).with(csrf()).contentType("application/json").content(body(moveBody(other,"Không được")))).andExpect(status().isBadRequest());
        propose(other,manager).andExpect(status().isBadRequest());
        UUID learner=UUID.nameUUIDFromBytes("ROLE_LEARNER".getBytes(java.nio.charset.StandardCharsets.UTF_8));
        mvc.perform(put(BASE+"/staff/"+learner+"/role").with(manager).with(csrf()).contentType("application/json").content(body(moveBody(other,"Không được")))).andExpect(status().isNotFound());
        var account=staff(CS_ROLE);
        var locked=data(mvc.perform(put("/api/admin/operations/staff/"+account.get("userId").asText()+"/status").with(admin()).with(csrf()).contentType("application/json").content(body(Map.of("status","SUSPENDED","expectedUpdatedAt",account.get("updatedAt").asText(),"reason","Tạm khóa")))).andExpect(status().isOk()).andReturn());
        mvc.perform(put(BASE+"/staff/"+account.get("userId").asText()+"/role").with(manager).with(csrf()).contentType("application/json").content(body(moveBody(locked,"Không được")))).andExpect(status().isBadRequest());
        assertEquals(CS_ROLE,roleOf(account));
    }
    @Test void approvalPromotesOnlyAfterAdminDecisionAndRecordsBothPeople() throws Exception {
        var account=staff(ACADEMIC_ROLE);String id=proposed(account);
        assertEquals(ACADEMIC_ROLE,roleOf(account),"Đề xuất chưa được đổi vai trò");
        propose(account,manager).andExpect(status().isConflict());
        mvc.perform(put(BASE+"/staff/"+account.get("userId").asText()+"/role").with(manager).with(csrf()).contentType("application/json").content(body(moveBody(account,"Đang chờ duyệt")))).andExpect(status().isConflict());
        decide(id,"approve",manager,null).andExpect(status().isForbidden());
        propose(staff(CS_ROLE),admin()).andExpect(status().isForbidden());
        mvc.perform(get(BASE+"/promotions").param("status","PENDING").with(admin())).andExpect(jsonPath("$.data.totalElements").value(1)).andExpect(jsonPath("$.data.items[0].requesterName").value("Người kiểm thử 3"));
        decide(id,"approve",admin(),"Theo quyết định 12").andExpect(status().isOk());
        assertEquals(MANAGER_ROLE,roleOf(account));
        String audit=jdbc.queryForObject("SELECT reason FROM staff_account_audit_events WHERE target_user_id=?::uuid AND action='ROLE' AND actor_user_id=?",String.class,account.get("userId").asText(),ADMIN);
        assertEquals("Cán bộ học vụ → Quản lý vận hành. Duyệt đề xuất của Người kiểm thử 3: Đã hoàn thành tốt nhiệm vụ Ghi chú: Theo quyết định 12",audit);
        decide(id,"approve",admin(),null).andExpect(status().isConflict());
        decide(id,"reject",admin(),"Đã chốt").andExpect(status().isConflict());
    }
    @Test void rejectAndCancelLeaveRoleUnchangedAndCancelBelongsToRequester() throws Exception {
        var account=staff(CS_ROLE);String id=proposed(account);
        decide(id,"reject",admin(),null).andExpect(status().isBadRequest());
        decide(id,"reject",admin(),"Chưa đủ thời gian công tác").andExpect(status().isOk());
        assertEquals(CS_ROLE,roleOf(account));
        id=proposed(account);
        var second=staff(MANAGER_ROLE);var secondManager=user(second.get("email").asText());
        jdbc.update("UPDATE users SET must_change_password=false WHERE user_id=?::uuid",second.get("userId").asText());
        mvc.perform(post(BASE+"/promotions/"+id+"/cancel").with(secondManager).with(csrf())).andExpect(status().isForbidden());
        mvc.perform(get(BASE+"/promotions").with(secondManager)).andExpect(jsonPath("$.data.totalElements").value(0));
        mvc.perform(get(BASE+"/promotions").with(manager)).andExpect(jsonPath("$.data.totalElements").value(2));
        mvc.perform(post(BASE+"/promotions/"+id+"/cancel").with(manager).with(csrf())).andExpect(status().isOk());
        mvc.perform(post(BASE+"/promotions/"+id+"/cancel").with(manager).with(csrf())).andExpect(status().isConflict());
        assertEquals(CS_ROLE,roleOf(account));
    }
    @Test void approvalRechecksTargetAndRollsBackWhenAuditFails() throws Exception {
        var account=staff(CS_ROLE);String id=proposed(account);
        var locked=data(mvc.perform(put("/api/admin/operations/staff/"+account.get("userId").asText()+"/status").with(admin()).with(csrf()).contentType("application/json").content(body(Map.of("status","SUSPENDED","expectedUpdatedAt",account.get("updatedAt").asText(),"reason","Tạm khóa")))).andExpect(status().isOk()).andReturn());
        decide(id,"approve",admin(),null).andExpect(status().isConflict());
        assertEquals(CS_ROLE,roleOf(account));
        mvc.perform(put("/api/admin/operations/staff/"+account.get("userId").asText()+"/status").with(admin()).with(csrf()).contentType("application/json").content(body(Map.of("status","ACTIVE","expectedUpdatedAt",locked.get("updatedAt").asText(),"reason","Mở lại")))).andExpect(status().isOk());
        jdbc.execute("ALTER TABLE staff_account_audit_events ADD CONSTRAINT staff_test_audit_failure CHECK (position('Duyệt đề xuất' in reason)=0) NOT VALID");
        decide(id,"approve",admin(),null).andExpect(status().isServiceUnavailable());
        assertEquals(CS_ROLE,roleOf(account));
        assertEquals("PENDING",jdbc.queryForObject("SELECT status FROM staff_role_change_requests WHERE request_id=?::uuid",String.class,id));
    }
}
