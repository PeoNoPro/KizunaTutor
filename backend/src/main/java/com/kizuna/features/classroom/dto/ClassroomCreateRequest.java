package com.kizuna.features.classroom.dto;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Gia sư tạo lớp cho một học viên đã kết nối (UC-93): lịch học hằng tuần (ít nhất 1 buổi),
 * ngày bắt đầu khóa (từ hôm nay trở đi) và tổng số buổi của khóa (1–500) — lịch tự sinh cho đủ số buổi rồi dừng.
 * Hình thức học (ONLINE | OFFLINE), link phòng học, địa điểm: để trống = lấy theo yêu cầu kết nối đã nhận.
 * Kiểm tra dữ liệu thực hiện trong service.
 */
public record ClassroomCreateRequest(UUID learnerId,
                                     String title,
                                     List<ClassScheduleRequest> schedules,
                                     @JsonFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
                                     Integer totalSessions,
                                     String learningMode,
                                     String meetingLink,
                                     String locationDetails) {

    public ClassroomCreateRequest(UUID learnerId, String title, List<ClassScheduleRequest> schedules,
                                  LocalDate startDate, Integer totalSessions) {
        this(learnerId, title, schedules, startDate, totalSessions, null, null, null);
    }
}
