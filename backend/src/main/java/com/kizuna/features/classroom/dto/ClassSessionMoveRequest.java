package com.kizuna.features.classroom.dto;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Dời một buổi học sang ngày/giờ mới trong cùng tuần.
 * @param newDate ngày mới "yyyy-MM-dd" (cùng tuần Thứ Hai – Chủ nhật với ngày gốc)
 */
public record ClassSessionMoveRequest(
        @JsonFormat(pattern = "yyyy-MM-dd") LocalDate newDate,
        LocalTime startTime,
        LocalTime endTime) {
}
