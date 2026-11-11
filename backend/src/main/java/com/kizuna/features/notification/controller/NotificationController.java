package com.kizuna.features.notification.controller;

import com.kizuna.core.dto.ApiResponse;
import com.kizuna.features.notification.service.NotificationInboxService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

/** FE-12: thông báo cá nhân và tùy chọn nhận thông báo cho sáu vai trò. Lệnh ghi cần mã CSRF lấy từ /csrf. */
@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class NotificationController {
    private final NotificationInboxService service;

    @GetMapping("/csrf")
    public ApiResponse<?> csrf(CsrfToken token) {
        service.unreadCount();
        return ApiResponse.ok(Map.of("headerName", token.getHeaderName(), "token", token.getToken()));
    }

    @GetMapping
    public ApiResponse<?> list(@RequestParam(required = false) String read, @RequestParam(required = false) String type,
                               @RequestParam(required = false) java.time.LocalDate fromDate, @RequestParam(required = false) java.time.LocalDate toDate,
                               @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.ok(service.list(read, type, fromDate, toDate, page, size));
    }

    @GetMapping("/me")
    public ApiResponse<?> me() { return ApiResponse.ok(service.me()); }

    @GetMapping("/unread-count")
    public ApiResponse<?> unread() { return ApiResponse.ok(service.unreadCount()); }

    @GetMapping("/preferences")
    public ApiResponse<?> preferences() { return ApiResponse.ok(service.preferences()); }

    @PutMapping("/preferences")
    public ApiResponse<?> savePreferences(@RequestBody NotificationInboxService.PreferenceUpdate request) { return ApiResponse.ok(service.savePreferences(request)); }

    @GetMapping("/{id}")
    public ApiResponse<?> detail(@PathVariable UUID id) { return ApiResponse.ok(service.detail(id)); }

    @PostMapping("/{id}/read")
    public ApiResponse<?> read(@PathVariable UUID id) { return ApiResponse.ok(service.markRead(id)); }

    @DeleteMapping("/{id}")
    public ApiResponse<?> delete(@PathVariable UUID id) { return ApiResponse.ok(service.delete(id)); }
}
