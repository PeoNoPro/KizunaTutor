package com.kizuna.features.tutor_post.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/** Dữ liệu vào/ra của chức năng bài đăng gia sư. */
public final class TutorPostDtos {

    private TutorPostDtos() {
    }

    /**
     * Gia sư gửi (hoặc sửa) bài đăng. teachingFormat: ONLINE | OFFLINE | BOTH.
     * prices: học phí riêng cho từng môn học / cấp độ (level là mã trong danh sách priceLevels).
     */
    public record SubmitPostRequest(String title, String description, String specialties,
                                    String teachingFormat, List<PriceItemRequest> prices) {
    }

    /** Một dòng học phí gia sư nhập: môn học / cấp độ, thời lượng một buổi (60 / 90 / 120 phút), giá một buổi. */
    public record PriceItemRequest(String level, Integer sessionMinutes, BigDecimal pricePerSession) {
    }

    /**
     * Một mức học phí kèm nhãn tiếng Việt của môn học / cấp độ.
     * hourlyRate = giá quy ra một giờ (tự tính); sessionMinutes / pricePerSession null với dữ liệu rất cũ.
     */
    public record PriceItemView(String level, String levelLabel, BigDecimal hourlyRate,
                                Integer sessionMinutes, BigDecimal pricePerSession) {
    }

    /** Một lựa chọn môn học / cấp độ cho ô chọn trên giao diện. */
    public record PriceLevelOption(String code, String label) {
    }

    /** Cán bộ học vụ từ chối bài đăng. */
    public record RejectPostRequest(String reason) {
    }

    /**
     * Nội dung một phiên bản bài đăng. hourlyRate = học phí thấp nhất (hiển thị "Từ ...");
     * prices = bảng học phí theo môn học / cấp độ (rỗng với bài cũ chỉ có một mức giá).
     */
    public record PostContent(String title, String description, String specialties,
                              String teachingFormat, String teachingFormatLabel, BigDecimal hourlyRate,
                              List<PriceItemView> prices) {
    }

    /**
     * Trang "Bài đăng của tôi".
     * canPost = gói hội viên còn hạn và hồ sơ đã được duyệt; post = null khi chưa từng đăng.
     */
    /**
     * editable = được soạn và gửi: chưa có bài, cán bộ học vụ yêu cầu bổ sung, sửa bài đã duyệt,
     * hoặc bài đầu tiên bị từ chối đã quá 7 ngày (và gói còn hạn).
     * resubmitAvailableAt (FE-16) = thời điểm được gửi bài mới khi bài đầu tiên bị từ chối; null ở các trường hợp khác.
     */
    public record MyPostView(boolean canPost, String blockedReason, boolean editable, OffsetDateTime resubmitAvailableAt,
                             LocalDate membershipExpiresOn,
                             PostView post, List<PriceLevelOption> priceLevels, List<Integer> sessionMinutesOptions) {
    }

    public record PostView(UUID postId, String reviewStatus, String reviewStatusLabel, String reviewFeedback,
                           OffsetDateTime submittedAt, OffsetDateTime reviewedAt, OffsetDateTime publishedAt,
                           PostContent draft, PostContent published) {
    }

    /** Một dòng trong danh sách duyệt của cán bộ học vụ (kèm cả bản chờ duyệt và bản đang hiển thị để so sánh). */
    public record ReviewItemView(UUID postId, UUID tutorId, String tutorName, String tutorEmail, String highestJlptLevel,
                                 String reviewStatus, String reviewStatusLabel, String reviewFeedback,
                                 OffsetDateTime submittedAt, OffsetDateTime reviewedAt, String reviewerName,
                                 boolean membershipActive, PostContent draft, PostContent published) {
    }

    public record ReviewSummaryView(long pendingCount, long needsInfoCount, long approvedCount, long rejectedCount) {
    }
}
