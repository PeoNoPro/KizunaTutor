package com.kizuna.dto.auth;

import lombok.Getter;

/**
 * Phân loại 3 trạng thái người dùng khi truy cập phân hệ "HỌC TẬP & LUYỆN TẬP":
 * 1. UNAUTHENTICATED: Khách vãng lai (chưa đăng nhập).
 * 2. UNASSIGNED_LEARNER: Đã đăng nhập vai trò Learner nhưng chưa được Gia sư thêm vào lớp.
 * 3. ACTIVE_OFFICIAL_LEARNER: Học viên chính thức, đã thuộc Không gian học tập (Learning Space) đang hoạt động.
 */
@Getter
public enum LearnerAccessState {
    UNAUTHENTICATED("Khách vãng lai (Chưa đăng nhập)"),
    UNASSIGNED_LEARNER("Học viên chưa liên kết Gia sư"),
    ACTIVE_OFFICIAL_LEARNER("Học viên chính thức (Đã mở khóa toàn diện)");

    private final String description;

    LearnerAccessState(String description) {
        this.description = description;
    }
}
