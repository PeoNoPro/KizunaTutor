package com.kizuna.features.classroom.service;

import java.util.UUID;

/**
 * Service công khai để feature khác (learning_support) hỏi học viên đang thuộc bao nhiêu lớp học
 * mà không gọi thẳng LearningSpaceRepository (Luật 3, AGENTS.md).
 */
public interface LearnerClassMembershipService {

    /**
     * Số lớp học còn mở (ACTIVE hoặc PAUSED) của học viên có user_id cho trước.
     * Lớp COMPLETED/CLOSED không được tính.
     */
    long countOpenClasses(UUID learnerUserId);
}
