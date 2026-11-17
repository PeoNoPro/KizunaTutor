package com.kizuna.features.ticket.service;
import com.kizuna.features.ticket.dto.SupportTicketActor;
import com.kizuna.core.exception.BusinessException;
import org.springframework.stereotype.Service;
import java.util.*;

/**
 * Quyền trên yêu cầu hỗ trợ (đổi 2026-11-16 theo ý kiến giảng viên: bỏ phân ca, nhóm chỉ có 1 nhân viên hỗ trợ).
 * <ul>
 *   <li>Mọi yêu cầu vào một hàng chờ chung. Nhân viên hỗ trợ (CS) đang hoạt động nào cũng xử lý được mọi yêu cầu;
 *       người phụ trách chỉ để ghi lại và hiển thị, không dùng để chặn quyền (đổi người CS thì người mới làm tiếp được ngay).</li>
 *   <li>"Nhận xử lý" (claim) khi yêu cầu còn Mới tiếp nhận; CS trả lời luôn thì hệ thống tự tính là đã nhận.</li>
 *   <li>Quản lý chỉ xem yêu cầu được chuyển cho mình; người gửi chỉ xem yêu cầu của mình.</li>
 * </ul>
 */
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
   if(owner){actions.add("message");if(status.equals("RESOLVED"))actions.add("close");}
   if(a.cs()){
     actions.add("message");actions.add("priority");
     if(status.equals("OPEN"))actions.add("claim");
     if(status.equals("IN_PROGRESS")){actions.add("resolve");actions.add("escalate");}
     if(status.equals("ESCALATED"))actions.add("recipient");
   }
   if(a.manager()&&a.userId().equals(t.get("escalated_to"))&&status.equals("ESCALATED")){actions.add("decision");actions.add("return");}
   return List.copyOf(actions);
 }
 public void command(SupportTicketActor a,Map<String,Object> t,String action){
   read(a,t);
   boolean owner=a.requester()&&a.userId().equals(t.get("user_id"));
   boolean authorized=switch(action){case "message"->owner||a.cs();case "close"->owner;case "claim","priority","resolve","escalate","recipient"->a.cs();case "decision","return"->a.manager()&&a.userId().equals(t.get("escalated_to"));default->false;};
   if(!authorized)throw new BusinessException(403,"Bạn không có quyền thực hiện thao tác này.");
   if(!actions(a,t).contains(action))throw new BusinessException(409,"Trạng thái yêu cầu đã thay đổi. Vui lòng tải lại trước khi thao tác.");
 }
}
