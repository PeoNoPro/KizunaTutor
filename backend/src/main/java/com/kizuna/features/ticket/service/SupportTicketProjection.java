package com.kizuna.features.ticket.service;
import com.kizuna.features.ticket.dto.SupportTicketActor;
import org.springframework.stereotype.Service;
import java.util.*;
@Service
public class SupportTicketProjection {
 public static final Map<String,String> STATUS=Map.of("OPEN","Mới tiếp nhận","IN_PROGRESS","Đang xử lý","ESCALATED","Đã chuyển quản lý","RESOLVED","Đã giải quyết","CLOSED","Đã đóng");
 public static final Map<String,String> ROLE=Map.of("ROLE_LEARNER","Học viên","ROLE_TUTOR","Gia sư tiếng Nhật","ROLE_CS_STAFF","Chăm sóc khách hàng","ROLE_MANAGER","Quản lý vận hành");
 public static final Map<String,String> PRIORITY=Map.of("LOW","Thấp","MEDIUM","Bình thường","HIGH","Cao","URGENT","Khẩn cấp");
 public static final Map<String,String> CATEGORY=Map.of("ACCOUNT","Tài khoản","BILLING","Thanh toán","TUTOR","Kết nối gia sư","BOOKING","Đặt học thử","MEMBERSHIP","Hội viên","TECHNICAL","Sự cố kỹ thuật","OTHER","Nội dung khác");
 private final SupportTicketAccessPolicy policy;
 public SupportTicketProjection(SupportTicketAccessPolicy policy){this.policy=policy;}
 public Map<String,Object> summary(SupportTicketActor actor,Map<String,Object> row){
   var result=new LinkedHashMap<String,Object>();copy(result,row,"ticketId","ticket_id","subject","subject","category","category","priority","priority","status","status","createdAt","created_at","updatedAt","updated_at","version","version");
   result.put("categoryLabel",CATEGORY.get(row.get("category")));result.put("priorityLabel",PRIORITY.get(row.get("priority")));result.put("statusLabel",STATUS.get(row.get("status")));
   result.put("allowedActions",policy.actions(actor,row));
   if(!actor.requester()){result.put("requesterDisplayName",row.get("requester_name"));result.put("assigneeId",row.get("assigned_to"));result.put("recipientId",row.get("escalated_to"));result.put("assigneeDisplayName",row.get("assignee_name"));result.put("assigneeActive",row.get("assignee_active"));result.put("needsHandover",SupportTicketAccessPolicy.needsHandover(row));result.put("recipientDisplayName",row.get("recipient_name"));}
   return result;
 }
 public static void copy(Map<String,Object> target,Map<String,Object> source,String... fields){for(int i=0;i<fields.length;i+=2)target.put(fields[i],source.get(fields[i+1]));}
 public List<Map<String,String>> historyChanges(Map<String,Object> row){
   var result=new ArrayList<Map<String,String>>();String[][] fields={{"status","Trạng thái"},{"priority","Ưu tiên"},{"assignee","Người phụ trách"},{"recipient","Quản lý tiếp nhận"}};
   for(var field:fields){String before=Objects.toString(row.get("previous_"+field[0]),""),after=Objects.toString(row.get("current_"+field[0]),"");if(before.equals(after))continue;
     if(field[0].equals("status")){before=STATUS.getOrDefault(before,"Chưa có");after=STATUS.getOrDefault(after,"Chưa có");}
     if(field[0].equals("priority")){before=PRIORITY.getOrDefault(before,"Chưa có");after=PRIORITY.getOrDefault(after,"Chưa có");}
     result.add(Map.of("label",field[1],"before",before.isEmpty()?"Chưa phân công":before,"after",after.isEmpty()?"Chưa phân công":after));
   }return result;
 }
}
