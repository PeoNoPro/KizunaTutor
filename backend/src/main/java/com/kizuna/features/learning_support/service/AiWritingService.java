package com.kizuna.features.learning_support.service;

import com.kizuna.features.learning_support.dto.AiWritingCheckRequest;
import com.kizuna.features.learning_support.dto.AiWritingCheckResponse;
import com.kizuna.features.learning_support.dto.AiWritingPromptDto;

import java.util.List;

public interface AiWritingService {
    AiWritingCheckResponse checkWriting(AiWritingCheckRequest request);
    List<AiWritingPromptDto> getGrammarChallengePrompts(String level);
}