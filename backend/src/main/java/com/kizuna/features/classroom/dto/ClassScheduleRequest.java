package com.kizuna.features.classroom.dto;

import java.time.LocalTime;

/** Một buổi học hằng tuần. dayOfWeek: 1 = Thứ Hai ... 7 = Chủ nhật; giờ dạng "HH:mm". Kiểm tra trong service. */
public record ClassScheduleRequest(Integer dayOfWeek, LocalTime startTime, LocalTime endTime) {
}
