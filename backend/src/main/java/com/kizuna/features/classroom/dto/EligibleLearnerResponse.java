package com.kizuna.features.classroom.dto;

import java.util.UUID;

public record EligibleLearnerResponse(UUID learnerId, String fullName, String email, String avatarUrl) {
}
