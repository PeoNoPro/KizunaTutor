package com.kizuna.features.verification.service;

import com.kizuna.core.entity.Tutor;
import com.kizuna.core.entity.User;
import com.kizuna.features.verification.dto.response.LatestFeedbackDto;
import com.kizuna.features.verification.dto.response.ProfileUpdateResponse;
import com.kizuna.features.verification.dto.response.TutorVerificationResponse;
import com.kizuna.features.verification.dto.response.VerificationDocumentResponse;
import com.kizuna.features.verification.dto.response.VerificationProfileDto;
import com.kizuna.features.verification.entity.TutorCredential;
import com.kizuna.features.verification.entity.TutorVerifAction;
import com.kizuna.features.verification.entity.TutorVerificationHistory;
import com.kizuna.features.verification.entity.TutorVerificationStatus;
import com.kizuna.features.verification.repository.TutorCredentialRepository;
import com.kizuna.features.verification.repository.TutorVerificationHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.Period;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Dựng dữ liệu hồ sơ xác minh trả ra API và kiểm tra điều kiện nộp hồ sơ (FR-005).
 * Dùng chung cho phía gia sư và phía cán bộ học vụ.
 */
@Component
@RequiredArgsConstructor
public class VerificationViewAssembler {

    public static final ZoneId ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    public static final int MIN_AGE = 18;
    public static final int MIN_BIO_LENGTH = 20;

    private final TutorCredentialRepository credentialRepository;
    private final TutorVerificationHistoryRepository historyRepository;
    private final VerificationDocumentStorage storage;

    /**
     * Kết quả kiểm tra điều kiện nộp: lỗi theo trường và nhóm giấy tờ còn thiếu.
     */
    public record SubmissionCheck(Map<String, String> fields, List<String> missingDocuments) {
        public boolean isComplete() {
            return fields.isEmpty() && missingDocuments.isEmpty();
        }

        public List<String> labels() {
            List<String> labels = new ArrayList<>(fields.values());
            labels.addAll(missingDocuments);
            return labels;
        }
    }

    public TutorVerificationResponse build(Tutor tutor, boolean forOfficer) {
        List<TutorCredential> credentials = credentialRepository.findByTutor_TutorIdOrderByUploadedAtAsc(tutor.getTutorId());
        Set<UUID> available = storage.availableIds(credentials);
        TutorVerificationStatus status = VerificationStateMachine.currentStatus(tutor);
        User user = tutor.getUser();

        TutorVerificationResponse.TutorVerificationResponseBuilder builder = TutorVerificationResponse.builder()
                .status(status.name())
                .statusLabel(VerificationLabels.status(status))
                .submittedAt(tutor.getSubmittedAt())
                .verifiedAt(tutor.getVerifiedAt())
                .profile(profile(tutor))
                .documents(credentials.stream().map(c -> document(c, available.contains(c.getCredentialId()))).toList())
                .latestFeedback(latestFeedback(tutor, status));

        if (status == TutorVerificationStatus.APPROVED) {
            builder.profileUpdate(ProfileUpdateTimeline.latest(historyRepository.findTimeline(tutor.getTutorId()))
                    .map(r -> profileUpdate(r, tutor))
                    .orElse(null));
            if (!forOfficer) {
                builder.updatable(true);
            }
        }

        if (forOfficer) {
            builder.tutorId(tutor.getTutorId()).email(user != null ? user.getEmail() : null);
        } else {
            boolean editable = VerificationStateMachine.isEditable(status);
            builder.editable(editable);
            builder.missingRequirements(editable ? check(tutor, credentials, available).labels() : List.of());
        }
        return builder.build();
    }

    public static ProfileUpdateResponse profileUpdate(ProfileUpdateTimeline.Request request, Tutor tutor) {
        TutorVerificationHistory decision = request.decision();
        String status = decision == null ? "PENDING"
                : decision.getAction() == TutorVerifAction.APPROVE ? "APPROVED" : "REJECTED";
        String label = switch (status) {
            case "PENDING" -> "Chờ thẩm định";
            case "APPROVED" -> "Đã chấp nhận";
            default -> "Không được chấp nhận";
        };
        String requested = request.requestedJlpt();
        String previous = request.previousJlpt();
        return ProfileUpdateResponse.builder()
                .updateId(request.submit().getHistoryId())
                .status(status)
                .statusLabel(label)
                .currentJlptLevel(requested == null ? tutor.getHighestJlptLevel()
                        : (previous == null || "—".equals(previous) ? null : previous))
                .requestedJlptLevel(requested)
                .note(request.submit().getFeedback())
                .feedback(decision != null && decision.getAction() == TutorVerifAction.REJECT ? decision.getFeedback() : null)
                .submittedAt(request.submit().getCreatedAt())
                .reviewedAt(decision != null ? decision.getCreatedAt() : null)
                .reviewedByName(decision != null && decision.getOfficer() != null ? decision.getOfficer().getFullName() : null)
                .build();
    }

    public SubmissionCheck check(Tutor tutor) {
        List<TutorCredential> credentials = credentialRepository.findByTutor_TutorIdOrderByUploadedAtAsc(tutor.getTutorId());
        return check(tutor, credentials, storage.availableIds(credentials));
    }

    /**
     * @param available mã các giấy tờ xem được tệp; giấy tờ mất tệp không được tính là đã nộp.
     */
    public SubmissionCheck check(Tutor tutor, List<TutorCredential> credentials, Set<UUID> available) {
        Map<String, String> fields = new LinkedHashMap<>();
        String fullName = tutor.getUser() != null ? tutor.getUser().getFullName() : null;
        if (fullName == null || fullName.trim().length() < 2) {
            fields.put("fullName", "Vui lòng nhập họ tên.");
        }
        if (isBlank(tutor.getPhone())) {
            fields.put("phone", "Vui lòng nhập số điện thoại.");
        }
        if (tutor.getDateOfBirth() == null) {
            fields.put("dateOfBirth", "Vui lòng nhập ngày sinh.");
        } else if (!isAdult(tutor.getDateOfBirth())) {
            fields.put("dateOfBirth", "Gia sư cần đủ 18 tuổi trở lên.");
        }
        if (isBlank(tutor.getAddress())) {
            fields.put("address", "Vui lòng nhập địa chỉ.");
        }
        if (tutor.getBio() == null || tutor.getBio().trim().length() < MIN_BIO_LENGTH) {
            fields.put("bio", "Vui lòng viết phần giới thiệu tối thiểu 20 ký tự.");
        }
        if (isBlank(tutor.getHighestJlptLevel())) {
            fields.put("highestJlptLevel", "Vui lòng chọn trình độ JLPT cao nhất.");
        }

        List<String> missingDocuments = new ArrayList<>();
        boolean hasIdentity = credentials.stream()
                .anyMatch(c -> c.getCredentialType() != null && c.getCredentialType().isIdentity()
                        && available.contains(c.getCredentialId()));
        boolean hasJapanese = credentials.stream()
                .anyMatch(c -> c.getCredentialType() != null && c.getCredentialType().isJapaneseProficiency()
                        && available.contains(c.getCredentialId()));
        if (!hasIdentity) {
            missingDocuments.add(VerificationLabels.MISSING_IDENTITY);
        }
        if (!hasJapanese) {
            missingDocuments.add(VerificationLabels.MISSING_JAPANESE);
        }
        return new SubmissionCheck(fields, missingDocuments);
    }

    public static boolean isAdult(LocalDate dateOfBirth) {
        return Period.between(dateOfBirth, LocalDate.now(ZONE)).getYears() >= MIN_AGE;
    }

    public VerificationDocumentResponse document(TutorCredential credential) {
        return document(credential, storage.availableIds(List.of(credential)).contains(credential.getCredentialId()));
    }

    private VerificationDocumentResponse document(TutorCredential credential, boolean fileAvailable) {
        String key = credential.getStorageKey();
        return VerificationDocumentResponse.builder()
                .id(credential.getCredentialId())
                .type(credential.getCredentialType() != null ? credential.getCredentialType().name() : null)
                .typeLabel(VerificationLabels.credentialType(credential.getCredentialType()))
                .title(credential.getTitle())
                .fileName(displayFileName(credential))
                .contentType(storage.contentType(key))
                .status(credential.getVerifiedStatus() != null ? credential.getVerifiedStatus().name() : null)
                .statusLabel(VerificationLabels.documentStatus(credential.getVerifiedStatus()))
                .uploadedAt(credential.getUploadedAt())
                .fileAvailable(fileAvailable)
                .build();
    }

    /**
     * Tên tệp hiển thị/tải về sinh từ tiêu đề; không bao giờ là tên lưu trên máy chủ.
     */
    public static String displayFileName(TutorCredential credential) {
        String base = credential.getTitle() == null ? "giay-to" : credential.getTitle().trim()
                .replaceAll("[\\\\/:*?\"<>|\\r\\n]+", " ")
                .replaceAll("\\s+", " ");
        if (base.isEmpty()) {
            base = "giay-to";
        }
        // Dữ liệu nhập trước FE-03 có thể lưu URL ngoài thay cho khóa tệp; khi đó không ghép đuôi tệp
        String ext = VerificationDocumentStorage.isManagedKey(credential.getStorageKey())
                ? VerificationDocumentStorage.extensionOf(credential.getStorageKey()) : "";
        return ext.isEmpty() ? base : base + "." + ext;
    }

    private VerificationProfileDto profile(Tutor tutor) {
        User user = tutor.getUser();
        return VerificationProfileDto.builder()
                .fullName(user != null ? user.getFullName() : null)
                .phone(tutor.getPhone())
                .dateOfBirth(tutor.getDateOfBirth())
                .address(tutor.getAddress())
                .bio(tutor.getBio())
                .experienceYears(tutor.getExperienceYears() != null ? tutor.getExperienceYears().intValue() : 0)
                .highestJlptLevel(tutor.getHighestJlptLevel())
                .build();
    }

    /**
     * Chỉ hiển thị phản hồi khi hồ sơ đang ở "Cần bổ sung thông tin" hoặc "Bị từ chối";
     * sau khi gia sư nộp lại, phản hồi cũ không còn là bước tiếp theo nên không hiển thị.
     */
    private LatestFeedbackDto latestFeedback(Tutor tutor, TutorVerificationStatus status) {
        if (status != TutorVerificationStatus.ADDITIONAL_INFO_REQUIRED && status != TutorVerificationStatus.REJECTED) {
            return null;
        }
        TutorVerifAction expected = status == TutorVerificationStatus.REJECTED
                ? TutorVerifAction.REJECT : TutorVerifAction.REQUEST_INFO;
        return historyRepository
                .findFirstByTutor_TutorIdAndActionInOrderByCreatedAtDesc(tutor.getTutorId(), EnumSet.of(expected))
                .map(this::feedbackFromHistory)
                .orElseGet(() -> legacyFeedback(tutor, expected));
    }

    private LatestFeedbackDto feedbackFromHistory(TutorVerificationHistory history) {
        String content = history.getAction() == TutorVerifAction.REJECT
                ? VerificationStateMachine.tutorFacingRejection(history.getReasonCode(), history.getFeedback())
                : history.getFeedback();
        return LatestFeedbackDto.builder()
                .kind(history.getAction().name())
                .kindLabel(VerificationLabels.action(history.getAction()))
                .reasonLabel(VerificationLabels.rejectionReason(history.getReasonCode()))
                .content(content)
                .createdAt(history.getCreatedAt())
                .build();
    }

    private LatestFeedbackDto legacyFeedback(Tutor tutor, TutorVerifAction kind) {
        if (isBlank(tutor.getVerificationFeedback())) {
            return null;
        }
        return LatestFeedbackDto.builder()
                .kind(kind.name())
                .kindLabel(VerificationLabels.action(kind))
                .content(tutor.getVerificationFeedback())
                .build();
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
