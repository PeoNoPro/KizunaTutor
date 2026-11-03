package com.kizuna.features.auth.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PendingRegistration {
    private String email;
    private String fullName;
    private String passwordHash;
    private String role;
    private String targetJlpt;
    private String otp;
    private Instant expiresAt;
    private int attempts;
    private Instant lastSentAt;
}
