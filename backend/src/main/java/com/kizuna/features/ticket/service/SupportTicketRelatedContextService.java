package com.kizuna.features.ticket.service;
import com.kizuna.features.ticket.repository.SupportTicketContextRepository;
import com.kizuna.features.ticket.dto.SupportTicketRequests.Create;
import com.kizuna.core.exception.BusinessException;
import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;
import java.util.*;
@Service @RequiredArgsConstructor
public class SupportTicketRelatedContextService {
 private static final Map<String,String> TYPES=Map.of("booking","Lịch học thử","membership","Gói hội viên","payment","Thanh toán gói");
 private static final Map<String,Map<String,String>> STATES=Map.of(
   "booking",Map.of("PENDING","Chờ xác nhận","CONFIRMED","Đã xác nhận","COMPLETED","Đã hoàn thành","CANCELLED","Đã hủy","RESCHEDULED","Đã đổi lịch"),
   "membership",Map.of("ACTIVE","Đang hiệu lực","EXPIRED","Đã hết hạn","CANCELLED","Đã hủy"),
   "payment",Map.of("PENDING","Đang chờ thanh toán","SUCCESS","Thành công","FAILED","Thất bại","REFUNDED","Đã hoàn tiền"));
 private final SupportTicketContextRepository repository;
 public void validate(UUID owner,Create r){
   if(r.relatedBookingId()!=null)require("booking",owner,r.relatedBookingId());
   Map<String,Object> membership=r.relatedMembershipId()==null?null:require("membership",owner,r.relatedMembershipId());
   Map<String,Object> payment=r.relatedTransactionId()==null?null:require("payment",owner,r.relatedTransactionId());
   if(membership!=null&&payment!=null&&!Objects.equals(membership.get("plan_id"),payment.get("plan_id")))throw new BusinessException(400,"Gói hội viên và giao dịch được chọn không khớp nhau.");
 }
 private Map<String,Object> require(String type,UUID owner,UUID id){var item=repository.find(type,owner,id);if(item==null)throw new BusinessException(400,"Thông tin liên quan không tồn tại hoặc không thuộc tài khoản của bạn.");return item;}
 public Map<String,Object> candidates(String type,UUID owner,int page,int size){SupportTicketValidation.page(page,size);return SupportTicketValidation.page(repository.list(type,owner,page,size).stream().map(x->projection(type,x)).toList(),page,size,repository.count(type,owner));}
 public List<Map<String,Object>> detail(Map<String,Object> t){var result=new ArrayList<Map<String,Object>>();String[][] types={{"booking","related_booking_id"},{"membership","related_membership_id"},{"payment","related_transaction_id"}};
   var original=repository.originalLinks((UUID)t.get("ticket_id"));
   for(var type:types){UUID id=(UUID)t.get(type[1]);if(id==null)id=(UUID)original.get(type[1]);if(id!=null){var item=repository.find(type[0],(UUID)t.get("user_id"),id);result.add(item==null?Map.of("type",type[0],"available",false,"label","Thông tin liên quan không còn được cung cấp"):projection(type[0],item));}}return result;
 }
 private Map<String,Object> projection(String type,Map<String,Object> item){return Map.of("type",type,"contextId",item.get("id"),"label",TYPES.get(type)+" "+item.get("label")+" · "+STATES.get(type).getOrDefault(item.get("state"),"Chưa rõ trạng thái"),"available",true);}
}
