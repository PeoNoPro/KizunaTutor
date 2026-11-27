package com.kizuna.features.verification.service;

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
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * Máy trạng thái xác minh gia sư (data-model.md mục 4, GB-01).
 * Mỗi chuyển trạng thái hợp lệ ghi đúng một mục lịch sử; trạng thái nguồn sai thì ném lỗi và không ghi gì.
 * Bên gọi PHẢI truyền Tutor đã khóa dòng (VerificationTutorRepository.findLocked*) trong cùng transaction.
 */
@Component
@RequiredArgsConstructor
public class VerificationStateMachine {

    /**
     * Chỉ hồ sơ chưa nộp hoặc được yêu cầu bổ sung mới chỉnh sửa/nộp được.
     * "Bị từ chối" khóa trong 3 ngày rồi được sửa / nộp lại (FE-16, 2026-11-26; trước đó là quyết định cuối cùng).
     */
    private static final Set<TutorVerificationStatus> EDITABLE = EnumSet.of(
            TutorVerificationStatus.UNVERIFIED,
            TutorVerificationStatus.ADDITIONAL_INFO_REQUIRED);

    /**
     * FE-16 (2026-11-26): hồ sơ bị từ chối được sửa và nộp lại sau 3 ngày kể từ lúc cán bộ học vụ từ chối
     * (specs/016-rejection-resubmit-cooldown, thay quy tắc "từ chối là cuối cùng").
     */
    public static final Duration RESUBMIT_COOLDOWN = Duration.ofDays(3);

    private final TutorVerificationHistoryRepository historyRepository;
    private final TutorCredentialRepository credentialRepository;

    public static TutorVerificationStatus currentStatus(Tutor tutor) {
        return tutor.getVerificationStatus() != null ? tutor.getVerificationStatus() : TutorVerificationStatus.UNVERIFIED;
    }

    /** Theo trạng thái thuần: hồ sơ bị từ chối coi là khóa. Có tính thời gian chờ thì dùng overload bên dưới. */
    public static boolean isEditable(TutorVerificationStatus status) {
        return EDITABLE.contains(status != null ? status : TutorVerificationStatus.UNVERIFIED);
    }

    /**
     * FE-16: như trên, nhưng hồ sơ bị từ chối được sửa khi đã hết thời gian chờ.
     * resubmitAvailableAt rỗng (dữ liệu cũ không có dòng lịch sử từ chối) = được sửa ngay.
     */
    public static boolean isEditable(TutorVerificationStatus status, OffsetDateTime resubmitAvailableAt, OffsetDateTime now) {
        if (status == TutorVerificationStatus.REJECTED) {
            return resubmitAvailableAt == null || !now.isBefore(resubmitAvailableAt);
        }
        return isEditable(status);
    }

    /**
     * FE-16: thời điểm hồ sơ bị từ chối được nộp lại = lần từ chối mới nhất + 3 ngày.
     * Hồ sơ không ở trạng thái bị từ chối, hoặc không tìm thấy lần từ chối, trả null.
     * Hồ sơ bị từ chối không thể có yêu cầu cập nhật (chỉ hồ sơ đã duyệt mới gửi được), nên dòng REJECT mới nhất là lần từ chối hồ sơ.
     */
    public static OffsetDateTime resubmitAvailableAt(Tutor tutor, TutorVerificationHistoryRepository historyRepository) {
        if (currentStatus(tutor) != TutorVerificationStatus.REJECTED || tutor.getTutorId() == null) {
            return null;
        }
        return historyRepository
                .findFirstByTutor_TutorIdAndActionInOrderByCreatedAtDesc(tutor.getTutorId(), EnumSet.of(TutorVerifAction.REJECT))
                .map(TutorVerificationHistory::getCreatedAt)
                .map(rejectedAt -> rejectedAt.plus(RESUBMIT_COOLDOWN))
                .orElse(null);
    }

    /**
     * Ném lỗi phù hợp nếu hồ sơ không được chỉnh sửa/nộp: bị từ chối (cuối cùng) hoặc đang thẩm định/đã duyệt.
     */
    public static void requireEditable(Tutor tutor) {
        TutorVerificationStatus status = currentStatus(tutor);
        if (status == TutorVerificationStatus.REJECTED) {
            throw VerificationException.rejectedFinal();
        }
        if (!isEditable(status)) {
            throw VerificationException.locked();
        }
    }

    /**
     * FE-16: như requireEditable(tutor), nhưng hồ sơ bị từ chối được sửa / nộp khi đã hết thời gian chờ;
     * còn trong thời gian chờ thì báo thời điểm được nộp lại.
     */
    public static void requireEditable(Tutor tutor, OffsetDateTime resubmitAvailableAt, OffsetDateTime now) {
        TutorVerificationStatus status = currentStatus(tutor);
        if (status == TutorVerificationStatus.REJECTED) {
            if (!isEditable(status, resubmitAvailableAt, now)) {
                throw VerificationException.rejectedUntil(resubmitAvailableAt);
            }
            return;
        }
        requireEditable(tutor);
    }

    /**
     * Gia sư nộp hoặc nộp lại hồ sơ: Chưa nộp / Cần bổ sung → Đang chờ thẩm định.
     */
    public TutorVerificationHistory submit(Tutor tutor) {
        requireEditable(tutor);
        return doSubmit(tutor);
    }

    /**
     * FE-16: nộp có tính thời gian chờ: thêm "Bị từ chối (đã hết 3 ngày)" → Đang chờ thẩm định.
     */
    public TutorVerificationHistory submit(Tutor tutor, OffsetDateTime resubmitAvailableAt, OffsetDateTime now) {
        requireEditable(tutor, resubmitAvailableAt, now);
        return doSubmit(tutor);
    }

    private TutorVerificationHistory doSubmit(Tutor tutor) {
        tutor.setVerificationStatus(TutorVerificationStatus.PENDING);
        tutor.setTutorState(TutorState.PENDING_VERIFICATION);
        tutor.setSubmittedAt(OffsetDateTime.now());
        return record(tutor, TutorVerifAction.SUBMIT, null, null, null, null);
    }

    /**
     * Cán bộ yêu cầu bổ sung: Đang chờ thẩm định → Cần bổ sung thông tin.
     */
    public TutorVerificationHistory requestInfo(Tutor tutor, User officer, String message) {
        requirePending(tutor);
        tutor.setVerificationStatus(TutorVerificationStatus.ADDITIONAL_INFO_REQUIRED);
        tutor.setTutorState(TutorState.REJECTED);
        tutor.setVerificationFeedback(message);
        return record(tutor, TutorVerifAction.REQUEST_INFO, officer, message, null, null);
    }

    /**
     * Cán bộ phê duyệt: Đang chờ thẩm định → Đã phê duyệt. Không hạ trạng thái gia sư đã kích hoạt gói hội viên.
     */
    public TutorVerificationHistory approve(Tutor tutor, User officer) {
        requirePending(tutor);
        tutor.setVerificationStatus(TutorVerificationStatus.APPROVED);
        if (tutor.getTutorState() != TutorState.ACTIVE_PAID) {
            tutor.setTutorState(TutorState.APPROVED_UNPAID);
        }
        tutor.setVerifiedAt(OffsetDateTime.now());
        tutor.setVerificationFeedback(null);
        credentialRepository.updateStatusForTutor(tutor.getTutorId(),
                TutorCredentialStatus.PENDING, TutorCredentialStatus.VERIFIED);
        return record(tutor, TutorVerifAction.APPROVE, officer, null, null, null);
    }

    /**
     * Cán bộ từ chối: Đang chờ thẩm định → Bị từ chối. FE-16: gia sư được nộp lại sau RESUBMIT_COOLDOWN.
     */
    public TutorVerificationHistory reject(Tutor tutor, User officer, RejectionReason reason,
                                           String feedback, String internalNote) {
        requirePending(tutor);
        tutor.setVerificationStatus(TutorVerificationStatus.REJECTED);
        tutor.setTutorState(TutorState.REJECTED);
        tutor.setVerificationFeedback(tutorFacingRejection(reason, feedback));
        credentialRepository.updateStatusForTutor(tutor.getTutorId(),
                TutorCredentialStatus.PENDING, TutorCredentialStatus.REJECTED);
        return record(tutor, TutorVerifAction.REJECT, officer, feedback, reason, internalNote);
    }

    // ----- Yêu cầu cập nhật hồ sơ đã phê duyệt (ghi vào lịch sử, xem ProfileUpdateTimeline) -----

    /** Gia sư gửi yêu cầu cập nhật; hồ sơ vẫn "Đã phê duyệt". note: ghi chú của gia sư (feedback). */
    public TutorVerificationHistory submitProfileUpdate(Tutor tutor, String requestedJlpt, String note) {
        return submitProfileUpdate(tutor, requestedJlpt, note, List.of());
    }

    /** Như trên, kèm giấy tờ đã xác minh gia sư đề nghị gỡ (ghi vào notes để cán bộ học vụ thấy). */
    public TutorVerificationHistory submitProfileUpdate(Tutor tutor, String requestedJlpt, String note,
                                                        List<ProfileUpdateTimeline.RemovedDocument> removed) {
        requireApproved(tutor);
        return record(tutor, TutorVerifAction.SUBMIT, null, note, null,
                ProfileUpdateTimeline.submitNotes(tutor.getHighestJlptLevel(), requestedJlpt, removed));
    }

    /** Chấp nhận: áp dụng JLPT đề xuất (nếu có) và xác minh các giấy tờ đang chờ; giấy tờ đề nghị gỡ do service xóa. */
    public TutorVerificationHistory approveProfileUpdate(Tutor tutor, User officer, String requestedJlpt) {
        requireApproved(tutor);
        if (requestedJlpt != null) {
            tutor.setHighestJlptLevel(requestedJlpt);
        }
        credentialRepository.updateStatusForTutor(tutor.getTutorId(),
                TutorCredentialStatus.PENDING, TutorCredentialStatus.VERIFIED);
        return record(tutor, TutorVerifAction.APPROVE, officer, null, null, ProfileUpdateTimeline.MARKER);
    }

    /** Từ chối: giấy tờ đang chờ bị đánh dấu không được chấp nhận, giấy tờ đề nghị gỡ được giữ; hồ sơ vẫn "Đã phê duyệt". */
    public TutorVerificationHistory rejectProfileUpdate(Tutor tutor, User officer, String feedback) {
        requireApproved(tutor);
        credentialRepository.updateStatusForTutor(tutor.getTutorId(),
                TutorCredentialStatus.PENDING, TutorCredentialStatus.REJECTED);
        return record(tutor, TutorVerifAction.REJECT, officer, feedback, null, ProfileUpdateTimeline.MARKER);
    }

    private static void requireApproved(Tutor tutor) {
        if (currentStatus(tutor) != TutorVerificationStatus.APPROVED) {
            throw VerificationException.alreadyProcessed();
        }
    }

    /**
     * Nội dung từ chối hiển thị cho gia sư: nhãn lý do, kèm phản hồi nếu cán bộ có nhập. Không chứa ghi chú nội bộ.
     */
    public static String tutorFacingRejection(RejectionReason reason, String feedback) {
        String label = VerificationLabels.rejectionReason(reason);
        if (feedback == null || feedback.isBlank()) {
            return label;
        }
        return label + ". " + feedback.trim();
    }

    private void requirePending(Tutor tutor) {
        if (currentStatus(tutor) != TutorVerificationStatus.PENDING) {
            throw VerificationException.alreadyProcessed();
        }
    }

    private TutorVerificationHistory record(Tutor tutor, TutorVerifAction action, User officer,
                                            String feedback, RejectionReason reason, String internalNote) {
        TutorVerificationHistory history = TutorVerificationHistory.builder()
                .tutor(tutor)
                .officer(officer)
                .action(action)
                .feedback(blankToNull(feedback))
                .reasonCode(reason)
                .internalNote(blankToNull(internalNote))
                .build();
        return historyRepository.save(history);
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
