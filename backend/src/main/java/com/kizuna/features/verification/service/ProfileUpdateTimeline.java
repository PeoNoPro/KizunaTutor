package com.kizuna.features.verification.service;

import com.kizuna.features.verification.entity.TutorVerifAction;
import com.kizuna.features.verification.entity.TutorVerificationHistory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Yêu cầu cập nhật hồ sơ của gia sư đã phê duyệt, lưu ngay trong bảng tutor_verification_histories (không có bảng riêng):
 * <ul>
 *   <li>Gia sư gửi yêu cầu = dòng SUBMIT có ghi chú (notes) bắt đầu bằng {@link #MARKER};
 *       ghi chú của gia sư nằm ở feedback, JLPT đề xuất nằm cuối notes ("... N2 → N1").
 *       Giấy tờ đã xác minh gia sư đề nghị gỡ nằm giữa: ". Đề nghị gỡ giấy tờ: Tên A; Tên B {mã A,mã B}" —
 *       giấy tờ chỉ bị xóa khi cán bộ chấp nhận yêu cầu.</li>
 *   <li>Quyết định = dòng APPROVE / REJECT đầu tiên sau dòng SUBMIT đó (notes cũng bắt đầu bằng {@link #MARKER};
 *       lý do từ chối nằm ở feedback).</li>
 *   <li>Đang chờ = dòng SUBMIT cập nhật mới nhất chưa có quyết định.</li>
 * </ul>
 * Lần nộp hồ sơ thông thường (notes trống) không phải yêu cầu cập nhật.
 */
public final class ProfileUpdateTimeline {

    public static final String MARKER = "Yêu cầu cập nhật hồ sơ";
    private static final String JLPT_PART = ". Đề xuất đổi trình độ JLPT: ";
    private static final String REMOVAL_PART = ". Đề nghị gỡ giấy tờ: ";
    private static final Pattern REQUESTED_JLPT = Pattern.compile("(\\S+) → (N[1-5])$");
    private static final Pattern REMOVAL = Pattern.compile(Pattern.quote(REMOVAL_PART) + "(.*?) \\{([0-9a-fA-F,-]+)\\}");

    /** Giấy tờ gia sư đề nghị gỡ (mã + tên lúc gửi yêu cầu). */
    public record RemovedDocument(UUID id, String title) {
    }

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

        /** Giấy tờ đã xác minh gia sư đề nghị gỡ trong yêu cầu này (rỗng nếu không có). */
        public List<RemovedDocument> removedDocuments() {
            return parseRemoved(submit.getInternalNote());
        }

        public List<UUID> removedDocumentIds() {
            return removedDocuments().stream().map(RemovedDocument::id).toList();
        }
    }

    public static boolean isUpdateRow(TutorVerificationHistory h) {
        return h.getInternalNote() != null && h.getInternalNote().startsWith(MARKER);
    }

    /** Ghi chú nội bộ của dòng SUBMIT cập nhật. */
    public static String submitNotes(String currentJlpt, String requestedJlpt) {
        return submitNotes(currentJlpt, requestedJlpt, List.of());
    }

    /** Ghi chú nội bộ của dòng SUBMIT cập nhật, kèm giấy tờ đề nghị gỡ (nếu có). */
    public static String submitNotes(String currentJlpt, String requestedJlpt, Collection<RemovedDocument> removed) {
        StringBuilder notes = new StringBuilder(MARKER);
        if (removed != null && !removed.isEmpty()) {
            notes.append(REMOVAL_PART)
                    .append(String.join("; ", removed.stream().map(d -> cleanTitle(d.title())).toList()))
                    .append(" {")
                    .append(String.join(",", removed.stream().map(d -> d.id().toString()).toList()))
                    .append('}');
        }
        if (requestedJlpt != null) {
            notes.append(JLPT_PART).append(currentJlpt == null || currentJlpt.isBlank() ? "—" : currentJlpt)
                    .append(" → ").append(requestedJlpt);
        }
        return notes.toString();
    }

    static List<RemovedDocument> parseRemoved(String notes) {
        if (notes == null) {
            return List.of();
        }
        Matcher m = REMOVAL.matcher(notes);
        if (!m.find()) {
            return List.of();
        }
        List<String> titles = Arrays.asList(m.group(1).split("; ", -1));
        String[] ids = m.group(2).split(",");
        List<RemovedDocument> result = new ArrayList<>();
        for (int i = 0; i < ids.length; i++) {
            try {
                result.add(new RemovedDocument(UUID.fromString(ids[i].trim()),
                        i < titles.size() && ids.length == titles.size() ? titles.get(i) : null));
            } catch (IllegalArgumentException ignored) {
                // mã hỏng: bỏ qua dòng đó
            }
        }
        return result;
    }

    /** Tên giấy tờ không được chứa ký tự dùng để tách trong notes. */
    private static String cleanTitle(String title) {
        String clean = title == null ? "" : title.replaceAll("[;{}\\r\\n]+", " ").replaceAll("\\s+", " ").trim();
        return clean.isEmpty() ? "Giấy tờ" : clean;
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
