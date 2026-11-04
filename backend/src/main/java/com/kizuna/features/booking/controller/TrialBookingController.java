package com.kizuna.features.booking.controller;

import com.kizuna.core.security.JwtTokenProvider;
import com.kizuna.features.booking.dto.request.CancelTrialBookingRequestDTO;
import com.kizuna.features.booking.dto.request.CreateTrialBookingRequestDTO;
import com.kizuna.features.booking.dto.request.RespondTrialBookingRequestDTO;
import com.kizuna.features.booking.dto.response.TrialBookingResponseDTO;
import com.kizuna.features.booking.service.TrialBookingService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping({"/api/v1/trial-bookings", "/api/trial-bookings"})
@CrossOrigin(origins = {"http://localhost:3000", "http://127.0.0.1:3000"})
@RequiredArgsConstructor
public class TrialBookingController {

    private final TrialBookingService trialBookingService;
    private final JwtTokenProvider jwtTokenProvider;

    @PostMapping
    public ResponseEntity<Map<String, Object>> createTrialBooking(
            @Valid @RequestBody CreateTrialBookingRequestDTO requestDto,
            HttpServletRequest request) {
        String userEmail = extractEmail(request);
        TrialBookingResponseDTO response = trialBookingService.createTrialBooking(userEmail, requestDto);

        Map<String, Object> body = new HashMap<>();
        body.put("success", true);
        body.put("code", 201);
        body.put("message", "Đã gửi yêu cầu đăng ký học thử thành công. Vui lòng chờ gia sư phản hồi.");
        body.put("data", response);

        return ResponseEntity.status(HttpStatus.CREATED).body(body);
    }

    @PutMapping("/{bookingId}/respond")
    public ResponseEntity<Map<String, Object>> respondTrialBooking(
            @PathVariable UUID bookingId,
            @Valid @RequestBody RespondTrialBookingRequestDTO requestDto,
            HttpServletRequest request) {
        String userEmail = extractEmail(request);
        TrialBookingResponseDTO response = trialBookingService.respondTrialBooking(userEmail, bookingId, requestDto);

        Map<String, Object> body = new HashMap<>();
        body.put("success", true);
        body.put("code", 200);
        body.put("message", "Đã xử lý yêu cầu học thử thành công");
        body.put("data", response);

        return ResponseEntity.ok(body);
    }

    @GetMapping("/my-requests")
    public ResponseEntity<Map<String, Object>> getMyTrialBookings(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            HttpServletRequest request) {
        String userEmail = extractEmail(request);
        Page<TrialBookingResponseDTO> pageResult = trialBookingService.getMyTrialBookings(userEmail, status, page, size);

        Map<String, Object> pageData = new HashMap<>();
        pageData.put("items", pageResult.getContent());
        pageData.put("content", pageResult.getContent());
        pageData.put("totalItems", pageResult.getTotalElements());
        pageData.put("totalElements", pageResult.getTotalElements());
        pageData.put("totalPages", pageResult.getTotalPages());
        pageData.put("currentPage", pageResult.getNumber());

        Map<String, Object> body = new HashMap<>();
        body.put("success", true);
        body.put("code", 200);
        body.put("message", "Lấy danh sách yêu cầu học thử thành công");
        body.put("data", pageData);

        return ResponseEntity.ok(body);
    }

    @GetMapping("/{bookingId}")
    public ResponseEntity<Map<String, Object>> getTrialBookingDetail(
            @PathVariable UUID bookingId,
            HttpServletRequest request) {
        String userEmail = extractEmail(request);
        TrialBookingResponseDTO response = trialBookingService.getTrialBookingDetail(userEmail, bookingId);

        Map<String, Object> body = new HashMap<>();
        body.put("success", true);
        body.put("code", 200);
        body.put("message", "Lấy chi tiết yêu cầu học thử thành công");
        body.put("data", response);

        return ResponseEntity.ok(body);
    }

    @PutMapping("/{bookingId}/cancel")
    public ResponseEntity<Map<String, Object>> cancelTrialBooking(
            @PathVariable UUID bookingId,
            @Valid @RequestBody CancelTrialBookingRequestDTO requestDto,
            HttpServletRequest request) {
        String userEmail = extractEmail(request);
        TrialBookingResponseDTO response = trialBookingService.cancelTrialBooking(userEmail, bookingId, requestDto);

        Map<String, Object> body = new HashMap<>();
        body.put("success", true);
        body.put("code", 200);
        body.put("message", "Đã hủy lịch học thử thành công");
        body.put("data", response);

        return ResponseEntity.ok(body);
    }

    private String extractEmail(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new AccessDeniedException("Vui lòng đăng nhập để thực hiện chức năng này");
        }
        String token = authHeader.substring(7).trim();
        try {
            return jwtTokenProvider.getEmailFromToken(token);
        } catch (Exception e) {
            log.error("Failed to extract email from token: {}", e.getMessage());
            throw new AccessDeniedException("Phiên đăng nhập đã hết hạn hoặc không hợp lệ. Vui lòng đăng nhập lại.");
        }
    }
}
