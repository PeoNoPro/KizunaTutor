package com.kizuna.features.progress.dto;

/** Tệp đề bài của bài tập sau khi tải lên/gỡ. attachmentName null = bài tập không có tệp. */
public record AssignmentAttachmentResponse(String attachmentName, String attachmentType) {
}
