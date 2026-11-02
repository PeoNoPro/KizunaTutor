package com.kizuna.features.auth.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserSummaryDto {
    private UUID id;
    private String fullName;
    private String email;
    private String role;
    private String roleBadge;
    private String avatarUrl;
    private String accountStatus;
    private boolean mustChangePassword;
}
