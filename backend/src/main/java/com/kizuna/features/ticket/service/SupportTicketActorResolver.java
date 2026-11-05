package com.kizuna.features.ticket.service;
import com.kizuna.features.ticket.repository.SupportTicketIdentityRepository;
import com.kizuna.features.ticket.dto.SupportTicketActor;
import com.kizuna.core.entity.User;
import com.kizuna.core.exception.BusinessException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;
import java.util.*;
@Service @RequiredArgsConstructor
public class SupportTicketActorResolver {
 private final SupportTicketIdentityRepository identities;
 public SupportTicketActor current(boolean lock){
   var auth=SecurityContextHolder.getContext().getAuthentication();
   if(auth==null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal()))throw new BusinessException(401,"Vui lòng đăng nhập để tiếp tục.");
   var a=identities.find(auth.getName(),auth.getPrincipal() instanceof User u?u.getUserId():null,lock);
   if(!a.requester()&&!a.cs()&&!a.manager())throw new BusinessException(403,"Tài khoản của bạn không thuộc bộ phận hỗ trợ hoặc người gửi yêu cầu.");
   return a;
 }
 public void recipient(UUID id,String role){
   try{if(id==null || !identities.find(null,id,true).role().equals(role))throw new BusinessException(400,"Người nhận không còn hoạt động hoặc không phù hợp.");}
   catch(BusinessException e){if(e.status()==403)throw new BusinessException(400,"Người nhận không còn hoạt động hoặc không phù hợp.");throw e;}
 }
 public List<Map<String,Object>> candidates(boolean managers){if(!current(false).cs())throw new BusinessException(403,"Bạn không có quyền xem danh sách này.");return identities.candidates(managers?"ROLE_MANAGER":"ROLE_CS_STAFF");}
}
