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

    /**
     * REQ-14: IPN từ VNPay. Kiểm tra chữ ký trước khi xử lý (97), rồi ánh xạ kết quả của
     * PaymentResultProcessor sang mã phản hồi chuẩn của cổng; chỉ ghi nhật ký cho giao dịch có thật.
     */
    @org.junit.jupiter.api.Nested
    class IpnHandling {

        private com.kizuna.features.payment.service.VnpaySigner signer;
        private PaymentResultProcessor ipnProcessor;
        private com.kizuna.features.payment.service.PaymentAudit audit;
        private com.kizuna.features.payment.service.VnpayIpnService ipn;

        @BeforeEach
        void setUpIpn() {
            signer = mock(com.kizuna.features.payment.service.VnpaySigner.class);
            ipnProcessor = mock(PaymentResultProcessor.class);
            audit = mock(com.kizuna.features.payment.service.PaymentAudit.class);
            ipn = new com.kizuna.features.payment.service.VnpayIpnService(signer, ipnProcessor, transactionRepository, audit);
            when(transactionRepository.existsById(tx.getTransactionId())).thenReturn(true);
        }

        private java.util.Map<String, String> params(String txnRef) {
            java.util.Map<String, String> p = new java.util.LinkedHashMap<>();
            p.put("vnp_TxnRef", txnRef);
            p.put("vnp_ResponseCode", "00");
            p.put("vnp_TransactionStatus", "00");
            p.put("vnp_Amount", "19900000");
            p.put("vnp_TransactionNo", "14123456");
            p.put("vnp_BankCode", "NCB");
            p.put("vnp_SecureHash", "abc");
            return p;
        }

        @SuppressWarnings("unchecked")
        private java.util.Map<String, String> loggedResponse() {
            ArgumentCaptor<java.util.Map<String, ?>> response = ArgumentCaptor.forClass(java.util.Map.class);
            verify(audit).log(eq(tx.getTransactionId()), eq(com.kizuna.features.payment.service.PaymentLogWriter.TYPE_IPN),
                    anyMap(), response.capture(), eq(200));
            return (java.util.Map<String, String>) response.getValue();
        }

        @Test
        void req14_invalidSignatureReturns97WithoutProcessing() {
            when(signer.verify(anyMap())).thenReturn(false);

            var reply = ipn.handle(params(tx.getTxnRef()));

            assertEquals("97", reply.RspCode());
            assertEquals("Invalid signature", reply.Message());
            verify(ipnProcessor, never()).process(any());
            assertEquals("INVALID_SIGNATURE", loggedResponse().get("outcome"));
        }

        @Test
        void req14_invalidSignatureForUnknownTransactionIsNotLogged() {
            when(signer.verify(anyMap())).thenReturn(false);

            assertEquals("97", ipn.handle(params(UUID.randomUUID().toString())).RspCode());
            assertEquals("97", ipn.handle(params("not-a-transaction-id")).RspCode());

            verify(ipnProcessor, never()).process(any());
            verify(audit, never()).log(any(), any(), any(), any(), any());
        }

        @org.junit.jupiter.params.ParameterizedTest
        @org.junit.jupiter.params.provider.CsvSource({
                "NOT_FOUND, 01, Order not found",
                "AMOUNT_MISMATCH, 04, Invalid amount",
                "ALREADY_PROCESSED, 02, Order already confirmed",
                "ACTIVATED, 00, Confirm Success",
                "FAILED, 00, Confirm Success",
                "STILL_PENDING, 00, Confirm Success"
        })
        void req14_processorOutcomeMapsToGatewayCode(Outcome outcome, String code, String message) {
            when(signer.verify(anyMap())).thenReturn(true);
            when(ipnProcessor.process(any())).thenReturn(outcome);

            var reply = ipn.handle(params(tx.getTxnRef()));

            assertEquals(code, reply.RspCode());
            assertEquals(message, reply.Message());
            java.util.Map<String, String> logged = loggedResponse();
            assertEquals(code, logged.get("RspCode"));
            assertEquals(outcome.name(), logged.get("outcome"));
        }

        @Test
        void req14_signatureIsCheckedBeforeProcessing() {
            when(signer.verify(anyMap())).thenReturn(true);
            when(ipnProcessor.process(any())).thenReturn(Outcome.ACTIVATED);

            ipn.handle(params(tx.getTxnRef()));

            org.mockito.InOrder order = inOrder(signer, ipnProcessor);
            order.verify(signer).verify(anyMap());
            order.verify(ipnProcessor).process(any());
        }

        @Test
        void req14_gatewayParamsArePassedToProcessor() {
            when(signer.verify(anyMap())).thenReturn(true);
            when(ipnProcessor.process(any())).thenReturn(Outcome.ACTIVATED);

            ipn.handle(params(tx.getTxnRef()));

            ArgumentCaptor<GatewayResult> passed = ArgumentCaptor.forClass(GatewayResult.class);
            verify(ipnProcessor).process(passed.capture());
            assertEquals(tx.getTxnRef(), passed.getValue().txnRef());
            assertEquals("00", passed.getValue().responseCode());
            assertEquals("00", passed.getValue().transactionStatus());
        }

        @Test
        void req14_unexpectedErrorReturns99AndIsLogged() {
            when(signer.verify(anyMap())).thenReturn(true);
            when(ipnProcessor.process(any())).thenThrow(new IllegalStateException("db down"));

            var reply = ipn.handle(params(tx.getTxnRef()));

            assertEquals("99", reply.RspCode());
            assertEquals("Unknown error", reply.Message());
            assertEquals("ERROR", loggedResponse().get("outcome"));
        }

        @Test
        void req14_validIpnForUnknownTransactionReturns01WithoutLog() {
            UUID unknown = UUID.randomUUID();
            when(signer.verify(anyMap())).thenReturn(true);
            when(ipnProcessor.process(any())).thenReturn(Outcome.NOT_FOUND);

            assertEquals("01", ipn.handle(params(unknown.toString())).RspCode());
            verify(audit, never()).log(any(), any(), any(), any(), any());
        }
    }
}
