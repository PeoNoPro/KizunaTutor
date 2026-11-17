package com.kizuna.features.profile.dto.response;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Thông tin chi tiết hồ sơ giảng dạy và cá nhân của gia sư (contracts/profile-api.md mục 2.1 & 2.2).
 */
public record TutorProfileDetailDto(
        UUID userId,
        UUID tutorId,
        Long tutorCode,
        String email,
        String fullName,
        String phone,
        String avatarUrl,
        String headline,
        String bio,
        Integer experienceYears,
        BigDecimal hourlyRate,
        String highestJlptLevel,
        String province,
        String addressDetail,
        String fullAddress,
        BigDecimal avgRating,
        Integer totalReviews,
        String verificationStatus,
        boolean isOwner,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt) {
}
