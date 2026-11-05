package com.kizuna.features.admin.service;

import com.kizuna.core.exception.BusinessException;
import com.kizuna.features.admin.dto.StaffAccountRequests.*;
import com.kizuna.features.admin.repository.StaffAccountRepository;
import com.kizuna.features.admin.repository.StaffAccountRepository.Account;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service @RequiredArgsConstructor
public class StaffAccountService {
    private final StaffAccountRepository staff;
    private final OperationsActorResolver actors;
    private final OperationsCommandExecutor locks;
    private final StaffCredentialService credentials;
    private final com.kizuna.features.admin.repository.StaffCredentialRepository emails;
    private static final Map<String,String> EMAIL_STATES=Map.of("PENDING","Email đang chờ gửi","SENT","Đã gửi email","FAILED","Gửi email thất bại");
    private final Clock operationsClock;
    static final Map<String,String> ROLES=Map.of("ROLE_ADMIN","Quản trị viên hệ thống","ROLE_MANAGER","Quản lý vận hành","ROLE_ACADEMIC_OFFICER","Cán bộ học vụ","ROLE_CS_STAFF","Chăm sóc khách hàng","ROLE_LEARNER","Học viên","ROLE_TUTOR","Gia sư");
    // Admin chỉ gán vai trò lúc tạo và không cấp quyền Admin; đổi vai trò sau tạo thuộc StaffRoleChangeService.
    private static final Set<String> CREATABLE_ROLES=Set.of("ROLE_MANAGER","ROLE_ACADEMIC_OFFICER","ROLE_CS_STAFF");
    private static final Map<String,String> STATES=Map.of("ACTIVE","Đang hoạt động","SUSPENDED","Tạm khóa","PENDING","Chờ kích hoạt","BANNED","Đã bị khóa","INACTIVE","Ngừng hoạt động");
    // Theo mô tả vai trò trong RoleEnum và quyền đang thực thi; thay đổi quyền thì cập nhật cùng lúc.
    private static final Map<String,List<String>> DUTIES=Map.of(
        "ROLE_ADMIN",List.of("Quản trị tài khoản nhân viên: tạo, nhập Excel, sửa thông tin, khóa và mở khóa","Duyệt hoặc từ chối đề xuất thăng chức","Cấu hình hệ thống và quản lý sự cố kỹ thuật","Xem thống kê, nhật ký, báo cáo và doanh thu"),
        "ROLE_MANAGER",List.of("Điều hành kinh doanh: xem doanh thu, gói hội viên và báo cáo tổng hợp","Điều chuyển nhân viên giữa hai vai trò, đề xuất thăng chức","Xử lý yêu cầu hỗ trợ được chuyển lên","Không tạo, khóa tài khoản và không sửa cấu hình"),
        "ROLE_CS_STAFF",List.of("Tiếp nhận và giải quyết yêu cầu hỗ trợ, khiếu nại của học viên và gia sư","Hỏi thêm thông tin, hướng dẫn và ghi kết quả xử lý","Chuyển yêu cầu vượt thẩm quyền lên Quản lý"),
        "ROLE_ACADEMIC_OFFICER",List.of("Thẩm định hồ sơ, bằng cấp và chứng chỉ JLPT của gia sư","Kiểm duyệt, chuẩn hóa từ điển Kanji"),
        "ROLE_TUTOR",List.of("Quản lý hồ sơ, lịch dạy và lớp học trực tuyến","Nhận yêu cầu kết nối, dạy học thử","Giao và theo dõi bài tập của học viên","Đăng ký gói hội viên gia sư"),
        "ROLE_LEARNER",List.of("Tìm gia sư, gửi yêu cầu kết nối và đặt lịch học thử","Tham gia lớp học, làm bài tập","Luyện viết Kanji và học từ vựng, ngữ pháp","Gửi yêu cầu hỗ trợ khi cần"));
    public static final Map<String,String> ACTION_LABELS=Map.of("CREATE","Tạo nhân viên","UPDATE","Sửa thông tin","ROLE","Đổi vai trò","STATUS","Đổi trạng thái tài khoản","PROPOSE","Đề xuất thăng chức","REJECT","Từ chối đề xuất thăng chức","CANCEL","Hủy đề xuất thăng chức");
    static final String LOCKED_EMAIL_MESSAGE="Email này thuộc tài khoản nhân viên đang bị khóa. Hãy mở lại tài khoản đó thay vì tạo mới.";
    OffsetDateTime now() { return OffsetDateTime.now(operationsClock).truncatedTo(ChronoUnit.MICROS); }
    OffsetDateTime nextTime(Account account) { var time=now();return time.isAfter(account.updatedAt())?time:account.updatedAt().plusNanos(1000); }
    private Map<String,Object> view(Account a,UUID actor) {
        var result=new LinkedHashMap<String,Object>();
        result.put("userId",a.id());result.put("fullName",a.name());result.put("email",a.email());result.put("phone",a.phone());
        result.put("roleId",a.roleId());result.put("roleLabel",ROLES.get(a.role()));result.put("status",a.status());
        result.put("statusLabel",STATES.getOrDefault(a.status(),"Cần kiểm tra trạng thái"));result.put("updatedAt",a.updatedAt().toString());result.put("self",actor.equals(a.id()));result.put("mustChangePassword",a.mustChangePassword());result.put("emailStatus",a.emailStatus());result.put("emailStatusLabel",a.emailStatus()==null?null:EMAIL_STATES.get(a.emailStatus()));result.put("emailSentAt",a.emailSentAt());return result;
    }
    @Transactional(readOnly=true,timeout=3)
    public Map<String,Object> list(String q,int page,int size) {
        var actor=actors.requireAdmin();int offset=OperationsValidation.page(page,size);
        if(q==null)q="";if(q.length()>200)throw BusinessException.invalid();
        return Map.of("items",staff.list(q.trim(),offset,size).stream().map(a->view(a,actor.userId())).toList(),"page",page,"size",size,"totalElements",staff.count(q.trim()),"emailCounts",emails.counts());
    }
    @Transactional(readOnly=true,timeout=3)
    public List<Map<String,Object>> roles() {
        actors.requireAdmin();return staff.roles().stream().map(r->Map.<String,Object>of("roleId",r[0],"label",ROLES.get((String)r[1]),"active",r[2],"staffAssignable",CREATABLE_ROLES.contains((String)r[1]),"internal",CREATABLE_ROLES.contains((String)r[1])||"ROLE_ADMIN".equals(r[1]),"accountCount",r[3],
            "capabilities",DUTIES.get((String)r[1]))).toList();
    }
    UUID beginWrite() {
        staff.serializeWrites();var actor=actors.requireAdmin();locks.lockAdmin(actor.userId());return actor.userId();
    }
    Account checked(UUID id,OffsetDateTime expected) {
        var a=staff.get(id,true);
        if(!a.updatedAt().toInstant().equals(expected.toInstant()))throw new BusinessException(409,"Tài khoản đã thay đổi. Vui lòng tải lại trước khi lưu.");return a;
    }
    private void uniqueEmail(String email,UUID id) {
        if(!staff.emailExists(email,id))return;
        if(staff.lockedStaffEmails(List.of(email)).contains(email))throw new BusinessException(400,LOCKED_EMAIL_MESSAGE);
        throw new BusinessException(400,"Email này đã được sử dụng. Vui lòng chọn email khác.");
    }
    @Transactional(timeout=5)
    public Map<String,Object> create(Create r) {
        UUID actor=beginWrite(),id=UUID.randomUUID();String email=r.email().trim().toLowerCase(Locale.ROOT);uniqueEmail(email,id);String createdRole=staff.activeRole(r.roleId());if(!CREATABLE_ROLES.contains(createdRole))throw new BusinessException(400,"Không cấp quyền Quản trị viên khi tạo tài khoản.");
        boolean manager=createdRole.equals("ROLE_MANAGER");String appointment=r.appointmentReason()==null?"":r.appointmentReason().trim();
        if(manager&&appointment.isEmpty())throw new BusinessException(400,"Vui lòng nhập lý do hoặc căn cứ bổ nhiệm Quản lý.");
        var time=now();
        staff.create(id,r.fullName().trim(),email,r.phone()==null?null:r.phone().trim(),r.roleId(),StaffCredentialService.UNUSABLE_HASH,time);
        staff.audit(actor,id,"CREATE",manager?"Tạo tài khoản Quản lý. Căn cứ: "+appointment:"Tạo tài khoản nhân viên",time);credentials.enqueue(id,actor,time);return view(staff.get(id,false),actor);
    }
    @Transactional(timeout=5)
    public Map<String,Object> update(UUID id,Update r) {
        UUID actor=beginWrite();var a=checked(id,r.expectedUpdatedAt());String email=r.email().trim().toLowerCase(Locale.ROOT);
        if(actor.equals(id)&&!a.email().equalsIgnoreCase(email))throw new BusinessException(400,"Không đổi email của tài khoản đang sử dụng trong màn hình này.");
        uniqueEmail(email,id);var time=nextTime(a);staff.update(id,r.fullName().trim(),email,r.phone()==null?null:r.phone().trim(),time);staff.audit(actor,id,"UPDATE",r.reason().trim(),time);return view(staff.get(id,false),actor);
    }
    /** Chỉ cấp lại cho người chưa đổi mật khẩu lần đầu, để Admin không thể chiếm tài khoản đang được dùng. */
    @Transactional(timeout=5)
    public Map<String,Object> resendCredentials(UUID id,Resend r) {
        UUID actor=beginWrite();var a=checked(id,r.expectedUpdatedAt());
        if(actor.equals(id))throw new BusinessException(400,"Không cấp lại mật khẩu cho tài khoản đang sử dụng.");
        if(!a.mustChangePassword())throw new BusinessException(409,"Nhân viên này đã đổi mật khẩu nên không thể cấp lại.");
        if(!a.status().equals("ACTIVE"))throw new BusinessException(409,"Chỉ cấp lại cho tài khoản đang hoạt động.");
        var time=nextTime(a);staff.resetPassword(id,StaffCredentialService.UNUSABLE_HASH,time);staff.audit(actor,id,"UPDATE","Cấp lại mật khẩu tạm qua email",time);
        credentials.enqueue(id,actor,time);
        return view(staff.get(id,false),actor);
    }
    private void protect(Account a,UUID actor,boolean removesAdmin) {
        if(actor.equals(a.id()))throw new BusinessException(400,"Không thay đổi vai trò hoặc khóa chính tài khoản đang sử dụng.");
        if(removesAdmin&&a.role().equals("ROLE_ADMIN")&&a.status().equals("ACTIVE")&&a.roleActive()&&staff.activeAdmins()<=1)
            throw new BusinessException(400,"Cần giữ ít nhất một quản trị viên đang hoạt động.");
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
