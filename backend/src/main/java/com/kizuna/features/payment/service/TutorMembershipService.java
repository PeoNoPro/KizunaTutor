package com.kizuna.features.payment.service;

import com.kizuna.core.entity.User;
import com.kizuna.features.payment.dto.response.MembershipViews;

import java.util.List;
import java.util.Map;

/**
 * Nghiệp vụ gói hội viên phía gia sư: UC-104 (mua gói) và UC-97 (xem gói, ngày hết hạn).
 */
public interface TutorMembershipService {

    /**
     * Gia sư có gói hội viên còn hiệu lực vào ngày {@code today} (trạng thái ACTIVE, chưa hết hạn).
     * Feature khác (lớp học, bài đăng) dùng phương thức này thay vì gọi thẳng repository của payment.
     */
    boolean hasActiveMembership(java.util.UUID tutorId, java.time.LocalDate today);

    /** Ngày hết hạn xa nhất trong các gói còn hiệu lực; rỗng nếu không có gói nào còn hạn. */
    java.util.Optional<java.time.LocalDate> activeMembershipEndDate(java.util.UUID tutorId, java.time.LocalDate today);

    List<MembershipViews.PlanView> listPlans();

    MembershipViews.MyMembershipView getMyMembership(User user);

    MembershipViews.PaymentCreatedView createPayment(User user, String planId, String clientIp);

    MembershipViews.PaymentResultView handleReturn(User user, Map<String, String> params, String clientIp);

    MembershipViews.PageView<MembershipViews.PaymentView> listMyPayments(User user, int page, int size);

    /** A6: gia sư tự kiểm tra giao dịch còn chờ của mình với cổng (querydr). */
    MembershipViews.PaymentResultView checkMyPayment(User user, String transactionId, String clientIp);

    /** A7: mở lại trang thanh toán cho giao dịch còn chờ, còn trong hạn. */
    MembershipViews.PaymentCreatedView resumePayment(User user, String transactionId, String clientIp);
}
