package com.kizuna.features.profile.mapper;

import com.kizuna.core.entity.Learner;
import com.kizuna.core.entity.User;
import com.kizuna.features.profile.dto.request.UpdateLearnerProfileRequest;
import com.kizuna.features.profile.dto.response.LearnerProfileResponseDto;

import java.time.OffsetDateTime;

/**
 * Chuyển đổi giữa User, Learner và DTO hồ sơ học viên.
 */
public final class LearnerProfileMapper {

    private LearnerProfileMapper() {
    }

    /** learner có thể null khi tài khoản học viên chưa có bản ghi learners. */
    public static LearnerProfileResponseDto toResponse(User user, Learner learner) {
        return new LearnerProfileResponseDto(
                user.getUserId(),
                user.getEmail(),
                user.getFullName(),
                user.getPhone(),
                user.getAvatarUrl(),
                learner == null ? null : learner.getCurrentLevel(),
                learner == null ? null : learner.getTargetLevel(),
                learner == null ? null : learner.getLearningGoals(),
                user.getCreatedAt(),
                latest(user.getUpdatedAt(), learner == null ? null : learner.getUpdatedAt()));
    }

    /** Ghi các trường được phép sửa; email không bao giờ bị thay đổi. */
    public static void apply(UpdateLearnerProfileRequest request, User user, Learner learner) {
        user.setFullName(request.fullName().trim());
        user.setPhone(blankToNull(request.phone()));
        user.setAvatarUrl(blankToNull(request.avatarUrl()));
        learner.setCurrentLevel(blankToNull(request.currentLevel()));
        learner.setTargetLevel(blankToNull(request.targetLevel()));
        learner.setLearningGoals(blankToNull(request.learningGoals()));
    }

    public static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private static OffsetDateTime latest(OffsetDateTime a, OffsetDateTime b) {
        if (a == null) {
            return b;
        }
        return b != null && b.isAfter(a) ? b : a;
    }
}
