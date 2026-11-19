package com.kizuna.features.profile.dto.response;

import com.kizuna.core.entity.UserAccountStatus;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Kết quả cập nhật trạng thái tài khoản người dùng (contracts/profile-api.md mục 4.3).
 */
public record UserStatusUpdateResponseDto(
        UUID userId,
        UserAccountStatus accountStatus,
        String reason,
        OffsetDateTime updatedAt) {
}
