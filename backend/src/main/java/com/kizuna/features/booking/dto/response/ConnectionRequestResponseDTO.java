package com.kizuna.features.booking.dto.response;

import lombok.*;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/** Yêu cầu kết nối trực tiếp, nhìn từ phía người đang xem (học viên hoặc gia sư). */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConnectionRequestResponseDTO {

    private UUID requestId;

    private UUID tutorId;
    private String tutorName;
    private UUID learnerId;

    // Đối tác: học viên xem thì là gia sư, gia sư xem thì là học viên
    private UUID partnerId;
    private String partnerName;
    private String partnerAvatarUrl;
    private String partnerRole;

    private List<SlotView> slots;
    /** Ngày bắt đầu học các ca hằng tuần (có thể null với yêu cầu cũ). */
    private java.time.LocalDate startDate;

    private String currentLevel;
    private String learningGoals;
    private String initialMessage;
    private String responseMessage;

    private String status;
    /** Nhãn tiếng Việt: "Chờ gia sư xác nhận", "Đã kết nối", "Đã từ chối", "Đã hủy"... */
    private String statusLabel;

    private OffsetDateTime createdAt;
    private OffsetDateTime respondedAt;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class SlotView {
        /** 1 = Chủ nhật, 2 = Thứ Hai ... 7 = Thứ Bảy */
        private Integer dayOfWeek;
        private String dayName;
        /** "HH:mm" */
        private String startTime;
        private String endTime;
        private Integer durationMinutes;
    }
}
