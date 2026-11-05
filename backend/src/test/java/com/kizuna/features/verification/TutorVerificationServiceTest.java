package com.kizuna.features.verification;

import com.kizuna.core.entity.Tutor;
import com.kizuna.core.entity.TutorState;
import com.kizuna.core.entity.User;
import com.kizuna.features.verification.dto.request.SubmitVerificationRequest;
import com.kizuna.features.verification.dto.request.UpdateVerificationProfileRequest;
import com.kizuna.features.verification.dto.response.TutorVerificationResponse;
import com.kizuna.features.verification.entity.CredentialType;
import com.kizuna.features.verification.entity.TutorCredential;
import com.kizuna.features.verification.entity.TutorVerifAction;
import com.kizuna.features.verification.entity.TutorVerificationStatus;
import com.kizuna.features.verification.repository.TutorCredentialRepository;
import com.kizuna.features.verification.repository.TutorVerificationHistoryRepository;
import com.kizuna.features.verification.repository.VerificationTutorRepository;
import com.kizuna.features.verification.service.CurrentUserProvider;
import com.kizuna.features.verification.service.VerificationDocumentStorage;
import com.kizuna.features.verification.service.VerificationException;
import com.kizuna.features.verification.service.VerificationStateMachine;
import com.kizuna.features.verification.service.VerificationViewAssembler;
import com.kizuna.features.verification.service.impl.TutorVerificationServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class TutorVerificationServiceTest {

    private VerificationTutorRepository tutorRepository;
    private TutorCredentialRepository credentialRepository;
    private TutorVerificationHistoryRepository historyRepository;
    private VerificationDocumentStorage storage;
    private TutorVerificationServiceImpl service;

    private final UUID userId = UUID.randomUUID();
    private Tutor tutor;
    private final List<TutorCredential> credentials = new ArrayList<>();

    @BeforeEach
    void setUp() {
        tutorRepository = mock(VerificationTutorRepository.class);
        credentialRepository = mock(TutorCredentialRepository.class);
        historyRepository = mock(TutorVerificationHistoryRepository.class);
        storage = mock(VerificationDocumentStorage.class);
        CurrentUserProvider currentUserProvider = mock(CurrentUserProvider.class);

        User user = User.builder().userId(userId).email("gs@kizuna.test").fullName("Nguyễn Gia Sư").build();
        tutor = Tutor.builder()
                .tutorId(UUID.randomUUID())
                .user(user)
                .verificationStatus(TutorVerificationStatus.UNVERIFIED)
                .tutorState(TutorState.DRAFT_PROFILE)
                .build();

        when(currentUserProvider.requireCurrentUser()).thenReturn(user);
        when(tutorRepository.findLockedByUserId(userId)).thenReturn(Optional.of(tutor));
        when(tutorRepository.findWithUserByUserId(userId)).thenReturn(Optional.of(tutor));
        when(credentialRepository.findByTutor_TutorIdOrderByUploadedAtAsc(any())).thenReturn(credentials);
        when(historyRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(storage.exists(any())).thenReturn(true);
        when(storage.contentType(any())).thenReturn("application/pdf");

        VerificationStateMachine stateMachine = new VerificationStateMachine(historyRepository, credentialRepository);
        VerificationViewAssembler assembler = new VerificationViewAssembler(credentialRepository, historyRepository, storage);
        service = new TutorVerificationServiceImpl(tutorRepository, credentialRepository, storage, stateMachine,
                assembler, currentUserProvider);
    }

    private void fillCompleteProfile() {
        tutor.setPhone("0905123456");
        tutor.setDateOfBirth(LocalDate.of(1995, 4, 12));
        tutor.setAddress("Đà Nẵng");
        tutor.setBio("Tôi có kinh nghiệm dạy tiếng Nhật cho người mới bắt đầu.");
        tutor.setHighestJlptLevel("N2");
    }

    private void addDocument(CredentialType type) {
        credentials.add(TutorCredential.builder()
                .credentialId(UUID.randomUUID()).tutor(tutor).credentialType(type)
                .title(type.name()).storageKey(UUID.randomUUID() + ".pdf").build());
    }

    @Test
    void saveDraftWhileEditableKeepsStatusAndRecordsNoHistory() {
        TutorVerificationResponse response = service.updateProfile(UpdateVerificationProfileRequest.builder()
                .fullName(" Trần Văn B ").phone("0905123456").highestJlptLevel("N3").build());

        assertEquals("Trần Văn B", tutor.getUser().getFullName());
        assertEquals("0905123456", tutor.getPhone());
        assertEquals(TutorVerificationStatus.UNVERIFIED, tutor.getVerificationStatus());
        assertTrue(response.getEditable());
        assertEquals(TutorVerificationServiceImpl.MSG_SAVED, response.getMessage());
        verify(historyRepository, never()).save(any());
    }

    @ParameterizedTest
    @EnumSource(value = TutorVerificationStatus.class, names = {"PENDING", "APPROVED"})
    void editingLockedProfileIsRejected(TutorVerificationStatus status) {
        tutor.setVerificationStatus(status);
        VerificationException ex = assertThrows(VerificationException.class,
                () -> service.updateProfile(new UpdateVerificationProfileRequest()));
        assertEquals("VERIFICATION_LOCKED", ex.getCode());
        assertThrows(VerificationException.class, () -> service.removeDocument(UUID.randomUUID()));
        assertThrows(VerificationException.class, () -> service.addDocument("JLPT_CERTIFICATE", "N2", null));
    }

    @Test
    void rejectedProfileCannotBeEditedOrResubmitted() {
        tutor.setVerificationStatus(TutorVerificationStatus.REJECTED);

        assertEquals("VERIFICATION_REJECTED", assertThrows(VerificationException.class,
                () -> service.updateProfile(new UpdateVerificationProfileRequest())).getCode());
        assertEquals("VERIFICATION_REJECTED", assertThrows(VerificationException.class,
                () -> service.addDocument("IDENTITY_DOCUMENT", "CCCD", null)).getCode());
        assertEquals("VERIFICATION_REJECTED", assertThrows(VerificationException.class,
                () -> service.removeDocument(UUID.randomUUID())).getCode());
        assertEquals("VERIFICATION_REJECTED", assertThrows(VerificationException.class,
                () -> service.submit(new SubmitVerificationRequest(true))).getCode());
        assertFalse(service.getMyVerification().getEditable());
        verify(historyRepository, never()).save(any());
    }

    @Test
    void underageDateOfBirthIsRejected() {
        VerificationException ex = assertThrows(VerificationException.class,
                () -> service.updateProfile(UpdateVerificationProfileRequest.builder()
                        .dateOfBirth(LocalDate.now().minusYears(17)).build()));
        assertTrue(ex.getFields().containsKey("dateOfBirth"));
    }

    @Test
    void submitWithMissingFieldsAndDocumentsReportsEverythingMissing() {
        addDocument(CredentialType.JLPT_CERTIFICATE);

        VerificationException ex = assertThrows(VerificationException.class,
                () -> service.submit(new SubmitVerificationRequest(true)));

        assertEquals("SUBMISSION_INCOMPLETE", ex.getCode());
        assertTrue(ex.getFields().keySet().containsAll(List.of("phone", "dateOfBirth", "address", "bio", "highestJlptLevel")));
        assertEquals(List.of("Giấy tờ tùy thân"), ex.getMissingDocuments());
        assertEquals(TutorVerificationStatus.UNVERIFIED, tutor.getVerificationStatus());
        verify(historyRepository, never()).save(any());
    }

    @Test
    void documentWhoseFileIsMissingDoesNotCountTowardsRequirements() {
        fillCompleteProfile();
        addDocument(CredentialType.IDENTITY_DOCUMENT);
        addDocument(CredentialType.OTHER_JAPANESE_CERTIFICATE);
        when(storage.exists(credentials.get(0).getStorageKey())).thenReturn(false);

        VerificationException ex = assertThrows(VerificationException.class,
                () -> service.submit(new SubmitVerificationRequest(true)));
        assertEquals(List.of("Giấy tờ tùy thân"), ex.getMissingDocuments());
    }

    @Test
    void submitWithoutCommitmentIsRejected() {
        fillCompleteProfile();
        addDocument(CredentialType.IDENTITY_DOCUMENT);
        addDocument(CredentialType.JLPT_CERTIFICATE);

        assertEquals("COMMITMENT_REQUIRED", assertThrows(VerificationException.class,
                () -> service.submit(new SubmitVerificationRequest(false))).getCode());
        assertEquals("COMMITMENT_REQUIRED", assertThrows(VerificationException.class,
                () -> service.submit(null)).getCode());
    }

    @Test
    void completeSubmissionMovesToPendingRecordsSubmitAndLocksForm() {
        fillCompleteProfile();
        addDocument(CredentialType.IDENTITY_DOCUMENT);
        addDocument(CredentialType.JLPT_CERTIFICATE);

        TutorVerificationResponse response = service.submit(new SubmitVerificationRequest(true));

        assertEquals(TutorVerificationStatus.PENDING, tutor.getVerificationStatus());
        assertEquals("Đang chờ thẩm định", response.getStatusLabel());
        assertFalse(response.getEditable());
        assertNotNull(response.getSubmittedAt());
        assertEquals(TutorVerificationServiceImpl.MSG_SUBMITTED, response.getMessage());
        var captor = ArgumentCaptor.forClass(com.kizuna.features.verification.entity.TutorVerificationHistory.class);
        verify(historyRepository, times(1)).save(captor.capture());
        assertEquals(TutorVerifAction.SUBMIT, captor.getValue().getAction());
    }

    @Test
    void secondSubmitIsRejectedAsLocked() {
        fillCompleteProfile();
        addDocument(CredentialType.IDENTITY_DOCUMENT);
        addDocument(CredentialType.JLPT_CERTIFICATE);
        service.submit(new SubmitVerificationRequest(true));

        assertEquals("VERIFICATION_LOCKED", assertThrows(VerificationException.class,
                () -> service.submit(new SubmitVerificationRequest(true))).getCode());
        verify(historyRepository, times(1)).save(any());
    }

    @Test
    void stateChangingOperationsAlwaysUseRowLock() {
        fillCompleteProfile();
        addDocument(CredentialType.IDENTITY_DOCUMENT);
        addDocument(CredentialType.JLPT_CERTIFICATE);

        service.updateProfile(UpdateVerificationProfileRequest.builder()
                .phone(tutor.getPhone()).dateOfBirth(tutor.getDateOfBirth()).address(tutor.getAddress())
                .bio(tutor.getBio()).highestJlptLevel(tutor.getHighestJlptLevel()).build());
        service.submit(new SubmitVerificationRequest(true));

        verify(tutorRepository, times(2)).findLockedByUserId(userId);
        verify(tutorRepository, never()).findWithUserByUserId(userId);
    }

    @Test
    void invalidDocumentTypeOrTitleIsRejectedBeforeStoringFile() {
        VerificationException badType = assertThrows(VerificationException.class,
                () -> service.addDocument("PASSPORT_XYZ", "Hộ chiếu", null));
        assertTrue(badType.getFields().containsKey("type"));
        VerificationException noTitle = assertThrows(VerificationException.class,
                () -> service.addDocument("IDENTITY_DOCUMENT", "  ", null));
        assertTrue(noTitle.getFields().containsKey("title"));
        verify(storage, never()).store(any());
    }

    @Test
    void documentOfAnotherTutorIsNotFound() {
        when(credentialRepository.findByCredentialIdAndTutor_TutorId(any(), eq(tutor.getTutorId())))
                .thenReturn(Optional.empty());
        assertEquals("NOT_FOUND", assertThrows(VerificationException.class,
                () -> service.loadMyDocumentFile(UUID.randomUUID())).getCode());
    }
}
