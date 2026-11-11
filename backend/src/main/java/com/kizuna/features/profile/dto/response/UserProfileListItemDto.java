package com.kizuna.features.profile.dto.response;

import com.kizuna.core.entity.UserAccountStatus;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Tóm tắt tài khoản người dùng trên bảng tra cứu cho Quản lý/Quản trị (contracts/profile-api.md mục 4.1).
 */
public record UserProfileListItemDto(
        UUID userId,
        String email,
        String fullName,
        String phone,
        String avatarUrl,
        String role,
        String roleBadge,
        UserAccountStatus accountStatus,
        OffsetDateTime createdAt) {
}
