package com.kizuna.features.profile.dto.response;

import com.kizuna.core.entity.UserAccountStatus;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Chi tiết hồ sơ người dùng (Học viên hoặc Gia sư) dành cho Quản lý vận hành và Quản trị viên.
 */
public record UserProfileDetailDto(
        UUID userId,
        String email,
        String fullName,
        String phone,
        String avatarUrl,
        String role,
        String roleBadge,
        UserAccountStatus accountStatus,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt,
        LearnerDetails learnerDetails,
        TutorDetails tutorDetails,
        List<StatusHistoryItem> statusHistory) {

    public record LearnerDetails(
            String currentLevel,
            String targetLevel,
            String learningGoals) {
    }

    public record TutorDetails(
            UUID tutorId,
            Long tutorCode,
            String headline,
            String bio,
            Integer experienceYears,
            BigDecimal hourlyRate,
            String highestJlptLevel,
            String fullAddress,
            BigDecimal avgRating,
            Integer totalReviews,
            String verificationStatus) {
    }

    public record StatusHistoryItem(
            String actionType,
            String previousStatus,
            String newStatus,
            String reason,
            String actorName,
            OffsetDateTime createdAt) {
    }
}
