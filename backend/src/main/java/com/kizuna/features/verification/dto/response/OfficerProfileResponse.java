package com.kizuna.features.verification.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Hồ sơ cá nhân của cán bộ học vụ.
 */
@Getter
@Builder
@AllArgsConstructor
public class OfficerProfileResponse {
    private UUID userId;
    private String fullName;
    private String email;
    private String phone;
    private String avatarUrl;
    private String roleLabel;
    private String accountStatus;
    private OffsetDateTime createdAt;
}
