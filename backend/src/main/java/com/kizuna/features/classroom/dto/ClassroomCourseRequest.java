package com.kizuna.features.classroom.dto;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDate;

/**
 * Gia sư sửa thông tin khóa học: tổng số buổi (không nhỏ hơn số buổi đã học)
 * và ngày bắt đầu khóa (chỉ đổi được khi khóa chưa có buổi nào diễn ra).
 */
public record ClassroomCourseRequest(@JsonFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
                                     Integer totalSessions) {
}
