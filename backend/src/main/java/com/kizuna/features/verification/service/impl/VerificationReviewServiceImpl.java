package com.kizuna.features.verification.service.impl;

import com.kizuna.core.entity.Tutor;
import com.kizuna.core.entity.User;
import com.kizuna.core.repository.UserRepository;
import com.kizuna.features.verification.dto.request.RejectVerificationRequest;
import com.kizuna.features.verification.dto.request.RequestInfoRequest;
import com.kizuna.features.verification.dto.request.VerificationQueueQuery;
import com.kizuna.features.verification.dto.response.DecisionResultResponse;
import com.kizuna.features.verification.dto.response.PageResponse;
import com.kizuna.features.verification.dto.response.TutorVerificationResponse;
import com.kizuna.features.verification.dto.response.VerificationHistoryItemResponse;
import com.kizuna.features.verification.dto.response.VerificationQueueItemResponse;
import com.kizuna.features.verification.dto.response.VerificationSummaryResponse;
import com.kizuna.features.verification.entity.RejectionReason;
import com.kizuna.features.verification.entity.TutorCredential;
import com.kizuna.features.verification.entity.TutorVerifAction;
import com.kizuna.features.verification.entity.TutorVerificationHistory;
import com.kizuna.features.verification.entity.TutorVerificationStatus;
import com.kizuna.features.verification.dto.request.RejectProfileUpdateRequest;
import com.kizuna.features.verification.entity.TutorCredentialStatus;
import com.kizuna.features.verification.service.ProfileUpdateTimeline;
import com.kizuna.features.verification.repository.TutorCredentialRepository;
import com.kizuna.features.verification.repository.TutorVerificationHistoryRepository;
import com.kizuna.features.verification.repository.VerificationQueueSpecification;
import com.kizuna.features.verification.repository.VerificationTutorRepository;
import com.kizuna.features.verification.service.CurrentUserProvider;
import com.kizuna.features.verification.service.DocumentFile;
import com.kizuna.features.verification.service.VerificationDocumentStorage;
import com.kizuna.features.verification.service.VerificationException;
import com.kizuna.features.verification.service.VerificationLabels;
import com.kizuna.features.verification.service.VerificationNotificationService;
import com.kizuna.features.verification.service.VerificationReviewService;
import com.kizuna.features.verification.service.VerificationStateMachine;
import com.kizuna.features.verification.service.VerificationViewAssembler;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class VerificationReviewServiceImpl implements VerificationReviewService {

    public static final String MSG_APPROVED = "Đã phê duyệt hồ sơ gia sư.";
    public static final String MSG_REJECTED = "Đã từ chối hồ sơ gia sư.";
    public static final String MSG_INFO_REQUESTED = "Đã gửi yêu cầu bổ sung thông tin.";
    public static final String MSG_REJECT_REQUIRED = "Vui lòng chọn lý do từ chối và nhập ghi chú trước khi xác nhận.";
    public static final String MSG_UPDATE_APPROVED = "Đã chấp nhận yêu cầu cập nhật hồ sơ.";
    public static final String MSG_UPDATE_REJECTED = "Đã từ chối yêu cầu cập nhật hồ sơ. Hồ sơ của gia sư vẫn giữ trạng thái đã phê duyệt.";
    public static final String MSG_UPDATE_FEEDBACK_REQUIRED = "Vui lòng nhập lý do không chấp nhận yêu cầu cập nhật.";
    /** Giá trị lọc hàng đợi: yêu cầu cập nhật hồ sơ của gia sư đã phê duyệt. */
    public static final String STATUS_PROFILE_UPDATE = "PROFILE_UPDATE";
    public static final String STATUS_PROFILE_UPDATE_LABEL = "Yêu cầu cập nhật hồ sơ";
    public static final String MSG_INFO_REQUIRED = "Vui lòng nêu rõ thông tin cần bổ sung trước khi gửi yêu cầu.";

    private static final int MAX_TEXT = 2000;
    private static final int DEFAULT_PAGE_SIZE = 10;
    private static final int MAX_PAGE_SIZE = 50;
    private static final int MAX_KEYWORD = 100;

    /** Hồ sơ đã từng nộp (bỏ "Chưa nộp hồ sơ") — dùng cho lựa chọn "Tất cả trạng thái". */
    private static final Set<TutorVerificationStatus> SUBMITTED_STATUSES = EnumSet.of(
            TutorVerificationStatus.PENDING, TutorVerificationStatus.ADDITIONAL_INFO_REQUIRED,
            TutorVerificationStatus.APPROVED, TutorVerificationStatus.REJECTED);

    private final VerificationTutorRepository tutorRepository;
    private final TutorCredentialRepository credentialRepository;
    private final TutorVerificationHistoryRepository historyRepository;
    private final UserRepository userRepository;
    private final VerificationDocumentStorage storage;
    private final VerificationStateMachine stateMachine;
    private final VerificationViewAssembler assembler;
    private final VerificationNotificationService notificationService;
    private final CurrentUserProvider currentUserProvider;

    @Override
    @Transactional(readOnly = true)
    public VerificationSummaryResponse getSummary() {
        ZonedDateTime now = ZonedDateTime.now(VerificationViewAssembler.ZONE);
        OffsetDateTime startOfDay = now.toLocalDate().atStartOfDay(VerificationViewAssembler.ZONE).toOffsetDateTime();
        OffsetDateTime startOfMonth = now.toLocalDate().withDayOfMonth(1).atStartOfDay(VerificationViewAssembler.ZONE).toOffsetDateTime();
        OffsetDateTime startOfNextMonth = startOfMonth.plusMonths(1);

        return VerificationSummaryResponse.builder()
                .totalTutors(tutorRepository.count())
                .notSubmittedCount(tutorRepository.countByVerificationStatus(TutorVerificationStatus.UNVERIFIED))
                .pendingCount(tutorRepository.countByVerificationStatus(TutorVerificationStatus.PENDING))
                .pendingNewToday(tutorRepository.countByVerificationStatusAndSubmittedAtGreaterThanEqual(
                        TutorVerificationStatus.PENDING, startOfDay))
                .pendingOver24h(tutorRepository.countByVerificationStatusAndSubmittedAtLessThan(
                        TutorVerificationStatus.PENDING, now.toOffsetDateTime().minusHours(24)))
                .needsInfoCount(tutorRepository.countByVerificationStatus(TutorVerificationStatus.ADDITIONAL_INFO_REQUIRED))
                .approvedCount(tutorRepository.countByVerificationStatus(TutorVerificationStatus.APPROVED))
                .rejectedCount(tutorRepository.countByVerificationStatus(TutorVerificationStatus.REJECTED))
                .approvedThisMonth(historyRepository.countByActionAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(
                        TutorVerifAction.APPROVE, startOfMonth, startOfNextMonth))
                .rejectedThisMonth(historyRepository.countByActionAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(
                        TutorVerifAction.REJECT, startOfMonth, startOfNextMonth))
                .profileUpdatePendingCount(historyRepository.countPendingProfileUpdates(
                        TutorVerifAction.SUBMIT, TutorVerificationStatus.APPROVED, ProfileUpdateTimeline.MARKER + "%"))
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<VerificationQueueItemResponse> getQueue(VerificationQueueQuery query) {
        VerificationQueueQuery q = query != null ? query : new VerificationQueueQuery();
        String keyword = q.getKeyword() == null ? null : q.getKeyword().trim();
        if (keyword != null && keyword.length() > MAX_KEYWORD) {
            throw VerificationException.validation(null, Map.of("keyword", "Từ khóa tìm kiếm tối đa 100 ký tự."));
        }
        String jlpt = q.getJlptLevel();
        if (jlpt != null && !jlpt.isBlank() && !jlpt.trim().toUpperCase().matches("N[1-5]")) {
            throw VerificationException.validation(null, Map.of("jlptLevel", "Trình độ JLPT không hợp lệ."));
        }
        LocalDate from = q.getSubmittedFrom();
        LocalDate to = q.getSubmittedTo();
        if (from != null && to != null && from.isAfter(to)) {
            throw VerificationException.validation(null, Map.of("submittedTo", "Ngày kết thúc phải sau hoặc bằng ngày bắt đầu."));
        }
        if (STATUS_PROFILE_UPDATE.equalsIgnoreCase(q.getStatus() == null ? "" : q.getStatus().trim())) {
            return profileUpdateQueue(keyword, q);
        }
        int page = q.getPage() == null || q.getPage() < 0 ? 0 : q.getPage();
        int size = q.getSize() == null || q.getSize() < 1 ? DEFAULT_PAGE_SIZE : Math.min(q.getSize(), MAX_PAGE_SIZE);
        boolean newestFirst = "newest".equalsIgnoreCase(q.getSort());

        Page<Tutor> result = tutorRepository.findAll(
                VerificationQueueSpecification.of(parseStatuses(q.getStatus()), keyword, jlpt, from, to, newestFirst),
                PageRequest.of(page, size));
        return PageResponse.of(result, this::toQueueItem);
    }

    @Override
    @Transactional(readOnly = true)
    public TutorVerificationResponse getDetail(UUID tutorId) {
        Tutor tutor = tutorRepository.findWithUserByTutorId(tutorId).orElseThrow(VerificationException::notFound);
        if (VerificationStateMachine.currentStatus(tutor) == TutorVerificationStatus.UNVERIFIED) {
            throw VerificationException.notFound();
        }
        return assembler.build(tutor, true);
    }

    @Override
    @Transactional(readOnly = true)
    public DocumentFile loadDocumentFile(UUID tutorId, UUID documentId) {
        TutorCredential credential = credentialRepository.findByCredentialIdAndTutor_TutorId(documentId, tutorId)
                .orElseThrow(VerificationException::notFound);
        return TutorVerificationServiceImpl.toDocumentFile(credential, storage);
    }

    @Override
    @Transactional
    public DecisionResultResponse approve(UUID tutorId) {
        Tutor tutor = lockTutor(tutorId);
        stateMachine.approve(tutor, currentOfficer());
        tutorRepository.save(tutor);
        notificationService.approvedAfterCommit(tutor.getUser().getEmail(), tutor.getUser().getFullName());
        return result(MSG_APPROVED, tutor);
    }

    @Override
    @Transactional
    public DecisionResultResponse reject(UUID tutorId, RejectVerificationRequest request) {
        RejectionReason reason = parseReason(request == null ? null : request.getReasonCode());
        String internalNote = trimToNull(request == null ? null : request.getInternalNote());
        String feedback = trimToNull(request == null ? null : request.getFeedback());
        Map<String, String> fields = new LinkedHashMap<>();
        if (reason == null) {
            fields.put("reasonCode", "Vui lòng chọn lý do từ chối.");
        }
        if (internalNote == null) {
            fields.put("internalNote", "Vui lòng nhập ghi chú nội bộ.");
        } else if (internalNote.length() > MAX_TEXT) {
            fields.put("internalNote", "Ghi chú nội bộ tối đa 2.000 ký tự.");
        }
        if (feedback != null && feedback.length() > MAX_TEXT) {
            fields.put("feedback", "Phản hồi gửi gia sư tối đa 2.000 ký tự.");
        }
        if (!fields.isEmpty()) {
            throw VerificationException.validation(MSG_REJECT_REQUIRED, fields);
        }

        Tutor tutor = lockTutor(tutorId);
        stateMachine.reject(tutor, currentOfficer(), reason, feedback, internalNote);
        tutorRepository.save(tutor);
        notificationService.rejectedAfterCommit(tutor.getUser().getEmail(), tutor.getUser().getFullName(),
                VerificationLabels.rejectionReason(reason), feedback);
        return result(MSG_REJECTED, tutor);
    }

    @Override
    @Transactional
    public DecisionResultResponse requestInfo(UUID tutorId, RequestInfoRequest request) {
        String message = trimToNull(request == null ? null : request.getMessage());
        if (message == null) {
            throw VerificationException.validation(MSG_INFO_REQUIRED, Map.of("message", MSG_INFO_REQUIRED));
        }
        if (message.length() > MAX_TEXT) {
            throw VerificationException.validation(null, Map.of("message", "Nội dung yêu cầu tối đa 2.000 ký tự."));
        }
        Tutor tutor = lockTutor(tutorId);
        stateMachine.requestInfo(tutor, currentOfficer(), message);
        tutorRepository.save(tutor);
        notificationService.infoRequestedAfterCommit(tutor.getUser().getEmail(), tutor.getUser().getFullName(), message);
        return result(MSG_INFO_REQUESTED, tutor);
    }

    @Override
    @Transactional(readOnly = true)
    public List<VerificationHistoryItemResponse> getHistory(UUID tutorId) {
        tutorRepository.findById(tutorId).orElseThrow(VerificationException::notFound);
        return historyRepository.findTimeline(tutorId).stream().map(this::toHistoryItem).toList();
    }

    private PageResponse<VerificationQueueItemResponse> profileUpdateQueue(String keyword, VerificationQueueQuery q) {
        int page = q.getPage() == null || q.getPage() < 0 ? 0 : q.getPage();
        int size = q.getSize() == null || q.getSize() < 1 ? DEFAULT_PAGE_SIZE : Math.min(q.getSize(), MAX_PAGE_SIZE);
        String pattern = keyword == null || keyword.isBlank() ? "" : "%" + keyword.toLowerCase() + "%";
        return PageResponse.of(historyRepository.searchPendingProfileUpdates(TutorVerifAction.SUBMIT,
                        TutorVerificationStatus.APPROVED, ProfileUpdateTimeline.MARKER + "%", pattern, PageRequest.of(page, size)),
                update -> {
                    VerificationQueueItemResponse item = toQueueItem(update.getTutor());
                    return VerificationQueueItemResponse.builder()
                            .tutorId(item.getTutorId()).fullName(item.getFullName()).email(item.getEmail())
                            .phone(item.getPhone()).highestJlptLevel(item.getHighestJlptLevel())
                            .experienceYears(item.getExperienceYears())
                            .submittedAt(update.getCreatedAt())
                            .status(STATUS_PROFILE_UPDATE)
                            .statusLabel(STATUS_PROFILE_UPDATE_LABEL)
                            .build();
                });
    }

    @Override
    @Transactional
    public DecisionResultResponse approveProfileUpdate(UUID tutorId) {
        Tutor tutor = lockTutor(tutorId);
        ProfileUpdateTimeline.Request request = pendingUpdate(tutor);
        stateMachine.approveProfileUpdate(tutor, currentOfficer(), request.requestedJlpt());
        tutorRepository.save(tutor);
        return result(MSG_UPDATE_APPROVED, tutor);
    }

    @Override
    @Transactional
    public DecisionResultResponse rejectProfileUpdate(UUID tutorId, RejectProfileUpdateRequest request) {
        String feedback = trimToNull(request == null ? null : request.getFeedback());
        if (feedback == null) {
            throw VerificationException.validation(MSG_UPDATE_FEEDBACK_REQUIRED, Map.of("feedback", MSG_UPDATE_FEEDBACK_REQUIRED));
        }
        if (feedback.length() > MAX_TEXT) {
            throw VerificationException.validation(null, Map.of("feedback", "Lý do tối đa 2.000 ký tự."));
        }
        Tutor tutor = lockTutor(tutorId);
        pendingUpdate(tutor);
        stateMachine.rejectProfileUpdate(tutor, currentOfficer(), feedback);
        return result(MSG_UPDATE_REJECTED, tutor);
    }

    /** Yêu cầu đang chờ của gia sư (hồ sơ phải còn "Đã phê duyệt"); không có thì đã bị cán bộ khác xử lý. */
    private ProfileUpdateTimeline.Request pendingUpdate(Tutor tutor) {
        if (VerificationStateMachine.currentStatus(tutor) != TutorVerificationStatus.APPROVED) {
            throw VerificationException.alreadyProcessed();
        }
        return ProfileUpdateTimeline.latest(historyRepository.findTimeline(tutor.getTutorId()))
                .filter(ProfileUpdateTimeline.Request::pending)
                .orElseThrow(VerificationException::alreadyProcessed);
    }

    private Tutor lockTutor(UUID tutorId) {
        return tutorRepository.findLockedByTutorId(tutorId).orElseThrow(VerificationException::notFound);
    }

    private User currentOfficer() {
        return userRepository.getReferenceById(currentUserProvider.requireCurrentUser().getUserId());
    }

    private DecisionResultResponse result(String message, Tutor tutor) {
        TutorVerificationStatus status = VerificationStateMachine.currentStatus(tutor);
        return DecisionResultResponse.builder()
                .message(message)
                .status(status.name())
                .statusLabel(VerificationLabels.status(status))
                .build();
    }

    private VerificationQueueItemResponse toQueueItem(Tutor tutor) {
        TutorVerificationStatus status = VerificationStateMachine.currentStatus(tutor);
        User user = tutor.getUser();
        return VerificationQueueItemResponse.builder()
                .tutorId(tutor.getTutorId())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .phone(tutor.getPhone() != null ? tutor.getPhone() : user.getPhone())
                .highestJlptLevel(tutor.getHighestJlptLevel())
                .experienceYears(tutor.getExperienceYears() != null ? tutor.getExperienceYears().intValue() : 0)
                .submittedAt(tutor.getSubmittedAt())
                .status(status.name())
                .statusLabel(VerificationLabels.status(status))
                .build();
    }

    private VerificationHistoryItemResponse toHistoryItem(TutorVerificationHistory h) {
        return VerificationHistoryItemResponse.builder()
                .action(h.getAction().name())
                .actionLabel(VerificationLabels.historyAction(h))
                .feedback(h.getFeedback())
                .reasonLabel(VerificationLabels.rejectionReason(h.getReasonCode()))
                .internalNote(h.getInternalNote())
                .officerName(h.getOfficer() != null ? h.getOfficer().getFullName() : null)
                .createdAt(h.getCreatedAt())
                .build();
    }

    private static Set<TutorVerificationStatus> parseStatuses(String status) {
        if (status == null || status.isBlank()) {
            return EnumSet.of(TutorVerificationStatus.PENDING);
        }
        String value = status.trim().toUpperCase();
        if ("ALL".equals(value)) {
            return SUBMITTED_STATUSES;
        }
        try {
            TutorVerificationStatus parsed = TutorVerificationStatus.valueOf(value);
            if (parsed == TutorVerificationStatus.UNVERIFIED) {
                throw new IllegalArgumentException();
            }
            return EnumSet.of(parsed);
        } catch (IllegalArgumentException e) {
            throw VerificationException.validation(null, Map.of("status", "Trạng thái lọc không hợp lệ."));
        }
    }

    private static RejectionReason parseReason(String code) {
        if (code == null || code.isBlank()) {
            return null;
        }
        try {
            return RejectionReason.valueOf(code.trim());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
