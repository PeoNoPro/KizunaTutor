package com.kizuna.features.user_management.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

/** Một dòng trong danh sách gia sư / học viên của Quản lý (UC-20). */
public record ManagedUserResponse(
        UUID userId,
        String fullName,
        String email,
        String category,
        String categoryLabel,
        String accountStatus,
        String accountStatusLabel,
        OffsetDateTime createdAt) {
}
