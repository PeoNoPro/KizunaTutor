package com.kizuna.features.auth.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginResponse {
    private String token;
    private UserSummaryDto user;
    private String redirectUrl;
    private String message;
    private boolean mustChangePassword;
}
