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
import com.kizuna.features.verification.dto.request.SubmitProfileUpdateRequest;
import com.kizuna.features.verification.entity.TutorVerificationHistory;
import com.kizuna.features.verification.service.ProfileUpdateTimeline;
import com.kizuna.features.verification.entity.TutorCredentialStatus;
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

import org.springframework.mock.web.MockMultipartFile;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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
    /** Giấy tờ không còn tệp (không có trong DB lẫn thư mục cũ). */
    private final Set<UUID> missingFiles = new HashSet<>();

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
        when(storage.availableIds(any())).thenAnswer(inv -> {
            Collection<TutorCredential> list = inv.getArgument(0);
            Set<UUID> ids = new HashSet<>();
            list.stream().map(TutorCredential::getCredentialId).filter(id -> !missingFiles.contains(id)).forEach(ids::add);
            return ids;
        });
        when(storage.contentType(any())).thenReturn("application/pdf");

        VerificationStateMachine stateMachine = new VerificationStateMachine(historyRepository, credentialRepository);
        VerificationViewAssembler assembler = new VerificationViewAssembler(credentialRepository, historyRepository, storage);
        service = new TutorVerificationServiceImpl(tutorRepository, credentialRepository, storage, stateMachine,
                assembler, currentUserProvider, historyRepository);
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
    @EnumSource(value = TutorVerificationStatus.class, names = {"PENDING"})
    void editingLockedProfileIsRejected(TutorVerificationStatus status) {
        tutor.setVerificationStatus(status);
        VerificationException ex = assertThrows(VerificationException.class,
                () -> service.updateProfile(new UpdateVerificationProfileRequest()));
        assertEquals("VERIFICATION_LOCKED", ex.getCode());
        assertThrows(VerificationException.class, () -> service.removeDocument(UUID.randomUUID()));
        assertThrows(VerificationException.class, () -> service.addDocument("JLPT_CERTIFICATE", "N2", null));
    }

    /** FE-16: giả lập lần từ chối hồ sơ cách đây ... (dòng lịch sử REJECT mới nhất). */
    private void rejectedAgo(java.time.Duration ago) {
        tutor.setVerificationStatus(TutorVerificationStatus.REJECTED);
        TutorVerificationHistory rejection = TutorVerificationHistory.builder().tutor(tutor)
                .action(TutorVerifAction.REJECT).createdAt(java.time.OffsetDateTime.now().minus(ago)).build();
        when(historyRepository.findFirstByTutor_TutorIdAndActionInOrderByCreatedAtDesc(eq(tutor.getTutorId()), any()))
                .thenReturn(Optional.of(rejection));
    }

    @Test
    void rejectedProfileCannotBeEditedOrResubmittedDuringCooldown() {
        rejectedAgo(java.time.Duration.ofDays(2));

        VerificationException first = assertThrows(VerificationException.class,
                () -> service.updateProfile(new UpdateVerificationProfileRequest()));
        assertEquals("VERIFICATION_REJECTED", first.getCode());
        assertTrue(first.getMessage().startsWith("Hồ sơ của bạn đã bị từ chối. Bạn có thể chỉnh sửa và nộp lại hồ sơ từ "),
                first.getMessage());
        assertEquals("VERIFICATION_REJECTED", assertThrows(VerificationException.class,
                () -> service.addDocument("IDENTITY_DOCUMENT", "CCCD", null)).getCode());
        assertEquals("VERIFICATION_REJECTED", assertThrows(VerificationException.class,
                () -> service.removeDocument(UUID.randomUUID())).getCode());
        assertEquals("VERIFICATION_REJECTED", assertThrows(VerificationException.class,
                () -> service.submit(new SubmitVerificationRequest(true))).getCode());
        TutorVerificationResponse view = service.getMyVerification();
        assertFalse(view.getEditable());
        assertNotNull(view.getResubmitAvailableAt());
        assertTrue(view.getMissingRequirements().isEmpty(), "chỉ xem thì không liệt kê phần còn thiếu");
        verify(historyRepository, never()).save(any());
    }

    @Test
    void fe16_rejectedProfileReopensAfterThreeDaysAndCanBeResubmittedWithNewDocuments() {
        rejectedAgo(java.time.Duration.ofDays(3).plusMinutes(1));
        fillCompleteProfile();
        // giấy tờ cũ đã bị đánh dấu "Không được chấp nhận" khi từ chối
        addDocument(CredentialType.IDENTITY_DOCUMENT);
        addDocument(CredentialType.JLPT_CERTIFICATE);
        credentials.forEach(c -> c.setVerifiedStatus(TutorCredentialStatus.REJECTED));

        TutorVerificationResponse view = service.getMyVerification();
        assertTrue(view.getEditable());
        assertEquals(2, view.getMissingRequirements().size(), "giấy tờ bị từ chối không được tính");
        assertEquals("SUBMISSION_INCOMPLETE", assertThrows(VerificationException.class,
                () -> service.submit(new SubmitVerificationRequest(true))).getCode());

        addDocument(CredentialType.IDENTITY_DOCUMENT);
        addDocument(CredentialType.JLPT_CERTIFICATE);
        TutorVerificationResponse response = service.submit(new SubmitVerificationRequest(true));

        assertEquals(TutorVerificationStatus.PENDING, tutor.getVerificationStatus());
        assertEquals(TutorVerificationServiceImpl.MSG_SUBMITTED, response.getMessage());
        var captor = ArgumentCaptor.forClass(TutorVerificationHistory.class);
        verify(historyRepository, times(1)).save(captor.capture());
        assertEquals(TutorVerifAction.SUBMIT, captor.getValue().getAction());
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
        missingFiles.add(credentials.get(0).getCredentialId());

        VerificationException ex = assertThrows(VerificationException.class,
                () -> service.submit(new SubmitVerificationRequest(true)));
        assertEquals(List.of("Giấy tờ tùy thân"), ex.getMissingDocuments());
    }

    @Test
    void req03_submitWithIdentityButNoJapaneseCertificateIsRejected() {
        fillCompleteProfile();
        addDocument(CredentialType.IDENTITY_DOCUMENT);

        VerificationException ex = assertThrows(VerificationException.class,
                () -> service.submit(new SubmitVerificationRequest(true)));

        assertEquals("SUBMISSION_INCOMPLETE", ex.getCode());
        assertTrue(ex.getMessage().startsWith("Không thể nộp hồ sơ. Vui lòng hoàn thiện"));
        assertEquals(List.of(com.kizuna.features.verification.service.VerificationLabels.MISSING_JAPANESE),
                ex.getMissingDocuments());
        assertEquals(TutorVerificationStatus.UNVERIFIED, tutor.getVerificationStatus());
        verify(historyRepository, never()).save(any());
    }

    @Test
    void req03_otherJapaneseCertificateCountsAsLanguageDocument() {
        fillCompleteProfile();
        addDocument(CredentialType.IDENTITY_DOCUMENT);
        addDocument(CredentialType.OTHER_JAPANESE_CERTIFICATE);

        service.submit(new SubmitVerificationRequest(true));

        assertEquals(TutorVerificationStatus.PENDING, tutor.getVerificationStatus());
    }

    @Test
    void req02_missingTypeAndBlankTitleShowFieldMessages() {
        assertEquals("Vui lòng chọn loại giấy tờ.", assertThrows(VerificationException.class,
                () -> service.addDocument(null, "Hộ chiếu", null)).getFields().get("type"));
        assertEquals("Vui lòng nhập tên giấy tờ.", assertThrows(VerificationException.class,
                () -> service.addDocument("IDENTITY_DOCUMENT", "  ", null)).getFields().get("title"));
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
    void invalidDocumentTypeOrTitleIsRejectedBeforeStoringFile() {
        VerificationException badType = assertThrows(VerificationException.class,
                () -> service.addDocument("PASSPORT_XYZ", "Hộ chiếu", null));
        assertTrue(badType.getFields().containsKey("type"));
        VerificationException noTitle = assertThrows(VerificationException.class,
                () -> service.addDocument("IDENTITY_DOCUMENT", "  ", null));
        assertTrue(noTitle.getFields().containsKey("title"));
        verify(storage, never()).validate(any());
        verify(storage, never()).saveContent(any(), any(), any());
    }

    @Test
    void addDocumentSavesFileContentToDatabaseInSameTransaction() {
        byte[] content = "%PDF-1.4 %%EOF".getBytes(StandardCharsets.US_ASCII);
        String key = UUID.randomUUID() + ".pdf";
        UUID savedId = UUID.randomUUID();
        when(storage.validate(any())).thenReturn(new VerificationDocumentStorage.ValidatedDocument(key, content));
        when(credentialRepository.save(any())).thenAnswer(inv -> {
            TutorCredential c = inv.getArgument(0);
            c.setCredentialId(savedId);
            return c;
        });

        var response = service.addDocument("IDENTITY_DOCUMENT", "CCCD",
                new MockMultipartFile("file", "cccd.pdf", "application/pdf", content));

        verify(storage).saveContent(savedId, key, content);
        assertTrue(response.isFileAvailable());
        assertEquals("CCCD.pdf", response.getFileName());
    }

    @Test
    void updateDocumentWithoutNewFileKeepsStoredContent() {
        addDocument(CredentialType.JLPT_CERTIFICATE);
        TutorCredential existing = credentials.get(0);
        when(credentialRepository.findByCredentialIdAndTutor_TutorId(existing.getCredentialId(), tutor.getTutorId()))
                .thenReturn(Optional.of(existing));
        when(credentialRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.updateDocument(existing.getCredentialId(), null, "JLPT N2", null);

        assertEquals("JLPT N2", existing.getTitle());
        verify(storage, never()).validate(any());
        verify(storage, never()).saveContent(any(), any(), any());
    }

    // ===== Cập nhật hồ sơ sau khi đã phê duyệt =====

    private void approve() {
        fillCompleteProfile();
        tutor.setVerificationStatus(TutorVerificationStatus.APPROVED);
        tutor.setTutorState(TutorState.APPROVED_UNPAID);
    }

    private TutorCredential credential(TutorCredentialStatus status) {
        TutorCredential c = TutorCredential.builder()
                .credentialId(UUID.randomUUID()).tutor(tutor).credentialType(CredentialType.JLPT_CERTIFICATE)
                .title("JLPT").storageKey(UUID.randomUUID() + ".pdf").verifiedStatus(status).build();
        credentials.add(c);
        when(credentialRepository.findByCredentialIdAndTutor_TutorId(c.getCredentialId(), tutor.getTutorId()))
                .thenReturn(Optional.of(c));
        return c;
    }

    @Test
    void approvedTutorSavesContactInfoImmediatelyButKeepsNameBirthDateAndJlpt() {
        approve();
        TutorVerificationResponse response = service.updateProfile(UpdateVerificationProfileRequest.builder()
                .fullName("Tên Khác").phone("0911222333").address("Hà Nội")
                .bio("Giới thiệu mới đủ dài hơn hai mươi ký tự.").experienceYears(6)
                .dateOfBirth(LocalDate.of(2000, 1, 1)).highestJlptLevel("N1").build());

        assertEquals(TutorVerificationStatus.APPROVED, tutor.getVerificationStatus());
        assertEquals("0911222333", tutor.getPhone());
        assertEquals("Hà Nội", tutor.getAddress());
        assertEquals((short) 6, tutor.getExperienceYears());
        assertEquals("Nguyễn Gia Sư", tutor.getUser().getFullName());
        assertEquals(LocalDate.of(1995, 4, 12), tutor.getDateOfBirth());
        assertEquals("N2", tutor.getHighestJlptLevel(), "đổi JLPT phải qua yêu cầu cập nhật");
        assertEquals(TutorVerificationServiceImpl.MSG_APPROVED_SAVED, response.getMessage());
        assertTrue(response.getUpdatable());
        verify(historyRepository, never()).save(any());
    }

    @Test
    void approvedTutorCannotBlankRequiredContactInfo() {
        approve();
        VerificationException ex = assertThrows(VerificationException.class, () -> service.updateProfile(
                UpdateVerificationProfileRequest.builder().phone(" ").address("").bio("ngắn").build()));
        assertEquals("VALIDATION_FAILED", ex.getCode());
        assertTrue(ex.getFields().keySet().containsAll(List.of("phone", "address", "bio")));
        assertEquals("0905123456", tutor.getPhone());
    }

    @Test
    void approvedTutorCannotChangeVerifiedDocumentButCanRemoveNewOnes() {
        approve();
        TutorCredential verified = credential(TutorCredentialStatus.VERIFIED);
        TutorCredential fresh = credential(TutorCredentialStatus.PENDING);

        assertEquals("DOCUMENT_VERIFIED", assertThrows(VerificationException.class,
                () -> service.removeDocument(verified.getCredentialId())).getCode());
        assertEquals("DOCUMENT_VERIFIED", assertThrows(VerificationException.class,
                () -> service.updateDocument(verified.getCredentialId(), "DEGREE", null, null)).getCode());

        service.removeDocument(fresh.getCredentialId());
        verify(credentialRepository).delete(fresh);
    }

    @Test
    void submitProfileUpdateRecordsPendingRequestAndKeepsApprovedStatus() {
        approve();
        credential(TutorCredentialStatus.PENDING);

        TutorVerificationResponse response = service.submitProfileUpdate(new SubmitProfileUpdateRequest("n1", " Đã đỗ N1 "));

        // Lưu vào lịch sử thẩm định: dòng SUBMIT đánh dấu cập nhật, JLPT đề xuất trong notes, ghi chú ở feedback
        ArgumentCaptor<TutorVerificationHistory> saved = ArgumentCaptor.forClass(TutorVerificationHistory.class);
        verify(historyRepository).save(saved.capture());
        assertEquals(TutorVerifAction.SUBMIT, saved.getValue().getAction());
        assertTrue(ProfileUpdateTimeline.isUpdateRow(saved.getValue()));
        assertEquals("N1", new ProfileUpdateTimeline.Request(saved.getValue(), null).requestedJlpt());
        assertEquals("N2", new ProfileUpdateTimeline.Request(saved.getValue(), null).previousJlpt());
        assertEquals("Đã đỗ N1", saved.getValue().getFeedback());
        assertEquals(TutorVerificationStatus.APPROVED, tutor.getVerificationStatus());
        assertEquals("N2", tutor.getHighestJlptLevel(), "chưa áp dụng cho tới khi được duyệt");
        assertEquals(TutorVerificationServiceImpl.MSG_UPDATE_SUBMITTED, response.getMessage());
    }

    @Test
    void submitProfileUpdateWithoutChangesIsRejected() {
        approve();
        credential(TutorCredentialStatus.VERIFIED);
        VerificationException ex = assertThrows(VerificationException.class,
                () -> service.submitProfileUpdate(new SubmitProfileUpdateRequest("N2", null)));
        assertEquals(TutorVerificationServiceImpl.MSG_NO_CHANGES, ex.getMessage());
        verify(historyRepository, never()).save(any());
    }

    @Test
    void pendingProfileUpdateLocksDocumentsButNotContactInfo() {
        approve();
        when(historyRepository.findTimeline(tutor.getTutorId())).thenReturn(List.of(
                TutorVerificationHistory.builder().action(TutorVerifAction.APPROVE).build(),
                TutorVerificationHistory.builder().action(TutorVerifAction.SUBMIT)
                        .internalNote(ProfileUpdateTimeline.submitNotes("N2", "N1")).build()));
        assertEquals("PROFILE_UPDATE_PENDING", assertThrows(VerificationException.class,
                () -> service.addDocument("JLPT_CERTIFICATE", "N1", null)).getCode());
        assertEquals("PROFILE_UPDATE_PENDING", assertThrows(VerificationException.class,
                () -> service.submitProfileUpdate(new SubmitProfileUpdateRequest("N1", null))).getCode());
        service.updateProfile(UpdateVerificationProfileRequest.builder().phone("0911222333").address("Huế")
                .bio("Giới thiệu mới đủ dài hơn hai mươi ký tự.").build());
        assertEquals("Huế", tutor.getAddress());
    }

    @Test
    void unapprovedTutorCannotSubmitProfileUpdate() {
        tutor.setVerificationStatus(TutorVerificationStatus.PENDING);
        assertEquals("VERIFICATION_LOCKED", assertThrows(VerificationException.class,
                () -> service.submitProfileUpdate(new SubmitProfileUpdateRequest("N1", null))).getCode());
    }

    // ===== REQ-01: giá trị biên khi cập nhật hồ sơ =====

    /** Ngày hiện tại theo múi giờ mà hệ thống dùng để tính tuổi. */
    private static LocalDate today() {
        return LocalDate.now(VerificationViewAssembler.ZONE);
    }

    @Test
    void req01_tutorTurningEighteenTodayIsAccepted() {
        LocalDate birthday = today().minusYears(18);

        service.updateProfile(UpdateVerificationProfileRequest.builder().dateOfBirth(birthday).build());

        assertEquals(birthday, tutor.getDateOfBirth());
        verify(tutorRepository).save(tutor);
    }

    @Test
    void req01_tutorOneDayShortOfEighteenIsRejectedAndNothingSaved() {
        VerificationException ex = assertThrows(VerificationException.class, () -> service.updateProfile(
                UpdateVerificationProfileRequest.builder().dateOfBirth(today().minusYears(18).plusDays(1)).build()));

        assertEquals("Gia sư cần đủ 18 tuổi trở lên.", ex.getFields().get("dateOfBirth"));
        assertNull(tutor.getDateOfBirth());
        verify(tutorRepository, never()).save(any());
    }

    @Test
    void req01_oneCharacterNameIsRejectedAndOldNameKept() {
        VerificationException ex = assertThrows(VerificationException.class,
                () -> service.updateProfile(UpdateVerificationProfileRequest.builder().fullName(" A ").build()));

        assertEquals("Họ tên cần tối thiểu 2 ký tự.", ex.getFields().get("fullName"));
        assertEquals("Nguyễn Gia Sư", tutor.getUser().getFullName());
        verify(tutorRepository, never()).save(any());
    }

    @Test
    void req01_twoCharacterNameIsAccepted() {
        service.updateProfile(UpdateVerificationProfileRequest.builder().fullName("An").build());

        assertEquals("An", tutor.getUser().getFullName());
    }

    @Test
    void req01_profileNeedingMoreInformationIsEditable() {
        tutor.setVerificationStatus(TutorVerificationStatus.ADDITIONAL_INFO_REQUIRED);

        TutorVerificationResponse response = service.updateProfile(
                UpdateVerificationProfileRequest.builder().phone("0911222333").build());

        assertEquals("0911222333", tutor.getPhone());
        assertEquals(TutorVerificationStatus.ADDITIONAL_INFO_REQUIRED, tutor.getVerificationStatus());
        assertTrue(response.getEditable());
    }

    @Test
    void req01_introductionOf19CharactersBlocksSubmission() {
        fillCompleteProfile();
        tutor.setBio("a".repeat(19));
        addDocument(CredentialType.IDENTITY_DOCUMENT);
        addDocument(CredentialType.JLPT_CERTIFICATE);

        VerificationException ex = assertThrows(VerificationException.class,
                () -> service.submit(new SubmitVerificationRequest(true)));

        assertEquals("SUBMISSION_INCOMPLETE", ex.getCode());
        assertEquals(Set.of("bio"), ex.getFields().keySet());
        assertEquals(TutorVerificationStatus.UNVERIFIED, tutor.getVerificationStatus());
    }

    // ===== REQ-02: giấy tờ xác minh =====

    private MockMultipartFile pdf() {
        return new MockMultipartFile("file", "giay-to.pdf", "application/pdf",
                "%PDF-1.4 %%EOF".getBytes(StandardCharsets.US_ASCII));
    }

    private void storageAcceptsAnyFile() {
        when(storage.validate(any())).thenReturn(new VerificationDocumentStorage.ValidatedDocument(
                UUID.randomUUID() + ".pdf", "%PDF-1.4 %%EOF".getBytes(StandardCharsets.US_ASCII)));
        when(credentialRepository.save(any())).thenAnswer(inv -> {
            TutorCredential c = inv.getArgument(0);
            c.setCredentialId(UUID.randomUUID());
            return c;
        });
    }

    @Test
    void req02_titleOf150CharactersIsAcceptedAndNewDocumentIsPending() {
        storageAcceptsAnyFile();
        String title = "T".repeat(150);

        service.addDocument("JLPT_CERTIFICATE", title, pdf());

        ArgumentCaptor<TutorCredential> saved = ArgumentCaptor.forClass(TutorCredential.class);
        verify(credentialRepository).save(saved.capture());
        assertEquals(title, saved.getValue().getTitle());
        assertEquals(CredentialType.JLPT_CERTIFICATE, saved.getValue().getCredentialType());
        assertEquals(TutorCredentialStatus.PENDING, saved.getValue().getVerifiedStatus());
        verify(storage).saveContent(eq(saved.getValue().getCredentialId()), any(), any());
    }

    @Test
    void req02_missingDocumentTypeIsRejected() {
        VerificationException ex = assertThrows(VerificationException.class,
                () -> service.addDocument(" ", "CCCD", pdf()));

        assertEquals("Vui lòng chọn loại giấy tờ.", ex.getFields().get("type"));
        verify(credentialRepository, never()).save(any());
    }

    @Test
    void req02_invalidFileIsRejectedAndNoDocumentRowIsCreated() {
        when(storage.validate(any())).thenThrow(VerificationException.invalidFile());

        assertThrows(VerificationException.class, () -> service.addDocument("IDENTITY_DOCUMENT", "CCCD", pdf()));

        verify(credentialRepository, never()).save(any());
        verify(storage, never()).saveContent(any(), any(), any());
    }

    /**
     * Tải tệp lên R2 lỗi thì ngoại lệ phải thoát ra khỏi addDocument (không bị nuốt), để @Transactional
     * huỷ cả dòng giấy tờ vừa thêm. Test đơn vị chỉ chứng minh ngoại lệ được ném ra; việc huỷ giao dịch
     * thật sự do Spring thực hiện và không được kiểm chứng ở đây.
     */
    @Test
    void req02_failedFileUploadPropagatesSoTransactionRollsBack() {
        storageAcceptsAnyFile();
        doThrow(new IllegalStateException("Không tải được giấy tờ xác minh lên R2"))
                .when(storage).saveContent(any(), any(), any());

        assertThrows(IllegalStateException.class, () -> service.addDocument("IDENTITY_DOCUMENT", "CCCD", pdf()));

        verify(credentialRepository, times(1)).save(any());
        verify(historyRepository, never()).save(any());
    }

    // ===== REQ-08: yêu cầu cập nhật hồ sơ sau khi đã phê duyệt =====

    @Test
    void req08_noteOf1000CharactersIsAccepted() {
        approve();

        service.submitProfileUpdate(new SubmitProfileUpdateRequest("N1", "g".repeat(1000)));

        ArgumentCaptor<TutorVerificationHistory> saved = ArgumentCaptor.forClass(TutorVerificationHistory.class);
        verify(historyRepository).save(saved.capture());
        assertEquals("g".repeat(1000), saved.getValue().getFeedback());
    }

    @Test
    void req08_invalidJlptLevelIsRejected() {
        approve();

        VerificationException ex = assertThrows(VerificationException.class,
                () -> service.submitProfileUpdate(new SubmitProfileUpdateRequest("N6", null)));

        assertTrue(ex.getFields().containsKey("highestJlptLevel"));
        verify(historyRepository, never()).save(any());
    }

    // ===== REQ-08: đề nghị gỡ giấy tờ đã xác minh khi gửi yêu cầu cập nhật =====

    private TutorCredential identity(TutorCredentialStatus status) {
        TutorCredential c = credential(status);
        c.setCredentialType(CredentialType.IDENTITY_DOCUMENT);
        c.setTitle("CCCD");
        return c;
    }

    private SubmitProfileUpdateRequest removing(UUID... ids) {
        return new SubmitProfileUpdateRequest(null, null, List.of(ids));
    }

    @Test
    void req08_removingVerifiedDocumentIsRecordedButNotDeleted() {
        approve();
        identity(TutorCredentialStatus.VERIFIED);
        credential(TutorCredentialStatus.VERIFIED);
        TutorCredential old = credential(TutorCredentialStatus.VERIFIED);
        old.setTitle("JLPT N3 cũ");

        TutorVerificationResponse response = service.submitProfileUpdate(removing(old.getCredentialId()));

        ArgumentCaptor<TutorVerificationHistory> saved = ArgumentCaptor.forClass(TutorVerificationHistory.class);
        verify(historyRepository).save(saved.capture());
        List<ProfileUpdateTimeline.RemovedDocument> removed = ProfileUpdateTimeline.latest(List.of(saved.getValue()))
                .orElseThrow().removedDocuments();
        assertEquals(List.of(new ProfileUpdateTimeline.RemovedDocument(old.getCredentialId(), "JLPT N3 cũ")), removed);
        verify(credentialRepository, never()).delete(any());
        assertEquals(TutorVerificationServiceImpl.MSG_UPDATE_SUBMITTED, response.getMessage());
    }

    @Test
    void req08_removingNewDocumentThroughUpdateIsRejected() {
        approve();
        identity(TutorCredentialStatus.VERIFIED);
        TutorCredential fresh = credential(TutorCredentialStatus.PENDING);

        VerificationException ex = assertThrows(VerificationException.class,
                () -> service.submitProfileUpdate(removing(fresh.getCredentialId())));

        assertEquals(TutorVerificationServiceImpl.MSG_REMOVE_ONLY_VERIFIED, ex.getMessage());
        verify(historyRepository, never()).save(any());
    }
}
