package com.kizuna.features.admin.service;
import com.kizuna.core.exception.BusinessException;
import com.kizuna.core.entity.User;
import com.kizuna.features.admin.dto.OperationsActor;
import com.kizuna.features.admin.repository.OperationsIdentityRepository;
import org.springframework.stereotype.Service;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.dao.DataAccessException;
import lombok.RequiredArgsConstructor;
import java.util.UUID;
@Service @RequiredArgsConstructor
public class OperationsActorResolver {
    private final OperationsIdentityRepository identities;
    private static final String REQUEST_ACTOR=OperationsActorResolver.class.getName()+".actor";
    /** Mỗi request chỉ tra DB một lần: bộ lọc quyền và service cùng hỏi, mà pool kiểm tra quyền rất nhỏ. Sang request sau vẫn đọc lại trạng thái mới nhất. */
    public OperationsActor current() {
        var request=org.springframework.web.context.request.RequestContextHolder.getRequestAttributes();
        int scope=org.springframework.web.context.request.RequestAttributes.SCOPE_REQUEST;
        if(request!=null&&request.getAttribute(REQUEST_ACTOR,scope) instanceof OperationsActor cached) return cached;
        var actor=lookup();
        if(request!=null) request.setAttribute(REQUEST_ACTOR,actor,scope);
        return actor;
    }
    private OperationsActor lookup() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            throw new BusinessException(401,"Vui lòng đăng nhập để tiếp tục.");
        }
        UUID id = auth.getPrincipal() instanceof User u ? u.getUserId() : null;
        try { return identities.find(auth.getName(), id); }
        catch (DataAccessException ex) { throw new BusinessException(503,"Tạm thời chưa xác minh được quyền truy cập. Vui lòng thử lại sau."); }
    }
    public OperationsActor requireAdmin() {
        var actor = current();
        if (!"ROLE_ADMIN".equals(actor.role())) throw new BusinessException(403,"Bạn không có quyền sử dụng chức năng này.");
        return actor;
    }
    public OperationsActor requireManager() {
        var actor = current();
        if (!"ROLE_MANAGER".equals(actor.role())) throw new BusinessException(403,"Bạn không có quyền sử dụng chức năng này.");
        return actor;
    }
    /** Áp dụng cho API FE-11; các feature khác cần chặn ở filter chung của FE-01. */
    public void rejectPendingPasswordChange() {
        if(current().mustChangePassword()) throw new BusinessException(403,"Vui lòng đổi mật khẩu lần đầu trước khi sử dụng chức năng này.");
    }
    public OperationsActor requireReader() {
        var actor = current();
        if (!java.util.Set.of("ROLE_LEARNER","ROLE_TUTOR").contains(actor.role())) throw new BusinessException(403,"Bạn không có quyền đọc nội dung này.");
        return actor;
    }
    public OperationsActor requireReporter() {
        var actor=current();if(!java.util.Set.of("ROLE_ADMIN","ROLE_MANAGER").contains(actor.role()))throw new BusinessException(403,"Bạn không có quyền xem báo cáo tổng hợp.");return actor;
    }
    public OperationsActor requireStaff() {
        var actor=current();if(!java.util.Set.of("ROLE_ADMIN","ROLE_MANAGER","ROLE_CS_STAFF","ROLE_ACADEMIC_OFFICER").contains(actor.role()))throw new BusinessException(403,"Bạn không có quyền vào khu vực nội bộ.");return actor;
    }
    public OperationsActor readerForShell(String shell) {
        var actor=requireReader();
        if(!actor.role().equals("ROLE_"+shell.toUpperCase(java.util.Locale.ROOT))) throw new BusinessException(403,"Vui lòng mở trang chính sách dành cho tài khoản của bạn.");
        return actor;
    }
    public OperationsActor activeAdmin(UUID id) {
        var actor = identities.find(null,id);
        if (!"ROLE_ADMIN".equals(actor.role())) throw BusinessException.invalid();
        return actor;
    }
    public String displayName(UUID id) { return identities.displayName(id); }
    public java.util.Map<String,Object> administratorProfile() {
        var actor=requireStaff();var roles=java.util.Map.of("ROLE_ADMIN","Quản trị viên hệ thống","ROLE_MANAGER","Quản lý vận hành","ROLE_ACADEMIC_OFFICER","Cán bộ học vụ","ROLE_CS_STAFF","Chăm sóc khách hàng");
        return java.util.Map.of("displayName",actor.fullName(),"roleLabel",roles.get(actor.role()),"avatarUrl",identities.avatarUrl(actor.userId()),"canAdmin",actor.role().equals("ROLE_ADMIN"),"canReport",java.util.Set.of("ROLE_ADMIN","ROLE_MANAGER").contains(actor.role()),"canSupport",java.util.Set.of("ROLE_CS_STAFF","ROLE_MANAGER").contains(actor.role()));
    }
    public java.util.Map<String,Object> personalProfile() {var actor=requireStaff();return identities.personalProfile(actor.userId(),(String)administratorProfile().get("roleLabel"));}
    public java.util.Map<UUID,String> names(java.util.Collection<UUID> ids) { return identities.names(ids); }
    public java.util.List<java.util.Map<String,Object>> admins() { requireAdmin(); return identities.admins(); }
}
