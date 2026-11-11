package com.kizuna.features.ticket.service;
import com.kizuna.features.ticket.dto.SupportTicketActor;
import com.kizuna.core.exception.BusinessException;
import org.springframework.stereotype.Service;
import java.util.*;
@Service
public class SupportTicketAccessPolicy {
 /** Ca làm của CS: chỉ người đang trong ca và đã xong yêu cầu trước mới thấy/được bấm "Nhận xử lý". */
 @org.springframework.beans.factory.annotation.Autowired @org.springframework.context.annotation.Lazy
 private SupportShiftService shifts;
 private static final Set<String> HANDOVER_STATUSES=Set.of("IN_PROGRESS","ESCALATED","RESOLVED");
 /** Người phụ trách đã bị khóa/xóa/đổi vai trò khi yêu cầu còn dở: Quản lý xem và tự chọn người tiếp nhận (không tự bàn giao). */
 public static boolean needsHandover(Map<String,Object> t){
   return t.get("assigned_to")!=null && Boolean.FALSE.equals(t.get("assignee_active")) && HANDOVER_STATUSES.contains(String.valueOf(t.get("status")));
 }
 public boolean canRead(SupportTicketActor a,Map<String,Object> t){
   return a.cs() || (a.requester() && a.userId().equals(t.get("user_id"))) || (a.manager() && (a.userId().equals(t.get("escalated_to")) || needsHandover(t)));
 }
 public void read(SupportTicketActor a,Map<String,Object> t){if(t==null || !canRead(a,t))throw new BusinessException(404,"Không tìm thấy yêu cầu hỗ trợ hoặc bạn không có quyền truy cập.");}
 public List<String> actions(SupportTicketActor a,Map<String,Object> t){
   if(!canRead(a,t))return List.of();
   String status=t.get("status").toString();if(status.equals("CLOSED"))return List.of();
   var actions=new ArrayList<String>();boolean owner=a.requester()&&a.userId().equals(t.get("user_id"));
   boolean assigned=a.cs()&&a.userId().equals(t.get("assigned_to"));
   if(owner||assigned)actions.add("message");
   if(owner&&status.equals("RESOLVED"))actions.add("close");
   // Không còn nhận thủ công: yêu cầu được hệ thống tự giao theo ca (2026-11-09).
   if((a.cs()&&t.get("assigned_to")!=null)||(a.manager()&&needsHandover(t)))actions.add("assignment");
   if(assigned){actions.add("priority");if(status.equals("IN_PROGRESS")){actions.add("resolve");actions.add("escalate");}if(status.equals("ESCALATED"))actions.add("recipient");}
   if(a.manager()&&a.userId().equals(t.get("escalated_to"))&&status.equals("ESCALATED")){actions.add("decision");actions.add("return");}
   return List.copyOf(actions);
 }
 public void command(SupportTicketActor a,Map<String,Object> t,String action){
   read(a,t);
   boolean owner=a.requester()&&a.userId().equals(t.get("user_id")),assigned=a.cs()&&a.userId().equals(t.get("assigned_to"));
   boolean authorized=switch(action){case "message"->owner||assigned;case "close"->owner;case "claim"->a.cs();case "assignment"->a.cs()||(a.manager()&&needsHandover(t));case "priority","resolve","escalate","recipient"->assigned;case "decision","return"->a.manager()&&a.userId().equals(t.get("escalated_to"));default->false;};
   if(!authorized)throw new BusinessException(403,"Bạn không có quyền thực hiện thao tác này.");
   if(!actions(a,t).contains(action))throw new BusinessException(409,"Trạng thái yêu cầu đã thay đổi. Vui lòng tải lại trước khi thao tác.");
 }
}
