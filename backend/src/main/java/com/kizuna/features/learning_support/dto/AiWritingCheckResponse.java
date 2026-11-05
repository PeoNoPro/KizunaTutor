package com.kizuna.features.learning_support.dto;

import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AiWritingCheckResponse {
    private String originalText;
    private String correctedText;
    private int score; // 0 - 100
    private String ratingBadge;
    private String overallComment;

    @Builder.Default
    private List<AiWritingErrorItemDto> errors = new ArrayList<>();

    @Builder.Default
    private List<AiWritingVocabularyDto> suggestedVocabulary = new ArrayList<>();

    @Builder.Default
    private List<String> nativeAlternatives = new ArrayList<>();
}
