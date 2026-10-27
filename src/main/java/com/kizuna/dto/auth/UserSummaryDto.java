package com.kizuna.dto.auth;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * DTO chứa thông tin tóm tắt của Người dùng sau khi xác thực thành công.
 */
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
