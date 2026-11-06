package com.kizuna.features.verification.service.impl;

import com.kizuna.core.entity.Tutor;
import com.kizuna.core.entity.User;
import com.kizuna.features.verification.dto.request.SubmitVerificationRequest;
import com.kizuna.features.verification.dto.request.UpdateVerificationProfileRequest;
import com.kizuna.features.verification.dto.response.TutorVerificationResponse;
import com.kizuna.features.verification.dto.response.VerificationDocumentResponse;
import com.kizuna.features.verification.entity.CredentialType;
import com.kizuna.features.verification.entity.TutorCredential;
import com.kizuna.features.verification.entity.TutorCredentialStatus;
import com.kizuna.features.verification.repository.TutorCredentialRepository;
import com.kizuna.features.verification.repository.VerificationTutorRepository;
import com.kizuna.features.verification.service.CurrentUserProvider;
import com.kizuna.features.verification.service.DocumentFile;
import com.kizuna.features.verification.service.TutorVerificationService;
import com.kizuna.features.verification.service.VerificationDocumentStorage;
import com.kizuna.features.verification.service.VerificationException;
import com.kizuna.features.verification.service.VerificationStateMachine;
import com.kizuna.features.verification.service.VerificationViewAssembler;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TutorVerificationServiceImpl implements TutorVerificationService {

    public static final String MSG_SAVED = "Đã cập nhật thông tin xác minh. Vui lòng nộp hồ sơ khi đã sẵn sàng.";
    public static final String MSG_SUBMITTED =
            "Đã nộp hồ sơ xác minh thành công. Cán bộ học vụ sẽ thẩm định và phản hồi cho bạn qua email.";
    private static final int MAX_TITLE_LENGTH = 150;

    private final VerificationTutorRepository tutorRepository;
    private final TutorCredentialRepository credentialRepository;
    private final VerificationDocumentStorage storage;
    private final VerificationStateMachine stateMachine;
    private final VerificationViewAssembler assembler;
    private final CurrentUserProvider currentUserProvider;

    @Override
    @Transactional(readOnly = true)
    public TutorVerificationResponse getMyVerification() {
        Tutor tutor = tutorRepository.findWithUserByUserId(currentUserId())
                .orElseThrow(VerificationException::notFound);
        return assembler.build(tutor, false);
    }

    /**
     * Ghi đè toàn bộ thông tin xác minh theo biểu mẫu; giá trị trống được lưu là trống.
     * Họ tên trống được giữ nguyên vì tài khoản luôn phải có họ tên.
     */
    @Override
    @Transactional
    public TutorVerificationResponse updateProfile(UpdateVerificationProfileRequest request) {
        Tutor tutor = lockEditableTutor();
        if (request.getDateOfBirth() != null && !VerificationViewAssembler.isAdult(request.getDateOfBirth())) {
            throw VerificationException.validation(null, Map.of("dateOfBirth", "Gia sư cần đủ 18 tuổi trở lên."));
        }
        String fullName = trimToNull(request.getFullName());
        if (fullName != null) {
            if (fullName.length() < 2) {
                throw VerificationException.validation(null, Map.of("fullName", "Họ tên cần tối thiểu 2 ký tự."));
            }
            tutor.getUser().setFullName(fullName);
        }
        tutor.setPhone(trimToNull(request.getPhone()));
        tutor.setDateOfBirth(request.getDateOfBirth());
        tutor.setAddress(trimToNull(request.getAddress()));
        tutor.setBio(trimToNull(request.getBio()));
        tutor.setExperienceYears(request.getExperienceYears() != null ? request.getExperienceYears().shortValue() : (short) 0);
        tutor.setHighestJlptLevel(trimToNull(request.getHighestJlptLevel()));
        tutorRepository.save(tutor);
        return assembler.build(tutor, false).toBuilder().message(MSG_SAVED).build();
    }

    @Override
    @Transactional
    public VerificationDocumentResponse addDocument(String type, String title, MultipartFile file) {
        Tutor tutor = lockEditableTutor();
        CredentialType credentialType = parseType(type, true);
        String cleanTitle = validTitle(title, true);
        VerificationDocumentStorage.ValidatedDocument document = storage.validate(file);
        TutorCredential credential = TutorCredential.builder()
                .tutor(tutor)
                .credentialType(credentialType)
                .title(cleanTitle)
                .storageKey(document.key())
                .verifiedStatus(TutorCredentialStatus.PENDING)
                .uploadedAt(OffsetDateTime.now())
                .build();
        TutorCredential saved = credentialRepository.save(credential);
        storage.saveContent(saved.getCredentialId(), document.content());
        return assembler.document(saved);
    }

    @Override
    @Transactional
    public VerificationDocumentResponse updateDocument(UUID documentId, String type, String title, MultipartFile file) {
        Tutor tutor = lockEditableTutor();
        TutorCredential credential = credentialRepository.findByCredentialIdAndTutor_TutorId(documentId, tutor.getTutorId())
                .orElseThrow(VerificationException::notFound);
        CredentialType credentialType = parseType(type, false);
        if (credentialType != null) {
            credential.setCredentialType(credentialType);
        }
        String cleanTitle = validTitle(title, false);
        if (cleanTitle != null) {
            credential.setTitle(cleanTitle);
        }
        VerificationDocumentStorage.ValidatedDocument document = null;
        if (file != null && !file.isEmpty()) {
            document = storage.validate(file);
            storage.deleteLegacyAfterCommit(credential.getStorageKey());
            credential.setStorageKey(document.key());
            credential.setVerifiedStatus(TutorCredentialStatus.PENDING);
            credential.setRejectionReason(null);
            credential.setUploadedAt(OffsetDateTime.now());
        }
        TutorCredential saved = credentialRepository.save(credential);
        if (document != null) {
            storage.saveContent(saved.getCredentialId(), document.content());
        }
        return assembler.document(saved);
    }

    @Override
    @Transactional
    public void removeDocument(UUID documentId) {
        Tutor tutor = lockEditableTutor();
        TutorCredential credential = credentialRepository.findByCredentialIdAndTutor_TutorId(documentId, tutor.getTutorId())
                .orElseThrow(VerificationException::notFound);
        credentialRepository.delete(credential);
        storage.deleteLegacyAfterCommit(credential.getStorageKey());
    }

    @Override
    @Transactional(readOnly = true)
    public DocumentFile loadMyDocumentFile(UUID documentId) {
        Tutor tutor = tutorRepository.findWithUserByUserId(currentUserId())
                .orElseThrow(VerificationException::notFound);
        TutorCredential credential = credentialRepository.findByCredentialIdAndTutor_TutorId(documentId, tutor.getTutorId())
                .orElseThrow(VerificationException::notFound);
        return toDocumentFile(credential, storage);
    }

    @Override
    @Transactional
    public TutorVerificationResponse submit(SubmitVerificationRequest request) {
        Tutor tutor = tutorRepository.findLockedByUserId(currentUserId())
                .orElseThrow(VerificationException::notFound);
        VerificationStateMachine.requireEditable(tutor);
        if (request == null || !Boolean.TRUE.equals(request.getCommitment())) {
            throw VerificationException.commitmentRequired();
        }
        VerificationViewAssembler.SubmissionCheck check = assembler.check(tutor);
        if (!check.isComplete()) {
            throw VerificationException.incomplete(check.fields(), check.missingDocuments());
        }
        stateMachine.submit(tutor);
        tutorRepository.save(tutor);
        return assembler.build(tutor, false).toBuilder().message(MSG_SUBMITTED).build();
    }

    static DocumentFile toDocumentFile(TutorCredential credential, VerificationDocumentStorage storage) {
        return storage.load(credential)
                .map(content -> new DocumentFile(content, storage.contentType(credential.getStorageKey()),
                        VerificationViewAssembler.displayFileName(credential)))
                .orElseThrow(VerificationException::notFound);
    }

    private Tutor lockEditableTutor() {
        Tutor tutor = tutorRepository.findLockedByUserId(currentUserId())
                .orElseThrow(VerificationException::notFound);
        VerificationStateMachine.requireEditable(tutor);
        return tutor;
    }

    private UUID currentUserId() {
        User user = currentUserProvider.requireCurrentUser();
        return user.getUserId();
    }

    private static CredentialType parseType(String type, boolean required) {
        if (type == null || type.isBlank()) {
            if (required) {
                throw VerificationException.validation(null, Map.of("type", "Vui lòng chọn loại giấy tờ."));
            }
            return null;
        }
        try {
            return CredentialType.valueOf(type.trim());
        } catch (IllegalArgumentException e) {
            throw VerificationException.validation(null, Map.of("type", "Vui lòng chọn loại giấy tờ."));
        }
    }

    private static String validTitle(String title, boolean required) {
        String clean = trimToNull(title);
        if (clean == null) {
            if (required) {
                throw VerificationException.validation(null, Map.of("title", "Vui lòng nhập tên giấy tờ."));
            }
            return null;
        }
        if (clean.length() > MAX_TITLE_LENGTH) {
            throw VerificationException.validation(null, Map.of("title", "Tên giấy tờ tối đa 150 ký tự."));
        }
        return clean;
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
