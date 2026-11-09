package com.kizuna.support;
import org.junit.jupiter.api.*;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.test.web.servlet.*;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.jdbc.core.JdbcTemplate;
import javax.sql.DataSource;
import java.nio.file.*;
import java.util.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import com.fasterxml.jackson.databind.*;
import static org.junit.jupiter.api.Assertions.*;

@SpringJUnitConfig(SupportTicketIntegrationConfig.class) @WebAppConfiguration
class SupportTicketWorkflowTest {
 @Autowired WebApplicationContext context;@Autowired DataSource ds;@Autowired ObjectMapper json;
 MockMvc mvc;JdbcTemplate jdbc;
 static final String BASE="/api/v1/tickets";
 static final String OWNER="fe10-owner@test.invalid",OTHER="fe10-other@test.invalid",CS="fe10-cs@test.invalid",CS2="fe10-cs2@test.invalid",MANAGER="fe10-manager@test.invalid",MANAGER2="fe10-manager2@test.invalid",ADMIN="fe10-admin@test.invalid";
 @BeforeEach void setup()throws Exception{
   jdbc=new JdbcTemplate(ds);jdbc.execute(Files.readString(Path.of("../database/seeds/fe10_support_ticket.sql")));
   String[][] accounts={{OWNER,"ROLE_LEARNER"},{OTHER,"ROLE_TUTOR"},{CS,"ROLE_CS_STAFF"},{CS2,"ROLE_CS_STAFF"},{MANAGER,"ROLE_MANAGER"},{MANAGER2,"ROLE_MANAGER"},{ADMIN,"ROLE_ADMIN"}};
   for(var a:accounts){jdbc.update("INSERT INTO roles(role_name,is_active) VALUES(?,true) ON CONFLICT(role_name) DO UPDATE SET is_active=true",a[1]);jdbc.update("INSERT INTO users(user_id,role_id,email,password_hash,full_name,account_status) SELECT ?,role_id,?,'not-a-login-secret',?,'ACTIVE' FROM roles WHERE role_name=? ON CONFLICT(email) DO UPDATE SET account_status='ACTIVE',deleted_at=null,role_id=excluded.role_id",UUID.randomUUID(),a[0],"Kiểm thử hỗ trợ",a[1]);}
   mvc=org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
 }
 JsonNode data(MvcResult r)throws Exception{return json.readTree(r.getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8)).path("data");}
 JsonNode create(String actor)throws Exception{return create(actor,false);}
 JsonNode create(String actor,boolean withFile)throws Exception{
   var request=multipart(BASE).file(new MockMultipartFile("payload","","application/json","{\"subject\":\"Cần hỗ trợ đặt học thử\",\"category\":\"BOOKING\",\"message\":\"Nhờ hướng dẫn đặt lịch\"}".getBytes(java.nio.charset.StandardCharsets.UTF_8)));
   if(withFile){var png=new java.io.ByteArrayOutputStream();javax.imageio.ImageIO.write(new java.awt.image.BufferedImage(8,8,java.awt.image.BufferedImage.TYPE_BYTE_GRAY),"png",png);request.file(new MockMultipartFile("files","context.png","image/png",png.toByteArray()));}
   return data(mvc.perform(request.with(user(actor)).with(csrf())).andExpect(status().isCreated()).andReturn());
 }
 JsonNode command(String id,String path,String actor,long version,String fields)throws Exception{return data(mvc.perform(post(BASE+"/"+id+"/"+path).with(user(actor)).with(csrf()).contentType("application/json").content("{\"expectedVersion\":"+version+fields+"}")).andExpect(status().isOk()).andReturn());}
 String id(String actor){return jdbc.queryForObject("SELECT user_id::text FROM users WHERE email=?",String.class,actor);}
 @Test void aggregateCountsMatchScopedListsAndDoNotLeakOtherRequesters()throws Exception{
   var before=data(mvc.perform(get(BASE+"/counts").with(user(OWNER))).andExpect(status().isOk()).andReturn());
   create(OTHER);
   var afterOther=data(mvc.perform(get(BASE+"/counts").with(user(OWNER))).andExpect(status().isOk()).andReturn());
   assertEquals(before,afterOther);
   create(OWNER);
   for(String actor:List.of(OWNER,OTHER,CS,MANAGER)){
     var counts=data(mvc.perform(get(BASE+"/counts?category=BOOKING").with(user(actor))).andExpect(status().isOk()).andReturn());
     for(String status:List.of("OPEN","IN_PROGRESS","ESCALATED","RESOLVED","CLOSED")){
       var list=data(mvc.perform(get(BASE+"?category=BOOKING&size=1&status="+status).with(user(actor))).andExpect(status().isOk()).andReturn());
       assertEquals(list.path("totalElements").asLong(),counts.path(status).asLong(),actor+" / "+status);
     }
   }
   mvc.perform(get(BASE+"/counts")).andExpect(status().isUnauthorized());
   mvc.perform(get(BASE+"/counts?assignedTo="+id(CS)).with(user(OWNER))).andExpect(status().isForbidden());
   mvc.perform(get(BASE+"/counts?category=INVALID").with(user(CS))).andExpect(status().isBadRequest());
 }
 @Test void requesterIsolationCsrfAndCurrentRole()throws Exception{
   mvc.perform(get(BASE)).andExpect(status().isUnauthorized());
   mvc.perform(post(BASE).with(user(OWNER))).andExpect(status().isForbidden());
   var t=create(OWNER);String tid=t.path("ticketId").asText();
   mvc.perform(get(BASE+"/"+tid).with(user(OTHER))).andExpect(status().isNotFound());
   mvc.perform(get(BASE+"/"+UUID.randomUUID()).with(user(OTHER))).andExpect(status().isNotFound());
   mvc.perform(get(BASE).with(user(ADMIN))).andExpect(status().isForbidden());
   mvc.perform(get(BASE+"/"+tid+"/history").with(user(OWNER))).andExpect(status().isForbidden());
   assertFalse(t.has("escalation"));assertFalse(t.has("assignee"));assertFalse(t.has("history"));assertFalse(t.has("requesterRoleLabel"));
   assertEquals("Mới tiếp nhận",t.path("statusLabel").asText());
   var mine=data(mvc.perform(get(BASE+"?size=50").with(user(OWNER))).andExpect(status().isOk()).andReturn());assertEquals("Nhờ hướng dẫn đặt lịch",mine.path("items").get(0).path("descriptionPreview").asText());
   var queue=data(mvc.perform(get(BASE+"?size=1").with(user(CS))).andExpect(status().isOk()).andReturn());assertFalse(queue.path("items").get(0).has("descriptionPreview"));assertFalse(queue.path("items").get(0).path("requesterDisplayName").asText().isBlank());assertFalse(mine.path("items").get(0).has("requesterDisplayName"));
   var grouped=data(mvc.perform(get(BASE+"?status=OPEN,IN_PROGRESS&size=50").with(user(OWNER))).andExpect(status().isOk()).andReturn());assertEquals(tid,grouped.path("items").get(0).path("ticketId").asText());
   var done=data(mvc.perform(get(BASE+"?status=RESOLVED,CLOSED&size=50").with(user(OWNER))).andExpect(status().isOk()).andReturn());assertFalse(done.toString().contains(tid));
   mvc.perform(get(BASE+"?status=OPEN,UNKNOWN").with(user(OWNER))).andExpect(status().isBadRequest());
   jdbc.update("UPDATE users SET account_status='SUSPENDED' WHERE email=?",OWNER);
   mvc.perform(get(BASE+"/"+tid).with(user(OWNER))).andExpect(status().isForbidden());
 }
 @Test void csDashboardUsesLiveCountsAndCurrentReturnedEscalationOnly()throws Exception{
   mvc.perform(get(BASE+"/dashboard")).andExpect(status().isUnauthorized());
   for(String actor:List.of(OWNER,OTHER,MANAGER,ADMIN))mvc.perform(get(BASE+"/dashboard").with(user(actor))).andExpect(status().isForbidden());
   var before=data(mvc.perform(get(BASE+"/dashboard").with(user(CS))).andExpect(status().isOk()).andReturn());
   var t=create(OWNER);String tid=t.path("ticketId").asText();
   var dashboard=data(mvc.perform(get(BASE+"/dashboard?category=BOOKING").with(user(CS))).andExpect(status().isOk()).andReturn());
   assertEquals(before.path("totalActive").asLong()+1,dashboard.path("totalActive").asLong());assertTrue(dashboard.path("queue").size()<=4);
   assertTrue(java.util.stream.StreamSupport.stream(dashboard.path("queue").spliterator(),false).allMatch(item->item.path("category").asText().equals("BOOKING")));
   assertFalse(dashboard.path("queue").get(0).path("requesterDisplayName").asText().isBlank());
   command(tid,"claim",CS,0,"");command(tid,"escalations",CS,1,",\"managerId\":\""+id(MANAGER)+"\",\"reason\":\"Cần hướng dẫn\",\"handlingSummary\":\"Đã kiểm tra\"");
   command(tid,"escalations/current/decision",MANAGER,2,",\"decisionText\":\"Hướng dẫn riêng\"");command(tid,"escalations/current/return",MANAGER,3,"");
   dashboard=data(mvc.perform(get(BASE+"/dashboard").with(user(CS))).andReturn());assertEquals(tid,dashboard.path("managerFeedback").path("ticketId").asText());assertFalse(dashboard.path("managerFeedback").has("decisionText"));
   var other=data(mvc.perform(get(BASE+"/dashboard").with(user(CS2))).andReturn());assertNotEquals(tid,other.path("managerFeedback").path("ticketId").asText());
   command(tid,"escalations",CS,4,",\"managerId\":\""+id(MANAGER)+"\",\"reason\":\"Cần xem xét tiếp\",\"handlingSummary\":\"Đã trao đổi\"");
   dashboard=data(mvc.perform(get(BASE+"/dashboard").with(user(CS))).andReturn());assertNotEquals(tid,dashboard.path("managerFeedback").path("ticketId").asText());
   mvc.perform(get(BASE+"/dashboard?category=invalid").with(user(CS))).andExpect(status().isBadRequest());
 }
 @Test void fullWorkflowStaleVersionAndManagerRevocation()throws Exception{
   var t=create(OWNER,true);String tid=t.path("ticketId").asText();
   var firstMessage=data(mvc.perform(get(BASE+"/"+tid+"/messages").with(user(OWNER))).andReturn());String file=firstMessage.path("items").get(0).path("attachments").get(0).path("downloadPath").asText();
   t=command(tid,"claim",CS,0,"");assertEquals("IN_PROGRESS",t.path("status").asText());
   mvc.perform(post(BASE+"/"+tid+"/claim").with(user(CS2)).with(csrf()).contentType("application/json").content("{\"expectedVersion\":0}")).andExpect(status().isConflict());
   t=command(tid,"assignment",CS2,1,",\"assigneeId\":\""+id(CS2)+"\"");
   mvc.perform(post(BASE+"/"+tid+"/resolve").with(user(CS)).with(csrf()).contentType("application/json").content("{\"expectedVersion\":2,\"resolutionText\":\"Không được phép\"}")).andExpect(status().isForbidden());
   t=command(tid,"escalations",CS2,2,",\"managerId\":\""+id(MANAGER)+"\",\"reason\":\"Cần hướng dẫn\",\"handlingSummary\":\"Đã đối chiếu lịch\"");
   assertEquals("Đã chuyển quản lý",t.path("statusLabel").asText());assertEquals("Học viên",t.path("requesterRoleLabel").asText());assertEquals("Kiểm thử hỗ trợ",t.path("requesterDisplayName").asText());
   mvc.perform(get(file).with(user(MANAGER))).andExpect(status().isOk());
   t=command(tid,"escalations/current/decision",MANAGER,3,",\"decisionText\":\"Hướng dẫn riêng tư\"");
   t=command(tid,"escalations/current/recipient",CS2,4,",\"managerId\":\""+id(MANAGER2)+"\",\"reason\":\"Đổi người phụ trách\"");
   mvc.perform(get(BASE+"/"+tid).with(user(MANAGER))).andExpect(status().isNotFound());
   mvc.perform(get(file).with(user(MANAGER))).andExpect(status().isNotFound());mvc.perform(get(file).with(user(MANAGER2))).andExpect(status().isOk());
   mvc.perform(post(BASE+"/"+tid+"/escalations/current/return").with(user(MANAGER2)).with(csrf()).contentType("application/json").content("{\"expectedVersion\":5}")).andExpect(status().isConflict());
   var owner=data(mvc.perform(get(BASE+"/"+tid).with(user(OWNER))).andExpect(status().isOk()).andReturn());assertFalse(owner.toString().contains("Hướng dẫn riêng tư"));assertFalse(owner.has("escalation"));
   command(tid,"escalations/current/decision",MANAGER2,5,",\"decisionText\":\"Tư vấn cho nhân viên\"");
   command(tid,"escalations/current/return",MANAGER2,6,"");
   command(tid,"resolve",CS2,7,",\"resolutionText\":\"Đã hướng dẫn đặt lịch\"");
   t=command(tid,"close",OWNER,8,"");assertEquals("CLOSED",t.path("status").asText());
   assertEquals(10,jdbc.queryForObject("SELECT count(*) FROM ticket_audit_events WHERE ticket_id=CAST(? AS uuid)",Integer.class,tid));
 }
 @Test void rejectsForgedContextAndInvalidFilesWithoutPartialWrites()throws Exception{
   long before=jdbc.queryForObject("SELECT count(*) FROM support_tickets",Long.class);
   mvc.perform(multipart(BASE).file(new MockMultipartFile("payload","","application/json",("{\"subject\":\"Phiếu\",\"category\":\"OTHER\",\"message\":\"Nội dung\",\"relatedBookingId\":\""+UUID.randomUUID()+"\"}").getBytes(java.nio.charset.StandardCharsets.UTF_8))).with(user(OWNER)).with(csrf())).andExpect(status().isBadRequest());
   mvc.perform(multipart(BASE).file(new MockMultipartFile("payload","","application/json","{\"subject\":\"Phiếu\",\"category\":\"OTHER\",\"message\":\"Nội dung\"}".getBytes(java.nio.charset.StandardCharsets.UTF_8))).file(new MockMultipartFile("files","bad.png","image/png",new byte[]{1,2,3})).with(user(OWNER)).with(csrf())).andExpect(status().is4xxClientError());
   assertEquals(before,jdbc.queryForObject("SELECT count(*) FROM support_tickets",Long.class));
 }
 @Test void fileRepliesReopenAndOnlyOwnerCloses()throws Exception{
   var t=create(OWNER);String tid=t.path("ticketId").asText();command(tid,"claim",CS,0,"");
   var bytes=new java.io.ByteArrayOutputStream();javax.imageio.ImageIO.write(new java.awt.image.BufferedImage(16,16,java.awt.image.BufferedImage.TYPE_INT_RGB),"png",bytes);
   var payload=new MockMultipartFile("payload","","application/json","{\"expectedVersion\":1,\"message\":\"Ảnh hướng dẫn\"}".getBytes(java.nio.charset.StandardCharsets.UTF_8));
   mvc.perform(multipart(BASE+"/"+tid+"/messages").file(payload).file(new MockMultipartFile("files","hướng dẫn.png","image/png",bytes.toByteArray())).with(user(CS)).with(csrf())).andExpect(status().isOk());
   var thread=data(mvc.perform(get(BASE+"/"+tid+"/messages").with(user(OWNER))).andExpect(status().isOk()).andReturn());
   assertEquals(2,thread.path("items").size());
   assertTrue(thread.path("items").get(0).path("fromRequester").asBoolean());assertEquals("Học viên",thread.path("items").get(0).path("senderRoleLabel").asText());
   assertFalse(thread.path("items").get(1).path("fromRequester").asBoolean());assertEquals("Chăm sóc khách hàng",thread.path("items").get(1).path("senderRoleLabel").asText());
   var attachment=thread.path("items").get(1).path("attachments").get(0);assertFalse(attachment.has("content"));
   String file=BASE+"/"+tid+"/attachments/"+attachment.path("attachmentId").asText();
   mvc.perform(get(file).with(user(OWNER))).andExpect(status().isOk()).andExpect(header().string("X-Content-Type-Options","nosniff")).andExpect(header().string("Cache-Control","no-store")).andExpect(content().bytes(bytes.toByteArray()));
   mvc.perform(get(file).with(user(OTHER))).andExpect(status().isNotFound());
   command(tid,"resolve",CS,2,",\"resolutionText\":\"Đã hướng dẫn\"");
   mvc.perform(post(BASE+"/"+tid+"/close").with(user(CS)).with(csrf()).contentType("application/json").content("{\"expectedVersion\":3}")).andExpect(status().isForbidden());
   var reopened=data(mvc.perform(multipart(BASE+"/"+tid+"/messages").file(new MockMultipartFile("payload","","application/json","{\"expectedVersion\":3,\"message\":\"Cần hướng dẫn thêm\"}".getBytes(java.nio.charset.StandardCharsets.UTF_8))).file(new MockMultipartFile("files","owner-followup.png","image/png",bytes.toByteArray())).with(user(OWNER)).with(csrf())).andExpect(status().isOk()).andReturn());
   assertEquals("IN_PROGRESS",reopened.path("status").asText());command(tid,"resolve",CS,4,",\"resolutionText\":\"Đã hướng dẫn bổ sung\"");command(tid,"close",OWNER,5,"");
   mvc.perform(multipart(BASE+"/"+tid+"/messages").file(new MockMultipartFile("payload","","application/json","{\"expectedVersion\":6,\"message\":\"Không thể thêm\"}".getBytes(java.nio.charset.StandardCharsets.UTF_8))).with(user(OWNER)).with(csrf())).andExpect(status().isConflict());
   jdbc.update("UPDATE ticket_messages SET sent_at='2026-01-01T00:00:00Z' WHERE ticket_id=CAST(? AS uuid)",tid);
   thread=data(mvc.perform(get(BASE+"/"+tid+"/messages").with(user(OWNER))).andReturn());
   for(int i=0;i<thread.path("items").size();i++)assertEquals(i+1,thread.path("items").get(i).path("sequenceNo").asInt());
 }
 @Test void validatesPaginationUnicodeAndStrictVersion()throws Exception{
   var t=create(OTHER);String tid=t.path("ticketId").asText();
   for(String query:List.of("?size=0","?size=51","?page=-1","?status=UNKNOWN","?category=UNKNOWN","?priority=UNKNOWN"))mvc.perform(get(BASE+query).with(user(OTHER))).andExpect(status().isBadRequest());
   mvc.perform(post(BASE+"/"+tid+"/claim").with(user(CS)).with(csrf().useInvalidToken()).contentType("application/json").content("{\"expectedVersion\":0}")).andExpect(status().isForbidden());
   for(String version:List.of("null","-1","1.5","\"0\""))mvc.perform(post(BASE+"/"+tid+"/claim").with(user(CS)).with(csrf()).contentType("application/json").content("{\"expectedVersion\":"+version+"}")).andExpect(status().isBadRequest());
   var form=json.createObjectNode().put("subject","😀".repeat(200)).put("category","OTHER").put("message","Nội dung Unicode");
   mvc.perform(multipart(BASE).file(new MockMultipartFile("payload","","application/json",json.writeValueAsBytes(form))).with(user(OTHER)).with(csrf())).andExpect(status().isCreated());
   form.put("subject","😀".repeat(201));mvc.perform(multipart(BASE).file(new MockMultipartFile("payload","","application/json",json.writeValueAsBytes(form))).with(user(OTHER)).with(csrf())).andExpect(status().isBadRequest());
 }
 @Test void ownedContextsAreReadOnlyAndUnrelatedPaymentsAreRejected()throws Exception{
   UUID learner=UUID.randomUUID(),tutor=UUID.randomUUID(),booking=UUID.randomUUID(),plan=UUID.randomUUID(),membership=UUID.randomUUID(),payment=UUID.randomUUID(),foreignPayment=UUID.randomUUID();
   jdbc.update("INSERT INTO learners(learner_id,user_id) VALUES(?,CAST(? AS uuid)) ON CONFLICT(user_id) DO NOTHING",learner,id(OWNER));learner=jdbc.queryForObject("SELECT learner_id FROM learners WHERE user_id=CAST(? AS uuid)",UUID.class,id(OWNER));
   jdbc.update("INSERT INTO tutors(tutor_id,user_id) VALUES(?,CAST(? AS uuid)) ON CONFLICT(user_id) DO NOTHING",tutor,id(OTHER));tutor=jdbc.queryForObject("SELECT tutor_id FROM tutors WHERE user_id=CAST(? AS uuid)",UUID.class,id(OTHER));
   jdbc.update("INSERT INTO membership_plans(plan_id,plan_name,duration_months,price) VALUES(?,'Gói kiểm thử riêng',1,100000)",plan);
   jdbc.update("INSERT INTO trial_bookings(booking_id,learner_id,tutor_id,scheduled_start_time,scheduled_end_time) VALUES(?,?,?,now()+interval '2 days',now()+interval '2 days 45 minutes')",booking,learner,tutor);
   jdbc.update("INSERT INTO tutor_memberships(membership_id,tutor_id,plan_id,start_date,end_date,transaction_ref) VALUES(?,?,?,now(),now()+interval '1 month','fe10-context-test')",membership,tutor,plan);
   jdbc.update("INSERT INTO payment_transactions(transaction_id,user_id,plan_id,amount,gateway_transaction_id) VALUES(?,CAST(? AS uuid),?,100000,'fe10-context-test'),(?,CAST(? AS uuid),?,100000,'other-owner')",payment,id(OTHER),plan,foreignPayment,id(CS),plan);
   var context=json.createObjectNode().put("subject","Cần đối chiếu hội viên").put("category","MEMBERSHIP").put("message","Nhờ kiểm tra thông tin").put("relatedMembershipId",membership.toString()).put("relatedTransactionId",payment.toString());
   var created=data(mvc.perform(multipart(BASE).file(new MockMultipartFile("payload","","application/json",json.writeValueAsBytes(context))).with(user(OTHER)).with(csrf())).andExpect(status().isCreated()).andReturn());String tid=created.path("ticketId").asText();assertEquals(2,created.path("relatedContext").size());
   assertTrue(created.path("relatedContext").toString().contains("Gói hội viên Gói kiểm thử riêng · Đang hiệu lực"));assertTrue(created.path("relatedContext").toString().contains("· Đang chờ thanh toán"));assertFalse(created.path("relatedContext").toString().contains("fe10-context-test"));
   var candidates=data(mvc.perform(get(BASE+"/related-contexts?type=payment").with(user(OTHER))).andExpect(status().isOk()).andReturn());assertTrue(candidates.toString().contains(payment.toString()));assertFalse(candidates.toString().contains(foreignPayment.toString()));
   context.put("relatedTransactionId",foreignPayment.toString());mvc.perform(multipart(BASE).file(new MockMultipartFile("payload","","application/json",json.writeValueAsBytes(context))).with(user(OTHER)).with(csrf())).andExpect(status().isBadRequest());
   command(tid,"claim",CS,0,"");command(tid,"resolve",CS,1,",\"resolutionText\":\"Đã hướng dẫn đối chiếu thông tin\"");command(tid,"close",OTHER,2,"");
   assertEquals("PENDING",jdbc.queryForObject("SELECT status::text FROM payment_transactions WHERE transaction_id=?",String.class,payment));assertEquals("ACTIVE",jdbc.queryForObject("SELECT status::text FROM tutor_memberships WHERE membership_id=?",String.class,membership));assertEquals("PENDING",jdbc.queryForObject("SELECT booking_status::text FROM trial_bookings WHERE booking_id=?",String.class,booking));
   jdbc.update("DELETE FROM payment_transactions WHERE transaction_id=?",payment);
   var missing=data(mvc.perform(get(BASE+"/"+tid+"/related-context").with(user(OTHER))).andExpect(status().isOk()).andReturn());assertTrue(java.util.stream.StreamSupport.stream(missing.spliterator(),false).anyMatch(item->item.path("type").asText().equals("payment")&&!item.path("available").asBoolean()));
   var bookingContext=json.createObjectNode().put("subject","Hỗ trợ đặt lịch").put("category","BOOKING").put("message","Nhờ tư vấn").put("relatedBookingId",booking.toString());
   var bookingTicket=data(mvc.perform(multipart(BASE).file(new MockMultipartFile("payload","","application/json",json.writeValueAsBytes(bookingContext))).with(user(OWNER)).with(csrf())).andExpect(status().isCreated()).andReturn());
   assertTrue(bookingTicket.path("relatedContext").get(0).path("label").asText().matches("Lịch học thử \\d{2}/\\d{2}/\\d{4} \\d{2}:\\d{2} · Chờ xác nhận"));
 }
 @Test void everyTicketRouteRequiresAuthenticationAndCsrf()throws Exception{
   String uuid=UUID.randomUUID().toString();
   for(String path:List.of("","/me","/csrf","/assignees","/managers","/related-contexts?type=booking","/"+uuid,"/"+uuid+"/messages","/"+uuid+"/history","/"+uuid+"/related-context","/"+uuid+"/attachments/"+uuid))mvc.perform(get(BASE+path)).andExpect(status().isUnauthorized());
   for(String path:List.of("/claim","/assignment","/priority","/resolve","/close","/messages","/escalations","/escalations/current/recipient","/escalations/current/decision","/escalations/current/return"))mvc.perform(post(BASE+"/"+uuid+path).with(user(CS)).contentType("application/json").content("{\"expectedVersion\":0}")).andExpect(status().isForbidden());
 }
 @Test void returnUsesCurrentAssigneeAndManagerAccessPersistsUntilReplaced()throws Exception{
   var t=create(OWNER);String tid=t.path("ticketId").asText();command(tid,"claim",CS,0,"");
   command(tid,"escalations",CS,1,",\"managerId\":\""+id(MANAGER)+"\",\"reason\":\"Cần tư vấn\",\"handlingSummary\":\"Đã tiếp nhận\"");
   command(tid,"assignment",CS2,2,",\"assigneeId\":\""+id(CS2)+"\"");
   command(tid,"escalations/current/decision",MANAGER,3,",\"decisionText\":\"Gửi hướng dẫn thêm\"");
   var returned=command(tid,"escalations/current/return",MANAGER,4,"");assertEquals(id(CS2),returned.path("assigneeId").asText());
   mvc.perform(get(BASE+"/"+tid).with(user(MANAGER))).andExpect(status().isOk());
   command(tid,"escalations",CS2,5,",\"managerId\":\""+id(MANAGER2)+"\",\"reason\":\"Cần tư vấn tiếp\",\"handlingSummary\":\"Đã có hướng dẫn trước\"");
   mvc.perform(get(BASE+"/"+tid).with(user(MANAGER))).andExpect(status().isNotFound());
   jdbc.update("UPDATE users SET role_id=(SELECT role_id FROM roles WHERE role_name='ROLE_LEARNER') WHERE email=?",CS2);
   mvc.perform(get(BASE+"/"+tid).with(user(CS2))).andExpect(status().isNotFound());
 }
}
