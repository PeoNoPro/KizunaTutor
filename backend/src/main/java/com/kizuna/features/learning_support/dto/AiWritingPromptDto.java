package com.kizuna.features.learning_support.dto;

import lombok.*;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AiWritingPromptDto {
    private String id;
    private String level;
    private String title;
    private String grammarPattern;
    private String promptHint;
    private String meaning;
    private List<String> sampleKeywords;
}