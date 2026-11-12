package com.kizuna.features.booking.controller;

import com.kizuna.core.dto.ApiResponse;
import com.kizuna.core.entity.User;
import com.kizuna.features.booking.dto.request.CreateConnectionRequestDTO;
import com.kizuna.features.booking.dto.request.RespondConnectionRequestDTO;
import com.kizuna.features.booking.dto.response.ConnectionRequestResponseDTO;
import com.kizuna.features.booking.service.ConnectionRequestService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/** Kết nối gia sư trực tiếp (không cần học thử). */
@RestController
@RequestMapping({"/api/v1/connection-requests", "/api/connection-requests"})
@CrossOrigin(origins = {"http://localhost:3000", "http://127.0.0.1:3000"})
@RequiredArgsConstructor
public class ConnectionRequestController {

    private final ConnectionRequestService connectionRequestService;

    @PostMapping
    @PreAuthorize("hasAuthority('ROLE_LEARNER')")
    public ResponseEntity<ApiResponse<ConnectionRequestResponseDTO>> create(
            @Valid @RequestBody CreateConnectionRequestDTO request) {
        ConnectionRequestResponseDTO body = connectionRequestService.createDirectRequest(currentEmail(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Đã gửi yêu cầu kết nối. Vui lòng chờ gia sư phản hồi.", body));
    }

    /** Xem trước lịch học dự kiến: từng buổi cụ thể, buổi nào đã kín, gợi ý tuần bắt đầu không vướng. */
    @PostMapping("/preview")
    @PreAuthorize("hasAuthority('ROLE_LEARNER')")
    public ResponseEntity<ApiResponse<com.kizuna.features.booking.dto.response.ConnectionPreviewResponseDTO>> preview(
            @Valid @RequestBody com.kizuna.features.booking.dto.request.ConnectionPreviewRequestDTO request) {
        return ResponseEntity.ok(ApiResponse.ok("Lịch học dự kiến",
                connectionRequestService.preview(currentEmail(), request)));
    }

    @GetMapping("/my-requests")
    @PreAuthorize("hasAnyAuthority('ROLE_LEARNER', 'ROLE_TUTOR')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> myRequests(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<ConnectionRequestResponseDTO> result =
                connectionRequestService.getMyDirectRequests(currentEmail(), status, page, size);
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("items", result.getContent());
        data.put("totalItems", result.getTotalElements());
        data.put("totalPages", result.getTotalPages());
        data.put("currentPage", result.getNumber());
        return ResponseEntity.ok(ApiResponse.ok("Lấy danh sách yêu cầu kết nối thành công", data));
    }

    @PutMapping("/{requestId}/respond")
    @PreAuthorize("hasAuthority('ROLE_TUTOR')")
    public ResponseEntity<ApiResponse<ConnectionRequestResponseDTO>> respond(
            @PathVariable UUID requestId,
            @Valid @RequestBody RespondConnectionRequestDTO request) {
        ConnectionRequestResponseDTO body = connectionRequestService.respond(currentEmail(), requestId, request);
        String message = "ACCEPTED".equals(body.getStatus())
                ? "Đã đồng ý kết nối với học viên. Bạn có thể tạo lớp học chính thức."
                : "Đã từ chối yêu cầu kết nối.";
        return ResponseEntity.ok(ApiResponse.ok(message, body));
    }

    @PutMapping("/{requestId}/cancel")
    @PreAuthorize("hasAuthority('ROLE_LEARNER')")
    public ResponseEntity<ApiResponse<ConnectionRequestResponseDTO>> cancel(@PathVariable UUID requestId) {
        ConnectionRequestResponseDTO body = connectionRequestService.cancel(currentEmail(), requestId);
        return ResponseEntity.ok(ApiResponse.ok("Đã hủy yêu cầu kết nối.", body));
    }

    private String currentEmail() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated()) {
            Object principal = auth.getPrincipal();
            if (principal instanceof User user && user.getEmail() != null) return user.getEmail();
            if (principal instanceof UserDetails details) return details.getUsername();
            if (principal instanceof String email && !email.isBlank() && !"anonymousUser".equals(email)) return email;
        }
        throw new InsufficientAuthenticationException("Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại.");
    }
}
