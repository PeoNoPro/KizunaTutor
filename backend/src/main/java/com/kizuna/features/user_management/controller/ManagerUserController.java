package com.kizuna.features.user_management.controller;

import com.kizuna.core.dto.ApiResponse;
import com.kizuna.core.dto.PageResponse;
import com.kizuna.core.entity.User;
import com.kizuna.features.payment.service.CurrentUserAccessor;
import com.kizuna.features.user_management.dto.ManagedUserDetailResponse;
import com.kizuna.features.user_management.dto.ManagedUserResponse;
import com.kizuna.features.user_management.dto.UpdateUserStatusRequest;
import com.kizuna.features.user_management.service.UserManagementService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * FE-14: Quản lý vận hành xem danh sách, chi tiết và đổi trạng thái tài khoản gia sư, học viên (UC-20, UC-21, UC-12).
 * Chỉ ROLE_MANAGER (SecurityConfig). Hợp đồng: specs/014-manager-user-management/contracts/manager-users-api.md.
 */
@RestController
@CrossOrigin(origins = {"http://localhost:3000", "http://127.0.0.1:3000"})
@RequestMapping("/api/manager/users")
@RequiredArgsConstructor
public class ManagerUserController {

    private final UserManagementService service;
    private final CurrentUserAccessor currentUser;

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<ManagedUserResponse>>> list(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.ok(service.list(category, keyword, status, page, size)));
    }

    @GetMapping("/{userId}")
    public ResponseEntity<ApiResponse<ManagedUserDetailResponse>> detail(@PathVariable UUID userId) {
        return ResponseEntity.ok(ApiResponse.ok(service.detail(userId)));
    }

    @PutMapping("/{userId}/status")
    public ResponseEntity<ApiResponse<ManagedUserDetailResponse>> updateStatus(
            @PathVariable UUID userId,
            @Valid @RequestBody UpdateUserStatusRequest request) {
        User manager = currentUser.requireUser();
        ManagedUserDetailResponse result = service.updateStatus(userId, request, manager.getUserId());
        return ResponseEntity.ok(ApiResponse.ok(
                "Đã cập nhật trạng thái tài khoản thành " + result.accountStatusLabel() + ".", result));
    }
}
