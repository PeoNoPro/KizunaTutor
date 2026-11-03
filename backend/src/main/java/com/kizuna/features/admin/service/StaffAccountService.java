package com.kizuna.features.admin.service;

import com.kizuna.core.exception.BusinessException;
import com.kizuna.features.admin.dto.StaffAccountRequests.*;
import com.kizuna.features.admin.repository.StaffAccountRepository;
import com.kizuna.features.admin.repository.StaffAccountRepository.Account;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Service @RequiredArgsConstructor
public class StaffAccountService {
    private final StaffAccountRepository staff;
    private final OperationsActorResolver actors;
    private final OperationsCommandExecutor locks;
    private final Clock operationsClock;
    private static final Map<String,String> ROLES=Map.of("ROLE_ADMIN","Quản trị viên hệ thống","ROLE_MANAGER","Quản lý vận hành","ROLE_ACADEMIC_OFFICER","Cán bộ học vụ","ROLE_CS_STAFF","Chăm sóc khách hàng","ROLE_LEARNER","Học viên","ROLE_TUTOR","Gia sư");
    private static final Set<String> STAFF_ROLES=Set.of("ROLE_ADMIN","ROLE_MANAGER","ROLE_ACADEMIC_OFFICER","ROLE_CS_STAFF");
    private static final Map<String,String> STATES=Map.of("ACTIVE","Đang hoạt động","SUSPENDED","Tạm khóa","PENDING","Chờ kích hoạt","BANNED","Đã bị khóa","INACTIVE","Ngừng hoạt động");
    public static final Map<String,String> ACTION_LABELS=Map.of("CREATE","Tạo nhân viên","UPDATE","Sửa thông tin","ROLE","Đổi vai trò","STATUS","Đổi trạng thái tài khoản");
    private OffsetDateTime now() { return OffsetDateTime.now(operationsClock).truncatedTo(ChronoUnit.MICROS); }
    private OffsetDateTime nextTime(Account account) { var time=now();return time.isAfter(account.updatedAt())?time:account.updatedAt().plusNanos(1000); }
    private Map<String,Object> view(Account a,UUID actor) {
        var result=new LinkedHashMap<String,Object>();
        result.put("userId",a.id());result.put("fullName",a.name());result.put("email",a.email());result.put("phone",a.phone());
        result.put("roleId",a.roleId());result.put("roleLabel",ROLES.get(a.role()));result.put("status",a.status());
        result.put("statusLabel",STATES.getOrDefault(a.status(),"Cần kiểm tra trạng thái"));result.put("updatedAt",a.updatedAt().toString());result.put("self",actor.equals(a.id()));return result;
    }
    @Transactional(readOnly=true,timeout=3)
    public Map<String,Object> list(String q,int page,int size) {
        var actor=actors.requireAdmin();int offset=OperationsValidation.page(page,size);
        if(q==null)q="";if(q.length()>200)throw BusinessException.invalid();
        return Map.of("items",staff.list(q.trim(),offset,size).stream().map(a->view(a,actor.userId())).toList(),"page",page,"size",size,"totalElements",staff.count(q.trim()));
    }
    @Transactional(readOnly=true,timeout=3)
    public List<Map<String,Object>> roles() {
        actors.requireAdmin();return staff.roles().stream().map(r->Map.<String,Object>of("roleId",r[0],"label",ROLES.get((String)r[1]),"active",r[2],"staffAssignable",STAFF_ROLES.contains((String)r[1]),"accountCount",r[3],
            "capabilities","ROLE_ADMIN".equals(r[1])?List.of("Quản lý nhân viên và gán vai trò","Cấu hình học thử","Giám sát hệ thống","Xử lý sự cố và xem báo cáo vận hành","Xem báo cáo tổng hợp"):
                "ROLE_MANAGER".equals(r[1])?List.of("Xem báo cáo tổng hợp người dùng, hoạt động và kết nối","Xem hồ sơ tài khoản của mình","Không sửa cấu hình, nhân viên hoặc sự cố"):
                List.of("Không có quyền vào khu vực quản trị vận hành","Quyền nghiệp vụ khác do phân hệ tương ứng kiểm tra"))).toList();
    }
    private UUID beginWrite() {
        staff.serializeWrites();var actor=actors.requireAdmin();locks.lockAdmin(actor.userId());return actor.userId();
    }
    private Account checked(UUID id,OffsetDateTime expected) {
        var a=staff.get(id,true);
        if(!a.updatedAt().toInstant().equals(expected.toInstant()))throw new BusinessException(409,"Tài khoản đã thay đổi. Vui lòng tải lại trước khi lưu.");return a;
    }
    private void uniqueEmail(String email,UUID id) {
        if(staff.emailExists(email,id))throw new BusinessException(400,"Email này đã được sử dụng. Vui lòng chọn email khác.");
    }
    @Transactional(timeout=5)
    public Map<String,Object> create(Create r) {
        if(r.initialPassword().getBytes(StandardCharsets.UTF_8).length>72)throw new BusinessException(400,"Mật khẩu tối đa 72 byte; vui lòng dùng mật khẩu ngắn hơn.");
        // Tính hash trước khi giữ khóa dữ liệu.
        String hash=new BCryptPasswordEncoder(12).encode(r.initialPassword());
        UUID actor=beginWrite(),id=UUID.randomUUID();String email=r.email().trim().toLowerCase(Locale.ROOT);uniqueEmail(email,id);staff.activeRole(r.roleId());var time=now();
        staff.create(id,r.fullName().trim(),email,r.phone()==null?null:r.phone().trim(),r.roleId(),hash,time);
        staff.audit(actor,id,"CREATE","Tạo tài khoản nhân viên",time);return view(staff.get(id,false),actor);
    }
    @Transactional(timeout=5)
    public Map<String,Object> update(UUID id,Update r) {
        UUID actor=beginWrite();var a=checked(id,r.expectedUpdatedAt());String email=r.email().trim().toLowerCase(Locale.ROOT);
        if(actor.equals(id)&&!a.email().equalsIgnoreCase(email))throw new BusinessException(400,"Không đổi email của tài khoản đang sử dụng trong màn hình này.");
        uniqueEmail(email,id);var time=nextTime(a);staff.update(id,r.fullName().trim(),email,r.phone()==null?null:r.phone().trim(),time);staff.audit(actor,id,"UPDATE",r.reason().trim(),time);return view(staff.get(id,false),actor);
    }
    private void protect(Account a,UUID actor,boolean removesAdmin) {
        if(actor.equals(a.id()))throw new BusinessException(400,"Không thay đổi vai trò hoặc khóa chính tài khoản đang sử dụng.");
        if(removesAdmin&&a.role().equals("ROLE_ADMIN")&&a.status().equals("ACTIVE")&&a.roleActive()&&staff.activeAdmins()<=1)
            throw new BusinessException(400,"Cần giữ ít nhất một quản trị viên đang hoạt động.");
    }
    @Transactional(timeout=5)
    public Map<String,Object> role(UUID id,Command r) {
        UUID actor=beginWrite();var a=checked(id,r.expectedUpdatedAt());if(r.roleId()==null)throw BusinessException.invalid();
        String role=staff.activeRole(r.roleId());protect(a,actor,!role.equals("ROLE_ADMIN"));var time=nextTime(a);staff.changeRole(id,r.roleId(),time);staff.audit(actor,id,"ROLE",r.reason().trim(),time);return view(staff.get(id,false),actor);
    }
    @Transactional(timeout=5)
    public Map<String,Object> status(UUID id,Command r) {
        UUID actor=beginWrite();var a=checked(id,r.expectedUpdatedAt());
        if(!Set.of("ACTIVE","SUSPENDED").contains(r.status()==null?"":r.status())||!Set.of("ACTIVE","SUSPENDED").contains(a.status()))throw BusinessException.invalid();
        protect(a,actor,!r.status().equals("ACTIVE"));if(r.status().equals("ACTIVE"))staff.activeRole(a.roleId());
        var time=nextTime(a);staff.changeStatus(id,r.status(),time);staff.audit(actor,id,"STATUS",r.reason().trim(),time);return view(staff.get(id,false),actor);
    }
    @Transactional(readOnly=true,timeout=3)
    public Map<String,Object> history(int page,int size) {
        actors.requireAdmin();int offset=OperationsValidation.page(page,size);
        return Map.of("items",staff.history(offset,size).stream().map(r->Map.of("actorName",r[0],"targetName",r[1],"actionLabel",ACTION_LABELS.get(r[2]),"reason",r[3],"occurredAt",r[4])).toList(),"page",page,"size",size,"totalElements",staff.historyCount());
    }
}
