package com.kizuna.controller.tutor;

import com.kizuna.dto.tutor.SaveTutorProfileRequest;
import com.kizuna.dto.tutor.TutorProfileDto;
import com.kizuna.entity.RoleEnum;
import com.kizuna.service.tutor.ITutorProfileService;
import com.kizuna.util.SecurityContextUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * REST API phục vụ Giai đoạn 1: Nộp & Cập nhật hồ sơ xác minh gia sư (UC 9-16).
 */
@RestController
@RequestMapping("/api/tutor/profile")
@PreAuthorize("hasAuthority('ROLE_TUTOR')")
@RequiredArgsConstructor
public class TutorProfileApiController {

    private final ITutorProfileService profileService;
    private final SecurityContextUtil securityUtil;

    /**
     * Lấy thông tin hồ sơ hiện tại của gia sư (UC 14, 15).
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> getProfile() {
        String email = securityUtil.getCurrentUserEmail(RoleEnum.ROLE_TUTOR);
        TutorProfileDto profile = profileService.getProfileByEmail(email);

        return ResponseEntity.ok(Map.of(
                "success", true,
                "data", profile
        ));
    }

    /**
     * Lưu bản nháp hồ sơ gia sư (UC 9, 10, 11, 12).
     */
    @PostMapping("/draft")
    public ResponseEntity<Map<String, Object>> saveDraft(@RequestBody SaveTutorProfileRequest request) {
        String email = securityUtil.getCurrentUserEmail(RoleEnum.ROLE_TUTOR);
        TutorProfileDto updated = profileService.saveDraft(email, request);

        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Đã lưu bản nháp hồ sơ thành công",
                "data", updated
        ));
    }

    /**
     * Nộp hồ sơ gia sư lên Ban Học Vụ để thẩm định (UC 13, 16).
     */
    @PostMapping("/submit")
    public ResponseEntity<Map<String, Object>> submitProfile(@RequestBody @Valid SaveTutorProfileRequest request) {
        String email = securityUtil.getCurrentUserEmail(RoleEnum.ROLE_TUTOR);
        TutorProfileDto updated = profileService.submitProfile(email, request);

        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Hồ sơ của bạn đã được gửi tới Ban Học Vụ thẩm định thành công",
                "data", updated
        ));
    }
}
