package com.kizuna.features.profile.service;

import com.kizuna.features.profile.dto.request.UpdateTutorProfileRequest;
import com.kizuna.features.profile.dto.response.TutorProfileDetailDto;

public interface TutorProfileService {

    TutorProfileDetailDto getMyProfile(String email);

    TutorProfileDetailDto updateMyProfile(String email, UpdateTutorProfileRequest request);
}
