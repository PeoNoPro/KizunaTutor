package com.kizuna.features.classroom.dto;

import com.kizuna.features.classroom.entity.LearningSpaceStatus;
import lombok.Builder;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/** Thông tin một lớp học. readOnly = lớp không còn hoạt động hoặc hội viên gia sư đã hết hạn. */
@Builder
public record ClassroomSummaryResponse(
        UUID spaceId,
        String title,
        LearningSpaceStatus status,
        UUID tutorId,
        String tutorName,
        UUID learnerId,
        String learnerName,
        OffsetDateTime createdAt,
        boolean readOnly,
        boolean canManage,
        long materialCount,
        List<ClassScheduleResponse> schedules) {
}
