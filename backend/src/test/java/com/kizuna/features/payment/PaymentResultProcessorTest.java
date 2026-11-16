package com.kizuna.features.payment;

import com.kizuna.core.entity.Tutor;
import com.kizuna.core.entity.TutorState;
import com.kizuna.core.entity.User;
import com.kizuna.features.payment.entity.MembershipPlan;
import com.kizuna.features.payment.entity.PaymentTransaction;
import com.kizuna.features.payment.entity.PaymentTransactionStatus;
import com.kizuna.features.payment.entity.TutorMembership;
import com.kizuna.features.payment.entity.TutorMembershipState;
import com.kizuna.features.payment.entity.TutorMembershipStatus;
import com.kizuna.features.payment.repository.MembershipTutorRepository;
import com.kizuna.features.payment.repository.PaymentTransactionRepository;
import com.kizuna.features.payment.repository.TutorMembershipRepository;
import com.kizuna.features.payment.service.GatewayResult;
import com.kizuna.features.payment.service.MembershipPeriodCalculator;
import com.kizuna.features.payment.service.PaymentNotificationService;
import com.kizuna.features.payment.service.PaymentResultProcessor;
import com.kizuna.features.payment.service.PaymentResultProcessor.Outcome;
import com.kizuna.features.verification.entity.TutorVerificationStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * FR-006/007/009, UC-104 AF-1, SC-002/003: xử lý kết quả chỉ một lần, đúng số tiền, kích hoạt nguyên khối.
 */
class PaymentResultProcessorTest {

    private PaymentTransactionRepository transactionRepository;
    private TutorMembershipRepository membershipRepository;
    private MembershipTutorRepository tutorRepository;
    private PaymentNotificationService notificationService;
    private PaymentResultProcessor processor;

    private PaymentTransaction tx;
    private Tutor tutor;

    @BeforeEach
    void setUp() {
        transactionRepository = mock(PaymentTransactionRepository.class);
        membershipRepository = mock(TutorMembershipRepository.class);
        tutorRepository = mock(MembershipTutorRepository.class);
        notificationService = mock(PaymentNotificationService.class);
        processor = new PaymentResultProcessor(transactionRepository, membershipRepository, tutorRepository,
                new MembershipPeriodCalculator(), notificationService);

        User user = User.builder().userId(UUID.randomUUID()).email("gs@kizuna.test").fullName("Gia sư A").build();
        MembershipPlan plan = MembershipPlan.builder().planId(UUID.randomUUID()).planName("Gói 1 tháng")
                .durationMonths(1).price(new BigDecimal("199000.00")).isActive(true).build();
        tx = PaymentTransaction.builder().transactionId(UUID.randomUUID()).user(user).plan(plan)
                .amount(new BigDecimal("199000.00")).status(PaymentTransactionStatus.PENDING)
                .transactionTime(OffsetDateTime.now()).build();
        tutor = Tutor.builder().tutorId(UUID.randomUUID()).user(user)
                .verificationStatus(TutorVerificationStatus.APPROVED).tutorState(TutorState.APPROVED_UNPAID)
                .membershipStatus(TutorMembershipStatus.FREE).build();

        when(transactionRepository.findLockedById(tx.getTransactionId())).thenReturn(Optional.of(tx));
        when(tutorRepository.findLockedByUserId(user.getUserId())).thenReturn(Optional.of(tutor));
        when(membershipRepository.findFirstByTransactionRef(anyString())).thenReturn(Optional.empty());
        when(membershipRepository.findMaxEndDate(any(), any())).thenReturn(Optional.empty());
    }

    private GatewayResult result(String responseCode, String status, long amountTimes100) {
        return new GatewayResult(tx.getTxnRef(), responseCode, status, "14123456", amountTimes100, "NCB");
    }

    @Test
    void successActivatesExactlyOneMembership() {
        Outcome outcome = processor.process(result("00", "00", 19_900_000L));

        assertEquals(Outcome.ACTIVATED, outcome);
        ArgumentCaptor<TutorMembership> saved = ArgumentCaptor.forClass(TutorMembership.class);
        verify(membershipRepository).save(saved.capture());
        TutorMembership m = saved.getValue();
        LocalDate today = LocalDate.now(ZoneId.of("Asia/Ho_Chi_Minh"));
        assertEquals(today, m.getStartDate());
        assertEquals(today.plusMonths(1).minusDays(1), m.getEndDate());
        assertEquals(TutorMembershipState.ACTIVE, m.getStatus());
        assertEquals("SUCCESS", m.getPaymentStatus());
        assertEquals(tx.getTransactionId().toString(), m.getTransactionRef());
        assertEquals(PaymentTransactionStatus.SUCCESS, tx.getStatus());
        assertEquals("14123456", tx.getGatewayTransactionId());
        assertEquals(TutorState.ACTIVE_PAID, tutor.getTutorState());
        assertEquals(TutorMembershipStatus.PREMIUM, tutor.getMembershipStatus());
        verify(notificationService).membershipActivatedAfterCommit(eq("gs@kizuna.test"), any(), eq("Gói 1 tháng"),
                any(), any(), eq(199_000L));
    }

    @Test
    void repeatedResultIsNotProcessedTwice() {
        processor.process(result("00", "00", 19_900_000L));
        Outcome second = processor.process(result("00", "00", 19_900_000L));

        assertEquals(Outcome.ALREADY_PROCESSED, second);
        verify(membershipRepository, times(1)).save(any());
    }

    @Test
    void amountMismatchChangesNothing() {
        Outcome outcome = processor.process(result("00", "00", 100L));

        assertEquals(Outcome.AMOUNT_MISMATCH, outcome);
        assertEquals(PaymentTransactionStatus.PENDING, tx.getStatus());
        verify(membershipRepository, never()).save(any());
    }

    @Test
    void cancelledPaymentMarksFailed() {
        Outcome outcome = processor.process(result("24", "02", 19_900_000L));

        assertEquals(Outcome.FAILED, outcome);
        assertEquals(PaymentTransactionStatus.FAILED, tx.getStatus());
        verify(membershipRepository, never()).save(any());
        assertEquals(TutorState.APPROVED_UNPAID, tutor.getTutorState());
    }

    @Test
    void stillPendingKeepsTransactionPending() {
        assertEquals(Outcome.STILL_PENDING, processor.process(result("00", "01", 19_900_000L)));
        assertEquals(PaymentTransactionStatus.PENDING, tx.getStatus());
    }

    @Test
    void unknownTransactionIsNotFound() {
        assertEquals(Outcome.NOT_FOUND, processor.process(new GatewayResult("khong-hop-le", "00", "00", "1", 1L, null)));
        assertEquals(Outcome.NOT_FOUND, processor.process(
                new GatewayResult(UUID.randomUUID().toString().replace("-", ""), "00", "00", "1", 1L, null)));
    }

    @Test
    void activationFailureThrowsSoTransactionRollsBackAndStaysRetryable() {
        when(membershipRepository.save(any())).thenThrow(new IllegalStateException("db"));

        assertThrows(IllegalStateException.class, () -> processor.process(result("00", "00", 19_900_000L)));
        // Trong DB thật transaction bị rollback; ở đây xác nhận trạng thái giao dịch chưa bị đặt SUCCESS trước khi lỗi.
        assertEquals(PaymentTransactionStatus.PENDING, tx.getStatus());
        verify(notificationService, never()).membershipActivatedAfterCommit(any(), any(), any(), any(), any(), anyLong());
    }

    @Test
    void existingMembershipForSameTransactionIsNotDuplicated() {
        when(membershipRepository.findFirstByTransactionRef(tx.getTransactionId().toString()))
                .thenReturn(Optional.of(TutorMembership.builder().build()));

        assertEquals(Outcome.ACTIVATED, processor.process(result("00", "00", 19_900_000L)));
        verify(membershipRepository, never()).save(any());
        assertEquals(PaymentTransactionStatus.SUCCESS, tx.getStatus());
    }

    @Test
    void stacksAfterLatestActiveMembership() {
        LocalDate latest = LocalDate.now(ZoneId.of("Asia/Ho_Chi_Minh")).plusDays(10);
        when(membershipRepository.findMaxEndDate(tutor.getTutorId(), TutorMembershipState.ACTIVE)).thenReturn(Optional.of(latest));

        processor.process(result("00", "00", 19_900_000L));

        ArgumentCaptor<TutorMembership> saved = ArgumentCaptor.forClass(TutorMembership.class);
        verify(membershipRepository).save(saved.capture());
        assertEquals(latest.plusDays(1), saved.getValue().getStartDate());
    }

    // ===== REQ-14: trạng thái cuối và chống trùng =====

    @Test
    void req14_failedTransactionIsFinalAndLaterSuccessDoesNotActivate() {
        assertEquals(Outcome.FAILED, processor.process(result("24", "02", 19_900_000L)));

        assertEquals(Outcome.ALREADY_PROCESSED, processor.process(result("00", "00", 19_900_000L)));

        assertEquals(PaymentTransactionStatus.FAILED, tx.getStatus());
        verify(membershipRepository, never()).save(any());
        assertEquals(TutorState.APPROVED_UNPAID, tutor.getTutorState());
        assertEquals(TutorMembershipStatus.FREE, tutor.getMembershipStatus());
    }

    @Test
    void req14_successfulTransactionIsFinalAndLaterFailureDoesNotChangeIt() {
        processor.process(result("00", "00", 19_900_000L));

        assertEquals(Outcome.ALREADY_PROCESSED, processor.process(result("24", "02", 19_900_000L)));

        assertEquals(PaymentTransactionStatus.SUCCESS, tx.getStatus());
        assertEquals(TutorState.ACTIVE_PAID, tutor.getTutorState());
        verify(membershipRepository, times(1)).save(any());
    }

    @Test
    void req14_failedPaymentDoesNotNotifyActivation() {
        processor.process(result("24", "02", 19_900_000L));

        verify(notificationService, never()).membershipActivatedAfterCommit(any(), any(), any(), any(), any(), anyLong());
    }

    @Test
    void req14_missingAmountIsTreatedAsMismatch() {
        assertEquals(Outcome.AMOUNT_MISMATCH, processor.process(result("00", "00", 0L)));
        assertEquals(Outcome.AMOUNT_MISMATCH, processor.process(
                new GatewayResult(tx.getTxnRef(), "00", "00", "14123456", null, "NCB")));

        assertEquals(PaymentTransactionStatus.PENDING, tx.getStatus());
        assertEquals(TutorState.APPROVED_UNPAID, tutor.getTutorState());
        verify(membershipRepository, never()).save(any());
    }

    @Test
    void req14_amountOffByOneDongIsRejected() {
        assertEquals(Outcome.AMOUNT_MISMATCH, processor.process(result("00", "00", 19_900_000L + 100L)));
        assertEquals(Outcome.AMOUNT_MISMATCH, processor.process(result("00", "00", 19_900_000L - 100L)));
        verify(membershipRepository, never()).save(any());
    }

    @Test
    void req14_mismatchedAmountDoesNotConsumeThePendingTransaction() {
        processor.process(result("00", "00", 100L));

        assertEquals(Outcome.ACTIVATED, processor.process(result("00", "00", 19_900_000L)),
                "số tiền sai không làm giao dịch rơi vào trạng thái cuối");
        assertEquals(PaymentTransactionStatus.SUCCESS, tx.getStatus());
    }

    @Test
    void req14_unknownTransactionTouchesNothing() {
        UUID unknown = UUID.randomUUID();
        when(transactionRepository.findLockedById(unknown)).thenReturn(Optional.empty());

        assertEquals(Outcome.NOT_FOUND, processor.process(
                new GatewayResult(unknown.toString().replace("-", ""), "00", "00", "1", 19_900_000L, "NCB")));

        verify(membershipRepository, never()).save(any());
        verify(tutorRepository, never()).findLockedByUserId(any());
    }

    @Test
    void req14_unapprovedTutorGetsMembershipButNotActivePaidState() {
        tutor.setVerificationStatus(TutorVerificationStatus.PENDING);
        tutor.setTutorState(TutorState.PENDING_VERIFICATION);

        assertEquals(Outcome.ACTIVATED, processor.process(result("00", "00", 19_900_000L)));

        assertEquals(TutorState.PENDING_VERIFICATION, tutor.getTutorState());
        assertEquals(TutorMembershipStatus.PREMIUM, tutor.getMembershipStatus());
        verify(membershipRepository, times(1)).save(any());
    }

    @Test
    void suspendedAccountAfterPaymentIsStillActivated() {
        tx.getUser().setAccountStatus(com.kizuna.core.entity.UserAccountStatus.SUSPENDED);
        assertEquals(Outcome.ACTIVATED, processor.process(result("00", "00", 19_900_000L)));
    }
}
