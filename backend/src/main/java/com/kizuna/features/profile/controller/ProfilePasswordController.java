package com.kizuna.features.profile.controller;

import com.kizuna.core.dto.ApiResponse;
import com.kizuna.core.entity.User;
import com.kizuna.features.profile.dto.request.ChangePasswordRequest;
import com.kizuna.features.profile.service.ProfilePasswordService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Đổi mật khẩu của học viên hoặc gia sư đang đăng nhập (contracts/profile-api.md mục 2A).
 */
@RestController
@RequestMapping("/api/profile/password")
@RequiredArgsConstructor
public class ProfilePasswordController {

    private final ProfilePasswordService profilePasswordService;

    @PutMapping
    @PreAuthorize("hasAnyAuthority('ROLE_LEARNER', 'ROLE_TUTOR')")
    public ApiResponse<Void> changeMyPassword(Authentication authentication,
                                              @Valid @RequestBody ChangePasswordRequest request) {
        String email = authentication.getPrincipal() instanceof User user ? user.getEmail() : authentication.getName();
        profilePasswordService.changeMyPassword(email, request);
        return ApiResponse.ok("Đổi mật khẩu thành công.", null);
    }
}
