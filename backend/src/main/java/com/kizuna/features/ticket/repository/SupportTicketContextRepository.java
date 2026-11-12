package com.kizuna.features.ticket.repository;
import org.springframework.stereotype.Repository;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import java.util.*;
/** Read-only ownership projections explicitly defined by FE-10's contract. */
@Repository
public class SupportTicketContextRepository {
 private final JdbcTemplate jdbc;
 public SupportTicketContextRepository(@Qualifier("ticketJdbc") JdbcTemplate jdbc){this.jdbc=jdbc;}
 /** Giờ bắt đầu buổi học thử (để đặt mức ưu tiên ban đầu); không có thì rỗng. */
 public Optional<java.time.OffsetDateTime> bookingStart(UUID booking){if(booking==null)return Optional.empty();return jdbc.query("SELECT scheduled_start_time FROM trial_bookings WHERE booking_id=?",(rs,n)->rs.getObject(1,java.time.OffsetDateTime.class),booking).stream().findFirst();}
 private String source(String type){return switch(type){
   case "booking"->"SELECT b.booking_id AS id,to_char(b.scheduled_start_time AT TIME ZONE 'Asia/Ho_Chi_Minh','DD/MM/YYYY HH24:MI') AS label,b.booking_status::text AS state,null::uuid AS plan_id,null::text AS transaction_ref FROM trial_bookings b LEFT JOIN learners l ON l.learner_id=b.learner_id LEFT JOIN tutors t ON t.tutor_id=b.tutor_id WHERE (l.user_id=? OR t.user_id=?)";
   case "membership"->"SELECT m.membership_id AS id,p.plan_name AS label,m.status::text AS state,m.plan_id,m.transaction_ref FROM tutor_memberships m JOIN tutors t ON t.tutor_id=m.tutor_id JOIN membership_plans p ON p.plan_id=m.plan_id WHERE t.user_id=?";
   case "payment"->"SELECT x.transaction_id AS id,p.plan_name||' · '||to_char(x.transaction_time AT TIME ZONE 'Asia/Ho_Chi_Minh','DD/MM/YYYY HH24:MI') AS label,x.status::text AS state,x.plan_id,x.gateway_transaction_id AS transaction_ref FROM payment_transactions x JOIN membership_plans p ON p.plan_id=x.plan_id WHERE x.user_id=? AND x.plan_id IS NOT NULL";
   default->throw new com.kizuna.core.exception.BusinessException(400,"Loại thông tin liên quan chưa hợp lệ.");};}
 private Object[] params(String type,UUID owner){return type.equals("booking")?new Object[]{owner,owner}:new Object[]{owner};}
 public Map<String,Object> find(String type,UUID owner,UUID id){var args=new ArrayList<>(Arrays.asList(params(type,owner)));args.add(id);var rows=jdbc.queryForList("SELECT * FROM ("+source(type)+") c WHERE id=?",args.toArray());return rows.isEmpty()?null:rows.get(0);}
 public List<Map<String,Object>> list(String type,UUID owner,int page,int size){var args=new ArrayList<>(Arrays.asList(params(type,owner)));args.add(size);args.add((long)page*size);return jdbc.queryForList(source(type)+" ORDER BY id LIMIT ? OFFSET ?",args.toArray());}
 public long count(String type,UUID owner){return jdbc.queryForObject("SELECT count(*) FROM ("+source(type)+") c",Long.class,params(type,owner));}
 public Map<String,Object> originalLinks(UUID ticket){var rows=jdbc.queryForList("SELECT (after_values->>'related_booking_id')::uuid AS related_booking_id,(after_values->>'related_membership_id')::uuid AS related_membership_id,(after_values->>'related_transaction_id')::uuid AS related_transaction_id FROM ticket_audit_events WHERE ticket_id=? AND event_type='CREATE' ORDER BY occurred_at,event_id LIMIT 1",ticket);return rows.isEmpty()?Map.of():rows.get(0);}
}
