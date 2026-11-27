package com.kizuna.features.classroom.dto;

/** Nội dung tệp tài liệu trả về cho người dùng có quyền (đọc từ cột file_data). */
public record MaterialFileContent(byte[] data, String contentType, String fileName) {
}
