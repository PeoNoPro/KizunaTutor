package com.kizuna.features.ticket.repository;
import org.springframework.stereotype.Repository;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import java.util.*;
@Repository
public class TicketMessageRepository {
 private final JdbcTemplate jdbc;
 public TicketMessageRepository(@Qualifier("ticketJdbc") JdbcTemplate jdbc){this.jdbc=jdbc;}
 public UUID append(UUID ticket,UUID sender,String text){UUID id=UUID.randomUUID();jdbc.update("INSERT INTO ticket_messages(message_id,ticket_id,sender_id,message_text,sent_at,sequence_no) SELECT ?,?,?,?,now(),coalesce(max(sequence_no),0)+1 FROM ticket_messages WHERE ticket_id=?",id,ticket,sender,text,ticket);return id;}
 public List<Map<String,Object>> list(UUID ticket,int page,int size){return jdbc.queryForList("SELECT m.message_id,m.sender_id,u.full_name AS sender_name,r.role_name AS sender_role,m.message_text,m.sent_at,m.sequence_no FROM ticket_messages m JOIN users u ON u.user_id=m.sender_id JOIN roles r ON r.role_id=u.role_id WHERE m.ticket_id=? ORDER BY m.sequence_no LIMIT ? OFFSET ?",ticket,size,(long)page*size);}
 public long count(UUID ticket){return jdbc.queryForObject("SELECT count(*) FROM ticket_messages WHERE ticket_id=?",Long.class,ticket);}
 public Map<UUID,String> previews(List<UUID> tickets){
   var result=new HashMap<UUID,String>();if(tickets.isEmpty())return result;
   jdbc.query("SELECT ticket_id,left(message_text,160) FROM ticket_messages WHERE sequence_no=1 AND ticket_id IN ("+String.join(",",Collections.nCopies(tickets.size(),"?"))+")",rs->{result.put(rs.getObject(1,UUID.class),rs.getString(2));},tickets.toArray());
   return result;
 }
 public String description(UUID ticket){var rows=jdbc.queryForList("SELECT message_text FROM ticket_messages WHERE ticket_id=? ORDER BY sequence_no LIMIT 1",String.class,ticket);return rows.isEmpty()?"":rows.get(0);}
}
