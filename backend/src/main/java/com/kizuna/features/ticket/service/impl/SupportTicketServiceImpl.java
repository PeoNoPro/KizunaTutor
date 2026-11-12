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
 @org.springframework.beans.factory.annotation.Autowired private com.kizuna.features.ticket.service.SupportShiftService shifts;
 private final SupportTicketRepository tickets;private final TicketMessageRepository messages;private final TicketAttachmentRepository attachments;private final TicketAuditEventRepository audit;
 private final SupportTicketActorResolver actors;private final SupportTicketAccessPolicy policy;private final SupportTicketProjection projection;private final SupportTicketRelatedContextService related;private final SupportTicketCommandExecutor commands;private final TicketAttachmentValidator validator;
 @Override @Transactional(readOnly=true) public Map<String,Object> me(){var a=actors.current(false);return Map.of("userId",a.userId(),"displayName",a.displayName(),"viewRole",a.viewRole(),"canCreate",a.requester(),"canHandle",a.cs(),"canDecide",a.manager());}
 @Override @Transactional(readOnly=true,isolation=org.springframework.transaction.annotation.Isolation.REPEATABLE_READ)
 public Map<String,Object> dashboard(String category){
   var actor=actors.current(false);if(!actor.cs())throw new BusinessException(403,"Chỉ nhân viên hỗ trợ được xem tổng quan này.");
   if(category!=null)SupportTicketValidation.choice(category,SupportTicketProjection.CATEGORY.keySet());
   var counts=new LinkedHashMap<String,Long>();SupportTicketProjection.CATEGORY.keySet().forEach(key->counts.put(key,0L));
   for(var row:tickets.activeCounts())counts.put(row.get("category").toString(),((Number)row.get("total")).longValue());
   String where="t.status IN ('OPEN','IN_PROGRESS','ESCALATED')";List<Object> args=List.of();
   if(category!=null){where+=" AND t.category::text=?";args=List.of(category);}
   var result=new LinkedHashMap<String,Object>();result.put("totalActive",counts.values().stream().mapToLong(Long::longValue).sum());result.put("countsByCategory",counts);result.put("selectedCategory",category);
   result.put("queue",tickets.list(where,args,0,4).stream().map(row->projection.summary(actor,row)).toList());
   result.put("managerFeedback",tickets.latestManagerFeedback(actor.userId()));
   result.put("shift",shifts.myShift(actor.userId()));return result;
 }
 // Chỉ đọc, mỗi bước một truy vấn: không mở transaction để bớt một lượt hỏi database (COMMIT) mỗi lần tải.
 @Override public Map<String,Object> list(List<String> status,String category,String priority,UUID assignedTo,boolean handover,int page,int size){
   SupportTicketValidation.page(page,size);var a=actors.current(false);var filter=filter(a,status,category,priority,assignedTo,handover);
   var result=tickets.listWithTotal(filter.where(),filter.args(),page,size,a.requester());
   return SupportTicketValidation.page(result.rows().stream().map(t->{var item=projection.summary(a,t);if(a.requester())item.put("descriptionPreview",t.get("description_preview"));return item;}).toList(),page,size,result.total());
 }
 @Override public Map<String,Long> counts(String category,String priority,UUID assignedTo,boolean handover){
   var filter=filter(actors.current(false),null,category,priority,assignedTo,handover);
   var result=new LinkedHashMap<String,Long>();SupportTicketProjection.STATUS.keySet().forEach(s->result.put(s,0L));
   result.putAll(tickets.counts(filter.where(),filter.args()));return result;
 }
 private record TicketFilter(String where,List<Object> args){}
 private TicketFilter filter(SupportTicketActor a,List<String> status,String category,String priority,UUID assignedTo,boolean handover){
   StringBuilder where=new StringBuilder("true");var args=new ArrayList<Object>();
   if(a.requester()){where.append(" AND t.user_id=?");args.add(a.userId());}else if(a.manager()){
     // Nhóm "Cần bàn giao": yêu cầu của nhân viên đã nghỉ/bị khóa; các nhóm khác chỉ là yêu cầu chuyển lên cho chính Quản lý này.
     if(handover)where.append(" AND ").append(com.kizuna.features.ticket.repository.SupportTicketRepository.NEEDS_HANDOVER);
     else{where.append(" AND t.escalated_to=?");args.add(a.userId());}
   }
   if(handover&&!a.manager())throw new BusinessException(403,"Chỉ Quản lý xem được nhóm yêu cầu cần bàn giao.");
   if(status!=null&&!status.isEmpty()){status.forEach(s->SupportTicketValidation.choice(s,SupportTicketProjection.STATUS.keySet()));where.append(" AND t.status::text IN (").append(String.join(",",Collections.nCopies(status.size(),"?"))).append(")");args.addAll(status);}
   String[] values={category,priority};String[] cols={"category","priority"};List<Set<String>> sets=List.of(SupportTicketProjection.CATEGORY.keySet(),SupportTicketProjection.PRIORITY.keySet());
   for(int i=0;i<2;i++)if(values[i]!=null){SupportTicketValidation.choice(values[i],sets.get(i));where.append(" AND t.").append(cols[i]).append("::text=?");args.add(values[i]);}
   if(assignedTo!=null){if(!a.cs())throw new BusinessException(403,"Bạn không có quyền lọc theo nhân viên.");where.append(" AND t.assigned_to=?");args.add(assignedTo);}
   return new TicketFilter(where.toString(),args);
 }
 @Override @Transactional(readOnly=true) public Map<String,Object> detail(UUID id){var a=actors.current(false);var t=tickets.find(id,false);policy.read(a,t);var result=projection.summary(a,t);result.put("description",messages.description(id));result.put("relatedContext",related.detail(t));
   if(!a.requester()){result.put("requesterRoleLabel",SupportTicketProjection.ROLE.get(t.get("requester_role")));var rows=tickets.escalation(id,false);if(!rows.isEmpty()){var e=new LinkedHashMap<String,Object>();SupportTicketProjection.copy(e,rows.get(0),"reason","reason","handlingSummary","handling_summary","decisionText","decision_text","recipientRevision","recipient_revision","pending","pending","decidedAt","decided_at");result.put("escalation",e);}}
   return result;
 }
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
