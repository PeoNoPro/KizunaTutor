package com.kizuna.features.verification.service;

import com.kizuna.features.verification.entity.TutorVerifAction;
import com.kizuna.features.verification.entity.TutorVerificationHistory;

import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Yêu cầu cập nhật hồ sơ của gia sư đã phê duyệt, lưu ngay trong bảng tutor_verification_histories (không có bảng riêng):
 * <ul>
 *   <li>Gia sư gửi yêu cầu = dòng SUBMIT có ghi chú (notes) bắt đầu bằng {@link #MARKER};
 *       ghi chú của gia sư nằm ở feedback, JLPT đề xuất nằm cuối notes ("... N2 → N1").</li>
 *   <li>Quyết định = dòng APPROVE / REJECT đầu tiên sau dòng SUBMIT đó (notes cũng bắt đầu bằng {@link #MARKER};
 *       lý do từ chối nằm ở feedback).</li>
 *   <li>Đang chờ = dòng SUBMIT cập nhật mới nhất chưa có quyết định.</li>
 * </ul>
 * Lần nộp hồ sơ thông thường (notes trống) không phải yêu cầu cập nhật.
 */
public final class ProfileUpdateTimeline {

    public static final String MARKER = "Yêu cầu cập nhật hồ sơ";
    private static final String JLPT_PART = ". Đề xuất đổi trình độ JLPT: ";
    private static final Pattern REQUESTED_JLPT = Pattern.compile("(\\S+) → (N[1-5])$");

    private ProfileUpdateTimeline() {
    }

    /** Một yêu cầu cập nhật và quyết định của cán bộ học vụ (null nếu đang chờ). */
    public record Request(TutorVerificationHistory submit, TutorVerificationHistory decision) {
        public boolean pending() {
            return decision == null;
        }

        /** Trình độ JLPT gia sư đề xuất; null nếu yêu cầu không đổi JLPT. */
        public String requestedJlpt() {
            String notes = submit.getInternalNote();
            if (notes == null) {
                return null;
            }
            Matcher m = REQUESTED_JLPT.matcher(notes);
            return m.find() ? m.group(2) : null;
        }

        /** Trình độ JLPT lúc gửi yêu cầu (để hiển thị "cũ → mới"); null nếu không đổi JLPT. */
        public String previousJlpt() {
            String notes = submit.getInternalNote();
            if (notes == null) {
                return null;
            }
            Matcher m = REQUESTED_JLPT.matcher(notes);
            return m.find() ? m.group(1) : null;
        }
    }

    public static boolean isUpdateRow(TutorVerificationHistory h) {
        return h.getInternalNote() != null && h.getInternalNote().startsWith(MARKER);
    }

    /** Ghi chú nội bộ của dòng SUBMIT cập nhật. */
    public static String submitNotes(String currentJlpt, String requestedJlpt) {
        if (requestedJlpt == null) {
            return MARKER;
        }
        return MARKER + JLPT_PART + (currentJlpt == null || currentJlpt.isBlank() ? "—" : currentJlpt) + " → " + requestedJlpt;
    }

    /** Yêu cầu cập nhật gần nhất trong lịch sử (sắp xếp cũ → mới). */
    public static Optional<Request> latest(List<TutorVerificationHistory> timelineAsc) {
        Request current = null;
        for (TutorVerificationHistory h : timelineAsc) {
            if (h.getAction() == TutorVerifAction.SUBMIT) {
                current = isUpdateRow(h) ? new Request(h, null) : null;
            } else if (current != null && current.pending()
                    && (h.getAction() == TutorVerifAction.APPROVE || h.getAction() == TutorVerifAction.REJECT)) {
                current = new Request(current.submit(), h);
            }
        }
        return Optional.ofNullable(current);
    }

    public static boolean hasPending(List<TutorVerificationHistory> timelineAsc) {
        return latest(timelineAsc).map(Request::pending).orElse(false);
    }
}
