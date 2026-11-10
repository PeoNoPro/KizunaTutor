package com.kizuna.features.booking.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateTrialBookingRequestDTO {

    @NotNull(message = "Mã gia sư không được để trống")
    private UUID tutorId;

    @NotNull(message = "Thời gian bắt đầu học không được để trống")
    private OffsetDateTime scheduledStartTime;

    @Builder.Default
    private Integer durationMinutes = 30;

    @Builder.Default
    private String learningMode = "ONLINE";

    private String locationDetails;

    private String currentLevel;

    private String learningGoals;

    private String initialMessage;
}

