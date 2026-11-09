package com.kizuna.features.ticket.service;
import com.kizuna.features.ticket.dto.*;
import com.kizuna.features.ticket.dto.SupportTicketRequests.*;
import com.kizuna.features.ticket.repository.*;
import com.kizuna.core.exception.BusinessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import java.util.*;
@Service @RequiredArgsConstructor
public class SupportTicketCommandExecutor {
 private final SupportTicketRepository tickets;private final TicketMessageRepository messages;private final TicketAttachmentRepository attachments;
 private final TicketAuditEventRepository audit;private final SupportTicketActorResolver actors;private final SupportTicketAccessPolicy policy;private final SupportTicketRelatedContextService contexts;
 /** Sau khi tạo yêu cầu hoặc khi nhân viên xong việc, giao tiếp yêu cầu đang chờ cho người rảnh trong ca. */
 private final SupportShiftService shifts;
 @Transactional public UUID create(Create r,List<TicketAttachmentValidator.ValidatedFile> files){
   var actor=actors.current(true);if(!actor.requester())throw new BusinessException(403,"Chỉ học viên và gia sư được gửi yêu cầu hỗ trợ.");
   String subject=SupportTicketValidation.text(r.subject(),200),text=SupportTicketValidation.text(r.message(),10000);
   SupportTicketValidation.choice(r.category(),SupportTicketProjection.CATEGORY.keySet());contexts.validate(actor.userId(),r);
   UUID id=UUID.randomUUID();
   // Mức ưu tiên ban đầu do hệ thống đặt theo nhóm và buổi học liên quan (người gửi không tự chọn).
   String priority=SupportTicketPriorityRule.initial(r.category(),contexts.bookingStart(r.relatedBookingId()).orElse(null),java.time.OffsetDateTime.now());
   tickets.create(id,actor.userId(),subject,r.category(),priority,r.relatedBookingId(),r.relatedMembershipId(),r.relatedTransactionId());
   append(id,actor.userId(),text,files);audit.append(id,actor.userId(),"CREATE",0,Map.of(),snapshot(tickets.find(id,false)),null);shifts.dispatchAfterCommit();return id;
 }
 @Transactional public void message(UUID id,Message r,List<TicketAttachmentValidator.ValidatedFile> files){
   shifts.lockAssignments();
   var ticket=locked(id,r.expectedVersion());var actor=actors.current(true);policy.command(actor,ticket,"message");
   var before=snapshot(ticket);append(id,actor.userId(),SupportTicketValidation.text(r.message(),10000),files);
   if(actor.requester()&&ticket.get("status").equals("RESOLVED"))ticket.put("status","IN_PROGRESS");
   finish(ticket,actor,"MESSAGE_SENT",before,null);
 }
 @Transactional public void command(UUID id,String action,Command r){
   shifts.lockAssignments();
   var t=locked(id,r.expectedVersion());var a=actors.current(true);policy.command(a,t,action);var before=snapshot(t);String note=null;
   switch(action){
     case "claim"->throw new BusinessException(400,"Yêu cầu được hệ thống tự giao cho nhân viên đang trong ca; không cần nhận thủ công.");
     case "assignment"->{actors.recipient(r.assigneeId(),"ROLE_CS_STAFF");if(Objects.equals(t.get("assigned_to"),r.assigneeId()))throw invalid();shifts.requireNewAssignment(r.assigneeId());t.put("assigned_to",r.assigneeId());note=r.reason()==null||r.reason().isBlank()?"Bàn giao yêu cầu cho nhân viên khác":SupportTicketValidation.text(r.reason(),10000);}
     case "priority"->t.put("priority",SupportTicketValidation.choice(r.priority(),SupportTicketProjection.PRIORITY.keySet()));
     case "resolve"->{append(id,a.userId(),SupportTicketValidation.text(r.resolutionText(),10000),List.of());t.put("status","RESOLVED");}
     case "close"->t.put("status","CLOSED");
     case "escalate"->{actors.recipient(r.managerId(),"ROLE_MANAGER");String reason=SupportTicketValidation.text(r.reason(),10000),summary=SupportTicketValidation.text(r.handlingSummary(),10000);tickets.escalate(id,a.userId(),r.managerId(),reason,summary);t.put("escalated_to",r.managerId());t.put("is_escalated",true);t.put("status","ESCALATED");note=reason;}
     case "recipient","decision","return"->note=escalation(t,a,action,r,before);
     default->throw invalid();
   }
   finish(t,a,event(action),before,note);
   if(Set.of("resolve","escalate","close","assignment","return").contains(action))shifts.dispatchAfterCommit();
 }
 private String escalation(Map<String,Object> t,SupportTicketActor a,String action,Command r,Map<String,Object> before){
   var rows=tickets.escalation((UUID)t.get("ticket_id"),true);if(rows.isEmpty()||!Boolean.TRUE.equals(rows.get(0).get("pending")))throw conflict();var e=rows.get(0);UUID eid=(UUID)e.get("escalation_id");
   before.put("escalation",new LinkedHashMap<>(e));
   if(action.equals("recipient")){actors.recipient(r.managerId(),"ROLE_MANAGER");if(Objects.equals(r.managerId(),e.get("current_manager_id")))throw invalid();String reason=SupportTicketValidation.text(r.reason(),10000);tickets.recipient(eid,r.managerId());t.put("escalated_to",r.managerId());return reason;}
   if(!a.userId().equals(e.get("current_manager_id")))throw new BusinessException(403,"Bạn không còn phụ trách yêu cầu này.");
   if(action.equals("decision")){String decision=SupportTicketValidation.text(r.decisionText(),10000);tickets.decision(eid,a.userId(),decision);return decision;}
   if(e.get("decision_text")==null||!a.userId().equals(e.get("decision_by"))||!Objects.equals(e.get("decision_revision"),e.get("recipient_revision")))throw conflict();
   tickets.returned(eid);t.put("status","IN_PROGRESS");t.put("is_escalated",false);return null;
 }
 private Map<String,Object> locked(UUID id,Long version){
   long expected=SupportTicketValidation.version(version);var actor=actors.current(false);var t=tickets.find(id,true);policy.read(actor,t);
   if(((Number)t.get("version")).longValue()!=expected)throw conflict();return t;
 }
 private void append(UUID ticket,UUID sender,String text,List<TicketAttachmentValidator.ValidatedFile> files){UUID message=messages.append(ticket,sender,text);for(var file:files)attachments.save(ticket,message,sender,file.name(),file.mime(),file.bytes());}
 private void finish(Map<String,Object> t,SupportTicketActor a,String event,Map<String,Object> before,String note){tickets.save(t);long next=((Number)t.get("version")).longValue()+1;var after=snapshot(t);after.put("version",next);
   var escalations=tickets.escalation((UUID)t.get("ticket_id"),false);if(!escalations.isEmpty())after.put("escalation",escalations.get(0));
   audit.append((UUID)t.get("ticket_id"),a.userId(),event,next,before,after,note);
 }
 private Map<String,Object> snapshot(Map<String,Object> t){var result=new LinkedHashMap<String,Object>();for(String key:List.of("status","priority","assigned_to","escalated_to","version","related_booking_id","related_membership_id","related_transaction_id"))result.put(key,t.get(key));return result;}
 private String event(String a){return switch(a){case "claim"->"CLAIM";case "assignment"->"REASSIGN_CS";case "priority"->"PRIORITY_CHANGE";case "resolve"->"STATUS_CHANGE";case "close"->"CLOSE";case "escalate"->"ESCALATE";case "recipient"->"REASSIGN_MANAGER";case "decision"->"SAVE_DECISION";case "return"->"RETURN_RESOLUTION";default->throw invalid();};}
 private BusinessException conflict(){return new BusinessException(409,"Yêu cầu đã thay đổi hoặc chưa đủ điều kiện xử lý. Vui lòng tải lại trước khi gửi.");}
 private BusinessException invalid(){return new BusinessException(400,"Vui lòng kiểm tra lựa chọn và thông tin đã nhập.");}
}
