package com.kizuna.features.classroom.dto;

import java.util.UUID;

/** Gia sư mở lớp cho một học viên đủ điều kiện (UC-93). Kiểm tra dữ liệu thực hiện trong service. */
public record ClassroomCreateRequest(UUID learnerId, String title) {
}
