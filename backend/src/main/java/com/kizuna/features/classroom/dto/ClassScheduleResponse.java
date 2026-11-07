package com.kizuna.features.classroom.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.kizuna.features.classroom.entity.ClassSchedule;

import java.time.LocalTime;
import java.util.UUID;

public record ClassScheduleResponse(
        UUID scheduleId,
        int dayOfWeek,
        @JsonFormat(pattern = "HH:mm") LocalTime startTime,
        @JsonFormat(pattern = "HH:mm") LocalTime endTime) {

    public static ClassScheduleResponse of(ClassSchedule s) {
        return new ClassScheduleResponse(s.getScheduleId(), s.getDayOfWeek(), s.getStartTime(), s.getEndTime());
    }
}
