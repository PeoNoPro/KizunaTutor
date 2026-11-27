package com.kizuna.features.verification;

import com.kizuna.core.repository.UserRepository;
import com.kizuna.features.verification.dto.response.VerificationSummaryResponse;
import com.kizuna.features.verification.entity.TutorVerifAction;
import com.kizuna.features.verification.entity.TutorVerificationStatus;
import com.kizuna.features.verification.repository.TutorCredentialRepository;
import com.kizuna.features.verification.repository.TutorVerificationHistoryRepository;
import com.kizuna.features.verification.repository.VerificationTutorRepository;
import com.kizuna.features.verification.service.CurrentUserProvider;
import com.kizuna.features.verification.service.ProfileUpdateTimeline;
import com.kizuna.features.verification.service.VerificationDocumentStorage;
import com.kizuna.features.verification.service.VerificationNotificationService;
import com.kizuna.features.verification.service.VerificationStateMachine;
import com.kizuna.features.verification.service.VerificationViewAssembler;
import com.kizuna.features.verification.service.impl.VerificationReviewServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Duration;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * REQ-28 (UC-31): bảng tổng quan thẩm định của cán bộ học vụ (FR-010, API GET /academic/verifications/summary).
 */
class VerificationSummaryTest {

    private VerificationTutorRepository tutorRepository;
    private TutorVerificationHistoryRepository historyRepository;
    private VerificationReviewServiceImpl service;

    @BeforeEach
    void setUp() {
        tutorRepository = mock(VerificationTutorRepository.class);
        historyRepository = mock(TutorVerificationHistoryRepository.class);
        TutorCredentialRepository credentialRepository = mock(TutorCredentialRepository.class);
        VerificationDocumentStorage storage = mock(VerificationDocumentStorage.class);
        VerificationStateMachine stateMachine = new VerificationStateMachine(historyRepository, credentialRepository);
        VerificationViewAssembler assembler = new VerificationViewAssembler(credentialRepository, historyRepository, storage);
        service = new VerificationReviewServiceImpl(tutorRepository, credentialRepository, historyRepository,
                mock(UserRepository.class), storage, stateMachine, assembler, mock(VerificationNotificationService.class),
                mock(CurrentUserProvider.class));
    }

    @Test
    void summaryCountsTutorsByVerificationStatus() {
        when(tutorRepository.count()).thenReturn(60L);
        when(tutorRepository.countByVerificationStatus(TutorVerificationStatus.UNVERIFIED)).thenReturn(1L);
        when(tutorRepository.countByVerificationStatus(TutorVerificationStatus.PENDING)).thenReturn(12L);
        when(tutorRepository.countByVerificationStatus(TutorVerificationStatus.ADDITIONAL_INFO_REQUIRED)).thenReturn(4L);
        when(tutorRepository.countByVerificationStatus(TutorVerificationStatus.APPROVED)).thenReturn(40L);
        when(tutorRepository.countByVerificationStatus(TutorVerificationStatus.REJECTED)).thenReturn(3L);
        when(tutorRepository.countByVerificationStatusAndSubmittedAtGreaterThanEqual(eq(TutorVerificationStatus.PENDING), any()))
                .thenReturn(3L);
        when(tutorRepository.countByVerificationStatusAndSubmittedAtLessThan(eq(TutorVerificationStatus.PENDING), any()))
                .thenReturn(2L);

        VerificationSummaryResponse summary = service.getSummary();

        assertEquals(60, summary.getTotalTutors());
        assertEquals(1, summary.getNotSubmittedCount());
        assertEquals(12, summary.getPendingCount());
        assertEquals(3, summary.getPendingNewToday());
        assertEquals(2, summary.getPendingOver24h());
        assertEquals(4, summary.getNeedsInfoCount());
        assertEquals(40, summary.getApprovedCount());
        assertEquals(3, summary.getRejectedCount());
    }

    @Test
    void approvedAndRejectedThisMonthUseCurrentMonthRange() {
        when(historyRepository.countByActionAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(eq(TutorVerifAction.APPROVE),
                any(), any())).thenReturn(8L);
        when(historyRepository.countByActionAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(eq(TutorVerifAction.REJECT),
                any(), any())).thenReturn(2L);

        VerificationSummaryResponse summary = service.getSummary();

        ArgumentCaptor<OffsetDateTime> from = ArgumentCaptor.forClass(OffsetDateTime.class);
        ArgumentCaptor<OffsetDateTime> to = ArgumentCaptor.forClass(OffsetDateTime.class);
        verify(historyRepository).countByActionAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(
                eq(TutorVerifAction.APPROVE), from.capture(), to.capture());
        LocalDate firstDay = LocalDate.now(VerificationViewAssembler.ZONE).withDayOfMonth(1);
        assertEquals(firstDay.atStartOfDay(VerificationViewAssembler.ZONE).toOffsetDateTime(), from.getValue());
        assertEquals(from.getValue().plusMonths(1), to.getValue());
        assertEquals(8, summary.getApprovedThisMonth());
        assertEquals(2, summary.getRejectedThisMonth());
    }

    @Test
    void newTodayStartsAtMidnightAndOver24hUsesCutoff() {
        service.getSummary();

        ArgumentCaptor<OffsetDateTime> from = ArgumentCaptor.forClass(OffsetDateTime.class);
        verify(tutorRepository).countByVerificationStatusAndSubmittedAtGreaterThanEqual(
                eq(TutorVerificationStatus.PENDING), from.capture());
        ZonedDateTime now = ZonedDateTime.now(VerificationViewAssembler.ZONE);
        assertEquals(now.toLocalDate().atStartOfDay(VerificationViewAssembler.ZONE).toOffsetDateTime(), from.getValue());

        ArgumentCaptor<OffsetDateTime> before = ArgumentCaptor.forClass(OffsetDateTime.class);
        verify(tutorRepository).countByVerificationStatusAndSubmittedAtLessThan(
                eq(TutorVerificationStatus.PENDING), before.capture());
        Duration gap = Duration.between(before.getValue(), now.toOffsetDateTime());
        assertTrue(gap.compareTo(Duration.ofHours(24)) >= 0 && gap.compareTo(Duration.ofHours(24).plusMinutes(1)) < 0);
    }

    @Test
    void profileUpdateRequestsWaitingForReviewAreCounted() {
        when(historyRepository.countPendingProfileUpdates(TutorVerifAction.SUBMIT, TutorVerificationStatus.APPROVED,
                ProfileUpdateTimeline.MARKER + "%")).thenReturn(5L);

        assertEquals(5, service.getSummary().getProfileUpdatePendingCount());
    }

    @Test
    void emptySystemShowsAllCountsAsZero() {
        VerificationSummaryResponse summary = service.getSummary();

        assertEquals(0, summary.getTotalTutors());
        assertEquals(0, summary.getPendingCount());
        assertEquals(0, summary.getPendingNewToday());
        assertEquals(0, summary.getApprovedThisMonth());
        assertEquals(0, summary.getRejectedThisMonth());
        assertEquals(0, summary.getProfileUpdatePendingCount());
    }
}
