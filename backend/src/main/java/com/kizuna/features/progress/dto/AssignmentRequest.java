package com.kizuna.features.progress.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/** Tạo/sửa bài tập (UC-66, UC-95). dueDate null = không có hạn; totalPoints null = 100. */
public record AssignmentRequest(String title, String description, OffsetDateTime dueDate, BigDecimal totalPoints) {
}
