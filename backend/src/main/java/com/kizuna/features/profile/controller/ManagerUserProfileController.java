package com.kizuna.features.profile.controller;

import com.kizuna.core.dto.ApiResponse;
import com.kizuna.features.profile.dto.request.UpdateUserStatusRequest;
import com.kizuna.features.profile.dto.response.PageResponse;
import com.kizuna.features.profile.dto.response.UserProfileDetailDto;
import com.kizuna.features.profile.dto.response.UserProfileListItemDto;
import com.kizuna.features.profile.dto.response.UserStatusUpdateResponseDto;
import com.kizuna.features.profile.service.ManagerUserProfileService;
import com.kizuna.features.profile.support.ProfileSupport;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * Tra cứu, xem chi tiết và cập nhật trạng thái tài khoản Học viên/Gia sư cho Quản lý & Quản trị viên.
 */
@RestController
@RequestMapping("/api/profile/users")
@RequiredArgsConstructor
@PreAuthorize("hasAnyAuthority('ROLE_MANAGER', 'ROLE_ADMIN')")
public class ManagerUserProfileController {

    private final ManagerUserProfileService managerUserProfileService;

    @GetMapping
    public ApiResponse<PageResponse<UserProfileListItemDto>> getUsers(
            @RequestParam(defaultValue = "ALL") String role,
            @RequestParam(defaultValue = "ALL") String status,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ApiResponse.ok(managerUserProfileService.getUsers(role, status, keyword, page, size));
    }

    @GetMapping("/{userId}")
    public ApiResponse<UserProfileDetailDto> getUserDetail(@PathVariable UUID userId) {
        return ApiResponse.ok(managerUserProfileService.getUserDetail(userId));
    }

    @PutMapping("/{userId}/status")
    public ApiResponse<UserStatusUpdateResponseDto> updateUserStatus(
            @PathVariable UUID userId,
            @Valid @RequestBody UpdateUserStatusRequest request,
            Authentication authentication,
            HttpServletRequest httpRequest) {
        String actorEmail = ProfileSupport.emailOf(authentication);
        String ipAddress = resolveClientIp(httpRequest);
        UserStatusUpdateResponseDto result =
                managerUserProfileService.updateUserStatus(userId, request, actorEmail, ipAddress);
        return ApiResponse.ok("Cập nhật trạng thái tài khoản thành công.", result);
    }

    private static String resolveClientIp(HttpServletRequest request) {
        if (request == null) {
            return null;
        }
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
