package com.kizuna.core.dto;

import java.time.OffsetDateTime;
import java.time.ZoneId;

/**
 * Khung phản hồi thành công theo BACKEND_RULES.md mục 6.2:
 * { "success": true, "message": "...", "data": {...}, "timestamp": "...+07:00" }.
 */
public record ApiResponse<T>(boolean success, String message, T data, OffsetDateTime timestamp) {

    private static final ZoneId VIETNAM = ZoneId.of("Asia/Ho_Chi_Minh");

    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(true, "Thành công.", data, OffsetDateTime.now(VIETNAM));
    }

    public static <T> ApiResponse<T> ok(String message, T data) {
        return new ApiResponse<>(true, message, data, OffsetDateTime.now(VIETNAM));
    }
}
