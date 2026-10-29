package com.kizuna.core.security;

import com.kizuna.core.entity.RoleEnum;
import com.kizuna.core.entity.Tutor;
import com.kizuna.core.entity.TutorState;
import com.kizuna.core.repository.TutorRepository;
import com.kizuna.core.security.SecurityContextUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * Aspect kiểm soát phân quyền 3 giai đoạn của Gia Sư.
 * Chặn toàn bộ truy cập vào các tính năng giảng dạy nếu tài khoản chưa ở trạng thái ACTIVE_PAID.
 */
@Aspect
@Component
@RequiredArgsConstructor
@Slf4j
public class TutorSecurityAspect {

    private final SecurityContextUtil securityUtil;
    private final TutorRepository tutorRepository;

    @Around("@annotation(com.kizuna.security.RequireActiveTutor) || @within(com.kizuna.security.RequireActiveTutor)")
    public Object checkActiveTutorAccess(ProceedingJoinPoint joinPoint) throws Throwable {
        String email = securityUtil.getCurrentUserEmail(RoleEnum.ROLE_TUTOR);
        if (email == null || email.isBlank()) {
            throw new AccessDeniedException("Vui lòng đăng nhập với tài khoản gia sư để tiếp tục");
        }

        Tutor tutor = tutorRepository.findByUser_Email(email).orElse(null);
        if (tutor == null || tutor.getTutorState() != TutorState.ACTIVE_PAID) {
            TutorState state = tutor != null ? tutor.getTutorState() : TutorState.DRAFT_PROFILE;
            log.warn("Chặn truy cập tính năng giảng dạy: Gia sư {} hiện đang ở trạng thái: {}", email, state);

            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            HttpServletRequest request = attributes != null ? attributes.getRequest() : null;

            if (request != null && request.getRequestURI().startsWith("/api/")) {
                throw new AccessDeniedException("Tài khoản gia sư cần kích hoạt gói thành viên để sử dụng chức năng này.");
            }

            // Đối với giao diện web, điều hướng đến trang phù hợp với giai đoạn
            if (state == TutorState.APPROVED_UNPAID) {
                return "redirect:/tutor/membership-plans";
            } else {
                return "redirect:/tutor/profile";
            }
        }

        return joinPoint.proceed();
    }
}
