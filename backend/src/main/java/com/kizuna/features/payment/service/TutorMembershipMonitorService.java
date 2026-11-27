package com.kizuna.features.payment.service;

import com.kizuna.features.payment.dto.response.MembershipViews;

/**
 * Giám sát hội viên của quản lý vận hành (UC-44, UC-45). Chỉ đọc, trừ cập nhật hết hạn khi xem chi tiết (FR-013).
 */
public interface TutorMembershipMonitorService {

    MembershipViews.PageView<MembershipViews.TutorMembershipRowView> listMemberships(String keyword, String planId,
                                                                                     String status, int page, int size);

    MembershipViews.TutorMembershipDetailView getDetail(String tutorId);
}
