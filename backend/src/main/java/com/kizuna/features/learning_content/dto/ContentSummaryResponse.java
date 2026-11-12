package com.kizuna.features.learning_content.dto;

import java.util.List;

public record ContentSummaryResponse(List<ContentTypeSummaryResponse> types, long maxFileSizeBytes) {
}
