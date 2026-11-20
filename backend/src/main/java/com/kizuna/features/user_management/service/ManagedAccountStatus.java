package com.kizuna.features.user_management.service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * FE-14: nhãn tiếng Việt của trạng thái tài khoản và nhóm người dùng mà Quản lý được xem (SRS UC-12, UC-20).
 * Mã (ACTIVE, ROLE_TUTOR...) chỉ dùng nội bộ, không hiển thị trên giao diện.
 */
public final class ManagedAccountStatus {

    /** Giá trị của user_account_status_enum trong DB, theo thứ tự hiển thị. */
    public static final Map<String, String> STATUS_LABELS;

    static {
        Map<String, String> labels = new LinkedHashMap<>();
        labels.put("PENDING", "Chờ kích hoạt");
        labels.put("ACTIVE", "Đang hoạt động");
        labels.put("SUSPENDED", "Tạm khóa");
        labels.put("BANNED", "Khóa vĩnh viễn");
        STATUS_LABELS = java.util.Collections.unmodifiableMap(labels);
    }

    /** Trạng thái Quản lý được chọn (UC-12 bước 3). PENDING thuộc luồng đăng ký, không đổi ở đây. */
    public static final List<String> TARGET_STATUSES = List.of("ACTIVE", "SUSPENDED", "BANNED");

    /** Nhóm trên giao diện → vai trò trong DB. Không bao giờ gồm vai trò nhân viên. */
    public static final Map<String, String> CATEGORY_ROLES = Map.of("TUTOR", "ROLE_TUTOR", "LEARNER", "ROLE_LEARNER");

    public static final Map<String, String> CATEGORY_LABELS = Map.of("TUTOR", "Gia sư", "LEARNER", "Học viên");

    public static final Set<Integer> PAGE_SIZES = Set.of(10, 20, 50);

    private ManagedAccountStatus() {
    }

    public static String label(String status) {
        return STATUS_LABELS.getOrDefault(status, "Cần kiểm tra trạng thái");
    }

    public static String categoryOfRole(String roleName) {
        return CATEGORY_ROLES.entrySet().stream()
                .filter(e -> e.getValue().equals(roleName))
                .map(Map.Entry::getKey)
                .findFirst().orElse(null);
    }
}
