package com.kizuna.features.user_management.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Chi tiết tài khoản gia sư / học viên cho Quản lý (UC-21) kèm lịch sử đổi trạng thái (UC-12 POST-2).
 * Không chứa mật khẩu, mã băm hay token.
 */
public record ManagedUserDetailResponse(
        UUID userId,
        String fullName,
        String email,
        String phone,
        String avatarUrl,
        String category,
        String categoryLabel,
        String accountStatus,
        String accountStatusLabel,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt,
        List<StatusOption> allowedStatuses,
        String statusChangeBlockedReason,
        TutorProfile tutorProfile,
        LearnerProfile learnerProfile,
        List<StatusChange> statusHistory) {

    public record StatusOption(String value, String label) {
    }

    public record TutorProfile(
            Long tutorCode,
            String headline,
            Integer experienceYears,
            String highestJlptLevel,
            BigDecimal avgRating,
            Integer totalReviews,
            String tutorStateLabel,
            LocalDate dateOfBirth,
            String address,
            OffsetDateTime verifiedAt) {
    }

    public record LearnerProfile(
            String currentLevel,
            String targetLevel,
            String learningGoals,
            String province,
            String address) {
    }

    public record StatusChange(
            OffsetDateTime changedAt,
            String actorName,
            String fromStatusLabel,
            String toStatusLabel,
            String reason,
            String messageToUser) {
    }
}
