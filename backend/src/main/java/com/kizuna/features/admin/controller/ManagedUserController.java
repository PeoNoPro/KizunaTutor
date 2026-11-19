package com.kizuna.features.admin.controller;

import com.kizuna.core.dto.ApiResponse;
import com.kizuna.features.admin.service.ManagedUserService;
import com.kizuna.features.admin.service.ManagedUserService.StatusChange;
import com.kizuna.features.admin.service.OperationsActorResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

/** FE-11 (2026-11-13): Quản lý xem và tạm khóa / mở lại tài khoản Học viên, Gia sư. Chỉ Quản lý vận hành. */
@RestController
@RequestMapping("/api/admin/operations/users")
@RequiredArgsConstructor
public class ManagedUserController {
    private final ManagedUserService users;
    private final OperationsActorResolver actors;

    /** CSRF chung chỉ dành cho Admin nên màn Quản lý lấy token ở endpoint riêng. */
    @GetMapping("/csrf")
    public ApiResponse<?> csrf(org.springframework.security.web.csrf.CsrfToken token) {
        actors.requireManager();
        return ApiResponse.ok(Map.of("token", token.getToken(), "headerName", token.getHeaderName()));
    }

    @GetMapping
    public ApiResponse<?> list(@RequestParam(required = false) String q, @RequestParam(required = false) String role,
                               @RequestParam(required = false) String status, @RequestParam(defaultValue = "0") int page,
                               @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.ok(users.list(q, role, status, page, size));
    }

    @GetMapping("/{id}")
    public ApiResponse<?> detail(@PathVariable UUID id) {
        return ApiResponse.ok(users.detail(id));
    }

    @PutMapping("/{id}/status")
    public ApiResponse<?> status(@PathVariable UUID id, @RequestBody StatusChange request) {
        return ApiResponse.ok(users.changeStatus(id, request));
    }
}
