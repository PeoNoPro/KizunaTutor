package com.kizuna.features.verification.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class DecisionResultResponse {
    private String message;
    private String status;
    private String statusLabel;
}
