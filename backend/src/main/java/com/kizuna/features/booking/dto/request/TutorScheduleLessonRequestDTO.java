package com.kizuna.features.booking.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * DTO dành cho Gia sư chủ động chốt lịch / lên lịch buổi học cho học viên
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TutorScheduleLessonRequestDTO {

    @NotNull(message = "Vui lòng chọn học viên để xếp lịch")
    private UUID learnerId;

    @NotNull(message = "Thời gian bắt đầu học không được để trống")
    private OffsetDateTime scheduledStartTime;

    @Builder.Default
    private Integer durationMinutes = 60;

    @Builder.Default
    private String learningMode = "ONLINE";

    private String meetingLink;

    private String lessonTitle;

    private String note;
}

