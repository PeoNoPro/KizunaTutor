package com.kizuna.features.tutor_search.controller;

import com.kizuna.core.dto.ApiResponse;
import com.kizuna.core.entity.User;
import com.kizuna.features.tutor_search.dto.CreateAvailabilityRequestDTO;
import com.kizuna.features.tutor_search.dto.TutorAvailabilityDTO;
import com.kizuna.features.tutor_search.service.TutorScheduleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping({"/api/v1/tutor/availability", "/api/tutor/availability"})
@CrossOrigin(origins = {"http://localhost:3000", "http://127.0.0.1:3000"})
@RequiredArgsConstructor
public class TutorScheduleController {

    private final TutorScheduleService tutorScheduleService;

    @GetMapping
    @PreAuthorize("hasAuthority('ROLE_TUTOR')")
    public ResponseEntity<ApiResponse<List<TutorAvailabilityDTO>>> getMyAvailabilities(Principal principal) {
        String userEmail = getAuthenticatedEmail(principal);
        List<TutorAvailabilityDTO> list = tutorScheduleService.getMyAvailabilities(userEmail);
        return ResponseEntity.ok(ApiResponse.ok("Lấy danh sách ca rảnh giảng dạy thành công", list));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('ROLE_TUTOR')")
    public ResponseEntity<ApiResponse<TutorAvailabilityDTO>> addAvailability(
            @Valid @RequestBody CreateAvailabilityRequestDTO requestDto,
            Principal principal) {
        String userEmail = getAuthenticatedEmail(principal);
        TutorAvailabilityDTO response = tutorScheduleService.addAvailability(userEmail, requestDto);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Đã thêm ca rảnh thành công", response));
    }

    @DeleteMapping("/{availabilityId}")
    @PreAuthorize("hasAuthority('ROLE_TUTOR')")
    public ResponseEntity<ApiResponse<Void>> removeAvailability(
            @PathVariable UUID availabilityId,
            Principal principal) {
        String userEmail = getAuthenticatedEmail(principal);
        tutorScheduleService.removeAvailability(userEmail, availabilityId);
        return ResponseEntity.ok(ApiResponse.ok("Đã xóa ca rảnh thành công", null));
    }

    @PatchMapping("/{availabilityId}/toggle")
    @PreAuthorize("hasAuthority('ROLE_TUTOR')")
    public ResponseEntity<ApiResponse<TutorAvailabilityDTO>> toggleAvailability(
            @PathVariable UUID availabilityId,
            @RequestParam boolean active,
            Principal principal) {
        String userEmail = getAuthenticatedEmail(principal);
        TutorAvailabilityDTO response = tutorScheduleService.toggleAvailability(userEmail, availabilityId, active);
        String msg = active ? "Đã mở lại ca rảnh" : "Đã tạm ẩn ca rảnh";
        return ResponseEntity.ok(ApiResponse.ok(msg, response));
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
