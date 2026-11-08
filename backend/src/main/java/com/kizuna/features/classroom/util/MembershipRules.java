package com.kizuna.features.classroom.util;

import com.kizuna.core.entity.Tutor;
import com.kizuna.features.payment.entity.TutorMembershipStatus;

/**
 * Điều kiện gói hội viên cho quản lý lớp học (SRS BF I.2.4 "membership still active"; FE-09 srs-gaps G-06):
 * chỉ gia sư có gói còn hiệu lực (PREMIUM/STANDARD) mới được mở lớp và thay đổi nội dung lớp.
 * FREE = chưa từng mua gói, EXPIRED = gói đã hết hạn → lớp ở chế độ chỉ xem.
 */
public final class MembershipRules {

    private MembershipRules() {
    }

    public static boolean isActive(Tutor tutor) {
        TutorMembershipStatus status = tutor == null ? null : tutor.getMembershipStatus();
        return status == TutorMembershipStatus.PREMIUM || status == TutorMembershipStatus.STANDARD;
    }

    /** Thông báo khi gia sư chưa có gói / hết gói mà tạo lớp mới. */
    public static String createBlockedMessage(Tutor tutor) {
        return tutor.getMembershipStatus() == TutorMembershipStatus.EXPIRED
                ? "Gói hội viên của bạn đã hết hạn. Vui lòng gia hạn để mở lớp học mới."
                : "Bạn chưa có gói hội viên. Vui lòng đăng ký gói hội viên để mở lớp học.";
    }

    /** Thông báo khi gia sư chưa có gói / hết gói mà thay đổi nội dung lớp (tài liệu, bài tập, lịch học). */
    public static String readOnlyMessage(Tutor tutor) {
        return tutor.getMembershipStatus() == TutorMembershipStatus.EXPIRED
                ? "Gói hội viên của bạn đã hết hạn nên lớp học đang ở chế độ chỉ xem."
                : "Bạn chưa có gói hội viên nên lớp học đang ở chế độ chỉ xem. Vui lòng đăng ký gói để tiếp tục.";
    }
}
