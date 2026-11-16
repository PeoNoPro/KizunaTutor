package com.kizuna.features.verification;

import com.kizuna.core.entity.Tutor;
import com.kizuna.core.entity.TutorState;
import com.kizuna.core.entity.User;
import com.kizuna.core.repository.UserRepository;
import com.kizuna.features.verification.dto.request.RejectVerificationRequest;
import com.kizuna.features.verification.dto.request.RequestInfoRequest;
import com.kizuna.features.verification.dto.request.VerificationQueueQuery;
import com.kizuna.features.verification.dto.response.DecisionResultResponse;
import com.kizuna.features.verification.entity.RejectionReason;
import com.kizuna.features.verification.entity.TutorVerifAction;
import com.kizuna.features.verification.entity.TutorVerificationHistory;
import com.kizuna.features.verification.entity.TutorVerificationStatus;
import com.kizuna.features.verification.repository.TutorCredentialRepository;
import com.kizuna.features.verification.service.ProfileUpdateTimeline;
import com.kizuna.features.verification.dto.request.RejectProfileUpdateRequest;
import com.kizuna.features.verification.entity.TutorCredentialStatus;
import com.kizuna.features.verification.repository.TutorVerificationHistoryRepository;
import com.kizuna.features.verification.repository.VerificationTutorRepository;
import com.kizuna.features.verification.service.CurrentUserProvider;
import com.kizuna.features.verification.service.VerificationDocumentStorage;
import com.kizuna.features.verification.service.VerificationException;
import com.kizuna.features.verification.service.VerificationNotificationService;
import com.kizuna.features.verification.service.VerificationStateMachine;
import com.kizuna.features.verification.service.VerificationViewAssembler;
import com.kizuna.features.verification.service.impl.VerificationReviewServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class VerificationReviewServiceTest {

    private VerificationTutorRepository tutorRepository;
    private TutorVerificationHistoryRepository historyRepository;
    private TutorCredentialRepository credentialRepository;
    private VerificationNotificationService notificationService;
    private VerificationReviewServiceImpl service;

    private final UUID officerId = UUID.randomUUID();
    private final User officer = User.builder().userId(officerId).fullName("Cán bộ A").build();
    private Tutor tutor;

    @BeforeEach
    void setUp() {
        tutorRepository = mock(VerificationTutorRepository.class);
        historyRepository = mock(TutorVerificationHistoryRepository.class);
        credentialRepository = mock(TutorCredentialRepository.class);
        notificationService = mock(VerificationNotificationService.class);
        UserRepository userRepository = mock(UserRepository.class);
        CurrentUserProvider currentUserProvider = mock(CurrentUserProvider.class);
        VerificationDocumentStorage storage = mock(VerificationDocumentStorage.class);

        tutor = Tutor.builder()
                .tutorId(UUID.randomUUID())
                .user(User.builder().userId(UUID.randomUUID()).email("gs@kizuna.test").fullName("Gia sư B").build())
                .verificationStatus(TutorVerificationStatus.PENDING)
                .tutorState(TutorState.PENDING_VERIFICATION)
                .build();

        when(currentUserProvider.requireCurrentUser()).thenReturn(officer);
        when(userRepository.getReferenceById(officerId)).thenReturn(officer);
        when(tutorRepository.findLockedByTutorId(tutor.getTutorId())).thenReturn(Optional.of(tutor));
        when(historyRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        VerificationStateMachine stateMachine = new VerificationStateMachine(historyRepository, credentialRepository);
        VerificationViewAssembler assembler = new VerificationViewAssembler(credentialRepository, historyRepository, storage);
        service = new VerificationReviewServiceImpl(tutorRepository, credentialRepository, historyRepository, userRepository,
                storage, stateMachine, assembler, notificationService, currentUserProvider);
    }

    @Test
    void approveMovesToApprovedRecordsOfficerAndSendsEmail() {
        DecisionResultResponse result = service.approve(tutor.getTutorId());

        assertEquals("Đã phê duyệt", result.getStatusLabel());
        assertEquals(VerificationReviewServiceImpl.MSG_APPROVED, result.getMessage());
        ArgumentCaptor<TutorVerificationHistory> captor = ArgumentCaptor.forClass(TutorVerificationHistory.class);
        verify(historyRepository).save(captor.capture());
        assertEquals(TutorVerifAction.APPROVE, captor.getValue().getAction());
        assertSame(officer, captor.getValue().getOfficer());
        verify(notificationService).approvedAfterCommit("gs@kizuna.test", "Gia sư B");
    }

    @Test
    void rejectWithoutReasonOrNoteIsRejectedWithSpecMessageBeforeLocking() {
        VerificationException ex = assertThrows(VerificationException.class,
                () -> service.reject(tutor.getTutorId(), RejectVerificationRequest.builder().feedback("x").build()));

        assertEquals(VerificationReviewServiceImpl.MSG_REJECT_REQUIRED, ex.getMessage());
        assertTrue(ex.getFields().containsKey("reasonCode"));
        assertTrue(ex.getFields().containsKey("internalNote"));
        verify(tutorRepository, never()).findLockedByTutorId(any());
        assertEquals(TutorVerificationStatus.PENDING, tutor.getVerificationStatus());
    }

    @Test
    void rejectStoresInternalNoteOnlyInHistoryAndEmailsReason() {
        service.reject(tutor.getTutorId(), RejectVerificationRequest.builder()
                .reasonCode("INFORMATION_MISMATCH").feedback("Họ tên không khớp").internalNote("Đã đối chiếu CCCD").build());

        assertEquals(TutorVerificationStatus.REJECTED, tutor.getVerificationStatus());
        assertFalse(tutor.getVerificationFeedback().contains("Đã đối chiếu"));
        ArgumentCaptor<TutorVerificationHistory> captor = ArgumentCaptor.forClass(TutorVerificationHistory.class);
        verify(historyRepository).save(captor.capture());
        assertEquals(RejectionReason.INFORMATION_MISMATCH, captor.getValue().getReasonCode());
        assertEquals("Đã đối chiếu CCCD", captor.getValue().getInternalNote());
        verify(notificationService).rejectedAfterCommit("gs@kizuna.test", "Gia sư B",
                "Thông tin không trùng khớp với giấy tờ", "Họ tên không khớp");
    }

    @Test
    void requestInfoRequiresMessage() {
        VerificationException ex = assertThrows(VerificationException.class,
                () -> service.requestInfo(tutor.getTutorId(), new RequestInfoRequest("   ")));
        assertEquals(VerificationReviewServiceImpl.MSG_INFO_REQUIRED, ex.getMessage());
        verifyNoInteractions(notificationService);
    }

    @Test
    void requestInfoMovesToNeedsInfoAndEmailsMessage() {
        DecisionResultResponse result = service.requestInfo(tutor.getTutorId(), new RequestInfoRequest("Ảnh CCCD bị mờ"));

        assertEquals("Cần bổ sung thông tin", result.getStatusLabel());
        assertEquals(VerificationReviewServiceImpl.MSG_INFO_REQUESTED, result.getMessage());
        verify(notificationService).infoRequestedAfterCommit("gs@kizuna.test", "Gia sư B", "Ảnh CCCD bị mờ");
    }

    @ParameterizedTest
    @EnumSource(value = TutorVerificationStatus.class, names = {"APPROVED", "REJECTED", "ADDITIONAL_INFO_REQUIRED"})
    void decisionOnAlreadyProcessedProfileIsConflictWithoutSideEffects(TutorVerificationStatus status) {
        tutor.setVerificationStatus(status);

        assertEquals("ALREADY_PROCESSED", assertThrows(VerificationException.class,
                () -> service.approve(tutor.getTutorId())).getCode());
        assertEquals("ALREADY_PROCESSED", assertThrows(VerificationException.class,
                () -> service.reject(tutor.getTutorId(), RejectVerificationRequest.builder()
                        .reasonCode("OTHER").internalNote("x").build())).getCode());
        assertEquals("ALREADY_PROCESSED", assertThrows(VerificationException.class,
                () -> service.requestInfo(tutor.getTutorId(), new RequestInfoRequest("x"))).getCode());
        verify(historyRepository, never()).save(any());
        verifyNoInteractions(notificationService);
    }

    @Test
    void decisionsAlwaysUseRowLock() {
        service.approve(tutor.getTutorId());
        verify(tutorRepository).findLockedByTutorId(tutor.getTutorId());
        verify(tutorRepository, never()).findWithUserByTutorId(any());
    }

    @Test
    void queueRejectsInvalidFilters() {
        assertThrows(VerificationException.class,
                () -> service.getQueue(VerificationQueueQuery.builder().keyword("a".repeat(101)).build()));
        assertThrows(VerificationException.class,
                () -> service.getQueue(VerificationQueueQuery.builder().jlptLevel("N9").build()));
        assertThrows(VerificationException.class,
                () -> service.getQueue(VerificationQueueQuery.builder().status("UNVERIFIED").build()));
        assertThrows(VerificationException.class, () -> service.getQueue(VerificationQueueQuery.builder()
                .submittedFrom(LocalDate.of(2026, 11, 3)).submittedTo(LocalDate.of(2026, 11, 1)).build()));
    }

    @Test
    void unsubmittedProfileIsNotVisibleToOfficer() {
        tutor.setVerificationStatus(TutorVerificationStatus.UNVERIFIED);
        when(tutorRepository.findWithUserByTutorId(tutor.getTutorId())).thenReturn(Optional.of(tutor));
        assertEquals("NOT_FOUND", assertThrows(VerificationException.class,
                () -> service.getDetail(tutor.getTutorId())).getCode());
    }

    @Test
    void historyIncludesInternalNoteAndOfficerName() {
        when(tutorRepository.findById(tutor.getTutorId())).thenReturn(Optional.of(tutor));
        when(historyRepository.findTimeline(tutor.getTutorId())).thenReturn(List.of(
                TutorVerificationHistory.builder().action(TutorVerifAction.SUBMIT).build(),
                TutorVerificationHistory.builder().action(TutorVerifAction.REJECT).officer(officer)
                        .reasonCode(RejectionReason.OTHER).internalNote("nội bộ").feedback("gửi gia sư").build()));

        var history = service.getHistory(tutor.getTutorId());

        assertEquals("Nộp hồ sơ", history.get(0).getActionLabel());
        assertNull(history.get(0).getOfficerName());
        assertEquals("Cán bộ A", history.get(1).getOfficerName());
        assertEquals("nội bộ", history.get(1).getInternalNote());
        assertEquals("Lý do khác", history.get(1).getReasonLabel());
        verify(notificationService, never()).approvedAfterCommit(anyString(), anyString());
    }

    // ===== Yêu cầu cập nhật hồ sơ của gia sư đã phê duyệt (lưu trong lịch sử thẩm định) =====

    private void pendingUpdate(String jlpt) {
        tutor.setVerificationStatus(TutorVerificationStatus.APPROVED);
        tutor.setHighestJlptLevel("N2");
        when(historyRepository.findTimeline(tutor.getTutorId())).thenReturn(List.of(
                TutorVerificationHistory.builder().action(TutorVerifAction.SUBMIT).build(),
                TutorVerificationHistory.builder().action(TutorVerifAction.APPROVE).build(),
                TutorVerificationHistory.builder().action(TutorVerifAction.SUBMIT)
                        .internalNote(ProfileUpdateTimeline.submitNotes("N2", jlpt)).build()));
    }

    private TutorVerificationHistory savedHistory() {
        ArgumentCaptor<TutorVerificationHistory> saved = ArgumentCaptor.forClass(TutorVerificationHistory.class);
        verify(historyRepository).save(saved.capture());
        return saved.getValue();
    }

    @Test
    void approveProfileUpdateAppliesJlptAndVerifiesNewDocuments() {
        pendingUpdate("N1");

        service.approveProfileUpdate(tutor.getTutorId());

        assertEquals("N1", tutor.getHighestJlptLevel());
        assertEquals(TutorVerificationStatus.APPROVED, tutor.getVerificationStatus());
        verify(credentialRepository).updateStatusForTutor(tutor.getTutorId(),
                TutorCredentialStatus.PENDING, TutorCredentialStatus.VERIFIED);
        TutorVerificationHistory decision = savedHistory();
        assertEquals(TutorVerifAction.APPROVE, decision.getAction());
        assertTrue(ProfileUpdateTimeline.isUpdateRow(decision));
        assertSame(officer, decision.getOfficer());
    }

    @Test
    void rejectProfileUpdateNeedsReasonAndKeepsProfileApproved() {
        pendingUpdate("N1");
        assertEquals("VALIDATION_FAILED", assertThrows(VerificationException.class,
                () -> service.rejectProfileUpdate(tutor.getTutorId(), new RejectProfileUpdateRequest(" "))).getCode());

        service.rejectProfileUpdate(tutor.getTutorId(), new RejectProfileUpdateRequest("Chứng chỉ bị mờ"));

        assertEquals("N2", tutor.getHighestJlptLevel());
        assertEquals(TutorVerificationStatus.APPROVED, tutor.getVerificationStatus());
        verify(credentialRepository).updateStatusForTutor(tutor.getTutorId(),
                TutorCredentialStatus.PENDING, TutorCredentialStatus.REJECTED);
        TutorVerificationHistory decision = savedHistory();
        assertEquals(TutorVerifAction.REJECT, decision.getAction());
        assertEquals("Chứng chỉ bị mờ", decision.getFeedback());
        assertNull(decision.getReasonCode());
    }

    @Test
    void profileUpdateAlreadyHandledReturnsConflict() {
        tutor.setVerificationStatus(TutorVerificationStatus.APPROVED);
        when(historyRepository.findTimeline(tutor.getTutorId())).thenReturn(List.of(
                TutorVerificationHistory.builder().action(TutorVerifAction.SUBMIT)
                        .internalNote(ProfileUpdateTimeline.MARKER).build(),
                TutorVerificationHistory.builder().action(TutorVerifAction.APPROVE)
                        .internalNote(ProfileUpdateTimeline.MARKER).build()));
        assertEquals("ALREADY_PROCESSED", assertThrows(VerificationException.class,
                () -> service.approveProfileUpdate(tutor.getTutorId())).getCode());
    }

    // ===== REQ-04: duyệt hồ sơ =====

    @Test
    void req04_approveTutorWithoutMembershipBecomesApprovedUnpaidAndDocumentsVerified() {
        service.approve(tutor.getTutorId());

        assertEquals(TutorVerificationStatus.APPROVED, tutor.getVerificationStatus());
        assertEquals(TutorState.APPROVED_UNPAID, tutor.getTutorState());
        assertNotNull(tutor.getVerifiedAt());
        verify(credentialRepository).updateStatusForTutor(tutor.getTutorId(),
                TutorCredentialStatus.PENDING, TutorCredentialStatus.VERIFIED);
    }

    @Test
    void req04_approveTutorWhoAlreadyPaidKeepsActivePaid() {
        tutor.setTutorState(TutorState.ACTIVE_PAID);

        service.approve(tutor.getTutorId());

        assertEquals(TutorVerificationStatus.APPROVED, tutor.getVerificationStatus());
        assertEquals(TutorState.ACTIVE_PAID, tutor.getTutorState());
    }

    /**
     * Hai cán bộ cùng duyệt: người thứ hai đọc lại hồ sơ dưới khóa dòng và thấy đã "Đã phê duyệt".
     * Test đơn vị mô phỏng thứ tự này; khóa dòng thật của PostgreSQL không được kiểm chứng ở đây.
     */
    @Test
    void req04_secondOfficerApprovingSameProfileGetsConflictAndNothingIsRecordedTwice() {
        service.approve(tutor.getTutorId());

        assertEquals("ALREADY_PROCESSED", assertThrows(VerificationException.class,
                () -> service.approve(tutor.getTutorId())).getCode());
        verify(historyRepository, times(1)).save(any());
        verify(notificationService, times(1)).approvedAfterCommit(anyString(), anyString());
    }

    // ===== REQ-05: từ chối hồ sơ =====

    private RejectVerificationRequest.RejectVerificationRequestBuilder rejectRequest() {
        return RejectVerificationRequest.builder().reasonCode("OTHER").internalNote("Ghi chú");
    }

    @Test
    void req05_internalNoteOf2000CharactersIsAccepted() {
        service.reject(tutor.getTutorId(), rejectRequest().internalNote("n".repeat(2000)).build());

        assertEquals(TutorVerificationStatus.REJECTED, tutor.getVerificationStatus());
        assertEquals("n".repeat(2000), savedHistory().getInternalNote());
    }

    @Test
    void req05_internalNoteOf2001CharactersIsRejectedWithoutLocking() {
        VerificationException ex = assertThrows(VerificationException.class,
                () -> service.reject(tutor.getTutorId(), rejectRequest().internalNote("n".repeat(2001)).build()));

        assertEquals("Ghi chú nội bộ tối đa 2.000 ký tự.", ex.getFields().get("internalNote"));
        verify(tutorRepository, never()).findLockedByTutorId(any());
        assertEquals(TutorVerificationStatus.PENDING, tutor.getVerificationStatus());
    }

    @Test
    void req05_feedbackOf2000CharactersIsAccepted() {
        service.reject(tutor.getTutorId(), rejectRequest().feedback("f".repeat(2000)).build());

        assertEquals("f".repeat(2000), savedHistory().getFeedback());
    }

    @Test
    void req05_feedbackOf2001CharactersIsRejected() {
        VerificationException ex = assertThrows(VerificationException.class,
                () -> service.reject(tutor.getTutorId(), rejectRequest().feedback("f".repeat(2001)).build()));

        assertEquals("Phản hồi gửi gia sư tối đa 2.000 ký tự.", ex.getFields().get("feedback"));
        verify(historyRepository, never()).save(any());
        verifyNoInteractions(notificationService);
    }

    // ===== REQ-06: yêu cầu bổ sung thông tin =====

    @Test
    void req06_requestOf2000CharactersMovesToNeedsInformation() {
        service.requestInfo(tutor.getTutorId(), new RequestInfoRequest("m".repeat(2000)));

        assertEquals(TutorVerificationStatus.ADDITIONAL_INFO_REQUIRED, tutor.getVerificationStatus());
        assertTrue(VerificationStateMachine.isEditable(tutor.getVerificationStatus()), "gia sư sửa lại được hồ sơ");
        assertEquals(TutorVerifAction.REQUEST_INFO, savedHistory().getAction());
    }

    @Test
    void req06_requestOf2001CharactersIsRejected() {
        VerificationException ex = assertThrows(VerificationException.class,
                () -> service.requestInfo(tutor.getTutorId(), new RequestInfoRequest("m".repeat(2001))));

        assertEquals("Nội dung yêu cầu tối đa 2.000 ký tự.", ex.getFields().get("message"));
        assertEquals(TutorVerificationStatus.PENDING, tutor.getVerificationStatus());
        verifyNoInteractions(notificationService);
    }

    // ===== REQ-07: danh sách hồ sơ chờ duyệt =====

    @SuppressWarnings("unchecked")
    private org.springframework.data.domain.Pageable queuePageRequest(VerificationQueueQuery query) {
        when(tutorRepository.findAll(any(org.springframework.data.jpa.domain.Specification.class),
                any(org.springframework.data.domain.Pageable.class)))
                .thenReturn(org.springframework.data.domain.Page.empty());
        service.getQueue(query);
        ArgumentCaptor<org.springframework.data.domain.Pageable> pageable =
                ArgumentCaptor.forClass(org.springframework.data.domain.Pageable.class);
        verify(tutorRepository).findAll(any(org.springframework.data.jpa.domain.Specification.class), pageable.capture());
        return pageable.getValue();
    }

    @ParameterizedTest
    @org.junit.jupiter.params.provider.CsvSource({"0, 10", "1, 1", "49, 49", "50, 50", "51, 50"})
    void req07_pageSizeUsesDefaultForZeroAndIsCappedAt50(int requested, int expected) {
        var pageable = queuePageRequest(VerificationQueueQuery.builder().size(requested).build());

        assertEquals(expected, pageable.getPageSize());
    }

    @Test
    void req07_keywordOf100CharactersIsAccepted() {
        assertDoesNotThrow(() -> queuePageRequest(VerificationQueueQuery.builder().keyword("k".repeat(100)).build()));
    }

    @Test
    void req07_keywordOf101CharactersIsRejected() {
        VerificationException ex = assertThrows(VerificationException.class,
                () -> service.getQueue(VerificationQueueQuery.builder().keyword("k".repeat(101)).build()));

        assertTrue(ex.getFields().containsKey("keyword"));
    }

    @Test
    void req07_sameStartAndEndDateIsAcceptedButEndBeforeStartIsRejected() {
        LocalDate day = LocalDate.of(2026, 11, 3);
        assertDoesNotThrow(() -> queuePageRequest(VerificationQueueQuery.builder()
                .submittedFrom(day).submittedTo(day).build()));

        VerificationException ex = assertThrows(VerificationException.class, () -> service.getQueue(
                VerificationQueueQuery.builder().submittedFrom(day).submittedTo(day.minusDays(1)).build()));
        assertTrue(ex.getFields().containsKey("submittedTo"));
    }

    @Test
    void req07_invalidJlptAndStatusFiltersAreRejectedWithFieldErrors() {
        assertTrue(assertThrows(VerificationException.class,
                () -> service.getQueue(VerificationQueueQuery.builder().jlptLevel("N6").build()))
                .getFields().containsKey("jlptLevel"));
        assertTrue(assertThrows(VerificationException.class,
                () -> service.getQueue(VerificationQueueQuery.builder().status("KHONG_CO").build()))
                .getFields().containsKey("status"));
    }

    // ===== REQ-09: duyệt / từ chối yêu cầu cập nhật hồ sơ =====

    @Test
    void req09_rejectReasonOf2000CharactersIsAccepted() {
        pendingUpdate("N1");

        service.rejectProfileUpdate(tutor.getTutorId(), new RejectProfileUpdateRequest("r".repeat(2000)));

        assertEquals("r".repeat(2000), savedHistory().getFeedback());
        assertEquals("N2", tutor.getHighestJlptLevel());
    }

    @Test
    void req09_rejectReasonOf2001CharactersIsRejectedAndProfileUnchanged() {
        pendingUpdate("N1");

        VerificationException ex = assertThrows(VerificationException.class,
                () -> service.rejectProfileUpdate(tutor.getTutorId(), new RejectProfileUpdateRequest("r".repeat(2001))));

        assertEquals("Lý do tối đa 2.000 ký tự.", ex.getFields().get("feedback"));
        assertEquals("N2", tutor.getHighestJlptLevel());
        verify(historyRepository, never()).save(any());
        verify(credentialRepository, never()).updateStatusForTutor(any(), any(), any());
    }

    @Test
    void firstSubmissionIsNotAProfileUpdate() {
        List<TutorVerificationHistory> timeline = List.of(
                TutorVerificationHistory.builder().action(TutorVerifAction.SUBMIT).build());
        assertTrue(ProfileUpdateTimeline.latest(timeline).isEmpty());
        assertFalse(ProfileUpdateTimeline.hasPending(timeline));
    }
}
