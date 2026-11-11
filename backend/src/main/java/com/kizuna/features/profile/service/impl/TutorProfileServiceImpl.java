package com.kizuna.features.profile.service.impl;

import com.kizuna.core.entity.Tutor;
import com.kizuna.core.entity.User;
import com.kizuna.core.exception.BadRequestException;
import com.kizuna.core.exception.ConflictException;
import com.kizuna.core.exception.ResourceNotFoundException;
import com.kizuna.core.repository.TutorRepository;
import com.kizuna.core.repository.UserRepository;
import com.kizuna.features.profile.dto.request.UpdateTutorProfileRequest;
import com.kizuna.features.profile.dto.response.TutorProfileDetailDto;
import com.kizuna.features.profile.mapper.TutorProfileMapper;
import com.kizuna.features.profile.service.TutorProfileService;
import com.kizuna.features.profile.support.ProfileSupport;
import com.kizuna.features.tutor_search.service.TutorSearchService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TutorProfileServiceImpl implements TutorProfileService {

    private static final String TUTOR_ROLE = "ROLE_TUTOR";

    private final UserRepository userRepository;
    private final TutorRepository tutorRepository;
    private final TutorSearchService tutorSearchService;

    @Override
    @Transactional(readOnly = true)
    public TutorProfileDetailDto getMyProfile(String email) {
        User user = findTutorUser(email);
        Tutor tutor = tutorRepository.findByUser_UserId(user.getUserId()).orElse(null);
        return TutorProfileMapper.toResponse(user, tutor);
    }

    @Override
    @Transactional
    public TutorProfileDetailDto updateMyProfile(String email, UpdateTutorProfileRequest request) {
        User user = findTutorUser(email);
        validateProvince(request.province());
        ProfileSupport.validateCoarseAddress(request.addressDetail());

        String cleanPhone = ProfileSupport.blankToNull(request.phone());
        if (cleanPhone != null && ProfileSupport.isPhoneUsedByOther(userRepository, cleanPhone, user.getUserId())) {
            throw new ConflictException(ProfileSupport.PHONE_TAKEN_MESSAGE);
        }

        Tutor tutor = tutorRepository.findByUser_UserId(user.getUserId())
                .orElseGet(() -> Tutor.builder().user(user).build());

        TutorProfileMapper.apply(request, user, tutor);
        User savedUser = userRepository.saveAndFlush(user);
        Tutor savedTutor = tutorRepository.saveAndFlush(tutor);
        return TutorProfileMapper.toResponse(savedUser, savedTutor);
    }

    private User findTutorUser(String email) {
        User user = userRepository.findByEmail(email)
                .filter(u -> u.getDeletedAt() == null)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy hồ sơ gia sư."));
        if (user.getRole() == null || !TUTOR_ROLE.equals(user.getRole().getRoleName())) {
            throw new AccessDeniedException("Chỉ tài khoản gia sư mới xem được hồ sơ này.");
        }
        return user;
    }

    private void validateProvince(String province) {
        String cleanProvince = ProfileSupport.blankToNull(province);
        List<String> supported = tutorSearchService.getAvailableLocations();
        if (cleanProvince == null || (supported != null && !supported.isEmpty() && !supported.contains(cleanProvince))) {
            throw new BadRequestException("Tỉnh/Thành phố không nằm trong danh sách hỗ trợ.");
        }
    }
}
