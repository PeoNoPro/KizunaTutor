package com.kizuna.features.booking.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RescheduleTrialBookingRequestDTO {

    @NotNull(message = "Thời gian học mới không được để trống")
    private OffsetDateTime newScheduledStartTime;

    private String rescheduleReason;
}

