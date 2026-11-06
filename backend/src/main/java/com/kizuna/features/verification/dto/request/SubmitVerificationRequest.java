package com.kizuna.features.verification.dto.request;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Nộp hồ sơ xác minh (UC-24). commitment phải là true (FR-006).
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SubmitVerificationRequest {
    private Boolean commitment;
}
