package com.kizuna.features.classroom.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.kizuna.features.classroom.entity.LearningSpaceStatus;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

/**
 * Một buổi học cụ thể của khóa (dùng cho lưới thời khóa biểu tuần).
 * - Buổi theo lịch hằng tuần: scheduleId + originalDate (ngày gốc); moved = đã được dời riêng (sang ngày bất kỳ trong khóa).
 * - Buổi thêm riêng: extra = true, extraId.
 * number / totalSessions: buổi thứ mấy của khóa (totalSessions null = lớp cũ, không giới hạn).
 * courseStartDate / courseEndDate: ngày bắt đầu khóa và ngày buổi cuối (null nếu không giới hạn) — giới hạn ngày khi dời buổi.
 */
public record ClassSessionResponse(
        UUID spaceId,
        String classTitle,
        String partnerName,
        LearningSpaceStatus classStatus,
        UUID scheduleId,
        UUID extraId,
        @JsonFormat(pattern = "yyyy-MM-dd") LocalDate originalDate,
        @JsonFormat(pattern = "yyyy-MM-dd") LocalDate date,
        @JsonFormat(pattern = "HH:mm") LocalTime startTime,
        @JsonFormat(pattern = "HH:mm") LocalTime endTime,
        boolean moved,
        boolean extra,
        int number,
        Integer totalSessions,
        @JsonFormat(pattern = "yyyy-MM-dd") LocalDate courseStartDate,
        @JsonFormat(pattern = "yyyy-MM-dd") LocalDate courseEndDate) {
}
