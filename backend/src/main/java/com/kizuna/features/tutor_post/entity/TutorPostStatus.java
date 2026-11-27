package com.kizuna.features.tutor_post.entity;

/** Khớp kiểu tutor_post_status_enum trên DB (NEEDS_REVISION thêm ở migration 012). */
public enum TutorPostStatus {
    PENDING_REVIEW("Chờ duyệt"),
    NEEDS_REVISION("Cần bổ sung"),
    PUBLISHED("Đã duyệt"),
    REJECTED("Bị từ chối");

    private final String label;

    TutorPostStatus(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }

    /** Mã trả cho frontend: PENDING | NEEDS_INFO | APPROVED | REJECTED. */
    public String code() {
        return switch (this) {
            case PENDING_REVIEW -> "PENDING";
            case NEEDS_REVISION -> "NEEDS_INFO";
            case PUBLISHED -> "APPROVED";
            case REJECTED -> "REJECTED";
        };
    }
}
