package com.kizuna.features.admin.controller;


import com.kizuna.core.entity.*;
import com.kizuna.features.admin.dto.*;
import com.kizuna.core.entity.User;
import com.kizuna.features.admin.service.IAdminUserService;
import com.kizuna.core.security.SecurityContextUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

/**
 * REST Controller phục vụ phân hệ Quản Lý User & Role trong Dashboard Quản Trị Viên:
 * - Bảo vệ nghiêm ngặt bằng @PreAuthorize("hasAuthority('ROLE_ADMIN')")
 * - Quản lý toàn diện vòng đời tài khoản 6 nhóm Actor.
 */
@Slf4j
@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('ROLE_ADMIN')")
public class AdminUserController {

    private final IAdminUserService adminUserService;
    private final SecurityContextUtil securityUtil;

    /**
     * 1. Danh sách người dùng có phân trang, tìm kiếm và bộ lọc (Role, Status).
     */
    @GetMapping
    public ResponseEntity<UserPageResponse> getUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String role,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "createdAt") String sort,
            @RequestParam(defaultValue = "desc") String direction) {

        User currentAdmin = securityUtil.getCurrentUser().orElse(null);
        UserPageResponse response = adminUserService.getUsers(page, size, keyword, role, status, sort, direction, currentAdmin);
        return ResponseEntity.ok(response);
    }

    /**
     * 2. Lấy 4 chỉ số thống kê KPI nhanh cho Dashboard.
     */
    @GetMapping("/kpis")
    public ResponseEntity<Map<String, Long>> getKpiStats() {
        return ResponseEntity.ok(adminUserService.getKpiStats());
    }

    /**
     * 3. Lấy thông tin chi tiết của một người dùng theo ID.
     */
    @GetMapping("/{id}")
    public ResponseEntity<UserDTO> getUserById(@PathVariable UUID id) {
        User currentAdmin = securityUtil.getCurrentUser().orElse(null);
        return ResponseEntity.ok(adminUserService.getUserById(id, currentAdmin));
    }

    /**
     * 4. Khởi tạo tài khoản nhân sự / người dùng mới.
     * Hỗ trợ cả 2 endpoint: POST /api/admin/users và POST /api/admin/users/create-staff
     */
    @PostMapping
    public ResponseEntity<UserDTO> createUser(@Valid @RequestBody CreateUserRequest request) {
        User currentAdmin = securityUtil.getCurrentUser().orElse(null);
        UserDTO created = adminUserService.createUser(request, currentAdmin);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PostMapping("/create-staff")
    public ResponseEntity<UserDTO> createStaff(@Valid @RequestBody CreateUserRequest request) {
        User currentAdmin = securityUtil.getCurrentUser().orElse(null);
        UserDTO created = adminUserService.createUser(request, currentAdmin);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    /**
     * 5. Cập nhật thông tin & thay đổi vai trò (Phân quyền).
     */
    @PutMapping("/{id}")
    public ResponseEntity<UserDTO> updateUser(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateUserRoleRequest request) {
        User currentAdmin = securityUtil.getCurrentUser().orElse(null);
        UserDTO updated = adminUserService.updateUser(id, request, currentAdmin);
        return ResponseEntity.ok(updated);
    }

    /**
     * 6. Bật/Tắt trạng thái (Khóa hoặc Mở khóa tài khoản).
     */
    @PatchMapping("/{id}/status")
    public ResponseEntity<UserDTO> updateStatus(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateStatusRequest request) {
        User currentAdmin = securityUtil.getCurrentUser().orElse(null);
        UserDTO updated = adminUserService.updateStatus(id, request.getStatus(), currentAdmin);
        return ResponseEntity.ok(updated);
    }

    /**
     * 7. Xóa mềm tài khoản người dùng (Soft Delete).
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> deleteUser(@PathVariable UUID id) {
        User currentAdmin = securityUtil.getCurrentUser().orElse(null);
        adminUserService.deleteUser(id, currentAdmin);
        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Đã xóa tài khoản người dùng thành công."
        ));
    }

    /**
     * 8. Gửi lại thông tin tài khoản & cấp mới mật khẩu tạm thời.
     */
    @PostMapping("/{id}/resend-credentials")
    public ResponseEntity<UserDTO> resendCredentials(@PathVariable UUID id) {
        User currentAdmin = securityUtil.getCurrentUser().orElse(null);
        UserDTO updated = adminUserService.resendCredentials(id, currentAdmin);
        return ResponseEntity.ok(updated);
    }
}
