package com.kizuna.features.profile.dto.response;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Hồ sơ học viên đang đăng nhập (contracts/profile-api.md mục 1.1, 1.2).
 * currentLevel: BEGINNER, N5..N1; targetLevel: N5..N1 (null nếu học viên chưa chọn).
 */
public record LearnerProfileResponseDto(
        UUID userId,
        String email,
        String fullName,
        String phone,
        String avatarUrl,
        String currentLevel,
        String targetLevel,
        String learningGoals,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt) {
}
