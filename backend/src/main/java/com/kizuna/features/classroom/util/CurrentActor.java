package com.kizuna.features.classroom.util;

import com.kizuna.core.entity.Learner;
import com.kizuna.core.entity.Tutor;
import com.kizuna.core.entity.User;

/** Người dùng hiện tại của API FE-06: đúng một trong tutor hoặc learner khác null. */
public record CurrentActor(User user, Tutor tutor, Learner learner) {

    public boolean isTutor() {
        return tutor != null;
    }

    public boolean isLearner() {
        return learner != null;
    }
}
