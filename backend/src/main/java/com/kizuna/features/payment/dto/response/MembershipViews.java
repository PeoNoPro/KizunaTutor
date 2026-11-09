package com.kizuna.features.payment.dto.response;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Dữ liệu trả về của API FE-09 (contracts/membership-api.md). Tiền là số nguyên VND; mọi mã trạng thái kèm nhãn tiếng Việt.
 */
public final class MembershipViews {

    private MembershipViews() {
    }

    public record PageView<T>(List<T> items, int page, int size, long totalItems, int totalPages) {
    }

    // ---------- Gia sư ----------

    public record PlanView(UUID planId, String planName, int durationMonths, long price, List<String> benefits) {
    }

    public record PaymentCreatedView(UUID transactionId, long amount, String planName, String paymentUrl,
                                     OffsetDateTime expiresAt) {
    }

    public record PaymentView(UUID transactionId, String planName, long amount, String status, String statusLabel,
                              OffsetDateTime transactionTime, boolean resumable) {
    }

    public record MembershipItemView(UUID membershipId, String planName, LocalDate startDate, LocalDate endDate,
                                     String status, String statusLabel, long amount, Long daysLeft,
                                     OffsetDateTime registeredAt) {
    }

    public record MyMembershipView(boolean eligible, String ineligibleReason, String tutorState, String tutorStateLabel,
                                   MembershipItemView current, List<MembershipItemView> upcoming, LocalDate expiresOn,
                                   List<MembershipItemView> history,
                                   /* Thời hạn (tháng) của gói cao nhất còn hiệu lực; null nếu chưa có. Chỉ gói dài hơn mới mua được. */
                                   Integer ownedDurationMonths) {
    }

    public record PeriodView(LocalDate startDate, LocalDate endDate) {
    }

    public record PaymentResultView(UUID transactionId, String status, String statusLabel, String resultMessage,
                                    String planName, long amount, PeriodView membership) {
    }

    // ---------- Quản lý vận hành ----------

    public record PlanAdminView(UUID planId, String planName, int durationMonths, long price, List<String> benefits,
                                boolean active, String activeLabel, OffsetDateTime createdAt) {
    }

    public record TutorMembershipRowView(UUID tutorId, String tutorName, String email, String planName,
                                         OffsetDateTime registeredAt, String status, String statusLabel,
                                         LocalDate expiresOn) {
    }

    public record TutorInfoView(UUID tutorId, String fullName, String email, String phone, String tutorStateLabel) {
    }

    public record TutorPaymentView(UUID transactionId, String planName, long amount, String status, String statusLabel,
                                   OffsetDateTime transactionTime, String gatewayTransactionId) {
    }

    public record TutorMembershipDetailView(TutorInfoView tutor, LocalDate expiresOn, List<MembershipItemView> memberships,
                                            List<TutorPaymentView> payments, boolean paymentsUnavailable) {
    }

    // ---------- Quản trị viên ----------

    public record AdminTransactionRowView(UUID transactionId, String txnRef, String tutorName, String email,
                                          String planName, long amount, String status, String statusLabel,
                                          boolean stale, OffsetDateTime transactionTime, String gatewayTransactionId) {
    }

    public record LogView(String type, String typeLabel, OffsetDateTime loggedAt, Integer httpStatusCode, String summary) {
    }

    public record NoteView(String note, String adminName, OffsetDateTime createdAt) {
    }

    public record AdminTransactionDetailView(AdminTransactionRowView transaction, MembershipItemView membership,
                                             List<LogView> logs, List<NoteView> notes) {
    }

    public record GatewayCheckView(String statusBefore, String statusAfter, String statusAfterLabel,
                                   String gatewayResponseCode, String gatewayMessage) {
    }
}
