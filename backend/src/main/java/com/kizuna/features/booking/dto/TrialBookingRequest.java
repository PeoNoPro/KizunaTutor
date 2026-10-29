package com.kizuna.features.booking.dto;


import com.kizuna.core.entity.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TrialBookingRequest {
    private String id;
}
