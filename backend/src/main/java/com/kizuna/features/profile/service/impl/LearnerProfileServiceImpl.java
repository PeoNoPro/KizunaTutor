package com.kizuna.features.profile.service.impl;

import com.kizuna.core.entity.Learner;
import com.kizuna.core.entity.User;
import com.kizuna.core.exception.ConflictException;
import com.kizuna.core.exception.ResourceNotFoundException;
import com.kizuna.core.repository.LearnerRepository;
import com.kizuna.core.repository.UserRepository;
import com.kizuna.features.profile.dto.request.UpdateLearnerProfileRequest;
import com.kizuna.features.profile.dto.response.LearnerProfileResponseDto;
import com.kizuna.features.profile.mapper.LearnerProfileMapper;
import com.kizuna.features.profile.service.LearnerProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class LearnerProfileServiceImpl implements LearnerProfileService {

    static final String LEARNER_ROLE = "ROLE_LEARNER";
    static final String PHONE_TAKEN_MESSAGE = "Số điện thoại này đã được liên kết với một tài khoản khác trong hệ thống.";

    private final UserRepository userRepository;
    private final LearnerRepository learnerRepository;

    @Override
    @Transactional(readOnly = true)
    public LearnerProfileResponseDto getMyProfile(String email) {
        User user = findLearnerUser(email);
        Learner learner = learnerRepository.findByUser_UserId(user.getUserId()).orElse(null);
        return LearnerProfileMapper.toResponse(user, learner);
    }

    @Override
    @Transactional
    public LearnerProfileResponseDto updateMyProfile(String email, UpdateLearnerProfileRequest request) {
        User user = findLearnerUser(email);
        String phone = LearnerProfileMapper.blankToNull(request.phone());
        if (phone != null && isPhoneUsedByOther(phone, user.getUserId())) {
            throw new ConflictException(PHONE_TAKEN_MESSAGE);
        }
        Learner learner = learnerRepository.findByUser_UserId(user.getUserId())
                .orElseGet(() -> Learner.builder().user(user).build());

        LearnerProfileMapper.apply(request, user, learner);
        User savedUser = userRepository.saveAndFlush(user);
        Learner savedLearner = learnerRepository.saveAndFlush(learner);
        return LearnerProfileMapper.toResponse(savedUser, savedLearner);
    }

    private User findLearnerUser(String email) {
        User user = userRepository.findByEmail(email)
                .filter(u -> u.getDeletedAt() == null)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy hồ sơ học viên."));
        if (user.getRole() == null || !LEARNER_ROLE.equals(user.getRole().getRoleName())) {
            throw new AccessDeniedException("Chỉ tài khoản học viên mới xem được hồ sơ này.");
        }
        return user;
    }

    private boolean isPhoneUsedByOther(String phone, UUID userId) {
        Specification<User> samePhoneOtherUser = (root, query, cb) -> cb.and(
                cb.equal(root.get("phone"), phone),
                cb.notEqual(root.get("userId"), userId));
        return userRepository.exists(samePhoneOtherUser);
    }
}
