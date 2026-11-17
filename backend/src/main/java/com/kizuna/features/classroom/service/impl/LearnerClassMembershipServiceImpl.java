package com.kizuna.features.classroom.service.impl;

import com.kizuna.features.classroom.entity.LearningSpaceStatus;
import com.kizuna.features.classroom.repository.LearningSpaceRepository;
import com.kizuna.features.classroom.service.LearnerClassMembershipService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumSet;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class LearnerClassMembershipServiceImpl implements LearnerClassMembershipService {

    /** Lớp tạm dừng vẫn tính là đang học: học viên vẫn thuộc lớp nên vẫn được dùng công cụ. */
    static final Set<LearningSpaceStatus> OPEN_STATUSES =
            EnumSet.of(LearningSpaceStatus.ACTIVE, LearningSpaceStatus.PAUSED);

    private final LearningSpaceRepository learningSpaceRepository;

    @Override
    @Transactional(readOnly = true)
    public long countOpenClasses(UUID learnerUserId) {
        if (learnerUserId == null) {
            return 0;
        }
        return learningSpaceRepository.countByLearner_User_UserIdAndStatusIn(learnerUserId, OPEN_STATUSES);
    }
}
