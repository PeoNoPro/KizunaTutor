package com.kizuna.features.learning_support.config;

import com.kizuna.core.entity.User;
import com.kizuna.core.exception.BusinessException;
import com.kizuna.features.learning_support.service.LearningToolAccessService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Chặn API công cụ học khi học viên chưa thuộc lớp học nào (đăng ký ở LearningToolAccessWebConfig).
 * Lỗi ném ra được GlobalExceptionHandler chuyển thành phản hồi 401/403 tiếng Việt.
 *
 * Service được lấy trễ qua ObjectProvider để các test @WebMvcTest của controller khác
 * (không nạp service này) vẫn khởi động được; thiếu service thì chặn luôn (fail closed).
 */
public class LearningToolAccessInterceptor implements HandlerInterceptor {

    private final ObjectProvider<LearningToolAccessService> accessServiceProvider;

    public LearningToolAccessInterceptor(ObjectProvider<LearningToolAccessService> accessServiceProvider) {
        this.accessServiceProvider = accessServiceProvider;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }
        LearningToolAccessService accessService = accessServiceProvider.getIfAvailable();
        if (accessService == null) {
            throw new BusinessException(403, LearningToolAccessService.LOCKED_MESSAGE);
        }
        accessService.requireAccess(currentUser());
        return true;
    }

    private static User currentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null && authentication.getPrincipal() instanceof User user ? user : null;
    }
}
