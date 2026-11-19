package com.kizuna.features.classroom.dto;

import java.util.UUID;

/**
 * Học viên gia sư được phép mở lớp. learningMode / locationDetails: hình thức học và địa điểm học viên đã chọn
 * trong yêu cầu kết nối được nhận gần nhất (null nếu không có), để form tạo lớp điền sẵn.
 */
public record EligibleLearnerResponse(UUID learnerId, String fullName, String email, String avatarUrl,
                                      String learningMode, String locationDetails) {

    public EligibleLearnerResponse(UUID learnerId, String fullName, String email, String avatarUrl) {
        this(learnerId, fullName, email, avatarUrl, null, null);
    }
}
