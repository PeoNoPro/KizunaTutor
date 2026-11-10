package com.kizuna.features.profile.service;

import com.kizuna.features.profile.dto.request.UpdateLearnerProfileRequest;
import com.kizuna.features.profile.dto.response.LearnerProfileResponseDto;

/**
 * Hồ sơ của học viên đang đăng nhập (FE-02, User Story 1). Chỉ thao tác trên hồ sơ của chính mình.
 */
public interface LearnerProfileService {

    LearnerProfileResponseDto getMyProfile(String email);

    LearnerProfileResponseDto updateMyProfile(String email, UpdateLearnerProfileRequest request);
}
