package com.kizuna.features.admin.service;
import jakarta.servlet.http.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
/** Verify current identity before a controller can start a business-pool transaction. */
@Component @RequiredArgsConstructor
public class OperationsAccessPolicy implements HandlerInterceptor {
    private final OperationsActorResolver actors;
    @Override public boolean preHandle(HttpServletRequest request,HttpServletResponse response,Object handler) {
        String path=request.getRequestURI().substring(request.getContextPath().length());
        if(path.startsWith("/learner/policies")) actors.readerForShell("learner");
        else if(path.startsWith("/tutor/policies")) actors.readerForShell("tutor");
        else if(path.startsWith("/api/policies")) actors.requireReader();
        else if(path.equals("/api/admin/operations/management-report")) actors.requireReporter();
        // Service kiểm tra tiếp từng thao tác: Manager đổi vai trò/đề xuất, Admin duyệt.
        else if(path.startsWith("/api/admin/operations/role-changes/") || path.startsWith("/api/admin/operations/reports/business/")) actors.requireReporter();
        // FE-12: Admin và Quản lý gửi thông báo (service kiểm lại quyền).
        else if(path.equals("/api/admin/operations/notification-broadcasts") || path.startsWith("/api/admin/operations/notification-broadcasts/")) actors.requireBroadcaster();
        else if(path.equals("/api/admin/operations/reported-incidents") || path.startsWith("/api/admin/operations/reported-incidents/")) actors.requireManager();
        else if(path.equals("/api/admin/operations/me") || path.equals("/api/admin/operations/profile")) actors.requireStaff();
        else actors.requireAdmin();
        if(path.startsWith("/api/admin/operations/")) actors.rejectPendingPasswordChange();
        return true;
    }
}
