package com.kizuna.features.classroom.dto;

import java.util.UUID;

/** Một tệp của tài liệu học tập. Không chứa dữ liệu tệp. fileType: PDF, DOC, DOCX, PNG, JPG, MP3. */
public record MaterialFileResponse(UUID fileId, String fileName, String fileType, long fileSize) {
}
