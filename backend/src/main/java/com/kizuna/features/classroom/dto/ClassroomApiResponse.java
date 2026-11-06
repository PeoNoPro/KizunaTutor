package com.kizuna.features.classroom.dto;

import java.time.OffsetDateTime;

/** Định dạng phản hồi chuẩn của API FE-06: {success, message, data, timestamp}. */
public record ClassroomApiResponse<T>(boolean success, String message, T data, OffsetDateTime timestamp) {

    public static <T> ClassroomApiResponse<T> ok(String message, T data) {
        return new ClassroomApiResponse<>(true, message, data, OffsetDateTime.now());
    }

    public static <T> ClassroomApiResponse<T> error(String message, T data) {
        return new ClassroomApiResponse<>(false, message, data, OffsetDateTime.now());
    }
}
