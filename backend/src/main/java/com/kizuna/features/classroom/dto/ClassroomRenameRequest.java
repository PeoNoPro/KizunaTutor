package com.kizuna.features.classroom.dto;

/** Gia sư đổi tên lớp học. Kiểm tra dữ liệu (1–150 ký tự) thực hiện trong service. */
public record ClassroomRenameRequest(String title) {
}
