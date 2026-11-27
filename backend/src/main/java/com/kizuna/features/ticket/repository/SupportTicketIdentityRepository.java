package com.kizuna.features.ticket.repository;
import com.kizuna.features.ticket.dto.SupportTicketActor;
import com.kizuna.core.exception.BusinessException;
import org.springframework.stereotype.Repository;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import java.util.*;
@Repository
public class SupportTicketIdentityRepository {
 private final JdbcTemplate jdbc;
 public SupportTicketIdentityRepository(@Qualifier("ticketJdbc") JdbcTemplate jdbc){this.jdbc=jdbc;}
 public SupportTicketActor find(String email,UUID id,boolean lock){
   var rows=jdbc.query("SELECT u.user_id,r.role_name,u.full_name FROM users u JOIN roles r ON r.role_id=u.role_id WHERE u.account_status::text='ACTIVE' AND u.deleted_at IS NULL AND r.is_active=true AND "+(id==null?"u.email=?":"u.user_id=?")+(lock?" FOR SHARE OF u,r":""),
      (rs,n)->new SupportTicketActor(rs.getObject(1,UUID.class),rs.getString(2),rs.getString(3)),id==null?email:id);
   if(rows.isEmpty())throw new BusinessException(403,"Tài khoản hiện không có quyền sử dụng chức năng này.");
   return rows.get(0);
 }
 public List<Map<String,Object>> candidates(String role){return jdbc.query("SELECT u.user_id,u.full_name FROM users u JOIN roles r ON r.role_id=u.role_id WHERE u.account_status::text='ACTIVE' AND u.deleted_at IS NULL AND r.is_active AND r.role_name=? ORDER BY u.full_name,u.user_id",(rs,n)->Map.of("userId",rs.getObject(1),"displayName",rs.getString(2)),role);}
}
