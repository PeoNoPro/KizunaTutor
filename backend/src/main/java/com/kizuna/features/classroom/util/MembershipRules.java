package com.kizuna.features.classroom.util;

import com.kizuna.core.entity.Tutor;
import com.kizuna.features.payment.entity.TutorMembershipStatus;

/**
 * Gói hội viên của gia sư dùng cho phía học viên: gia sư chưa có gói / hết gói thì lớp ở chế độ chỉ xem với học viên
 * (vẫn xem lớp, tài liệu, bài tập, điểm nhưng không nộp bài được).
 * Bản thân gia sư chưa có gói / hết gói bị chặn mọi chức năng lớp học ở CurrentUserResolver nên không xét lại ở đây.
 */
public final class MembershipRules {

    private MembershipRules() {
    }

    public static boolean isActive(Tutor tutor) {
        TutorMembershipStatus status = tutor == null ? null : tutor.getMembershipStatus();
        return status == TutorMembershipStatus.PREMIUM || status == TutorMembershipStatus.STANDARD;
    }
}
