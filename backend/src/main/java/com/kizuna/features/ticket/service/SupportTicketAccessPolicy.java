package com.kizuna.features.ticket.service;
import com.kizuna.features.ticket.dto.SupportTicketActor;
import com.kizuna.core.exception.BusinessException;
import org.springframework.stereotype.Service;
import java.util.*;
@Service
public class SupportTicketAccessPolicy {
 public boolean canRead(SupportTicketActor a,Map<String,Object> t){
   return a.cs() || (a.requester() && a.userId().equals(t.get("user_id"))) || (a.manager() && a.userId().equals(t.get("escalated_to")));
 }
 public void read(SupportTicketActor a,Map<String,Object> t){if(t==null || !canRead(a,t))throw new BusinessException(404,"Không tìm thấy yêu cầu hỗ trợ hoặc bạn không có quyền truy cập.");}
 public List<String> actions(SupportTicketActor a,Map<String,Object> t){
   if(!canRead(a,t))return List.of();
   String status=t.get("status").toString();if(status.equals("CLOSED"))return List.of();
   var actions=new ArrayList<String>();boolean owner=a.requester()&&a.userId().equals(t.get("user_id"));
   boolean assigned=a.cs()&&a.userId().equals(t.get("assigned_to"));
   if(owner||assigned)actions.add("message");
   if(owner&&status.equals("RESOLVED"))actions.add("close");
   if(a.cs()&&status.equals("OPEN")&&t.get("assigned_to")==null)actions.add("claim");
   if(a.cs()&&t.get("assigned_to")!=null)actions.add("assignment");
   if(assigned){actions.add("priority");if(status.equals("IN_PROGRESS")){actions.add("resolve");actions.add("escalate");}if(status.equals("ESCALATED"))actions.add("recipient");}
   if(a.manager()&&status.equals("ESCALATED")){actions.add("decision");actions.add("return");}
   return List.copyOf(actions);
 }
 public void command(SupportTicketActor a,Map<String,Object> t,String action){
   read(a,t);
   boolean owner=a.requester()&&a.userId().equals(t.get("user_id")),assigned=a.cs()&&a.userId().equals(t.get("assigned_to"));
   boolean authorized=switch(action){case "message"->owner||assigned;case "close"->owner;case "claim","assignment"->a.cs();case "priority","resolve","escalate","recipient"->assigned;case "decision","return"->a.manager()&&a.userId().equals(t.get("escalated_to"));default->false;};
   if(!authorized)throw new BusinessException(403,"Bạn không có quyền thực hiện thao tác này.");
   if(!actions(a,t).contains(action))throw new BusinessException(409,"Trạng thái yêu cầu đã thay đổi. Vui lòng tải lại trước khi thao tác.");
 }
}
