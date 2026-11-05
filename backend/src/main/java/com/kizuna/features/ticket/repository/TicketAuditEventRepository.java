package com.kizuna.features.ticket.repository;
import org.springframework.stereotype.Repository;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.*;
@Repository
public class TicketAuditEventRepository {
 private final JdbcTemplate jdbc;private final ObjectMapper mapper;
 public TicketAuditEventRepository(@Qualifier("ticketJdbc") JdbcTemplate jdbc,ObjectMapper mapper){this.jdbc=jdbc;this.mapper=mapper;}
 public void append(UUID ticket,UUID actor,String event,long version,Map<String,Object> before,Map<String,Object> after,String note){
   try{jdbc.update("INSERT INTO ticket_audit_events(event_id,ticket_id,actor_user_id,event_type,ticket_version,before_values,after_values,note) VALUES(?,?,?,?,?,CAST(? AS jsonb),CAST(? AS jsonb),?)",UUID.randomUUID(),ticket,actor,event,version,mapper.writeValueAsString(before),mapper.writeValueAsString(after),note);}catch(com.fasterxml.jackson.core.JsonProcessingException e){throw new IllegalStateException("Cannot serialize ticket audit",e);}
 }
 public List<Map<String,Object>> list(UUID ticket,int page,int size){return jdbc.queryForList("""
   SELECT e.event_id,e.event_type,e.occurred_at,e.ticket_version,u.full_name AS actor_name,e.note,
     e.before_values->>'status' AS previous_status,e.after_values->>'status' AS current_status,
     e.before_values->>'priority' AS previous_priority,e.after_values->>'priority' AS current_priority,
     old_cs.full_name AS previous_assignee,new_cs.full_name AS current_assignee,
     old_manager.full_name AS previous_recipient,new_manager.full_name AS current_recipient
   FROM ticket_audit_events e JOIN users u ON u.user_id=e.actor_user_id
   LEFT JOIN users old_cs ON old_cs.user_id=(e.before_values->>'assigned_to')::uuid
   LEFT JOIN users new_cs ON new_cs.user_id=(e.after_values->>'assigned_to')::uuid
   LEFT JOIN users old_manager ON old_manager.user_id=(e.before_values->>'escalated_to')::uuid
   LEFT JOIN users new_manager ON new_manager.user_id=(e.after_values->>'escalated_to')::uuid
   WHERE e.ticket_id=? ORDER BY e.occurred_at,e.event_id LIMIT ? OFFSET ?
   """,ticket,size,(long)page*size);}
 public long count(UUID ticket){return jdbc.queryForObject("SELECT count(*) FROM ticket_audit_events WHERE ticket_id=?",Long.class,ticket);}
}
