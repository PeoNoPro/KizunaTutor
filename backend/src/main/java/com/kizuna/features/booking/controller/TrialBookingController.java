package com.kizuna.features.booking.controller;

import com.kizuna.core.dto.ApiResponse;
import com.kizuna.core.entity.User;
import com.kizuna.features.booking.dto.request.CancelTrialBookingRequestDTO;
import com.kizuna.features.booking.dto.request.CreateTrialBookingRequestDTO;
import com.kizuna.features.booking.dto.request.RescheduleTrialBookingRequestDTO;
import com.kizuna.features.booking.dto.request.RespondTrialBookingRequestDTO;
import com.kizuna.features.booking.dto.response.TrialBookingResponseDTO;
import com.kizuna.features.booking.service.TrialBookingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
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

    @PostMapping
    @PreAuthorize("hasAuthority('ROLE_LEARNER')")
    public ResponseEntity<ApiResponse<TrialBookingResponseDTO>> createTrialBooking(
            @Valid @RequestBody CreateTrialBookingRequestDTO requestDto,
            Principal principal) {
        String userEmail = getAuthenticatedEmail(principal);
        TrialBookingResponseDTO response = trialBookingService.createTrialBooking(userEmail, requestDto);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Đã gửi yêu cầu đăng ký học thử thành công. Vui lòng chờ gia sư phản hồi.", response));
    }

    @PutMapping("/{bookingId}/respond")
    @PreAuthorize("hasAuthority('ROLE_TUTOR')")
    public ResponseEntity<ApiResponse<TrialBookingResponseDTO>> respondTrialBooking(
            @PathVariable UUID bookingId,
            @Valid @RequestBody RespondTrialBookingRequestDTO requestDto,
            Principal principal) {
        String userEmail = getAuthenticatedEmail(principal);
        TrialBookingResponseDTO response = trialBookingService.respondTrialBooking(userEmail, bookingId, requestDto);
        return ResponseEntity.ok(ApiResponse.ok("Đã xử lý yêu cầu học thử thành công", response));
    }

    @GetMapping("/my-requests")
    @PreAuthorize("hasAnyAuthority('ROLE_LEARNER', 'ROLE_TUTOR')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getMyTrialBookings(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Principal principal) {
        String userEmail = getAuthenticatedEmail(principal);
        Page<TrialBookingResponseDTO> pageResult = trialBookingService.getMyTrialBookings(userEmail, status, page, size);

        Map<String, Object> pageData = new HashMap<>();
        pageData.put("items", pageResult.getContent());
        pageData.put("content", pageResult.getContent());
        pageData.put("totalItems", pageResult.getTotalElements());
        pageData.put("totalElements", pageResult.getTotalElements());
        pageData.put("totalPages", pageResult.getTotalPages());
        pageData.put("currentPage", pageResult.getNumber());

        return ResponseEntity.ok(ApiResponse.ok("Lấy danh sách yêu cầu học thử thành công", pageData));
    }

    @GetMapping("/{bookingId}")
    @PreAuthorize("hasAnyAuthority('ROLE_LEARNER', 'ROLE_TUTOR')")
    public ResponseEntity<ApiResponse<TrialBookingResponseDTO>> getTrialBookingDetail(
            @PathVariable UUID bookingId,
            Principal principal) {
        String userEmail = getAuthenticatedEmail(principal);
        TrialBookingResponseDTO response = trialBookingService.getTrialBookingDetail(userEmail, bookingId);
        return ResponseEntity.ok(ApiResponse.ok("Lấy chi tiết yêu cầu học thử thành công", response));
    }

    @PutMapping("/{bookingId}/cancel")
    @PreAuthorize("hasAnyAuthority('ROLE_LEARNER', 'ROLE_TUTOR')")
    public ResponseEntity<ApiResponse<TrialBookingResponseDTO>> cancelTrialBooking(
            @PathVariable UUID bookingId,
            @Valid @RequestBody CancelTrialBookingRequestDTO requestDto,
            Principal principal) {
        String userEmail = getAuthenticatedEmail(principal);
        TrialBookingResponseDTO response = trialBookingService.cancelTrialBooking(userEmail, bookingId, requestDto);
        return ResponseEntity.ok(ApiResponse.ok("Đã hủy lịch học thử thành công", response));
    }

    @PutMapping("/{bookingId}/reschedule")
    @PreAuthorize("hasAnyAuthority('ROLE_LEARNER', 'ROLE_TUTOR')")
    public ResponseEntity<ApiResponse<TrialBookingResponseDTO>> rescheduleTrialBooking(
            @PathVariable UUID bookingId,
            @Valid @RequestBody RescheduleTrialBookingRequestDTO requestDto,
            Principal principal) {
        String userEmail = getAuthenticatedEmail(principal);
        TrialBookingResponseDTO response = trialBookingService.rescheduleTrialBooking(userEmail, bookingId, requestDto);
        return ResponseEntity.ok(ApiResponse.ok("Đã dời lịch học thử thành công", response));
    }

    @PutMapping("/{bookingId}/complete")
    @PreAuthorize("hasAnyAuthority('ROLE_LEARNER', 'ROLE_TUTOR')")
    public ResponseEntity<ApiResponse<TrialBookingResponseDTO>> completeTrialBooking(
            @PathVariable UUID bookingId,
            Principal principal) {
        String userEmail = getAuthenticatedEmail(principal);
        TrialBookingResponseDTO response = trialBookingService.completeTrialBooking(userEmail, bookingId);
        return ResponseEntity.ok(ApiResponse.ok("Đã xác nhận hoàn thành buổi học thử thành công", response));
    }

    @PostMapping("/schedule-lesson")
    @PreAuthorize("hasAuthority('ROLE_TUTOR')")
    public ResponseEntity<ApiResponse<TrialBookingResponseDTO>> scheduleLessonByTutor(
            @Valid @RequestBody com.kizuna.features.booking.dto.request.TutorScheduleLessonRequestDTO requestDto,
            Principal principal) {
        String userEmail = getAuthenticatedEmail(principal);
        TrialBookingResponseDTO response = trialBookingService.scheduleLessonByTutor(userEmail, requestDto);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Đã lên lịch buổi học cho học viên thành công", response));
    }

    private String getAuthenticatedEmail(Principal principal) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal())) {
            Object p = auth.getPrincipal();
            if (p instanceof User user && user.getEmail() != null) {
                return user.getEmail();
            }
            if (p instanceof org.springframework.security.core.userdetails.UserDetails userDetails) {
                return userDetails.getUsername();
            }
            if (p instanceof String email && !email.isBlank()) {
                return email;
            }
        }
        if (principal != null) {
            if (principal instanceof Authentication authentication && authentication.getPrincipal() instanceof User user && user.getEmail() != null) {
                return user.getEmail();
            }
            String name = principal.getName();
            if (name != null && !name.isBlank()) {
                return name;
            }
        }
        throw new InsufficientAuthenticationException("Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại.");
    }
}
