package com.kizuna.features.ticket.repository;
import org.springframework.stereotype.Repository;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import java.util.*;
@Repository
public class SupportTicketRepository {
 private final JdbcTemplate jdbc;
 public SupportTicketRepository(@Qualifier("ticketJdbc") JdbcTemplate jdbc){this.jdbc=jdbc;}
 /** Người phụ trách còn là nhân viên hỗ trợ đang hoạt động (bị khóa, xóa hoặc đổi vai trò thì false). */
 public static final String ASSIGNEE_ACTIVE="EXISTS(SELECT 1 FROM users au JOIN roles ar ON ar.role_id=au.role_id WHERE au.user_id=t.assigned_to AND au.account_status::text='ACTIVE' AND au.deleted_at IS NULL AND ar.is_active=true AND ar.role_name='ROLE_CS_STAFF')";
 /** Yêu cầu đang dở mà người phụ trách không còn hoạt động: chờ Quản lý bàn giao tay. */
 public static final String NEEDS_HANDOVER="(t.assigned_to IS NOT NULL AND t.status::text IN ('IN_PROGRESS','ESCALATED','RESOLVED') AND NOT "+ASSIGNEE_ACTIVE+")";
 private static final String COLUMNS="t.ticket_id,t.user_id,t.assigned_to,t.escalated_to,t.is_escalated,t.subject,t.category::text,t.priority::text,t.status::text,t.related_booking_id,t.related_membership_id,t.related_transaction_id,t.created_at,t.updated_at,t.version,a.full_name AS assignee_name,e.full_name AS recipient_name,r.full_name AS requester_name,rr.role_name AS requester_role,"+ASSIGNEE_ACTIVE+" AS assignee_active";
 private static final String SOURCE=" FROM support_tickets t JOIN users r ON r.user_id=t.user_id JOIN roles rr ON rr.role_id=r.role_id LEFT JOIN users a ON a.user_id=t.assigned_to LEFT JOIN users e ON e.user_id=t.escalated_to";
 public List<Map<String,Object>> activeCounts(){return jdbc.queryForList("SELECT category::text,count(*) AS total FROM support_tickets WHERE status IN ('OPEN','IN_PROGRESS','ESCALATED') GROUP BY category");}
 public Map<String,Object> latestManagerFeedback(UUID assignee){
   var rows=jdbc.queryForList("""
     SELECT t.ticket_id,t.subject,e.returned_at FROM support_tickets t
     JOIN LATERAL (SELECT pending,returned_at,decision_text,current_manager_id,decision_by,decision_revision,recipient_revision
       FROM ticket_escalations WHERE ticket_id=t.ticket_id ORDER BY created_at DESC,escalation_id DESC LIMIT 1) e ON true
     WHERE t.assigned_to=? AND t.status='IN_PROGRESS' AND NOT e.pending AND e.returned_at IS NOT NULL
       AND e.decision_text IS NOT NULL AND e.current_manager_id=t.escalated_to
       AND e.decision_by=e.current_manager_id AND e.decision_revision=e.recipient_revision
     ORDER BY e.returned_at DESC,t.ticket_id DESC LIMIT 1
     """,assignee);
   if(rows.isEmpty())return null;var result=new LinkedHashMap<String,Object>();result.put("ticketId",rows.get(0).get("ticket_id"));result.put("subject",rows.get(0).get("subject"));result.put("returnedAt",rows.get(0).get("returned_at"));return result;
 }
 public Map<String,Object> find(UUID id,boolean lock){var rows=jdbc.queryForList("SELECT "+COLUMNS+SOURCE+" WHERE t.ticket_id=?"+(lock?" FOR UPDATE OF t":""),id);return rows.isEmpty()?null:rows.get(0);}
 public List<Map<String,Object>> list(String where,List<Object> args,int page,int size){var params=new ArrayList<>(args);params.add(size);params.add((long)page*size);return jdbc.queryForList("SELECT "+COLUMNS+SOURCE+" WHERE "+where+" ORDER BY t.updated_at DESC,t.ticket_id DESC LIMIT ? OFFSET ?",params.toArray());}
 /**
  * Danh sách + tổng số trong MỘT truy vấn (count(*) OVER()), kèm đoạn mô tả đầu cho người gửi khi cần.
  * Database ở xa (Supabase) nên mỗi lần hỏi tốn ~0,1–0,3 giây; gộp lại để trang danh sách tải nhanh hơn.
  * Trang trống (vượt quá số trang) thì vẫn đếm riêng để trả đúng tổng.
  */
 public record Page(List<Map<String,Object>> rows,long total){}
 public Page listWithTotal(String where,List<Object> args,int page,int size,boolean withPreview){
   var params=new ArrayList<>(args);params.add(size);params.add((long)page*size);
   String preview=withPreview?",(SELECT left(m.message_text,160) FROM ticket_messages m WHERE m.ticket_id=t.ticket_id AND m.sequence_no=1) AS description_preview":"";
   var rows=jdbc.queryForList("SELECT "+COLUMNS+",count(*) OVER() AS total_count"+preview+SOURCE+" WHERE "+where+" ORDER BY t.updated_at DESC,t.ticket_id DESC LIMIT ? OFFSET ?",params.toArray());
   long total=rows.isEmpty()?(page==0?0:count(where,args)):((Number)rows.get(0).get("total_count")).longValue();
   return new Page(rows,total);
 }
 public long count(String where,List<Object> args){return jdbc.queryForObject("SELECT count(*) FROM support_tickets t WHERE "+where,Long.class,args.toArray());}
 public Map<String,Long> counts(String where,List<Object> args){
   var result=new LinkedHashMap<String,Long>();
   jdbc.query("SELECT t.status::text AS status,count(*) AS total FROM support_tickets t WHERE "+where+" GROUP BY t.status",rs->{result.put(rs.getString("status"),rs.getLong("total"));},args.toArray());
   return result;
 }
 public void create(UUID id,UUID user,String subject,String category,String priority,UUID booking,UUID membership,UUID payment){jdbc.update("INSERT INTO support_tickets(ticket_id,user_id,is_escalated,subject,category,priority,status,related_booking_id,related_membership_id,related_transaction_id,created_at,updated_at,version) VALUES(?,?,false,?,CAST(? AS support_ticket_category_enum),CAST(? AS support_ticket_priority_enum),'OPEN',?,?,?,now(),now(),0)",id,user,subject,category,priority,booking,membership,payment);}
 public void save(Map<String,Object> t){jdbc.update("UPDATE support_tickets SET assigned_to=?,escalated_to=?,is_escalated=?,priority=CAST(? AS support_ticket_priority_enum),status=CAST(? AS support_ticket_status_enum),version=version+1,updated_at=now() WHERE ticket_id=?",t.get("assigned_to"),t.get("escalated_to"),t.get("is_escalated"),t.get("priority"),t.get("status"),t.get("ticket_id"));}
 public List<Map<String,Object>> escalation(UUID id,boolean lock){return jdbc.queryForList("SELECT escalation_id,ticket_id,referred_by,current_manager_id,reason,handling_summary,created_at,returned_at,pending,recipient_revision,decision_text,decision_by,decision_revision,decided_at FROM ticket_escalations WHERE ticket_id=? ORDER BY created_at DESC,escalation_id DESC LIMIT 1"+(lock?" FOR UPDATE":""),id);}
 public void escalate(UUID id,UUID actor,UUID manager,String reason,String summary){jdbc.update("INSERT INTO ticket_escalations(escalation_id,ticket_id,referred_by,current_manager_id,reason,handling_summary) VALUES(?,?,?,?,?,?)",UUID.randomUUID(),id,actor,manager,reason,summary);}
 public void recipient(UUID escalation,UUID manager){jdbc.update("UPDATE ticket_escalations SET current_manager_id=?,recipient_revision=recipient_revision+1,decision_text=null,decision_by=null,decision_revision=null,decided_at=null WHERE escalation_id=?",manager,escalation);}
 public void decision(UUID escalation,UUID manager,String decision){jdbc.update("UPDATE ticket_escalations SET decision_text=?,decision_by=?,decision_revision=recipient_revision,decided_at=now() WHERE escalation_id=?",decision,manager,escalation);}
 public void returned(UUID escalation){jdbc.update("UPDATE ticket_escalations SET pending=false,returned_at=now() WHERE escalation_id=?",escalation);}
}
