package com.kizuna.features.learning_support.dto;

/**
 * Trạng thái mở khóa công cụ học của người dùng hiện tại.
 * allowed = false khi học viên chưa thuộc lớp học nào còn mở (ACTIVE/PAUSED).
 */
public record LearningToolAccessResponse(boolean allowed, long openClassCount, String message) {
}
