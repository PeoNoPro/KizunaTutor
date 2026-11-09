package com.kizuna.features.classroom.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.kizuna.features.classroom.entity.LearningSpaceStatus;
import lombok.Builder;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Thông tin một lớp học. readOnly = lớp không còn hoạt động hoặc gia sư không có gói hội viên còn hiệu lực (FREE/EXPIRED).
 * Khóa học: courseStartDate, totalSessions (null = lớp cũ, lịch lặp hằng tuần), startedSessions (số buổi đã bắt đầu),
 * courseEndDate (ngày buổi cuối, null nếu không giới hạn số buổi).
 */
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
        List<ClassScheduleResponse> schedules,
        @JsonFormat(pattern = "yyyy-MM-dd") LocalDate courseStartDate,
        Integer totalSessions,
        int startedSessions,
        @JsonFormat(pattern = "yyyy-MM-dd") LocalDate courseEndDate) {
}
