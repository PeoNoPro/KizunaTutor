package com.kizuna.features.payment;

import com.kizuna.core.entity.Tutor;
import com.kizuna.core.entity.TutorState;
import com.kizuna.core.entity.User;
import com.kizuna.core.exception.BadRequestException;
import com.kizuna.core.exception.ConflictException;
import com.kizuna.core.exception.ResourceNotFoundException;
import com.kizuna.core.exception.ServiceUnavailableException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kizuna.features.payment.config.VnpayProperties;
import com.kizuna.features.payment.dto.request.MembershipRequests;
import com.kizuna.features.payment.entity.MembershipPlan;
import com.kizuna.features.payment.entity.PaymentTransaction;
import com.kizuna.features.payment.entity.PaymentTransactionStatus;
import com.kizuna.features.payment.entity.TutorMembership;
import com.kizuna.features.payment.entity.TutorMembershipState;
import com.kizuna.features.payment.entity.TutorMembershipStatus;
import com.kizuna.features.payment.repository.MembershipPlanRepository;
import com.kizuna.features.payment.repository.MembershipTutorRepository;
import com.kizuna.features.payment.repository.PaymentTransactionRepository;
import com.kizuna.features.payment.repository.TutorMembershipRepository;
import com.kizuna.features.payment.service.*;
import com.kizuna.features.payment.service.impl.MembershipPlanAdminServiceImpl;
import com.kizuna.features.payment.service.impl.TutorMembershipServiceImpl;
import com.kizuna.features.verification.entity.TutorVerificationStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Service FE-09: điều kiện mua gói (FR-002/003), trang kết quả (FR-005), hết hạn khi đọc (FR-013), cấu hình gói (FR-014–016).
 */
class MembershipServicesTest {

    static final LocalDate TODAY = LocalDate.of(2026, 11, 4);

    static User user() {
        return User.builder().userId(UUID.randomUUID()).email("gs@kizuna.test").fullName("Gia sư A").build();
    }

    static Tutor tutor(User user, TutorVerificationStatus verification, TutorState state) {
        return Tutor.builder().tutorId(UUID.randomUUID()).user(user).verificationStatus(verification)
                .tutorState(state).membershipStatus(TutorMembershipStatus.FREE).build();
    }

    static MembershipPlan plan(boolean active) {
        return MembershipPlan.builder().planId(UUID.randomUUID()).planName("Gói 1 tháng").durationMonths(1)
                .price(new BigDecimal("199000.00")).isActive(active).featuresJson("[\"Mở khóa phòng học\"]").build();
    }

    @Nested
    class Expiry {
        TutorMembershipRepository membershipRepository = mock(TutorMembershipRepository.class);
        MembershipTutorRepository tutorRepository = mock(MembershipTutorRepository.class);
        MembershipExpiryService service = new MembershipExpiryService(membershipRepository, tutorRepository);
        Tutor tutor = tutor(user(), TutorVerificationStatus.APPROVED, TutorState.ACTIVE_PAID);

        @BeforeEach
        void setUp() {
            when(membershipRepository.existsByTutorTutorId(tutor.getTutorId())).thenReturn(true);
            when(tutorRepository.findWithUserByTutorId(tutor.getTutorId())).thenReturn(Optional.of(tutor));
            when(tutorRepository.findLockedByTutorId(tutor.getTutorId())).thenReturn(Optional.of(tutor));
            tutor.setMembershipStatus(TutorMembershipStatus.PREMIUM);
        }

        @Test
        void expiresEndedMembershipAndResetsTutorState() {
            TutorMembership ended = TutorMembership.builder().status(TutorMembershipState.ACTIVE)
                    .startDate(TODAY.minusMonths(1)).endDate(TODAY.minusDays(1)).build();
            when(membershipRepository.countEndedBefore(any(), eq(TutorMembershipState.ACTIVE), eq(TODAY))).thenReturn(1L);
            when(membershipRepository.countValidOn(any(), eq(TutorMembershipState.ACTIVE), eq(TODAY))).thenReturn(0L);
            when(membershipRepository.findEndedBefore(any(), any(), any())).thenReturn(List.of(ended));

            service.refresh(tutor.getTutorId(), TODAY);

            assertEquals(TutorMembershipState.EXPIRED, ended.getStatus());
            assertEquals(TutorState.APPROVED_UNPAID, tutor.getTutorState());
            assertEquals(TutorMembershipStatus.EXPIRED, tutor.getMembershipStatus());
        }

        @Test
        void keepsActiveWhenStackedMembershipStillValid() {
            TutorMembership ended = TutorMembership.builder().status(TutorMembershipState.ACTIVE)
                    .startDate(TODAY.minusMonths(1)).endDate(TODAY.minusDays(1)).build();
            when(membershipRepository.countEndedBefore(any(), any(), any())).thenReturn(1L);
            when(membershipRepository.countValidOn(any(), any(), any())).thenReturn(1L);
            when(membershipRepository.findEndedBefore(any(), any(), any())).thenReturn(List.of(ended));

            service.refresh(tutor.getTutorId(), TODAY);

            assertEquals(TutorMembershipState.EXPIRED, ended.getStatus());
            assertEquals(TutorState.ACTIVE_PAID, tutor.getTutorState());
            assertEquals(TutorMembershipStatus.PREMIUM, tutor.getMembershipStatus());
        }

        @Test
        void tutorWithoutMembershipIsUntouched() {
            when(membershipRepository.existsByTutorTutorId(tutor.getTutorId())).thenReturn(false);
            service.refresh(tutor.getTutorId(), TODAY);
            verify(tutorRepository, never()).findLockedByTutorId(any());
            assertEquals(TutorState.ACTIVE_PAID, tutor.getTutorState());
        }
    }

    @Nested
    class TutorPurchase {
        MembershipPlanRepository planRepository = mock(MembershipPlanRepository.class);
        PaymentTransactionRepository transactionRepository = mock(PaymentTransactionRepository.class);
        TutorMembershipRepository membershipRepository = mock(TutorMembershipRepository.class);
        MembershipTutorRepository tutorRepository = mock(MembershipTutorRepository.class);
        PaymentResultProcessor processor = mock(PaymentResultProcessor.class);
        VnpayGatewayClient gatewayClient = mock(VnpayGatewayClient.class);
        VnpaySigner signer = mock(VnpaySigner.class);
        PaymentAudit audit = mock(PaymentAudit.class);
        VnpayProperties properties = VnpaySignerTest.properties(VnpaySignerTest.SECRET);
        User user = user();

        TutorMembershipServiceImpl service(VnpayProperties props) {
            return new TutorMembershipServiceImpl(planRepository, transactionRepository, membershipRepository, tutorRepository,
                    mock(MembershipExpiryService.class), processor, gatewayClient, signer, props,
                    new MembershipViewMapper(new ObjectMapper(), props), audit);
        }

        @Test
        void createsPendingTransactionWithPlanPriceAndReturnsGatewayUrl() {
            MembershipPlan plan = plan(true);
            when(tutorRepository.findWithUserByUserId(user.getUserId()))
                    .thenReturn(Optional.of(tutor(user, TutorVerificationStatus.APPROVED, TutorState.APPROVED_UNPAID)));
            when(planRepository.findById(plan.getPlanId())).thenReturn(Optional.of(plan));
            when(transactionRepository.save(any())).thenAnswer(inv -> {
                PaymentTransaction tx = inv.getArgument(0);
                tx.setTransactionId(UUID.randomUUID());
                return tx;
            });
            when(gatewayClient.paymentParams(any(), any())).thenReturn(Map.of("vnp_TxnRef", "x"));
            when(gatewayClient.buildPaymentUrl(any())).thenReturn("https://pay.test/vpcpay.html?x");

            var created = service(properties).createPayment(user, plan.getPlanId().toString(), "127.0.0.1");

            ArgumentCaptor<PaymentTransaction> saved = ArgumentCaptor.forClass(PaymentTransaction.class);
            verify(transactionRepository).save(saved.capture());
            assertEquals(PaymentTransactionStatus.PENDING, saved.getValue().getStatus());
            assertEquals(new BigDecimal("199000.00"), saved.getValue().getAmount());
            assertEquals(199_000L, created.amount());
            assertEquals("https://pay.test/vpcpay.html?x", created.paymentUrl());
        }

        @Test
        void rejectsUnapprovedTutor() {
            when(tutorRepository.findWithUserByUserId(user.getUserId()))
                    .thenReturn(Optional.of(tutor(user, TutorVerificationStatus.PENDING, TutorState.PENDING_VERIFICATION)));
            ConflictException e = assertThrows(ConflictException.class,
                    () -> service(properties).createPayment(user, UUID.randomUUID().toString(), null));
            assertEquals(PaymentMessages.MSG34_NOT_APPROVED, e.getMessage());
            verify(transactionRepository, never()).save(any());
        }

        @Test
        void rejectsInactivePlanAndUnknownPlan() {
            MembershipPlan plan = plan(false);
            when(tutorRepository.findWithUserByUserId(user.getUserId()))
                    .thenReturn(Optional.of(tutor(user, TutorVerificationStatus.APPROVED, TutorState.APPROVED_UNPAID)));
            when(planRepository.findById(plan.getPlanId())).thenReturn(Optional.of(plan));

            assertEquals(PaymentMessages.MSG35_PLAN_INACTIVE, assertThrows(ConflictException.class,
                    () -> service(properties).createPayment(user, plan.getPlanId().toString(), null)).getMessage());
            assertThrows(ResourceNotFoundException.class,
                    () -> service(properties).createPayment(user, "khong-phai-uuid", null));
        }

        @Test
        void missingGatewayConfigurationReportsUnavailable() {
            VnpayProperties empty = VnpaySignerTest.properties("");
            assertEquals(PaymentMessages.MSG37_GATEWAY_UNAVAILABLE, assertThrows(ServiceUnavailableException.class,
                    () -> service(empty).createPayment(user, UUID.randomUUID().toString(), null)).getMessage());
        }

        @Test
        void returnWithInvalidSignatureIsRejectedWithoutChanges() {
            when(signer.verify(any())).thenReturn(false);
            assertEquals(PaymentMessages.INVALID_RESULT, assertThrows(BadRequestException.class,
                    () -> service(properties).handleReturn(user, Map.of("vnp_TxnRef", "x"), null)).getMessage());
            verifyNoInteractions(processor);
        }

        @Test
        void returnOfAnotherTutorsTransactionIsNotFound() {
            PaymentTransaction other = PaymentTransaction.builder().transactionId(UUID.randomUUID()).user(user())
                    .plan(plan(true)).amount(BigDecimal.TEN).status(PaymentTransactionStatus.PENDING)
                    .transactionTime(OffsetDateTime.now()).build();
            when(signer.verify(any())).thenReturn(true);
            when(transactionRepository.findWithDetailsById(other.getTransactionId())).thenReturn(Optional.of(other));

            assertThrows(ResourceNotFoundException.class,
                    () -> service(properties).handleReturn(user, Map.of("vnp_TxnRef", other.getTxnRef()), null));
            verifyNoInteractions(processor);
        }

        @Test
        void pendingReturnUsesVerifiedGatewayQueryThenProcesses() {
            PaymentTransaction own = PaymentTransaction.builder().transactionId(UUID.randomUUID()).user(user)
                    .plan(plan(true)).amount(new BigDecimal("199000")).status(PaymentTransactionStatus.PENDING)
                    .transactionTime(OffsetDateTime.now()).build();
            GatewayResult queried = new GatewayResult(own.getTxnRef(), "00", "00", "1", 19_900_000L, "NCB");
            when(signer.verify(any())).thenReturn(true);
            when(transactionRepository.findWithDetailsById(own.getTransactionId())).thenReturn(Optional.of(own));
            when(gatewayClient.queryTransaction(any(), any())).thenReturn(
                    new VnpayGatewayClient.QueryOutcome(true, 200, "00", "OK", queried, Map.of(), "{}"));
            when(membershipRepository.findFirstByTransactionRef(any())).thenReturn(Optional.empty());

            var view = service(properties).handleReturn(user,
                    Map.of("vnp_TxnRef", own.getTxnRef(), "vnp_ResponseCode", "00", "vnp_TransactionStatus", "00"), null);

            verify(processor).process(queried);
            assertEquals(PaymentMessages.MSG33_PENDING, view.resultMessage());
        }

        private PaymentTransaction ownPending(OffsetDateTime createdAt) {
            PaymentTransaction own = PaymentTransaction.builder().transactionId(UUID.randomUUID()).user(user)
                    .plan(plan(true)).amount(new BigDecimal("199000")).status(PaymentTransactionStatus.PENDING)
                    .transactionTime(createdAt).build();
            when(transactionRepository.findWithDetailsById(own.getTransactionId())).thenReturn(Optional.of(own));
            when(membershipRepository.findFirstByTransactionRef(any())).thenReturn(Optional.empty());
            return own;
        }

        @Test
        void tutorCheckProcessesVerifiedGatewayResult() {
            PaymentTransaction own = ownPending(OffsetDateTime.now().minusHours(1));
            GatewayResult queried = new GatewayResult(own.getTxnRef(), "00", "00", "9", 19_900_000L, "NCB");
            when(gatewayClient.queryTransaction(any(), any())).thenReturn(
                    new VnpayGatewayClient.QueryOutcome(true, 200, "00", "OK", queried, Map.of(), "{}"));

            service(properties).checkMyPayment(user, own.getTransactionId().toString(), null);

            verify(processor).process(queried);
        }

        @Test
        void tutorCheckOfFinishedTransactionDoesNotCallGateway() {
            PaymentTransaction own = ownPending(OffsetDateTime.now());
            own.setStatus(PaymentTransactionStatus.FAILED);

            var view = service(properties).checkMyPayment(user, own.getTransactionId().toString(), null);

            verifyNoInteractions(gatewayClient, processor);
            assertEquals(PaymentMessages.MSG32_FAILED, view.resultMessage());
        }

        @Test
        void resumeRebuildsUrlForFreshPendingOnly() {
            PaymentTransaction fresh = ownPending(OffsetDateTime.now().minusMinutes(3));
            when(gatewayClient.paymentParams(any(), any())).thenReturn(Map.of("vnp_TxnRef", fresh.getTxnRef()));
            when(gatewayClient.buildPaymentUrl(any())).thenReturn("https://pay.test?again");

            assertEquals("https://pay.test?again",
                    service(properties).resumePayment(user, fresh.getTransactionId().toString(), null).paymentUrl());
            verify(transactionRepository, never()).save(any());

            PaymentTransaction stale = ownPending(OffsetDateTime.now().minusMinutes(30));
            assertEquals(PaymentMessages.PAYMENT_EXPIRED, assertThrows(ConflictException.class,
                    () -> service(properties).resumePayment(user, stale.getTransactionId().toString(), null)).getMessage());
        }

        @Test
        void pendingReturnFallsBackToSignedReturnWhenQueryUnavailable() {
            PaymentTransaction own = PaymentTransaction.builder().transactionId(UUID.randomUUID()).user(user)
                    .plan(plan(true)).amount(new BigDecimal("199000")).status(PaymentTransactionStatus.PENDING)
                    .transactionTime(OffsetDateTime.now()).build();
            when(signer.verify(any())).thenReturn(true);
            when(transactionRepository.findWithDetailsById(own.getTransactionId())).thenReturn(Optional.of(own));
            when(gatewayClient.queryTransaction(any(), any()))
                    .thenThrow(new ServiceUnavailableException(PaymentMessages.MSG37_GATEWAY_UNAVAILABLE));
            when(membershipRepository.findFirstByTransactionRef(any())).thenReturn(Optional.empty());

            service(properties).handleReturn(user,
                    Map.of("vnp_TxnRef", own.getTxnRef(), "vnp_ResponseCode", "24", "vnp_TransactionStatus", "02",
                            "vnp_Amount", "19900000"), null);

            ArgumentCaptor<GatewayResult> used = ArgumentCaptor.forClass(GatewayResult.class);
            verify(processor).process(used.capture());
            assertEquals("24", used.getValue().responseCode());
        }
    }

    @Nested
    class PlanAdmin {
        MembershipPlanRepository planRepository = mock(MembershipPlanRepository.class);
        MembershipPlanAdminServiceImpl service = new MembershipPlanAdminServiceImpl(planRepository,
                new MembershipViewMapper(new ObjectMapper(), VnpaySignerTest.properties("")));

        @Test
        void createsPlanWithTrimmedBenefits() {
            when(planRepository.existsByPlanNameIgnoreCase("Gói 12 tháng")).thenReturn(false);
            when(planRepository.findById(any())).thenAnswer(inv -> Optional.of(MembershipPlan.builder()
                    .planId(inv.getArgument(0)).planName("Gói 12 tháng").durationMonths(12)
                    .price(new BigDecimal("1500000")).isActive(true).featuresJson("[\"A\",\"B\"]").build()));

            var view = service.createPlan(new MembershipRequests.CreatePlanRequest("  Gói 12 tháng ", 12, 1_500_000L,
                    List.of(" A ", "", "B")));

            verify(planRepository).insertPlan(any(), eq("Gói 12 tháng"), eq(12), eq(BigDecimal.valueOf(1_500_000L)),
                    eq("[\"A\",\"B\"]"));
            assertEquals(List.of("A", "B"), view.benefits());
            assertTrue(view.active());
        }

        @Test
        void rejectsDuplicateNamePriceOutOfRangeAndTooManyBenefits() {
            when(planRepository.existsByPlanNameIgnoreCase("Trùng")).thenReturn(true);
            assertThrows(ConflictException.class,
                    () -> service.createPlan(new MembershipRequests.CreatePlanRequest("Trùng", 1, 100_000L, List.of())));
            assertEquals(PaymentMessages.MSG36_PRICE_RANGE, assertThrows(BadRequestException.class,
                    () -> service.createPlan(new MembershipRequests.CreatePlanRequest("Mới", 1, 0L, List.of()))).getMessage());
            assertThrows(BadRequestException.class,
                    () -> service.createPlan(new MembershipRequests.CreatePlanRequest("Mới", 1, 100_000_001L, List.of())));
            List<String> tooMany = java.util.stream.IntStream.range(0, 21).mapToObj(i -> "Q" + i).toList();
            assertThrows(BadRequestException.class,
                    () -> service.createPlan(new MembershipRequests.CreatePlanRequest("Mới", 1, 100_000L, tooMany)));
            verify(planRepository, never()).insertPlan(any(), any(), anyInt(), any(), any());
        }

        @Test
        void updateKeepsDurationAndUsesNewPrice() {
            MembershipPlan existing = plan(true);
            when(planRepository.findById(existing.getPlanId())).thenReturn(Optional.of(existing));

            service.updatePlan(existing.getPlanId().toString(),
                    new MembershipRequests.UpdatePlanRequest("Gói 1 tháng", 249_000L, List.of("X")));

            verify(planRepository).updatePlan(existing.getPlanId(), "Gói 1 tháng", BigDecimal.valueOf(249_000L), "[\"X\"]");
        }

        @Test
        void deactivateOnlyWritesWhenStateChanges() {
            MembershipPlan existing = plan(false);
            when(planRepository.findById(existing.getPlanId())).thenReturn(Optional.of(existing));
            service.setActive(existing.getPlanId().toString(), false);
            verify(planRepository, never()).updateActive(any(), anyBoolean());
            service.setActive(existing.getPlanId().toString(), true);
            verify(planRepository).updateActive(existing.getPlanId(), true);
        }
    }
}
