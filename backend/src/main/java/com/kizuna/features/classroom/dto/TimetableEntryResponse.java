package com.kizuna.features.classroom.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.kizuna.features.classroom.entity.LearningSpaceStatus;

import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Một buổi học chính thức lặp hằng tuần, dùng cho thời khóa biểu (lưới tuần FE-05 "Lịch giảng dạy").
 * partnerName: tên học viên nếu người xem là gia sư, tên gia sư nếu người xem là học viên.
 * classStartedAt: ngày mở lớp — giao diện chỉ vẽ buổi học từ tuần này trở đi.
 */
public record TimetableEntryResponse(
        UUID scheduleId,
        UUID spaceId,
        String classTitle,
        String partnerName,
        LearningSpaceStatus classStatus,
        int dayOfWeek,
        @JsonFormat(pattern = "HH:mm") LocalTime startTime,
        @JsonFormat(pattern = "HH:mm") LocalTime endTime,
        OffsetDateTime classStartedAt) {
}
