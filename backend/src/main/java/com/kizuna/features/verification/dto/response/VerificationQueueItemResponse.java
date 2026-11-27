package com.kizuna.features.verification.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@Builder
@AllArgsConstructor
public class VerificationQueueItemResponse {
    private UUID tutorId;
    private String fullName;
    private String email;
    private String phone;
    private String highestJlptLevel;
    private Integer experienceYears;
    private OffsetDateTime submittedAt;
    private String status;
    private String statusLabel;
}
