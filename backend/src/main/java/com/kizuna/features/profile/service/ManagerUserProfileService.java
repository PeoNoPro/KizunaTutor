package com.kizuna.features.profile.service;

import com.kizuna.features.profile.dto.request.UpdateUserStatusRequest;
import com.kizuna.features.profile.dto.response.PageResponse;
import com.kizuna.features.profile.dto.response.UserProfileDetailDto;
import com.kizuna.features.profile.dto.response.UserProfileListItemDto;
import com.kizuna.features.profile.dto.response.UserStatusUpdateResponseDto;

import java.util.UUID;

public interface ManagerUserProfileService {

    PageResponse<UserProfileListItemDto> getUsers(String role, String status, String keyword, int page, int size);

    UserProfileDetailDto getUserDetail(UUID userId);

    UserStatusUpdateResponseDto updateUserStatus(UUID userId,
                                                 UpdateUserStatusRequest request,
                                                 String actorEmail,
                                                 String ipAddress);
}
