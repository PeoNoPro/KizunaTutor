package com.kizuna.features.profile.controller;

import com.kizuna.core.dto.ApiResponse;
import com.kizuna.core.entity.User;
import com.kizuna.features.profile.dto.request.UpdateLearnerProfileRequest;
import com.kizuna.features.profile.dto.response.LearnerProfileResponseDto;
import com.kizuna.features.profile.service.LearnerProfileService;
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
 * Hồ sơ của học viên đang đăng nhập (contracts/profile-api.md mục 1).
 * Không nhận ID trên đường dẫn: luôn thao tác trên tài khoản trong phiên đăng nhập.
 */
@RestController
@RequestMapping("/api/profile/learner/me")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('ROLE_LEARNER')")
public class LearnerProfileController {

    private final LearnerProfileService learnerProfileService;

    @GetMapping
    public ApiResponse<LearnerProfileResponseDto> getMyProfile(Authentication authentication) {
        return ApiResponse.ok(learnerProfileService.getMyProfile(emailOf(authentication)));
    }

    @PutMapping
    public ApiResponse<LearnerProfileResponseDto> updateMyProfile(Authentication authentication,
                                                                  @Valid @RequestBody UpdateLearnerProfileRequest request) {
        LearnerProfileResponseDto result = learnerProfileService.updateMyProfile(emailOf(authentication), request);
        return ApiResponse.ok("Cập nhật hồ sơ học viên thành công.", result);
    }

    /** JwtAuthenticationFilter đặt principal là User; các cách đăng nhập khác dùng tên đăng nhập là email. */
    private static String emailOf(Authentication authentication) {
        if (authentication.getPrincipal() instanceof User user) {
            return user.getEmail();
        }
        return authentication.getName();
    }
}
