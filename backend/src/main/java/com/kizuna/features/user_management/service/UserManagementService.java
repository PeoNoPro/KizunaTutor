package com.kizuna.features.user_management.service;

import com.kizuna.core.dto.PageResponse;
import com.kizuna.features.user_management.dto.ManagedUserDetailResponse;
import com.kizuna.features.user_management.dto.ManagedUserResponse;
import com.kizuna.features.user_management.dto.UpdateUserStatusRequest;

import java.util.UUID;

/**
 * FE-14: Quản lý vận hành xem và đổi trạng thái tài khoản gia sư, học viên (SRS UC-20, UC-21, UC-12).
 * Hợp đồng: specs/014-manager-user-management/contracts/manager-users-api.md.
 */
public interface UserManagementService {

    PageResponse<ManagedUserResponse> list(String category, String keyword, String status, int page, int size);

    ManagedUserDetailResponse detail(UUID userId);

    ManagedUserDetailResponse updateStatus(UUID userId, UpdateUserStatusRequest request, UUID managerId);
}
