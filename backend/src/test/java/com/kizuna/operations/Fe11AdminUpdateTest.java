package com.kizuna.operations;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.*;
import org.springframework.http.MediaType;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.beans.factory.annotation.Autowired;
import java.nio.file.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;

/** Phạm vi 2026-11-05: trả lại sự cố, trạng thái email, bắt đổi mật khẩu trên API FE-11, giới hạn đính kèm. */
class Fe11AdminUpdateTest extends OperationsIntegrationBase {
    private static final String OPS="/api/admin/operations";
    @Autowired JavaMailSender mail;
    private final List<UUID> created=new ArrayList<>();
    @BeforeEach void schema() throws Exception {
        jdbc.execute(Files.readString(Path.of("../database/seeds/fe11_operations_schema.sql")));
        jdbc.update("DELETE FROM system_setting_audit_events");
        jdbc.update("DELETE FROM system_configurations WHERE config_key='SUPPORT_ATTACHMENT_MAX_MB'");
    }
    @AfterEach void cleanup() {
        doNothing().when(mail).send(any(jakarta.mail.internet.MimeMessage.class));
        jdbc.update("UPDATE users SET account_status='ACTIVE' WHERE user_id=?",ADMIN2);
        jdbc.update("DELETE FROM system_setting_audit_events");
        jdbc.update("DELETE FROM system_configurations WHERE config_key='SUPPORT_ATTACHMENT_MAX_MB'");
        for(var id:created){jdbc.update("DELETE FROM staff_account_audit_events WHERE target_user_id=?",id);jdbc.update("DELETE FROM users WHERE user_id=?",id);}
    }
    private String body(Object value) throws Exception {return json.writeValueAsString(value);}
    private JsonNode staff() throws Exception {
        var data=data(mvc.perform(post(OPS+"/staff").with(admin()).with(csrf()).contentType("application/json").content(body(Map.of("fullName","Nhân viên "+UUID.randomUUID(),"email",UUID.randomUUID()+"@test.invalid","roleId",5)))).andExpect(status().isCreated()).andReturn());
        created.add(UUID.fromString(data.get("userId").asText()));return data;
    }
    private String emailStatus(JsonNode account) {
        return jdbc.queryForObject("SELECT status FROM staff_credential_emails WHERE user_id=?::uuid",String.class,account.get("userId").asText());
    }
    private void waitForStatus(JsonNode account,String expected) throws Exception {
        long end=System.currentTimeMillis()+8000;
        while(System.currentTimeMillis()<end){if(expected.equals(emailStatus(account)))return;Thread.sleep(50);}
        assertEquals(expected,emailStatus(account));
    }
    private JsonNode incident() throws Exception {
        return data(mvc.perform(post(OPS+"/incidents").with(admin()).with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{\"subject\":\"Kiểm tra trả lại\",\"description\":\"Mô tả\",\"severity\":\"HIGH\",\"components\":[\"DATABASE\"]}")).andExpect(status().isCreated()).andReturn());
    }
    private org.springframework.test.web.servlet.ResultActions act(JsonNode i,String action,String extra,org.springframework.test.web.servlet.request.RequestPostProcessor actor) throws Exception {
        return mvc.perform(post(OPS+"/incidents/"+i.get("incidentId").asText()+"/"+action).with(actor).with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{\"expectedVersion\":"+i.get("version").asLong()+extra+"}"));
    }

    @Test void emailStatusSeparatesAccountCreationFromDeliveryAndRetrySendsNewPassword() throws Exception {
        var sent=staff();waitForStatus(sent,"SENT");
        doThrow(new MailSendException("smtp")).when(mail).send(any(jakarta.mail.internet.MimeMessage.class));
        var failed=staff();waitForStatus(failed,"FAILED");
        assertEquals(1,jdbc.queryForObject("SELECT count(*) FROM users WHERE user_id=?::uuid",Integer.class,failed.get("userId").asText()),"Gửi lỗi không làm mất tài khoản");
        var listed=data(mvc.perform(get(OPS+"/staff").param("q",failed.get("email").asText()).with(admin())).andExpect(status().isOk()).andReturn()).get("items").get(0);
        assertEquals("FAILED",listed.get("emailStatus").asText());assertEquals("Gửi email thất bại",listed.get("emailStatusLabel").asText());assertFalse(listed.toString().contains("smtp"));
        assertTrue(listed.get("mustChangePassword").asBoolean());
        doNothing().when(mail).send(any(jakarta.mail.internet.MimeMessage.class));
        mvc.perform(post(OPS+"/staff/"+failed.get("userId").asText()+"/resend-credentials").with(admin()).with(csrf()).contentType("application/json").content(body(Map.of("expectedUpdatedAt",listed.get("updatedAt").asText())))).andExpect(status().isOk());
        waitForStatus(failed,"SENT");
        var columns=jdbc.queryForList("SELECT column_name FROM information_schema.columns WHERE table_name='staff_credential_emails'",String.class);
        assertTrue(columns.stream().noneMatch(name->name.contains("password")||name.contains("hash")),"Hàng đợi không được lưu bí mật");
    }
    @Test void pendingFirstPasswordChangeBlocksFe11ApisUntilChanged() throws Exception {
        var account=staff();String path=OPS+"/me";
        jdbc.update("UPDATE roles SET is_active=true WHERE role_id=5");
        mvc.perform(get(path).with(user(account.get("email").asText()))).andExpect(status().isForbidden());
        jdbc.update("UPDATE users SET must_change_password=false WHERE user_id=?::uuid",account.get("userId").asText());
        mvc.perform(get(path).with(user(account.get("email").asText()))).andExpect(status().isOk());
    }
    @Test void managerAccountCreationNeedsAppointmentReason() throws Exception {
        var request=new HashMap<String,Object>(Map.of("fullName","Quản lý mới","email",UUID.randomUUID()+"@test.invalid","roleId",4));
        mvc.perform(post(OPS+"/staff").with(admin()).with(csrf()).contentType("application/json").content(body(request))).andExpect(status().isBadRequest());
        request.put("appointmentReason","Quyết định số 12");
        var data=data(mvc.perform(post(OPS+"/staff").with(admin()).with(csrf()).contentType("application/json").content(body(request))).andExpect(status().isCreated()).andReturn());
        created.add(UUID.fromString(data.get("userId").asText()));
        assertTrue(jdbc.queryForObject("SELECT reason FROM staff_account_audit_events WHERE target_user_id=?::uuid",String.class,data.get("userId").asText()).contains("Quyết định số 12"));
    }
    @Test void assignmentIsGoneAndOwnerCanReleaseForAnotherAdminToClaim() throws Exception {
        var second=user("second@test.invalid");
        var claimed=data(act(incident(),"claim","",admin()).andExpect(status().isOk()).andReturn());
        act(claimed,"assignment",",\"assigneeId\":\""+ADMIN2+"\",\"reason\":\"Không còn\"",admin()).andExpect(status().is4xxClientError());
        act(claimed,"release",",\"reason\":\"Không rảnh\"",second).andExpect(status().isForbidden());
        act(claimed,"release","",admin()).andExpect(status().isBadRequest());
        var released=data(act(claimed,"release",",\"reason\":\"Bận việc khác\"",admin()).andExpect(status().isOk()).andReturn());
        assertEquals("Chưa xử lý",released.get("statusLabel").asText());assertEquals("Chưa phân công",released.get("assigneeLabel").asText());
        var next=data(act(released,"claim","",second).andExpect(status().isOk()).andReturn());
        // Người chậm chân dù gửi đúng phiên bản cũ vẫn nhận thông báo đã có người xử lý.
        act(released,"claim","",admin()).andExpect(status().isConflict()).andExpect(jsonPath("$.message").value("Sự cố đã có người nhận xử lý."));
        var history=data(mvc.perform(get(OPS+"/incidents/"+next.get("incidentId").asText()+"/events").with(admin())).andExpect(status().isOk()).andReturn());
        assertEquals(4,history.get("totalElements").asInt());assertEquals("Trả lại danh sách chờ",history.get("items").get(2).get("actionLabel").asText());
    }
    @Test void anotherAdminCanReleaseOnlyWhenOwnerIsNoLongerActive() throws Exception {
        var second=user("second@test.invalid");
        var claimed=data(act(incident(),"claim","",second).andExpect(status().isOk()).andReturn());
        act(claimed,"release",",\"reason\":\"Giành xử lý\"",admin()).andExpect(status().isForbidden());
        jdbc.update("UPDATE users SET account_status='SUSPENDED' WHERE user_id=?",ADMIN2);
        var detail=data(mvc.perform(get(OPS+"/incidents/"+claimed.get("incidentId").asText()).with(admin())).andExpect(status().isOk()).andReturn());
        assertTrue(detail.get("allowedActions").toString().contains("release"));
        act(claimed,"release",",\"reason\":\"Người phụ trách đã bị khóa\"",admin()).andExpect(status().isOk());
    }
    @Test void attachmentLimitIsValidatedVersionedAndAudited() throws Exception {
        String path=OPS+"/settings/attachment-limit";
        var current=data(mvc.perform(get(path).with(admin())).andExpect(status().isOk()).andReturn());
        assertEquals(10,current.get("maxMegabytes").asInt());
        mvc.perform(get(path).with(user("ROLE_MANAGER@test.invalid"))).andExpect(status().isForbidden());
        for(int bad:new int[]{0,11})mvc.perform(put(path).with(admin()).with(csrf()).contentType("application/json").content(body(Map.of("maxMegabytes",bad,"expectedVersion",0,"reason","Sai")))).andExpect(status().isBadRequest());
        mvc.perform(put(path).with(admin()).with(csrf()).contentType("application/json").content(body(Map.of("maxMegabytes",5,"expectedVersion",0," reason","")))).andExpect(status().isBadRequest());
        mvc.perform(put(path).with(admin()).with(csrf()).contentType("application/json").content(body(Map.of("maxMegabytes",5,"expectedVersion",0,"reason","Giảm tải")))).andExpect(status().isOk()).andExpect(jsonPath("$.data.maxMegabytes").value(5)).andExpect(jsonPath("$.data.version").value(1));
        mvc.perform(put(path).with(admin()).with(csrf()).contentType("application/json").content(body(Map.of("maxMegabytes",6,"expectedVersion",0,"reason","Dữ liệu cũ")))).andExpect(status().isConflict());
        mvc.perform(get(OPS+"/settings/history").with(admin())).andExpect(status().isOk()).andExpect(jsonPath("$.data.items[0].before").value("10")).andExpect(jsonPath("$.data.items[0].after").value("5"));
        mvc.perform(get(OPS+"/audit-log?source=CONFIG&size=50").with(admin())).andExpect(status().isOk()).andExpect(jsonPath("$.data.items[?(@.reason=='Giảm tải')].resultLabel").value("10 → 5 MB"));
    }
    @Test void healthApiIsNoLongerServed() throws Exception {
        mvc.perform(get(OPS+"/health").with(admin())).andExpect(status().is4xxClientError());
    }
}
