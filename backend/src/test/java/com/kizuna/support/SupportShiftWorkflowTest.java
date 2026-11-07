package com.kizuna.support;

import com.kizuna.core.exception.BusinessException;
import com.kizuna.features.ticket.dto.SupportTicketRequests.*;
import com.kizuna.features.ticket.service.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import javax.sql.DataSource;
import java.nio.file.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;

/** Actual PostgreSQL transactions: no production DB, mail or fake dispatch success. */
@SpringJUnitConfig(SupportTicketIntegrationConfig.class) @WebAppConfiguration
class SupportShiftWorkflowTest {
 @Autowired SupportTicketService service;
 @Autowired SupportShiftService shifts;
 @Autowired PlatformTransactionManager transactions;
 @Autowired DataSource ds;
 JdbcTemplate jdbc; Clock originalClock;
 final LocalDate day=LocalDate.of(2026,11,9);
 final UUID owner=UUID.randomUUID(),cs=UUID.randomUUID(),other=UUID.randomUUID(),manager=UUID.randomUUID();
 String email(UUID id){return "shift-"+id+"@test.invalid";}
 void as(UUID id){SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(email(id),null,List.of()));}
 void time(String instant){
  ReflectionTestUtils.setField(shifts,"clock",Clock.fixed(Instant.parse(instant),SupportShiftService.ZONE));
  // A new request at the new time must not reuse the previous request's UI capability cache.
  org.springframework.web.context.request.RequestContextHolder.resetRequestAttributes();
 }
 @BeforeEach void setup()throws Exception {
  jdbc=new JdbcTemplate(ds);
  jdbc.execute(Files.readString(Path.of("../database/seeds/fe10_support_ticket.sql")));
  jdbc.update("DELETE FROM support_shifts");
  // The test datasource strictly permits only *_ticket_test. Isolate the dispatch queue from other test classes.
  for(String table:List.of("ticket_attachments","ticket_audit_events","ticket_escalations","ticket_messages","support_tickets"))jdbc.update("DELETE FROM "+table);
  originalClock=(Clock)ReflectionTestUtils.getField(shifts,"clock");time("2026-11-09T03:00:00Z");
  for(var item:Map.of(owner,"ROLE_LEARNER",cs,"ROLE_CS_STAFF",other,"ROLE_CS_STAFF",manager,"ROLE_MANAGER").entrySet()){
   jdbc.update("INSERT INTO roles(role_name,is_active) VALUES(?,true) ON CONFLICT(role_name) DO UPDATE SET is_active=true",item.getValue());
   jdbc.update("INSERT INTO users(user_id,role_id,email,password_hash,full_name,account_status) SELECT ?,role_id,?,'test-only','Kiểm thử phân ca','ACTIVE' FROM roles WHERE role_name=?",item.getKey(),email(item.getKey()),item.getValue());
  }
 }
 @AfterEach void clean(){
  jdbc.update("DELETE FROM support_shifts");
  jdbc.update("UPDATE users SET account_status='ACTIVE' WHERE user_id IN (?,?,?,?)",owner,cs,other,manager);
  ReflectionTestUtils.setField(shifts,"clock",originalClock);SecurityContextHolder.clearContext();
 }
 UUID create(){as(owner);return (UUID)service.create(new Create("Yêu cầu phân ca","OTHER","Nội dung hỗ trợ",null,null,null),List.of()).get("ticketId");}
 void schedule(UUID staff){jdbc.update("INSERT INTO support_shifts(shift_id,staff_id,shift_date,shift_code,created_by) VALUES(?,?,?,'MORNING',?)",UUID.randomUUID(),staff,day,manager);}
 Command command(long v){return new Command(v,null,null,null,null,null,null,null);}
 Map<String,Object> ticket(UUID id){return jdbc.queryForMap("SELECT * FROM support_tickets WHERE ticket_id=?",id);}
 int active(UUID staff){return jdbc.queryForObject("SELECT count(*) FROM support_tickets WHERE assigned_to=? AND status='IN_PROGRESS'",Integer.class,staff);}
 int claim(UUID id)throws Exception {as(cs);try{service.command(id,"claim",command(0));return 200;}catch(BusinessException ex){return ex.status();}finally{SecurityContextHolder.clearContext();}}

 @Test void simultaneousManualClaimsForDifferentTicketsRespectCapacity()throws Exception {
  UUID first=create(),second=create();schedule(cs);
  var start=new CountDownLatch(1);var ready=new CountDownLatch(2);var pool=Executors.newFixedThreadPool(2);
  try {
   var a=pool.submit(()->{ready.countDown();start.await();return claim(first);});
   var b=pool.submit(()->{ready.countDown();start.await();return claim(second);});
   assertTrue(ready.await(5,TimeUnit.SECONDS));start.countDown();
   var result=new ArrayList<>(List.of(a.get(15,TimeUnit.SECONDS),b.get(15,TimeUnit.SECONDS)));Collections.sort(result);
   assertEquals(List.of(200,409),result);assertEquals(1,active(cs));
  }finally{pool.shutdownNow();}
 }

 @Test void manualClaimAndDispatcherShareTheSameAllocationLock()throws Exception {
  UUID first=create();create();schedule(cs);
  var start=new CountDownLatch(1);var pool=Executors.newFixedThreadPool(2);
  try {
   var manual=pool.submit(()->{start.await();return claim(first);});
   var automatic=pool.submit(()->{start.await();return shifts.dispatch();});
   start.countDown();assertTrue(Set.of(200,409).contains(manual.get(15,TimeUnit.SECONDS)));automatic.get(15,TimeUnit.SECONDS);
   assertEquals(1,active(cs));
  }finally{pool.shutdownNow();}
 }

 @Test void afterCommitDispatchPersistsAssignmentAndAuditInNewTransaction(){
  schedule(cs);UUID id=create();var t=ticket(id);
  assertEquals(cs,t.get("assigned_to"));assertEquals("IN_PROGRESS",t.get("status"));
  assertEquals(1L,((Number)t.get("version")).longValue());
  assertEquals(1,jdbc.queryForObject("SELECT count(*) FROM ticket_audit_events WHERE ticket_id=? AND event_type='AUTO_ASSIGN'",Integer.class,id));
 }

 @Test void outerRollbackDoesNotDispatchOrLeaveATicket(){
  schedule(cs);var id=new java.util.concurrent.atomic.AtomicReference<UUID>();
  new TransactionTemplate(transactions).executeWithoutResult(tx->{id.set(create());tx.setRollbackOnly();});
  assertEquals(0,jdbc.queryForObject("SELECT count(*) FROM support_tickets WHERE ticket_id=?",Integer.class,id.get()));assertEquals(0,active(cs));
 }

 @Test void auditFailureRollsBackAutomaticAssignment(){
  UUID id=create();schedule(cs);
  jdbc.execute("CREATE OR REPLACE FUNCTION shift_test_reject_audit() RETURNS trigger LANGUAGE plpgsql AS $$ BEGIN IF NEW.event_type='AUTO_ASSIGN' THEN RAISE EXCEPTION 'test rollback'; END IF; RETURN NEW; END $$");
  jdbc.execute("CREATE TRIGGER shift_test_reject BEFORE INSERT ON ticket_audit_events FOR EACH ROW EXECUTE FUNCTION shift_test_reject_audit()");
  try {assertThrows(RuntimeException.class,()->shifts.dispatch());}
  finally {jdbc.execute("DROP TRIGGER shift_test_reject ON ticket_audit_events");jdbc.execute("DROP FUNCTION shift_test_reject_audit()");}
  assertNull(ticket(id).get("assigned_to"));assertEquals("OPEN",ticket(id).get("status"));
  assertEquals(0L,((Number)ticket(id).get("version")).longValue());
  assertEquals(1,jdbc.queryForObject("SELECT count(*) FROM ticket_audit_events WHERE ticket_id=?",Integer.class,id));
 }

 @Test void changingShiftEndsNewWorkButKeepsExistingWork(){
  UUID current=create(),waiting=create();schedule(cs);as(cs);service.command(current,"claim",command(0));
  time("2026-11-09T05:00:00Z"); // 12:00 Vietnam: end is exclusive.
  as(cs);assertFalse(shifts.canClaim(cs));
  assertEquals(409,assertThrows(BusinessException.class,()->service.command(waiting,"claim",command(0))).status());
  service.command(current,"priority",new Command(1L,null,null,"HIGH",null,null,null,null));
  assertEquals(cs,ticket(current).get("assigned_to"));assertNull(ticket(waiting).get("assigned_to"));
 }

 @Test void managerReturnsToOriginalCsEvenWhenCsHasNewWork(){
  UUID first=create(),second=create(),third=create();schedule(cs);as(cs);service.command(first,"claim",command(0));
  service.command(first,"escalate",new Command(1L,null,manager,null,"Cần quản lý","Đã kiểm tra",null,null));
  assertEquals(cs,ticket(second).get("assigned_to"));
  as(manager);service.command(first,"decision",new Command(2L,null,null,null,null,null,"Hướng xử lý nội bộ",null));
  service.command(first,"return",command(3));
  assertEquals(cs,ticket(first).get("assigned_to"));assertEquals(2,active(cs));assertNull(ticket(third).get("assigned_to"));
  as(cs);assertEquals(409,assertThrows(BusinessException.class,()->service.command(third,"claim",command(0))).status());
 }

 @Test void unavailableCsIsHandedOverWithoutLosingMessagesOrEscalation(){
  UUID id=create();schedule(cs);as(cs);service.command(id,"claim",command(0));
  service.command(id,"escalate",new Command(1L,null,manager,null,"Cần quản lý","Đã kiểm tra",null,null));
  schedule(other);jdbc.update("UPDATE users SET account_status='SUSPENDED' WHERE user_id=?",cs);
  assertEquals(1,shifts.dispatch());var t=ticket(id);
  assertEquals(other,t.get("assigned_to"));assertEquals(manager,t.get("escalated_to"));assertEquals("ESCALATED",t.get("status"));
  assertEquals(1,jdbc.queryForObject("SELECT count(*) FROM ticket_messages WHERE ticket_id=?",Integer.class,id));
  assertEquals(1,jdbc.queryForObject("SELECT count(*) FROM ticket_escalations WHERE ticket_id=? AND pending",Integer.class,id));
  assertEquals(1,jdbc.queryForObject("SELECT count(*) FROM ticket_audit_events WHERE ticket_id=? AND event_type='REASSIGN_CS' AND before_values->>'assigned_to'=? AND after_values->>'assigned_to'=?",Integer.class,id,cs.toString(),other.toString()));
 }

 @Test void handoverToBusyCsIsRejected(){
  UUID first=create(),second=create();schedule(cs);schedule(other);
  as(cs);service.command(first,"claim",command(0));as(other);service.command(second,"claim",command(0));
  assertEquals(409,assertThrows(BusinessException.class,()->service.command(first,"assignment",new Command(1L,other,null,null,"Bàn giao",null,null,null))).status());
  assertEquals(cs,ticket(first).get("assigned_to"));
 }

 @Test void noFreeReplacementKeepsExistingOwnerAndHistory(){
  UUID id=create();schedule(cs);as(cs);service.command(id,"claim",command(0));
  jdbc.update("UPDATE users SET account_status='SUSPENDED' WHERE user_id=?",cs);
  assertEquals(0,shifts.dispatch());assertEquals(cs,ticket(id).get("assigned_to"));
  assertEquals(1L,((Number)ticket(id).get("version")).longValue());
 }

 @Test void removingCurrentShiftDoesNotDropOwnedTicket(){
  UUID id=create();schedule(cs);as(cs);service.command(id,"claim",command(0));
  UUID shift=jdbc.queryForObject("SELECT shift_id FROM support_shifts WHERE staff_id=?",UUID.class,cs);
  as(manager);shifts.unassign(shift,day);
  assertEquals(cs,ticket(id).get("assigned_to"));assertEquals("IN_PROGRESS",ticket(id).get("status"));
 }

 @Test void onlyManagerSchedulesAndPastShiftsCannotBeRemoved(){
  as(cs);assertEquals(403,assertThrows(BusinessException.class,()->shifts.assign(cs,day,"MORNING")).status());
  as(manager);shifts.assign(cs,day,"MORNING");
  UUID id=jdbc.queryForObject("SELECT shift_id FROM support_shifts WHERE staff_id=?",UUID.class,cs);
  time("2026-11-10T03:00:00Z");as(manager);
  assertEquals(400,assertThrows(BusinessException.class,()->shifts.unassign(id,day)).status());
 }
 @Test void onlyManagerCanAssignOrRemoveShifts(){
  // CS and learners get 403 from the service even when they call the API directly; nothing is written.
  for(UUID caller:List.of(cs,owner)){
   as(caller);
   assertEquals(403,assertThrows(BusinessException.class,()->shifts.assign(cs,day,"MORNING")).status());
  }
  assertEquals(0,jdbc.queryForObject("SELECT count(*) FROM support_shifts",Integer.class));
  as(manager);shifts.assign(cs,day,"MORNING");
  UUID id=jdbc.queryForObject("SELECT shift_id FROM support_shifts WHERE staff_id=?",UUID.class,cs);
  for(UUID caller:List.of(cs,owner)){
   as(caller);
   assertEquals(403,assertThrows(BusinessException.class,()->shifts.unassign(id,day)).status());
  }
  assertEquals(1,jdbc.queryForObject("SELECT count(*) FROM support_shifts",Integer.class));
  // Learners cannot even read the schedule; CS can read but not edit.
  as(owner);assertEquals(403,assertThrows(BusinessException.class,()->shifts.week(day)).status());
  as(cs);assertEquals(false,shifts.week(day).get("canEdit"));
 }
 @Test void assignAndRemoveLeaveAnAppendOnlyHistory(){
  as(manager);shifts.assign(cs,day,"AFTERNOON");
  UUID id=jdbc.queryForObject("SELECT shift_id FROM support_shifts WHERE staff_id=? AND shift_code='AFTERNOON'",UUID.class,cs);
  shifts.unassign(id,day);
  assertEquals(0,jdbc.queryForObject("SELECT count(*) FROM support_shifts WHERE shift_id=?",Integer.class,id));
  var events=jdbc.queryForList("SELECT action,staff_id,actor_id,shift_code,shift_date FROM support_shift_events WHERE shift_id=? ORDER BY occurred_at,action",id);
  assertEquals(List.of("ASSIGN","REMOVE"),events.stream().map(e->e.get("action")).toList());
  events.forEach(e->{assertEquals(cs,e.get("staff_id"));assertEquals(manager,e.get("actor_id"));assertEquals("AFTERNOON",e.get("shift_code"));});
  // Lịch sử chỉ được thêm: sửa hoặc xóa đều bị chặn ở DB.
  assertThrows(Exception.class,()->jdbc.update("UPDATE support_shift_events SET actor_id=? WHERE shift_id=?",cs,id));
  assertThrows(Exception.class,()->jdbc.update("DELETE FROM support_shift_events WHERE shift_id=?",id));
 }
}
