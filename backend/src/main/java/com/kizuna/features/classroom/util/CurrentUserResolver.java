package com.kizuna.features.classroom.util;

import com.kizuna.core.entity.User;
import com.kizuna.core.entity.UserAccountStatus;
import com.kizuna.core.repository.LearnerRepository;
import com.kizuna.core.repository.TutorRepository;
import com.kizuna.core.repository.UserRepository;
import com.kizuna.core.security.JwtTokenProvider;
import com.kizuna.features.classroom.exception.ClassroomException;
import lombok.RequiredArgsConstructor;
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
            return tutorRepository.findByUser_UserId(user.getUserId())
                    .map(tutor -> new CurrentActor(user, tutor, null))
                    .orElseThrow(() -> ClassroomException.forbidden("Không tìm thấy hồ sơ gia sư của tài khoản này."));
        }
        if (ROLE_LEARNER.equals(roleName)) {
            return learnerRepository.findByUser_UserId(user.getUserId())
                    .map(learner -> new CurrentActor(user, null, learner))
                    .orElseThrow(() -> ClassroomException.forbidden("Không tìm thấy hồ sơ học viên của tài khoản này."));
        }
        throw ClassroomException.forbidden("Chức năng lớp học chỉ dành cho gia sư và học viên.");
    }
}
