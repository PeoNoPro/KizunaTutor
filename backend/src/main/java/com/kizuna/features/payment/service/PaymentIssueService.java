package com.kizuna.features.payment.service;

import com.kizuna.core.entity.User;
import com.kizuna.features.payment.dto.response.MembershipViews;

/**
 * Xử lý sự cố thanh toán của quản trị viên (UC-98): xem giao dịch và nhật ký, kiểm tra với cổng, ghi kết quả điều tra.
 */
public interface PaymentIssueService {

    MembershipViews.PageView<MembershipViews.AdminTransactionRowView> listTransactions(String status, String from, String to,
                                                                                       String keyword, int page, int size);

    MembershipViews.AdminTransactionDetailView getDetail(String transactionId);

    MembershipViews.GatewayCheckView checkWithGateway(String transactionId, User admin, String clientIp);

    MembershipViews.NoteView addNote(String transactionId, User admin, String note);
}
