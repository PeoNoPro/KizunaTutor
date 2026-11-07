package com.kizuna.features.tutor_search.controller;

import com.kizuna.core.security.JwtTokenProvider;
import com.kizuna.features.tutor_search.dto.CreateAvailabilityRequestDTO;
import com.kizuna.features.tutor_search.dto.TutorAvailabilityDTO;
import com.kizuna.features.tutor_search.service.TutorScheduleService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping({"/api/v1/tutor/availability", "/api/tutor/availability"})
@CrossOrigin(origins = {"http://localhost:3000", "http://127.0.0.1:3000"})
@RequiredArgsConstructor
public class TutorScheduleController {

    private final TutorScheduleService tutorScheduleService;
    private final JwtTokenProvider jwtTokenProvider;

    @GetMapping
    public ResponseEntity<Map<String, Object>> getMyAvailabilities(HttpServletRequest request) {
        String userEmail = extractEmail(request);
        List<TutorAvailabilityDTO> list = tutorScheduleService.getMyAvailabilities(userEmail);

        Map<String, Object> body = new HashMap<>();
        body.put("success", true);
        body.put("code", 200);
        body.put("message", "Lấy danh sách ca rảnh giảng dạy thành công");
        body.put("data", list);

        return ResponseEntity.ok(body);
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> addAvailability(
            @Valid @RequestBody CreateAvailabilityRequestDTO requestDto,
            HttpServletRequest request) {
        String userEmail = extractEmail(request);
        TutorAvailabilityDTO response = tutorScheduleService.addAvailability(userEmail, requestDto);

        Map<String, Object> body = new HashMap<>();
        body.put("success", true);
        body.put("code", 201);
        body.put("message", "Đã thêm ca rảnh thành công");
        body.put("data", response);

        return ResponseEntity.status(HttpStatus.CREATED).body(body);
    }

    @DeleteMapping("/{availabilityId}")
    public ResponseEntity<Map<String, Object>> removeAvailability(
            @PathVariable UUID availabilityId,
            HttpServletRequest request) {
        String userEmail = extractEmail(request);
        tutorScheduleService.removeAvailability(userEmail, availabilityId);

        Map<String, Object> body = new HashMap<>();
        body.put("success", true);
        body.put("code", 200);
        body.put("message", "Đã xóa ca rảnh thành công");

        return ResponseEntity.ok(body);
    }

    @PatchMapping("/{availabilityId}/toggle")
    public ResponseEntity<Map<String, Object>> toggleAvailability(
            @PathVariable UUID availabilityId,
            @RequestParam boolean active,
            HttpServletRequest request) {
        String userEmail = extractEmail(request);
        TutorAvailabilityDTO response = tutorScheduleService.toggleAvailability(userEmail, availabilityId, active);

        Map<String, Object> body = new HashMap<>();
        body.put("success", true);
        body.put("code", 200);
        body.put("message", active ? "Đã bật ca rảnh" : "Đã tạm ẩn ca rảnh");
        body.put("data", response);

        return ResponseEntity.ok(body);
    }

    private String extractEmail(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new AccessDeniedException("Vui lòng đăng nhập tài khoản gia sư để quản lý lịch giảng dạy");
        }
        String token = authHeader.substring(7).trim();
        try {
            return jwtTokenProvider.getEmailFromToken(token);
        } catch (Exception e) {
            log.error("Failed to extract email from token in TutorScheduleController: {}", e.getMessage());
            throw new AccessDeniedException("Phiên đăng nhập đã hết hạn hoặc không hợp lệ. Vui lòng đăng nhập lại.");
        }
    }
}

