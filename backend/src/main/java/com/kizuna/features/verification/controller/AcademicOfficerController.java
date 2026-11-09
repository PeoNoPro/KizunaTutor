package com.kizuna.features.verification.controller;

import com.kizuna.features.verification.dto.request.UpdateOfficerProfileRequest;
import com.kizuna.features.verification.dto.response.OfficerProfileResponse;
import com.kizuna.features.verification.dto.response.PageResponse;
import com.kizuna.features.verification.dto.response.ReviewRecordResponse;
import com.kizuna.features.verification.service.AcademicOfficerService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Lịch sử xét duyệt và hồ sơ cá nhân của cán bộ học vụ.
 * Chỉ ROLE_ACADEMIC_OFFICER truy cập được (SecurityConfig).
 */
@RestController
@CrossOrigin(origins = {"http://localhost:3000", "http://127.0.0.1:3000"})
@RequestMapping("/api/academic")
@RequiredArgsConstructor
public class AcademicOfficerController {

    private final AcademicOfficerService officerService;

    @GetMapping("/reviews")
    public ResponseEntity<PageResponse<ReviewRecordResponse>> getReviewHistory(
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        return ResponseEntity.ok(officerService.getReviewHistory(action, keyword, page, size));
    }

    @GetMapping("/profile")
    public ResponseEntity<OfficerProfileResponse> getProfile() {
        return ResponseEntity.ok(officerService.getProfile());
    }

    @PutMapping("/profile")
    public ResponseEntity<Map<String, Object>> updateProfile(@RequestBody(required = false) UpdateOfficerProfileRequest request) {
        return ResponseEntity.ok(officerService.updateProfile(request));
    }
}
