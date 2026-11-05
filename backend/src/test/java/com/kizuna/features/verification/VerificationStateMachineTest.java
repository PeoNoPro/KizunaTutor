package com.kizuna.features.verification;

import com.kizuna.core.entity.Tutor;
import com.kizuna.core.entity.TutorState;
import com.kizuna.core.entity.User;
import com.kizuna.features.verification.entity.RejectionReason;
import com.kizuna.features.verification.entity.TutorCredentialStatus;
import com.kizuna.features.verification.entity.TutorVerifAction;
import com.kizuna.features.verification.entity.TutorVerificationHistory;
import com.kizuna.features.verification.entity.TutorVerificationStatus;
import com.kizuna.features.verification.repository.TutorCredentialRepository;
import com.kizuna.features.verification.repository.TutorVerificationHistoryRepository;
import com.kizuna.features.verification.service.VerificationException;
import com.kizuna.features.verification.service.VerificationStateMachine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class VerificationStateMachineTest {

    private TutorVerificationHistoryRepository historyRepository;
    private TutorCredentialRepository credentialRepository;
    private VerificationStateMachine stateMachine;
    private final User officer = User.builder().userId(UUID.randomUUID()).fullName("Cán bộ A").build();

    @BeforeEach
    void setUp() {
        historyRepository = mock(TutorVerificationHistoryRepository.class);
        credentialRepository = mock(TutorCredentialRepository.class);
        when(historyRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        stateMachine = new VerificationStateMachine(historyRepository, credentialRepository);
    }

    private Tutor tutor(TutorVerificationStatus status, TutorState state) {
        return Tutor.builder().tutorId(UUID.randomUUID()).verificationStatus(status).tutorState(state).build();
    }

    @ParameterizedTest
    @EnumSource(value = TutorVerificationStatus.class, names = {"UNVERIFIED", "ADDITIONAL_INFO_REQUIRED"})
    void submitFromEditableStatusMovesToPendingAndRecordsOneHistory(TutorVerificationStatus from) {
        Tutor tutor = tutor(from, TutorState.DRAFT_PROFILE);

        TutorVerificationHistory history = stateMachine.submit(tutor);

        assertEquals(TutorVerificationStatus.PENDING, tutor.getVerificationStatus());
        assertEquals(TutorState.PENDING_VERIFICATION, tutor.getTutorState());
        assertNotNull(tutor.getSubmittedAt());
        assertEquals(TutorVerifAction.SUBMIT, history.getAction());
        assertNull(history.getOfficer());
        verify(historyRepository, times(1)).save(any());
    }

    @ParameterizedTest
    @EnumSource(value = TutorVerificationStatus.class, names = {"PENDING", "APPROVED"})
    void submitFromLockedStatusIsRejectedWithoutHistory(TutorVerificationStatus from) {
        Tutor tutor = tutor(from, TutorState.PENDING_VERIFICATION);

        VerificationException ex = assertThrows(VerificationException.class, () -> stateMachine.submit(tutor));

        assertEquals("VERIFICATION_LOCKED", ex.getCode());
        assertEquals(from, tutor.getVerificationStatus());
        verifyNoInteractions(historyRepository);
    }

    @Test
    void rejectedProfileIsFinalAndCannotBeResubmitted() {
        Tutor tutor = tutor(TutorVerificationStatus.REJECTED, TutorState.REJECTED);

        assertFalse(VerificationStateMachine.isEditable(TutorVerificationStatus.REJECTED));
        VerificationException ex = assertThrows(VerificationException.class, () -> stateMachine.submit(tutor));

        assertEquals("VERIFICATION_REJECTED", ex.getCode());
        assertEquals(VerificationException.MSG_REJECTED_FINAL, ex.getMessage());
        assertEquals(TutorVerificationStatus.REJECTED, tutor.getVerificationStatus());
        verifyNoInteractions(historyRepository);
    }

    @Test
    void nullStatusIsTreatedAsNotSubmitted() {
        Tutor tutor = tutor(null, TutorState.DRAFT_PROFILE);
        assertTrue(VerificationStateMachine.isEditable(VerificationStateMachine.currentStatus(tutor)));
        stateMachine.submit(tutor);
        assertEquals(TutorVerificationStatus.PENDING, tutor.getVerificationStatus());
    }

    @Test
    void requestInfoMovesPendingToNeedsInfoWithFeedback() {
        Tutor tutor = tutor(TutorVerificationStatus.PENDING, TutorState.PENDING_VERIFICATION);

        TutorVerificationHistory history = stateMachine.requestInfo(tutor, officer, "Ảnh CCCD bị mờ");

        assertEquals(TutorVerificationStatus.ADDITIONAL_INFO_REQUIRED, tutor.getVerificationStatus());
        assertEquals(TutorState.REJECTED, tutor.getTutorState());
        assertEquals("Ảnh CCCD bị mờ", tutor.getVerificationFeedback());
        assertEquals(TutorVerifAction.REQUEST_INFO, history.getAction());
        assertEquals("Ảnh CCCD bị mờ", history.getFeedback());
        assertSame(officer, history.getOfficer());
    }

    @Test
    void approveMovesToApprovedUnpaidAndVerifiesPendingDocuments() {
        Tutor tutor = tutor(TutorVerificationStatus.PENDING, TutorState.PENDING_VERIFICATION);
        tutor.setVerificationFeedback("phản hồi cũ");

        TutorVerificationHistory history = stateMachine.approve(tutor, officer);

        assertEquals(TutorVerificationStatus.APPROVED, tutor.getVerificationStatus());
        assertEquals(TutorState.APPROVED_UNPAID, tutor.getTutorState());
        assertNotNull(tutor.getVerifiedAt());
        assertNull(tutor.getVerificationFeedback());
        assertEquals(TutorVerifAction.APPROVE, history.getAction());
        verify(credentialRepository).updateStatusForTutor(tutor.getTutorId(),
                TutorCredentialStatus.PENDING, TutorCredentialStatus.VERIFIED);
    }

    @Test
    void approveKeepsActivePaidState() {
        Tutor tutor = tutor(TutorVerificationStatus.PENDING, TutorState.ACTIVE_PAID);
        stateMachine.approve(tutor, officer);
        assertEquals(TutorState.ACTIVE_PAID, tutor.getTutorState());
    }

    @Test
    void rejectStoresReasonFeedbackAndInternalNoteSeparately() {
        Tutor tutor = tutor(TutorVerificationStatus.PENDING, TutorState.PENDING_VERIFICATION);

        TutorVerificationHistory history = stateMachine.reject(tutor, officer,
                RejectionReason.INFORMATION_MISMATCH, "Họ tên không khớp CCCD", "Đã gọi điện xác nhận");

        assertEquals(TutorVerificationStatus.REJECTED, tutor.getVerificationStatus());
        assertEquals(TutorState.REJECTED, tutor.getTutorState());
        assertEquals("Thông tin không trùng khớp với giấy tờ. Họ tên không khớp CCCD", tutor.getVerificationFeedback());
        assertFalse(tutor.getVerificationFeedback().contains("Đã gọi điện"));
        assertEquals(RejectionReason.INFORMATION_MISMATCH, history.getReasonCode());
        assertEquals("Họ tên không khớp CCCD", history.getFeedback());
        assertEquals("Đã gọi điện xác nhận", history.getInternalNote());
    }

    @Test
    void rejectWithoutFeedbackUsesReasonLabelForTutor() {
        Tutor tutor = tutor(TutorVerificationStatus.PENDING, TutorState.PENDING_VERIFICATION);
        stateMachine.reject(tutor, officer, RejectionReason.INSUFFICIENT_QUALIFICATION, "  ", "ghi chú");
        assertEquals("Chưa đủ trình độ chuyên môn", tutor.getVerificationFeedback());
    }

    @ParameterizedTest
    @EnumSource(value = TutorVerificationStatus.class, names = {"UNVERIFIED", "ADDITIONAL_INFO_REQUIRED", "APPROVED", "REJECTED"})
    void officerActionsOnNonPendingAreAlreadyProcessed(TutorVerificationStatus from) {
        Tutor tutor = tutor(from, TutorState.REJECTED);

        assertEquals("ALREADY_PROCESSED",
                assertThrows(VerificationException.class, () -> stateMachine.approve(tutor, officer)).getCode());
        assertEquals("ALREADY_PROCESSED",
                assertThrows(VerificationException.class,
                        () -> stateMachine.reject(tutor, officer, RejectionReason.OTHER, null, "x")).getCode());
        assertEquals("ALREADY_PROCESSED",
                assertThrows(VerificationException.class, () -> stateMachine.requestInfo(tutor, officer, "x")).getCode());
        assertEquals(from, tutor.getVerificationStatus());
        verifyNoInteractions(historyRepository, credentialRepository);
    }

    @Test
    void eachTransitionRecordsExactlyOneHistoryEntry() {
        Tutor tutor = tutor(TutorVerificationStatus.UNVERIFIED, TutorState.DRAFT_PROFILE);
        stateMachine.submit(tutor);
        stateMachine.requestInfo(tutor, officer, "Bổ sung");
        stateMachine.submit(tutor);
        stateMachine.requestInfo(tutor, officer, "Bổ sung lần 2");
        stateMachine.submit(tutor);
        stateMachine.reject(tutor, officer, RejectionReason.OTHER, null, "ghi chú");

        ArgumentCaptor<TutorVerificationHistory> captor = ArgumentCaptor.forClass(TutorVerificationHistory.class);
        verify(historyRepository, times(6)).save(captor.capture());
        assertEquals(
                java.util.List.of(TutorVerifAction.SUBMIT, TutorVerifAction.REQUEST_INFO, TutorVerifAction.SUBMIT,
                        TutorVerifAction.REQUEST_INFO, TutorVerifAction.SUBMIT, TutorVerifAction.REJECT),
                captor.getAllValues().stream().map(TutorVerificationHistory::getAction).toList());
        assertThrows(VerificationException.class, () -> stateMachine.submit(tutor));
    }
}
