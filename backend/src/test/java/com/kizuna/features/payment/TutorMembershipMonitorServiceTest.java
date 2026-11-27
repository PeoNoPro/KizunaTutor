package com.kizuna.features.payment;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kizuna.core.entity.Tutor;
import com.kizuna.core.entity.TutorState;
import com.kizuna.core.entity.User;
import com.kizuna.core.exception.BadRequestException;
import com.kizuna.core.exception.ResourceNotFoundException;
import com.kizuna.features.payment.dto.response.MembershipViews;
import com.kizuna.features.payment.entity.MembershipPlan;
import com.kizuna.features.payment.entity.PaymentTransaction;
import com.kizuna.features.payment.entity.PaymentTransactionStatus;
import com.kizuna.features.payment.entity.TutorMembership;
import com.kizuna.features.payment.entity.TutorMembershipState;
import com.kizuna.features.payment.repository.MembershipTutorRepository;
import com.kizuna.features.payment.repository.PaymentTransactionRepository;
import com.kizuna.features.payment.repository.TutorMembershipRepository;
import com.kizuna.features.payment.service.MembershipExpiryService;
import com.kizuna.features.payment.service.MembershipViewMapper;
import com.kizuna.features.payment.service.PaymentMessages;
import com.kizuna.features.payment.service.VnpayGatewayClient;
import com.kizuna.features.payment.service.impl.TutorMembershipMonitorServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * REQ-27 (UC-101, UC-102): quản lý xem danh sách và chi tiết hội viên của gia sư.
 */
class TutorMembershipMonitorServiceTest {

    TutorMembershipRepository membershipRepository = mock(TutorMembershipRepository.class);
    MembershipTutorRepository tutorRepository = mock(MembershipTutorRepository.class);
    PaymentTransactionRepository transactionRepository = mock(PaymentTransactionRepository.class);
    MembershipExpiryService expiryService = mock(MembershipExpiryService.class);
    TutorMembershipMonitorServiceImpl service = new TutorMembershipMonitorServiceImpl(membershipRepository, tutorRepository,
            transactionRepository, expiryService, new MembershipViewMapper(new ObjectMapper(), VnpaySignerTest.properties("")));

    final LocalDate today = LocalDate.now(VnpayGatewayClient.VIETNAM);
    final MembershipPlan plan = MembershipPlan.builder().planId(UUID.randomUUID()).planName("Gói 1 tháng")
            .durationMonths(1).price(new BigDecimal("199000.00")).isActive(true).build();
    final User user = User.builder().userId(UUID.randomUUID()).email("gs@kizuna.test").fullName("Gia sư A")
            .phone("0905000111").build();
    final Tutor tutor = Tutor.builder().tutorId(UUID.randomUUID()).user(user).tutorState(TutorState.ACTIVE_PAID).build();

    TutorMembership membership(TutorMembershipState status, LocalDate start, LocalDate end) {
        return TutorMembership.builder().membershipId(UUID.randomUUID()).tutor(tutor).plan(plan).status(status)
                .startDate(start).endDate(end).amount(new BigDecimal("199000.00"))
                .createdAt(OffsetDateTime.now(VnpayGatewayClient.VIETNAM)).build();
    }

    @SuppressWarnings("unchecked")
    void pageReturns(List<TutorMembership> rows) {
        when(membershipRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenAnswer(inv -> new PageImpl<>(rows, inv.getArgument(1), rows.size()));
    }

    @SuppressWarnings("unchecked")
    Pageable capturedPageable() {
        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        verify(membershipRepository).findAll(any(Specification.class), pageable.capture());
        return pageable.getValue();
    }

    @Nested
    class ListMemberships {

        @Test
        void listShowsTutorPlanAndValidMembership() {
            pageReturns(List.of(membership(TutorMembershipState.ACTIVE, today.minusDays(5), today.plusDays(20))));

            MembershipViews.PageView<MembershipViews.TutorMembershipRowView> page =
                    service.listMemberships("", null, null, 0, 10);

            assertEquals(1, page.totalItems());
            MembershipViews.TutorMembershipRowView row = page.items().get(0);
            assertEquals(tutor.getTutorId(), row.tutorId());
            assertEquals("Gia sư A", row.tutorName());
            assertEquals("gs@kizuna.test", row.email());
            assertEquals("Gói 1 tháng", row.planName());
            assertEquals("ACTIVE", row.status());
            assertEquals("Đang hiệu lực", row.statusLabel());
            assertEquals(today.plusDays(20), row.expiresOn());
        }

        @Test
        void activeMembershipEndedYesterdayIsShownAsExpired() {
            pageReturns(List.of(membership(TutorMembershipState.ACTIVE, today.minusMonths(1), today.minusDays(1))));

            MembershipViews.TutorMembershipRowView row = service.listMemberships(null, null, null, 0, 10).items().get(0);

            assertEquals("EXPIRED", row.status());
            assertEquals("Đã hết hạn", row.statusLabel());
        }

        @Test
        void membershipEndingTodayIsStillActive() {
            pageReturns(List.of(membership(TutorMembershipState.ACTIVE, today.minusMonths(1), today)));

            MembershipViews.TutorMembershipRowView row = service.listMemberships(null, null, null, 0, 10).items().get(0);

            assertEquals("ACTIVE", row.status());
            assertEquals("Đang hiệu lực", row.statusLabel());
        }

        @Test
        void keywordOf100CharactersIsAccepted() {
            pageReturns(List.of());

            assertDoesNotThrow(() -> service.listMemberships("a".repeat(100), null, null, 0, 10));
        }

        @Test
        void keywordOf101CharactersIsRejected() {
            BadRequestException ex = assertThrows(BadRequestException.class,
                    () -> service.listMemberships("a".repeat(101), null, null, 0, 10));

            assertEquals("Từ khóa tìm kiếm tối đa 100 ký tự.", ex.getMessage());
            verifyNoInteractions(membershipRepository);
        }

        @Test
        void invalidPlanFilterIsRejected() {
            BadRequestException ex = assertThrows(BadRequestException.class,
                    () -> service.listMemberships("", "not-a-uuid", null, 0, 10));

            assertEquals("Bộ lọc gói không hợp lệ.", ex.getMessage());
        }

        @Test
        void planAndLowercaseStatusFiltersAreAccepted() {
            pageReturns(List.of());

            assertDoesNotThrow(() -> service.listMemberships(" gia su ", plan.getPlanId().toString(), "expired", 0, 10));
        }

        @Test
        void pageSizeAbove50IsLimitedTo50() {
            pageReturns(List.of());

            service.listMemberships("", null, null, 2, 100);

            Pageable pageable = capturedPageable();
            assertEquals(50, pageable.getPageSize());
            assertEquals(2, pageable.getPageNumber());
        }
    }

    @Nested
    class Detail {

        @BeforeEach
        void setUp() {
            when(tutorRepository.findWithUserByTutorId(tutor.getTutorId())).thenReturn(Optional.of(tutor));
        }

        @Test
        void invalidTutorIdIsNotFound() {
            ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class, () -> service.getDetail("abc"));

            assertEquals(PaymentMessages.TUTOR_NOT_FOUND, ex.getMessage());
            verifyNoInteractions(expiryService);
        }

        @Test
        void unknownTutorIsNotFound() {
            UUID unknown = UUID.randomUUID();
            when(tutorRepository.findWithUserByTutorId(unknown)).thenReturn(Optional.empty());

            ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class,
                    () -> service.getDetail(unknown.toString()));

            assertEquals(PaymentMessages.TUTOR_NOT_FOUND, ex.getMessage());
            verifyNoInteractions(expiryService);
        }

        @Test
        void detailRefreshesExpiryAndShowsLatestEndDate() {
            when(membershipRepository.findByTutorTutorIdOrderByStartDateDesc(tutor.getTutorId())).thenReturn(List.of(
                    membership(TutorMembershipState.ACTIVE, today.plusDays(10), today.plusDays(40)),
                    membership(TutorMembershipState.ACTIVE, today.minusDays(20), today.plusDays(9)),
                    membership(TutorMembershipState.EXPIRED, today.minusMonths(3), today.minusMonths(2))));
            when(transactionRepository.findByUserUserIdAndPlanIsNotNullOrderByTransactionTimeDesc(user.getUserId()))
                    .thenReturn(List.of());

            MembershipViews.TutorMembershipDetailView view = service.getDetail(" " + tutor.getTutorId() + " ");

            verify(expiryService).refresh(tutor.getTutorId(), today);
            assertEquals(today.plusDays(40), view.expiresOn());
            assertEquals(3, view.memberships().size());
            assertEquals("Đã hết hạn", view.memberships().get(2).statusLabel());
        }

        @Test
        void tutorWithoutValidMembershipHasNoExpiryDate() {
            when(membershipRepository.findByTutorTutorIdOrderByStartDateDesc(tutor.getTutorId())).thenReturn(List.of(
                    membership(TutorMembershipState.ACTIVE, today.minusMonths(1), today.minusDays(1))));
            when(transactionRepository.findByUserUserIdAndPlanIsNotNullOrderByTransactionTimeDesc(user.getUserId()))
                    .thenReturn(List.of());

            MembershipViews.TutorMembershipDetailView view = service.getDetail(tutor.getTutorId().toString());

            assertNull(view.expiresOn());
            assertEquals("EXPIRED", view.memberships().get(0).status());
        }

        @Test
        void tutorInfoUsesUserPhoneWhenTutorPhoneIsEmpty() {
            when(membershipRepository.findByTutorTutorIdOrderByStartDateDesc(tutor.getTutorId())).thenReturn(List.of());
            when(transactionRepository.findByUserUserIdAndPlanIsNotNullOrderByTransactionTimeDesc(user.getUserId()))
                    .thenReturn(List.of());

            MembershipViews.TutorInfoView info = service.getDetail(tutor.getTutorId().toString()).tutor();

            assertEquals("Gia sư A", info.fullName());
            assertEquals("0905000111", info.phone());
            assertEquals("Đang hoạt động", info.tutorStateLabel());
        }

        @Test
        void paymentHistoryIsShownWithStatusLabel() {
            PaymentTransaction tx = PaymentTransaction.builder().transactionId(UUID.randomUUID()).user(user).plan(plan)
                    .amount(new BigDecimal("199000.00")).status(PaymentTransactionStatus.SUCCESS)
                    .gatewayTransactionId("14567890").transactionTime(OffsetDateTime.now(VnpayGatewayClient.VIETNAM)).build();
            when(membershipRepository.findByTutorTutorIdOrderByStartDateDesc(tutor.getTutorId())).thenReturn(List.of());
            when(transactionRepository.findByUserUserIdAndPlanIsNotNullOrderByTransactionTimeDesc(user.getUserId()))
                    .thenReturn(List.of(tx));

            MembershipViews.TutorMembershipDetailView view = service.getDetail(tutor.getTutorId().toString());

            assertFalse(view.paymentsUnavailable());
            MembershipViews.TutorPaymentView payment = view.payments().get(0);
            assertEquals("Gói 1 tháng", payment.planName());
            assertEquals(199000L, payment.amount());
            assertEquals("Thành công", payment.statusLabel());
            assertEquals("14567890", payment.gatewayTransactionId());
        }

        @Test
        void paymentHistoryErrorStillShowsMembershipDetail() {
            when(membershipRepository.findByTutorTutorIdOrderByStartDateDesc(tutor.getTutorId())).thenReturn(List.of(
                    membership(TutorMembershipState.ACTIVE, today.minusDays(1), today.plusDays(29))));
            when(transactionRepository.findByUserUserIdAndPlanIsNotNullOrderByTransactionTimeDesc(user.getUserId()))
                    .thenThrow(new RuntimeException("database timeout"));

            MembershipViews.TutorMembershipDetailView view = service.getDetail(tutor.getTutorId().toString());

            assertTrue(view.paymentsUnavailable());
            assertNull(view.payments());
            assertEquals(today.plusDays(29), view.expiresOn());
        }
    }
}
