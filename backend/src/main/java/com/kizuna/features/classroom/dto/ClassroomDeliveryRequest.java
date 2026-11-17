package com.kizuna.features.classroom.dto;

/**
 * Gia sư đổi hình thức học của lớp: ONLINE (kèm link phòng học, có thể bổ sung sau) hoặc OFFLINE (bắt buộc địa điểm).
 */
public record ClassroomDeliveryRequest(String learningMode, String meetingLink, String locationDetails) {
}
