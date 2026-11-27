package com.kizuna.features.ticket.repository;
import com.kizuna.features.storage.R2FileStore;
import org.springframework.stereotype.Repository;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import java.util.*;
@Repository
public class TicketAttachmentRepository {
 private final JdbcTemplate jdbc;private final R2FileStore r2;
 public TicketAttachmentRepository(@Qualifier("ticketJdbc") JdbcTemplate jdbc,R2FileStore r2){this.jdbc=jdbc;this.r2=r2;}
 static String r2Key(Object attachment){return "tickets/"+attachment;}
 /** Bật R2 thì nội dung lên R2, cột content để NULL. */
 public void save(UUID ticket,UUID message,UUID uploader,String name,String mime,byte[] bytes){UUID id=UUID.randomUUID();jdbc.update("INSERT INTO ticket_attachments(attachment_id,ticket_id,message_id,uploaded_by,original_name,content_type,size_bytes,content) VALUES(?,?,?,?,?,?,?,?)",id,ticket,message,uploader,name,mime,bytes.length,r2.put(r2Key(id),bytes,mime));}
 public List<Map<String,Object>> metadata(UUID ticket,Collection<UUID> messages){
   if(messages.isEmpty())return List.of();var args=new ArrayList<Object>();args.add(ticket);args.addAll(messages);
   return jdbc.queryForList("SELECT a.attachment_id,a.message_id,a.original_name,a.content_type,a.size_bytes,a.created_at,u.full_name AS uploaded_by_name FROM ticket_attachments a JOIN users u ON u.user_id=a.uploaded_by WHERE a.ticket_id=? AND a.message_id IN ("+String.join(",",Collections.nCopies(messages.size(),"?"))+") ORDER BY a.created_at,a.attachment_id",args.toArray());
 }
 public Map<String,Object> content(UUID ticket,UUID attachment){var rows=jdbc.queryForList("SELECT original_name,content_type,content FROM ticket_attachments WHERE ticket_id=? AND attachment_id=?",ticket,attachment);if(rows.isEmpty())return null;var row=new LinkedHashMap<>(rows.get(0));byte[] data=r2.read((byte[])row.get("content"),r2Key(attachment));if(data==null)return null;row.put("content",data);return row;}
}
