package com.kizuna.features.notification.controller;

import com.kizuna.core.dto.ApiResponse;
import com.kizuna.features.notification.service.NotificationBroadcastService;
import com.kizuna.features.notification.service.NotificationBroadcastService.BroadcastRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.UUID;

/**
 * FE-12: Quản trị viên gửi thông báo và xem lịch sử. Nằm dưới /api/admin/operations nên dùng chung kiểm quyền Admin,
 * chặn tài khoản chưa đổi mật khẩu lần đầu và mã CSRF của khu quản trị (/api/admin/operations/csrf).
 */
@RestController
@RequestMapping("/api/admin/operations/notification-broadcasts")
@RequiredArgsConstructor
public class NotificationBroadcastController {
    private final NotificationBroadcastService service;

    /** CSRF chung (/csrf) chỉ dành cho Admin; Quản lý lấy mã ở đây để gửi thông báo. */
    @GetMapping("/csrf")
    public ApiResponse<?> csrf(org.springframework.security.web.csrf.CsrfToken token) {
        service.requireSender();
        return ApiResponse.ok(java.util.Map.of("headerName", token.getHeaderName(), "token", token.getToken()));
    }

    @PostMapping("/preview")
    public ApiResponse<?> preview(@RequestBody BroadcastRequest request) { return ApiResponse.ok(service.preview(request)); }

    @PostMapping
    public ResponseEntity<?> submit(@RequestBody BroadcastRequest request) { return ResponseEntity.status(202).body(ApiResponse.ok(service.submit(request))); }

    @GetMapping
    public ApiResponse<?> history(@RequestParam(required = false) LocalDate fromDate, @RequestParam(required = false) LocalDate toDate,
                                  @RequestParam(required = false) String status, @RequestParam(required = false) String channel,
                                  @RequestParam(required = false) String role, @RequestParam(required = false) String title,
                                  @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.ok(service.history(fromDate, toDate, status, channel, role, title, page, size));
    }

    @GetMapping("/{id}")
    public ApiResponse<?> detail(@PathVariable UUID id) { return ApiResponse.ok(service.detail(id)); }
}
