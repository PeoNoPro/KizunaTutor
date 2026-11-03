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
        else if(path.equals("/api/admin/operations/me") || path.equals("/api/admin/operations/profile")) actors.requireStaff();
        else actors.requireAdmin();
        return true;
    }
}
