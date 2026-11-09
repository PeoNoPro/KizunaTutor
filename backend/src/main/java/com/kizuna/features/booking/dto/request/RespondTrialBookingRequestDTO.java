package com.kizuna.features.booking.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RespondTrialBookingRequestDTO {

    @NotBlank(message = "Hành động xử lý (ACCEPT hoặc DECLINE) không được để trống")
    private String action;

    private String meetingLink;

    private String declineReason;

    private String note;
}

