package com.kizuna.features.payment.service;

import com.kizuna.core.entity.Tutor;
import com.kizuna.core.entity.TutorState;
import com.kizuna.core.entity.User;
import com.kizuna.features.payment.entity.MembershipPlan;
import com.kizuna.features.payment.entity.PaymentTransaction;
import com.kizuna.features.payment.entity.PaymentTransactionStatus;
import com.kizuna.features.payment.entity.TutorMembership;
import com.kizuna.features.payment.entity.TutorMembershipState;
import com.kizuna.features.payment.entity.TutorMembershipStatus;
import com.kizuna.features.payment.repository.MembershipQuerySpecification;
import com.kizuna.features.payment.repository.MembershipTutorRepository;
import com.kizuna.features.payment.repository.PaymentTransactionRepository;
import com.kizuna.features.payment.repository.TutorMembershipRepository;
import com.kizuna.features.verification.entity.TutorVerificationStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

/**
 * Bước xử lý kết quả thanh toán DUY NHẤT, dùng chung cho IPN, trang trả về và tra cứu của quản trị viên (phương án C).
 * Idempotent (FR-006): khóa dòng giao dịch; chỉ giao dịch còn PENDING mới được đổi; SUCCESS/FAILED là trạng thái cuối (GB-01).
 * Kích hoạt (FR-007) chạy trong cùng transaction; lỗi bất kỳ → rollback, giao dịch vẫn PENDING để xử lý lại mà không thu tiền
 * lần nữa (UC-104 AF-1).
 */
@Slf4j
@Service
public class PaymentResultProcessor {

    public enum Outcome { NOT_FOUND, AMOUNT_MISMATCH, ALREADY_PROCESSED, ACTIVATED, FAILED, STILL_PENDING }

    private final PaymentTransactionRepository transactionRepository;
    private final TutorMembershipRepository membershipRepository;
    private final MembershipTutorRepository tutorRepository;
    private final MembershipPeriodCalculator periodCalculator;
    private final PaymentNotificationService notificationService;

    public PaymentResultProcessor(PaymentTransactionRepository transactionRepository,
                                  TutorMembershipRepository membershipRepository,
                                  MembershipTutorRepository tutorRepository,
                                  MembershipPeriodCalculator periodCalculator,
                                  PaymentNotificationService notificationService) {
        this.transactionRepository = transactionRepository;
        this.membershipRepository = membershipRepository;
        this.tutorRepository = tutorRepository;
        this.periodCalculator = periodCalculator;
        this.notificationService = notificationService;
    }

    /** Kết quả phải đã được xác thực chữ ký trước khi gọi. */
    @Transactional
    public Outcome process(GatewayResult result) {
        UUID transactionId = MembershipQuerySpecification.parseTransactionKeyword(result.txnRef());
        if (transactionId == null) {
            return Outcome.NOT_FOUND;
        }
        Optional<PaymentTransaction> locked = transactionRepository.findLockedById(transactionId);
        if (locked.isEmpty()) {
            return Outcome.NOT_FOUND;
        }
        PaymentTransaction tx = locked.get();

        long expectedAmount = VnpayGatewayClient.amountTimes100(tx.getAmount());
        if (result.amountTimes100() == null || result.amountTimes100() != expectedAmount) {
            log.warn("Số tiền cổng báo cho giao dịch {} không khớp: {} ≠ {}", tx.getTxnRef(), result.amountTimes100(), expectedAmount);
            return Outcome.AMOUNT_MISMATCH;
        }
        if (tx.getStatus() != PaymentTransactionStatus.PENDING) {
            return Outcome.ALREADY_PROCESSED;
        }
        if (result.isSuccess()) {
            activate(tx, result);
            return Outcome.ACTIVATED;
        }
        if (result.isStillPending()) {
            return Outcome.STILL_PENDING;
        }
        tx.setStatus(PaymentTransactionStatus.FAILED);
        applyGatewayNo(tx, result);
        log.info("Giao dịch {} thất bại (mã {} / {})", tx.getTxnRef(), result.responseCode(), result.transactionStatus());
        return Outcome.FAILED;
    }

    private void activate(PaymentTransaction tx, GatewayResult result) {
        User user = tx.getUser();
        MembershipPlan plan = tx.getPlan();
        Tutor tutor = tutorRepository.findLockedByUserId(user.getUserId())
                .orElseThrow(() -> new IllegalStateException("Giao dịch " + tx.getTxnRef() + " không gắn với hồ sơ gia sư"));

        String reference = tx.getTransactionId().toString();
        if (membershipRepository.findFirstByTransactionRef(reference).isEmpty()) {
            LocalDate today = LocalDate.now(VnpayGatewayClient.VIETNAM);
            LocalDate latestEnd = membershipRepository
                    .findMaxEndDate(tutor.getTutorId(), TutorMembershipState.ACTIVE).orElse(null);
            MembershipPeriodCalculator.Period period = periodCalculator.calculate(today, latestEnd, plan.getDurationMonths());

            membershipRepository.save(TutorMembership.builder()
                    .tutor(tutor)
                    .plan(plan)
                    .startDate(period.startDate())
                    .endDate(period.endDate())
                    .status(TutorMembershipState.ACTIVE)
                    .amount(tx.getAmount())
                    .transactionRef(reference)
                    .paymentStatus("SUCCESS")
                    .paymentGateway("VNPay")
                    .build());

            notificationService.membershipActivatedAfterCommit(user.getEmail(), user.getFullName(), plan.getPlanName(),
                    period.startDate(), period.endDate(), VnpayGatewayClient.amountTimes100(tx.getAmount()) / 100);
        }

        tx.setStatus(PaymentTransactionStatus.SUCCESS);
        applyGatewayNo(tx, result);
        if (tutor.getVerificationStatus() == TutorVerificationStatus.APPROVED) {
            tutor.setTutorState(TutorState.ACTIVE_PAID);
        }
        tutor.setMembershipStatus(TutorMembershipStatus.PREMIUM);
        log.info("Đã kích hoạt gói {} cho gia sư {} từ giao dịch {}", plan.getPlanName(), tutor.getTutorId(), tx.getTxnRef());
    }

    private static void applyGatewayNo(PaymentTransaction tx, GatewayResult result) {
        String no = result.transactionNo();
        if (no != null && !no.isBlank() && !"0".equals(no.trim())) {
            tx.setGatewayTransactionId(no.trim());
        }
    }
}
