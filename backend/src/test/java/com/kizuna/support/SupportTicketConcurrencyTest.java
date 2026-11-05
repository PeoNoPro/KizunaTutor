package com.kizuna.support;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.jdbc.core.JdbcTemplate;
import javax.sql.DataSource;
import com.kizuna.features.ticket.service.SupportTicketService;
import com.kizuna.features.ticket.dto.SupportTicketRequests.*;
import com.kizuna.core.exception.BusinessException;
import java.util.*;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;
@SpringJUnitConfig(SupportTicketIntegrationConfig.class) @WebAppConfiguration
class SupportTicketConcurrencyTest {
 @Autowired SupportTicketService service;@Autowired DataSource ds;
 void as(String email){SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(email,null,List.of()));}
 @org.junit.jupiter.api.BeforeEach void prepareActors()throws Exception{
   var jdbc=new JdbcTemplate(ds);jdbc.execute(java.nio.file.Files.readString(java.nio.file.Path.of("../database/seeds/fe10_support_ticket.sql")));
   for(String role:List.of("ROLE_LEARNER","ROLE_CS_STAFF","ROLE_MANAGER"))jdbc.update("INSERT INTO roles(role_name,is_active) VALUES(?,true) ON CONFLICT(role_name) DO NOTHING",role);
   for(var pair:List.of(new String[]{"race-owner@test.invalid","ROLE_LEARNER"},new String[]{"race-cs@test.invalid","ROLE_CS_STAFF"},new String[]{"race-cs2@test.invalid","ROLE_CS_STAFF"},new String[]{"race-manager@test.invalid","ROLE_MANAGER"},new String[]{"race-manager2@test.invalid","ROLE_MANAGER"}))jdbc.update("INSERT INTO users(user_id,role_id,email,password_hash,full_name,account_status) SELECT ?,role_id,?,'test-only','Kiểm thử đồng thời','ACTIVE' FROM roles WHERE role_name=? ON CONFLICT(email) DO UPDATE SET account_status='ACTIVE',deleted_at=null,role_id=excluded.role_id",UUID.randomUUID(),pair[0],pair[1]);
 }
 @org.junit.jupiter.api.AfterEach void clear(){SecurityContextHolder.clearContext();}
 UUID create(){as("race-owner@test.invalid");return (UUID)service.create(new Create("Kiểm tra xung đột","OTHER","Nội dung ban đầu",null,null,null),List.of()).get("ticketId");}
 UUID userId(String email){return new JdbcTemplate(ds).queryForObject("SELECT user_id FROM users WHERE email=?",UUID.class,email);}
 Command version(long v){return new Command(v,null,null,null,null,null,null,null);}
 List<Integer> race(String firstActor,Runnable first,String secondActor,Runnable second)throws Exception{
   var ready=new CountDownLatch(2);var start=new CountDownLatch(1);var pool=Executors.newFixedThreadPool(2);
   java.util.function.BiFunction<String,Runnable,Callable<Integer>> task=(actor,action)->()->{as(actor);ready.countDown();start.await();try{action.run();return 200;}catch(BusinessException e){return e.status();}finally{SecurityContextHolder.clearContext();}};
   try{var one=pool.submit(task.apply(firstActor,first));var two=pool.submit(task.apply(secondActor,second));assertTrue(ready.await(5,TimeUnit.SECONDS));start.countDown();var results=List.of(one.get(15,TimeUnit.SECONDS),two.get(15,TimeUnit.SECONDS));assertEquals(1,results.stream().filter(s->s==200).count());return results;}finally{pool.shutdownNow();}
 }
 @Test void onlyOneConcurrentClaimAndAuditFailureRollsBack()throws Exception{
   var jdbc=new JdbcTemplate(ds);
   for(String role:List.of("ROLE_LEARNER","ROLE_CS_STAFF"))jdbc.update("INSERT INTO roles(role_name,is_active) VALUES(?,true) ON CONFLICT(role_name) DO NOTHING",role);
   for(var pair:List.of(new String[]{"race-owner@test.invalid","ROLE_LEARNER"},new String[]{"race-cs@test.invalid","ROLE_CS_STAFF"}))jdbc.update("INSERT INTO users(user_id,role_id,email,password_hash,full_name,account_status) SELECT ?,role_id,?,'test-only','Kiểm thử đồng thời','ACTIVE' FROM roles WHERE role_name=? ON CONFLICT(email) DO NOTHING",UUID.randomUUID(),pair[0],pair[1]);
   as("race-owner@test.invalid");UUID id=(UUID)service.create(new Create("Yêu cầu đồng thời","OTHER","Nội dung kiểm thử",null,null,null),List.of()).get("ticketId");
   var ready=new CountDownLatch(2);var start=new CountDownLatch(1);var pool=Executors.newFixedThreadPool(2);
   Callable<Integer> claim=()->{as("race-cs@test.invalid");ready.countDown();start.await();try{service.command(id,"claim",new Command(0L,null,null,null,null,null,null,null));return 200;}catch(BusinessException e){return e.status();}finally{SecurityContextHolder.clearContext();}};
   try{var one=pool.submit(claim);var two=pool.submit(claim);assertTrue(ready.await(5,TimeUnit.SECONDS));start.countDown();var results=new ArrayList<>(List.of(one.get(10,TimeUnit.SECONDS),two.get(10,TimeUnit.SECONDS)));Collections.sort(results);assertEquals(List.of(200,409),results);}finally{pool.shutdownNow();SecurityContextHolder.clearContext();}
   assertEquals(1L,jdbc.queryForObject("SELECT version FROM support_tickets WHERE ticket_id=?",Long.class,id));
   assertEquals(2,jdbc.queryForObject("SELECT count(*) FROM ticket_audit_events WHERE ticket_id=?",Integer.class,id));
   // A test-only trigger forces the last write to fail; no production hooks or fake success.
   jdbc.execute("CREATE OR REPLACE FUNCTION fe10_test_reject_audit() RETURNS trigger LANGUAGE plpgsql AS $$ BEGIN IF NEW.event_type='MESSAGE_SENT' THEN RAISE EXCEPTION 'test rollback'; END IF; RETURN NEW; END $$");
   jdbc.execute("CREATE TRIGGER fe10_test_reject BEFORE INSERT ON ticket_audit_events FOR EACH ROW EXECUTE FUNCTION fe10_test_reject_audit()");
   var image=new java.io.ByteArrayOutputStream();javax.imageio.ImageIO.write(new java.awt.image.BufferedImage(8,8,java.awt.image.BufferedImage.TYPE_INT_RGB),"png",image);
   var attachment=new org.springframework.mock.web.MockMultipartFile("files","rollback.png","image/png",image.toByteArray());
   try{as("race-owner@test.invalid");assertThrows(RuntimeException.class,()->service.message(id,new Message(1L,"Không được lưu một phần"),List.of(attachment)));}
   finally{jdbc.execute("DROP TRIGGER fe10_test_reject ON ticket_audit_events");jdbc.execute("DROP FUNCTION fe10_test_reject_audit()");SecurityContextHolder.clearContext();}
   assertEquals(1L,jdbc.queryForObject("SELECT version FROM support_tickets WHERE ticket_id=?",Long.class,id));
   assertEquals(1,jdbc.queryForObject("SELECT count(*) FROM ticket_messages WHERE ticket_id=?",Integer.class,id));
   assertEquals(0,jdbc.queryForObject("SELECT count(*) FROM ticket_attachments WHERE ticket_id=?",Integer.class,id));
 }
 @Test void competingMessagesGetUniqueSequenceAndOneStaleConflict()throws Exception{
   UUID id=create();var results=race("race-owner@test.invalid",()->service.message(id,new Message(0L,"Trao đổi thứ nhất"),List.of()),"race-owner@test.invalid",()->service.message(id,new Message(0L,"Trao đổi thứ hai"),List.of()));
   assertTrue(results.contains(409));var jdbc=new JdbcTemplate(ds);assertEquals(List.of(1L,2L),jdbc.queryForList("SELECT sequence_no FROM ticket_messages WHERE ticket_id=? ORDER BY sequence_no",Long.class,id));
 }
 @Test void closeAndOwnerReplyCannotBothCommit()throws Exception{
   UUID id=create();as("race-cs@test.invalid");service.command(id,"claim",version(0));service.command(id,"resolve",new Command(1L,null,null,null,null,null,null,"Đã giải quyết"));
   var results=race("race-owner@test.invalid",()->service.command(id,"close",version(2)),"race-owner@test.invalid",()->service.message(id,new Message(2L,"Cần hỗ trợ thêm"),List.of()));assertTrue(results.contains(409));
   assertEquals(3L,new JdbcTemplate(ds).queryForObject("SELECT version FROM support_tickets WHERE ticket_id=?",Long.class,id));
 }
 @Test void reassignmentAndOldAssigneeReplyAreSerialized()throws Exception{
   UUID id=create();as("race-cs@test.invalid");service.command(id,"claim",version(0));UUID recipient=userId("race-cs2@test.invalid");
   var results=race("race-cs@test.invalid",()->service.message(id,new Message(1L,"Hướng dẫn của người phụ trách"),List.of()),"race-cs2@test.invalid",()->service.command(id,"assignment",new Command(1L,recipient,null,null,null,null,null,null)));
   assertTrue(results.stream().anyMatch(s->s==409||s==403));assertEquals(2L,new JdbcTemplate(ds).queryForObject("SELECT version FROM support_tickets WHERE ticket_id=?",Long.class,id));
 }
 @Test void managerDecisionAndRecipientChangeCannotReuseOldDecision()throws Exception{
   UUID id=create();as("race-cs@test.invalid");service.command(id,"claim",version(0));UUID manager=userId("race-manager@test.invalid"),other=userId("race-manager2@test.invalid");
   service.command(id,"escalate",new Command(1L,null,manager,null,"Cần tư vấn","Đã tiếp nhận",null,null));
   var results=race("race-manager@test.invalid",()->service.command(id,"decision",new Command(2L,null,null,null,null,null,"Hướng dẫn nội bộ",null)),"race-cs@test.invalid",()->service.command(id,"recipient",new Command(2L,null,other,null,"Đổi người tiếp nhận",null,null,null)));
   assertTrue(results.stream().anyMatch(s->s==409||s==404));var jdbc=new JdbcTemplate(ds);assertEquals(3L,jdbc.queryForObject("SELECT version FROM support_tickets WHERE ticket_id=?",Long.class,id));
   UUID current=jdbc.queryForObject("SELECT escalated_to FROM support_tickets WHERE ticket_id=?",UUID.class,id);
   if(current.equals(other))assertNull(jdbc.queryForObject("SELECT decision_text FROM ticket_escalations WHERE ticket_id=? AND pending",String.class,id));
   assertEquals(1,jdbc.queryForObject("SELECT count(*) FROM ticket_escalations WHERE ticket_id=? AND pending",Integer.class,id));
 }
 @Test void recipientDeactivationDuringEscalationCannotCommitAssignment()throws Exception{
   UUID id=create();as("race-cs@test.invalid");service.command(id,"claim",version(0));UUID manager=userId("race-manager@test.invalid");var pool=Executors.newSingleThreadExecutor();
   try(var connection=ds.getConnection()){
     connection.setAutoCommit(false);try(var statement=connection.prepareStatement("UPDATE users SET account_status='SUSPENDED' WHERE user_id=?")){statement.setObject(1,manager);statement.executeUpdate();}
     var started=new CountDownLatch(1);var pending=pool.submit(()->{as("race-cs@test.invalid");started.countDown();try{service.command(id,"escalate",new Command(1L,null,manager,null,"Cần xem xét","Đã kiểm tra",null,null));return 200;}catch(BusinessException e){return e.status();}finally{SecurityContextHolder.clearContext();}});
     assertTrue(started.await(5,TimeUnit.SECONDS));connection.commit();assertEquals(400,pending.get(10,TimeUnit.SECONDS));
   }finally{pool.shutdownNow();}
   var jdbc=new JdbcTemplate(ds);assertEquals(1L,jdbc.queryForObject("SELECT version FROM support_tickets WHERE ticket_id=?",Long.class,id));assertEquals(0,jdbc.queryForObject("SELECT count(*) FROM ticket_escalations WHERE ticket_id=?",Integer.class,id));
 }
}
