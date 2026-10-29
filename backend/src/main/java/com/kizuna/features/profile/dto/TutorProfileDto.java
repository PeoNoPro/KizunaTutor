package com.kizuna.features.profile.dto;


import com.kizuna.core.entity.*;
import com.kizuna.core.entity.TutorState;
import com.kizuna.features.verification.entity.TutorVerificationStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * DTO phản hồi thông tin chi tiết hồ sơ gia sư.
 * Phục vụ UC 9-16 (Onboarding và Thẩm định hồ sơ gia sư).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TutorProfileDto {
    private UUID tutorId;
    private UUID userId;
    private String fullName;
    private String email;
    private String phone;
    private LocalDate dateOfBirth;
    private String address;
    private String headline;
    private String bio;
    private String highestJlptLevel;
    private Short experienceYears;
    private BigDecimal hourlyRate;
    private String certificateUrls;
    private TutorState tutorState;
    private TutorVerificationStatus verificationStatus;
    private String verificationFeedback;
    private OffsetDateTime submittedAt;
    private OffsetDateTime verifiedAt;
}
