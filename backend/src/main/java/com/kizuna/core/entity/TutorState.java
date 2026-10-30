package com.kizuna.core.entity;

import lombok.Getter;

/**
 * State Machine biểu diễn vòng đời trạng thái của Gia Sư (Tutor Lifecycle):
 * 1. DRAFT_PROFILE: Vừa tạo tài khoản, chưa hoàn thiện hồ sơ giảng dạy.
 * 2. PENDING_VERIFICATION: Đã gửi hồ sơ lên Cán bộ học vụ (Academic Officer), đang chờ duyệt.
 * 3. REJECTED: Hồ sơ bị từ chối hoặc yêu cầu bổ sung thông tin, bằng cấp.
 * 4. APPROVED_UNPAID: Hồ sơ đã được phê duyệt thành công, nhưng chưa thanh toán gói thành viên.
 * 5. ACTIVE_PAID: Đã thanh toán gói thành viên thành công (VNPay), mở khóa 100% tính năng giảng dạy.
 */
@Getter
public enum TutorState {
    DRAFT_PROFILE("Chưa Nộp Hồ Sơ", "bg-slate-100 text-slate-700 border-slate-200", "Bắt buộc"),
    PENDING_VERIFICATION("Đang Chờ Xác Minh", "bg-amber-50 text-amber-700 border-amber-200", "Đang duyệt"),
    REJECTED("Yêu Cầu Sửa Đổi", "bg-rose-50 text-rose-700 border-rose-200", "Cần nộp lại"),
    APPROVED_UNPAID("Chờ Kích Hoạt Gói", "bg-purple-50 text-purple-700 border-purple-200", "Cần kích hoạt"),
    ACTIVE_PAID("Đang Hoạt Động", "bg-emerald-50 text-emerald-700 border-emerald-200", "Chính thức");

    private final String displayName;
    private final String badgeClass;
    private final String sidebarTag;

    TutorState(String displayName, String badgeClass, String sidebarTag) {
        this.displayName = displayName;
        this.badgeClass = badgeClass;
        this.sidebarTag = sidebarTag;
    }
}
