package com.kizuna.features.classroom.util;

import com.kizuna.core.entity.Tutor;
import com.kizuna.core.entity.User;
import com.kizuna.features.payment.service.TutorMembershipService;
import java.time.LocalDate;
import java.time.ZoneId;
import com.kizuna.core.entity.UserAccountStatus;
import com.kizuna.core.repository.LearnerRepository;
import com.kizuna.core.repository.TutorRepository;
import com.kizuna.core.repository.UserRepository;
import com.kizuna.core.security.JwtTokenProvider;
import com.kizuna.features.classroom.exception.ClassroomException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Xác định người dùng hiện tại cho API FE-06 từ header "Authorization: Bearer &lt;token&gt;".
 * SecurityConfig hiện chưa có JWT filter nên lớp này tự kiểm token bằng JwtTokenProvider sẵn có
 * (giống cách AuthController đang làm), không sửa core/.
 */
@Component
@RequiredArgsConstructor
public class CurrentUserResolver {

    static final String ROLE_TUTOR = "ROLE_TUTOR";
    static final String ROLE_LEARNER = "ROLE_LEARNER";
    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtTokenProvider jwtTokenProvider;
    private final UserRepository userRepository;
    private final TutorRepository tutorRepository;
    private final LearnerRepository learnerRepository;

    /**
     * Inject qua setter (không đưa vào constructor) để giữ nguyên constructor 4 tham số mà
     * CurrentUserResolverTest đang dùng; khi null (unit test dựng tay) thì bỏ qua kiểm gói hội viên.
     */
    private TutorMembershipService membershipService;

    /** Luật 3 (AGENTS.md): hỏi gói hội viên qua Service công khai của feature payment, không gọi repository. */
    @Autowired
    public void setMembershipService(TutorMembershipService membershipService) {
        this.membershipService = membershipService;
    }

    private static final ZoneId ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    static final String MSG_NEED_MEMBERSHIP =
            "Bạn cần đăng ký gói hội viên còn hiệu lực để sử dụng chức năng này.";

    public CurrentActor resolve(String authorizationHeader) {
        if (authorizationHeader == null || !authorizationHeader.startsWith(BEARER_PREFIX)) {
            throw ClassroomException.unauthorized();
        }
        String token = authorizationHeader.substring(BEARER_PREFIX.length()).trim();
        if (token.isEmpty() || !jwtTokenProvider.validateToken(token)) {
            throw ClassroomException.unauthorized();
        }

        User user = userRepository.findByEmail(jwtTokenProvider.getEmailFromToken(token))
                .orElseThrow(ClassroomException::unauthorized);
        if (user.getDeletedAt() != null || user.getAccountStatus() != UserAccountStatus.ACTIVE) {
            throw ClassroomException.forbidden("Tài khoản của bạn hiện không thể sử dụng chức năng lớp học.");
        }

        String roleName = user.getRole() != null ? user.getRole().getRoleName() : null;
        if (ROLE_TUTOR.equals(roleName)) {
            Tutor tutor = tutorRepository.findByUser_UserId(user.getUserId())
                    .orElseThrow(() -> ClassroomException.forbidden("Không tìm thấy hồ sơ gia sư của tài khoản này."));
            // Lớp học, tài liệu, bài tập chỉ dành cho gia sư có gói hội viên còn hạn
            if (membershipService != null && !membershipService.hasActiveMembership(tutor.getTutorId(), LocalDate.now(ZONE))) {
                throw ClassroomException.forbidden(MSG_NEED_MEMBERSHIP);
            }
            return new CurrentActor(user, tutor, null);
        }
        if (ROLE_LEARNER.equals(roleName)) {
            return learnerRepository.findByUser_UserId(user.getUserId())
                    .map(learner -> new CurrentActor(user, null, learner))
                    .orElseThrow(() -> ClassroomException.forbidden("Không tìm thấy hồ sơ học viên của tài khoản này."));
        }
        throw ClassroomException.forbidden("Chức năng lớp học chỉ dành cho gia sư và học viên.");
    }
}
