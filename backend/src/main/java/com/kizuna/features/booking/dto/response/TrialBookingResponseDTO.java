package com.kizuna.features.booking.dto.response;

import lombok.*;

import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TrialBookingResponseDTO {

    private UUID bookingId;
    private UUID requestId;

    // Tương thích hợp đồng API
    private UUID tutorId;
    private String tutorName;

    // Thông tin đối tác (Nếu người xem là Học viên -> đối tác là Gia sư; Nếu người xem là Gia sư -> đối tác là Học viên)
    private UUID partnerId;
    private String partnerName;
    private String partnerAvatarUrl;
    private String partnerRole;

    private OffsetDateTime scheduledStartTime;
    private OffsetDateTime scheduledEndTime;
    private Integer durationMinutes;

    private String learningMode;
    private String meetingLink;
    private String locationDetails;

    private String currentLevel;
    private String learningGoals;
    private String initialMessage;

    private String bookingStatus;
    private String statusLabel; // Nhãn hiển thị tiếng Việt tự nhiên: "Chờ gia sư xác nhận", "Đã xác nhận", "Đã từ chối", "Đã hủy"

    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}

