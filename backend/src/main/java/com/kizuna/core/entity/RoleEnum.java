package com.kizuna.core.entity;

import lombok.Getter;

/**
 * Định nghĩa chuẩn 6 Actor trong hệ thống KizunaTutor.
 * Ánh xạ chính xác giữa Enum, Route điều hướng, Tên hiển thị Badge và Phong cách màu sắc Figma.
 */
@Getter
public enum RoleEnum {
    ROLE_ADMIN(
            "ROLE_ADMIN",
            "/admin/dashboard",
            "Quản Trị Viên",
            "bg-slate-900 text-white border-slate-700",
            "#0F172A",
            "Toàn quyền quản trị hệ thống, giám sát kỹ thuật và bảo mật"
    ),
    ROLE_MANAGER(
            "ROLE_MANAGER",
            "/manager/dashboard",
            "Quản Lý Vận Hành",
            "bg-indigo-700 text-white border-indigo-600",
            "#4338CA",
            "Điều hành kinh doanh, doanh thu, gói hội viên và hợp đồng gia sư"
    ),
    ROLE_CS_STAFF(
            "ROLE_CS_STAFF",
            "/cs/dashboard",
            "Chăm Sóc Khách Hàng",
            "bg-sky-600 text-white border-sky-500",
            "#0284C7",
            "Tiếp nhận và giải quyết vé hỗ trợ, khiếu nại của học viên và gia sư"
    ),
    ROLE_ACADEMIC_OFFICER(
            "ROLE_ACADEMIC_OFFICER",
            "/academic/dashboard",
            "Cán Bộ Học Vụ",
            "bg-teal-600 text-white border-teal-500",
            "#0D9488",
            "Thẩm định hồ sơ bằng cấp gia sư, kiểm duyệt chuẩn hóa từ điển Kanji"
    ),
    ROLE_TUTOR(
            "ROLE_TUTOR",
            "/tutor/dashboard",
            "Gia Sư Kizuna",
            "bg-amber-600 text-white border-amber-500",
            "#D97706",
            "Quản lý lịch dạy, lớp học trực tuyến và theo dõi bài tập học viên"
    ),
    ROLE_LEARNER(
            "ROLE_LEARNER",
            "/learner/dashboard",
            "Học Viên",
            "bg-emerald-600 text-white border-emerald-500",
            "#059669",
            "Tham gia khóa học, luyện viết Kanji AI và đặt lịch học thử"
    );

    private final String roleName;
    private final String dashboardRoute;
    private final String badgeText;
    private final String badgeClass;
    private final String brandColor;
    private final String roleDescription;

    RoleEnum(String roleName, String dashboardRoute, String badgeText, String badgeClass, String brandColor, String roleDescription) {
        this.roleName = roleName;
        this.dashboardRoute = dashboardRoute;
        this.badgeText = badgeText;
        this.badgeClass = badgeClass;
        this.brandColor = brandColor;
        this.roleDescription = roleDescription;
    }

    /**
     * Tìm kiếm RoleEnum theo chuỗi roleName (ví dụ "ROLE_ADMIN", "ADMIN")
     */
    public static RoleEnum fromString(String roleStr) {
        if (roleStr == null) return ROLE_LEARNER;
        String normalized = roleStr.trim().toUpperCase();
        if (!normalized.startsWith("ROLE_")) {
            normalized = "ROLE_" + normalized;
        }
        for (RoleEnum r : values()) {
            if (r.name().equalsIgnoreCase(normalized) || r.roleName.equalsIgnoreCase(normalized)) {
                return r;
            }
        }
        return ROLE_LEARNER;
    }
}
