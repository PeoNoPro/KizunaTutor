package com.kizuna.features.profile.controller;

import com.kizuna.core.dto.ApiResponse;
import com.kizuna.features.profile.dto.request.UpdateTutorProfileRequest;
import com.kizuna.features.profile.dto.response.TutorProfileDetailDto;
import com.kizuna.features.profile.service.TutorProfileService;
import com.kizuna.features.profile.support.ProfileSupport;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Hồ sơ cá nhân và giảng dạy của gia sư đang đăng nhập (contracts/profile-api.md mục 2).
 */
@RestController
@RequestMapping("/api/profile/tutor/me")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('ROLE_TUTOR')")
public class TutorProfileController {

    private final TutorProfileService tutorProfileService;

    @GetMapping
    public ApiResponse<TutorProfileDetailDto> getMyProfile(Authentication authentication) {
        return ApiResponse.ok(tutorProfileService.getMyProfile(ProfileSupport.emailOf(authentication)));
    }

    @PutMapping
    public ApiResponse<TutorProfileDetailDto> updateMyProfile(Authentication authentication,
                                                              @Valid @RequestBody UpdateTutorProfileRequest request) {
        TutorProfileDetailDto result = tutorProfileService.updateMyProfile(ProfileSupport.emailOf(authentication), request);
        return ApiResponse.ok("Cập nhật hồ sơ giảng dạy thành công.", result);
    }
}
