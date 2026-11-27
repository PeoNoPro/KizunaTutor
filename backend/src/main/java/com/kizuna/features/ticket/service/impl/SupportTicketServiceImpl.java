package com.kizuna.features.ticket.service.impl;
import com.kizuna.features.ticket.service.*;
import com.kizuna.features.ticket.dto.*;
import com.kizuna.features.ticket.dto.SupportTicketRequests.*;
import com.kizuna.features.ticket.repository.*;
import com.kizuna.core.exception.BusinessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import lombok.RequiredArgsConstructor;
import java.util.*;
@Service @RequiredArgsConstructor
public class SupportTicketServiceImpl implements SupportTicketService {
 /** Ghi chú nội bộ (đợt 3, 2026-11-12). */
 private final TicketInternalNoteRepository internalNotes;
 private final SupportTicketRepository tickets;private final TicketMessageRepository messages;private final TicketAttachmentRepository attachments;private final TicketAuditEventRepository audit;
 private final SupportTicketActorResolver actors;private final SupportTicketAccessPolicy policy;private final SupportTicketProjection projection;private final SupportTicketRelatedContextService related;private final SupportTicketCommandExecutor commands;private final TicketAttachmentValidator validator;
 @Override @Transactional(readOnly=true) public Map<String,Object> me(){var a=actors.current(false);return Map.of("userId",a.userId(),"displayName",a.displayName(),"viewRole",a.viewRole(),"canCreate",a.requester(),"canHandle",a.cs(),"canDecide",a.manager());}
 @Override public Map<String,Long> overview(){
   if(!actors.current(false).manager())throw new BusinessException(403,"Chỉ Quản lý được xem tình hình yêu cầu hỗ trợ.");
   return tickets.overview();
 }
 @Override @Transactional(readOnly=true,isolation=org.springframework.transaction.annotation.Isolation.REPEATABLE_READ)
 public Map<String,Object> dashboard(String category,String view,String priority){
   var actor=actors.current(false);if(!actor.cs())throw new BusinessException(403,"Chỉ nhân viên hỗ trợ được xem tổng quan này.");
   if(category!=null)SupportTicketValidation.choice(category,SupportTicketProjection.CATEGORY.keySet());
   var counts=new LinkedHashMap<String,Long>();SupportTicketProjection.CATEGORY.keySet().forEach(key->counts.put(key,0L));
   for(var row:tickets.activeCounts())counts.put(row.get("category").toString(),((Number)row.get("total")).longValue());
   String where="t.status IN ('OPEN','IN_PROGRESS','ESCALATED')";List<Object> args=List.of();
   if(category!=null){where+=" AND t.category::text=?";args=List.of(category);}
   // Bộ lọc nhanh của hàng đợi: MINE = yêu cầu mình phụ trách, OVERDUE = quá hạn phản hồi.
   if(view!=null)SupportTicketValidation.choice(view,Set.of("MINE","OVERDUE"));
   var queueArgs=new ArrayList<Object>(args);
   if("MINE".equals(view)){where+=" AND t.assigned_to=?";queueArgs.add(actor.userId());}
   if("OVERDUE".equals(view))where+=" AND "+SupportTicketSla.OVERDUE_SQL;
   // Lọc theo mức ưu tiên (2026-11-13), chỉ áp dụng cho danh sách hàng đợi, không đổi số đếm theo nhóm.
   if(priority!=null){SupportTicketValidation.choice(priority,SupportTicketProjection.PRIORITY.keySet());where+=" AND t.priority::text=?";queueArgs.add(priority);}
   var result=new LinkedHashMap<String,Object>();result.put("totalActive",counts.values().stream().mapToLong(Long::longValue).sum());result.put("countsByCategory",counts);result.put("selectedCategory",category);
   result.put("selectedView",view);result.put("selectedPriority",priority);
   var queue=tickets.listWithTotal(where,queueArgs,0,5,false,true);
   result.put("queue",queue.rows().stream().map(row->withSla(projection.summary(actor,row),row)).toList());result.put("queueTotal",queue.total());
   result.put("sla",tickets.slaMetrics(actor.userId()));
   result.put("managerFeedback",tickets.latestManagerFeedback(actor.userId()));
   return result;
 }
 // Chỉ đọc, mỗi bước một truy vấn: không mở transaction để bớt một lượt hỏi database (COMMIT) mỗi lần tải.
 @Override public Map<String,Object> list(List<String> status,String category,String priority,UUID assignedTo,String q,boolean overdue,int page,int size){
   SupportTicketValidation.page(page,size);var a=actors.current(false);var filter=filter(a,status,category,priority,assignedTo,q,overdue);
   var result=tickets.listWithTotal(filter.where(),filter.args(),page,size,a.requester(),!a.requester());
   return SupportTicketValidation.page(result.rows().stream().map(t->{var item=projection.summary(a,t);if(a.requester())item.put("descriptionPreview",t.get("description_preview"));else withSla(item,t);return item;}).toList(),page,size,result.total());
 }
 /**
  * Thời gian chờ (chỉ nhân viên thấy): đang chờ trả lời từ lúc nào, hạn phản hồi theo mức ưu tiên, đã quá hạn chưa.
  * Yêu cầu đã giải quyết/đóng hoặc nhân viên đã trả lời sau tin cuối của người gửi thì không có thời gian chờ.
  */
 private static Map<String,Object> withSla(Map<String,Object> item,Map<String,Object> row){
   var since=row.get("waiting_since");var status=String.valueOf(row.get("status"));
   if(since==null||!Set.of("OPEN","IN_PROGRESS","ESCALATED").contains(status))return item;
   var waiting=since instanceof java.time.OffsetDateTime o?o:since instanceof java.sql.Timestamp ts?ts.toInstant().atOffset(java.time.ZoneOffset.UTC):null;
   if(waiting==null)return item;
   var due=SupportTicketSla.dueAt(String.valueOf(row.get("priority")),waiting);
   item.put("waitingSince",waiting);item.put("responseDueAt",due);item.put("overdue",due.isBefore(java.time.OffsetDateTime.now()));
   return item;
 }
 @Override public Map<String,Long> counts(String category,String priority,UUID assignedTo,String q,boolean overdue){
   var filter=filter(actors.current(false),null,category,priority,assignedTo,q,overdue);
   var result=new LinkedHashMap<String,Long>();SupportTicketProjection.STATUS.keySet().forEach(s->result.put(s,0L));
   result.putAll(tickets.counts(filter.where(),filter.args()));return result;
 }
 private record TicketFilter(String where,List<Object> args){}
 private TicketFilter filter(SupportTicketActor a,List<String> status,String category,String priority,UUID assignedTo,String q,boolean overdue){
   StringBuilder where=new StringBuilder("true");var args=new ArrayList<Object>();
   if(a.requester()){where.append(" AND t.user_id=?");args.add(a.userId());}
   else if(a.manager()){where.append(" AND t.escalated_to=?");args.add(a.userId());}
   if(status!=null&&!status.isEmpty()){status.forEach(s->SupportTicketValidation.choice(s,SupportTicketProjection.STATUS.keySet()));where.append(" AND t.status::text IN (").append(String.join(",",Collections.nCopies(status.size(),"?"))).append(")");args.addAll(status);}
   String[] values={category,priority};String[] cols={"category","priority"};List<Set<String>> sets=List.of(SupportTicketProjection.CATEGORY.keySet(),SupportTicketProjection.PRIORITY.keySet());
   for(int i=0;i<2;i++)if(values[i]!=null){SupportTicketValidation.choice(values[i],sets.get(i));where.append(" AND t.").append(cols[i]).append("::text=?");args.add(values[i]);}
   if(assignedTo!=null){if(!a.cs())throw new BusinessException(403,"Bạn không có quyền lọc theo nhân viên.");where.append(" AND t.assigned_to=?");args.add(assignedTo);}
   // Tìm theo tiêu đề, tên người gửi hoặc mã yêu cầu (8 ký tự đầu hiện trên giao diện, có thể gõ kèm dấu #).
   if(q!=null&&!q.isBlank()){
     var text=q.strip();if(text.length()>100)throw new BusinessException(400,"Từ khóa tìm kiếm tối đa 100 ký tự.");
     var lower=text.toLowerCase(java.util.Locale.ROOT);var code=(lower.startsWith("#")?lower.substring(1):lower).replaceAll("[^0-9a-f-]","");
     // strpos: tìm chuỗi con, không cần thoát ký tự % hoặc _ như LIKE.
     where.append(" AND (strpos(lower(t.subject),?)>0 OR EXISTS(SELECT 1 FROM users qu WHERE qu.user_id=t.user_id AND strpos(lower(qu.full_name),?)>0)");
     args.add(lower);args.add(lower);
     if(code.length()>=4){where.append(" OR left(t.ticket_id::text,?)=?");args.add(code.length());args.add(code);}
     where.append(")");
   }
   if(overdue){if(a.requester())throw new BusinessException(403,"Bạn không có quyền lọc theo thời hạn phản hồi.");where.append(" AND ").append(SupportTicketSla.OVERDUE_SQL);}
   return new TicketFilter(where.toString(),args);
 }
 @Override @Transactional(readOnly=true) public Map<String,Object> detail(UUID id){var a=actors.current(false);var t=tickets.find(id,false);policy.read(a,t);var result=projection.summary(a,t);result.put("description",messages.description(id));result.put("relatedContext",related.detail(t));
   if(!a.requester()){result.put("requesterRoleLabel",SupportTicketProjection.ROLE.get(t.get("requester_role")));var rows=tickets.escalation(id,false);if(!rows.isEmpty()){var e=new LinkedHashMap<String,Object>();SupportTicketProjection.copy(e,rows.get(0),"reason","reason","handlingSummary","handling_summary","decisionText","decision_text","recipientRevision","recipient_revision","pending","pending","decidedAt","decided_at");result.put("escalation",e);}
   var sla=new HashMap<String,Object>(t);sla.put("waiting_since",tickets.waitingSince(id));withSla(result,sla);
   result.put("slaTargetMinutes",SupportTicketSla.target(String.valueOf(t.get("priority"))).toMinutes());
   if(a.cs())result.put("requester",requester(t));}
   return result;
 }
 /** Người gửi: tổng số yêu cầu đã gửi và tối đa 5 yêu cầu khác gần nhất (CS xem được mọi yêu cầu nên mở được liên kết). */
 private Map<String,Object> requester(Map<String,Object> t){
   var user=(UUID)t.get("user_id");var out=new LinkedHashMap<String,Object>();
   out.put("displayName",t.get("requester_name"));out.put("roleLabel",SupportTicketProjection.ROLE.get(t.get("requester_role")));
   out.put("ticketCount",tickets.requesterTicketCount(user));
   out.put("otherTickets",tickets.requesterTickets(user,(UUID)t.get("ticket_id"),5).stream().map(row->{var item=new LinkedHashMap<String,Object>();
     SupportTicketProjection.copy(item,row,"ticketId","ticket_id","subject","subject","status","status","createdAt","created_at");item.put("statusLabel",SupportTicketProjection.STATUS.get(row.get("status")));return item;}).toList());
   return out;
 }
 /** Ghi chú nội bộ: chỉ nhân viên hỗ trợ và Quản lý đọc được yêu cầu này; người gửi luôn nhận 403. */
 @Override @Transactional(readOnly=true) public List<Map<String,Object>> notes(UUID id){
   var a=actors.current(false);var t=tickets.find(id,false);policy.read(a,t);
   if(a.requester())throw new BusinessException(403,"Ghi chú nội bộ chỉ dành cho bộ phận xử lý.");
   return internalNotes.list(id).stream().map(this::note).toList();
 }
 @Override @Transactional public Map<String,Object> addNote(UUID id,Note r){
   var a=actors.current(true);var t=tickets.find(id,true);policy.read(a,t);
   if(a.requester())throw new BusinessException(403,"Ghi chú nội bộ chỉ dành cho bộ phận xử lý.");
   if("CLOSED".equals(String.valueOf(t.get("status"))))throw new BusinessException(409,"Yêu cầu đã đóng, không thêm ghi chú được nữa.");
   internalNotes.add(id,a.userId(),SupportTicketValidation.text(r.note(),5000));
   return Map.of("items",internalNotes.list(id).stream().map(this::note).toList());
 }
 private Map<String,Object> note(Map<String,Object> n){var out=new LinkedHashMap<String,Object>();
   SupportTicketProjection.copy(out,n,"noteId","note_id","note","note_text","createdAt","created_at","authorId","author_id","authorDisplayName","author_name");
   out.put("authorRoleLabel",SupportTicketProjection.ROLE.getOrDefault(n.get("author_role"),"Nhân viên"));return out;}
 @Override @Transactional(readOnly=true) public Map<String,Object> messages(UUID id,int page,int size){SupportTicketValidation.page(page,size);var a=actors.current(false);var t=tickets.find(id,false);policy.read(a,t);var rows=messages.list(id,page,size);var metadata=attachments.metadata(id,rows.stream().map(x->(UUID)x.get("message_id")).toList());
   var items=rows.stream().map(m->{var item=new LinkedHashMap<String,Object>();SupportTicketProjection.copy(item,m,"messageId","message_id","senderDisplayName","sender_name","message","message_text","sentAt","sent_at","sequenceNo","sequence_no");item.put("senderRoleLabel",SupportTicketProjection.ROLE.getOrDefault(m.get("sender_role"),"Vai trò khác"));item.put("fromRequester",Objects.equals(m.get("sender_id"),t.get("user_id")));item.put("attachments",metadata.stream().filter(f->Objects.equals(f.get("message_id"),m.get("message_id"))).map(f->file(id,f)).toList());return item;}).toList();
   return SupportTicketValidation.page(items,page,size,messages.count(id));
 }
 private Map<String,Object> file(UUID id,Map<String,Object> f){var out=new LinkedHashMap<String,Object>();SupportTicketProjection.copy(out,f,"attachmentId","attachment_id","originalName","original_name","contentType","content_type","sizeBytes","size_bytes","createdAt","created_at","uploadedByDisplayName","uploaded_by_name");out.put("downloadPath","/api/v1/tickets/"+id+"/attachments/"+f.get("attachment_id"));return out;}
 @Override @Transactional(readOnly=true) public Map<String,Object> history(UUID id,int page,int size){SupportTicketValidation.page(page,size);var a=actors.current(false);policy.read(a,tickets.find(id,false));if(a.requester())throw new BusinessException(403,"Lịch sử nội bộ chỉ dành cho bộ phận xử lý.");return SupportTicketValidation.page(audit.list(id,page,size).stream().map(e->{var out=new LinkedHashMap<String,Object>();SupportTicketProjection.copy(out,e,"eventType","event_type","occurredAt","occurred_at","actorDisplayName","actor_name","note","note","version","ticket_version");out.put("changes",projection.historyChanges(e));return out;}).toList(),page,size,audit.count(id));}
 @Override public Map<String,Object> create(Create r,List<MultipartFile> files){var a=actors.current(false);if(!a.requester())throw new BusinessException(403,"Chỉ học viên và gia sư được gửi yêu cầu hỗ trợ.");return detail(commands.create(r,validator.validate(files)));}
 @Override public Map<String,Object> message(UUID id,Message r,List<MultipartFile> files){var a=actors.current(false);var t=tickets.find(id,false);policy.command(a,t,"message");commands.message(id,r,validator.validate(files));return detail(id);}
 @Override public Map<String,Object> command(UUID id,String action,Command r){commands.command(id,action,r);return detail(id);}
 @Override @Transactional public Map<String,Object> download(UUID id,UUID attachment){var t=tickets.find(id,true);policy.read(actors.current(true),t);var file=attachments.content(id,attachment);if(file==null)throw new BusinessException(404,"Không tìm thấy yêu cầu hỗ trợ hoặc bạn không có quyền truy cập.");return file;}
 @Override @Transactional(readOnly=true) public Map<String,Object> contexts(String type,int page,int size){var a=actors.current(false);if(!a.requester())throw new BusinessException(403,"Chỉ người gửi được chọn thông tin liên quan.");return related.candidates(type,a.userId(),page,size);}
 @Override @Transactional(readOnly=true) public List<Map<String,Object>> context(UUID id){var t=tickets.find(id,false);policy.read(actors.current(false),t);return related.detail(t);}
 @Override public List<Map<String,Object>> candidates(boolean managers){return actors.candidates(managers);}
}
