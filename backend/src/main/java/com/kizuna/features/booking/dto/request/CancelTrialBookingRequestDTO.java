package com.kizuna.features.booking.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CancelTrialBookingRequestDTO {

    @NotBlank(message = "Lý do hủy buổi học không được để trống")
    private String cancelReason;
}

