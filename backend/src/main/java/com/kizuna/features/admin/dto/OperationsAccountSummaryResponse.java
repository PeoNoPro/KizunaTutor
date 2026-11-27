package com.kizuna.features.admin.dto;

import java.time.OffsetDateTime;

public record OperationsAccountSummaryResponse(long totalAccounts, long activeAccounts,
        long tutorAccounts, long activeTutorAccounts, long learnerAccounts, long activeLearnerAccounts,
        long staffAccounts, long activeStaffAccounts, long otherAccounts, OffsetDateTime generatedAt) { }
