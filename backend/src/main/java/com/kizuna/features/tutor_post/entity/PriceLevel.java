package com.kizuna.features.tutor_post.entity;

/**
 * Môn học / cấp độ gia sư có thể đặt học phí riêng trong bài đăng (danh sách cố định, mỗi mục tối đa một mức giá).
 * Thứ tự khai báo là thứ tự hiển thị.
 */
public enum PriceLevel {
    JLPT_N5("JLPT N5"),
    JLPT_N4("JLPT N4"),
    JLPT_N3("JLPT N3"),
    JLPT_N2("JLPT N2"),
    JLPT_N1("JLPT N1"),
    KAIWA("Giao tiếp (Kaiwa)"),
    BUSINESS("Tiếng Nhật thương mại"),
    EJU("Luyện thi EJU");

    private final String label;

    PriceLevel(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }

    /** Đọc mã gửi từ giao diện; null nếu không hợp lệ. */
    public static PriceLevel fromCode(String code) {
        if (code == null) {
            return null;
        }
        try {
            return valueOf(code.trim().toUpperCase(java.util.Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
