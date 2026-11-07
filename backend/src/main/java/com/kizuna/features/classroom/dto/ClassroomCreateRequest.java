package com.kizuna.features.classroom.dto;

import java.util.List;
import java.util.UUID;

/**
 * Gia sư tạo lớp cho một học viên đã kết nối (UC-93), kèm lịch học hằng tuần (ít nhất 1 buổi).
 * Kiểm tra dữ liệu thực hiện trong service.
 */
public record ClassroomCreateRequest(UUID learnerId, String title, List<ClassScheduleRequest> schedules) {
}
