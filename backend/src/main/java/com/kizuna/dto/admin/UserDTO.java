package com.kizuna.dto.admin;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * DTO dữ liệu người dùng phục vụ hiển thị trên Admin Dashboard.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserDTO {

    private UUID id;
    private String fullName;
    private String email;
    private String phone;
    private String role;
    private String roleBadge;
    private String status;
    private String statusBadge;
    private String avatarUrl;
    private String avatarText;
    private String createdAt;
    private String updatedAt;
    private boolean isDeleted;
    private boolean isSelf;
    private boolean mustChangePassword;
}
