package com.kizuna.features.learning_support.service.impl;

import com.kizuna.core.entity.User;
import com.kizuna.core.exception.BusinessException;
import com.kizuna.features.classroom.service.LearnerClassMembershipService;
import com.kizuna.features.learning_support.dto.LearningToolAccessResponse;
import com.kizuna.features.learning_support.service.LearningToolAccessService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class LearningToolAccessServiceImpl implements LearningToolAccessService {

    static final String ROLE_LEARNER = "ROLE_LEARNER";

    private final LearnerClassMembershipService learnerClassMembershipService;

    @Override
    public LearningToolAccessResponse getAccess(User user) {
        if (user == null) {
            throw new BusinessException(401, LOGIN_REQUIRED_MESSAGE);
        }
        if (!isLearner(user)) {
            return new LearningToolAccessResponse(true, 0, null);
        }
        long openClasses = learnerClassMembershipService.countOpenClasses(user.getUserId());
        return openClasses > 0
                ? new LearningToolAccessResponse(true, openClasses, null)
                : new LearningToolAccessResponse(false, 0, LOCKED_MESSAGE);
    }

    @Override
    public void requireAccess(User user) {
        if (!getAccess(user).allowed()) {
            throw new BusinessException(403, LOCKED_MESSAGE);
        }
    }

    private static boolean isLearner(User user) {
        return user.getRole() != null && ROLE_LEARNER.equals(user.getRole().getRoleName());
    }
}
