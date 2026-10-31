package com.kizuna.service;

import com.kizuna.dto.grammar.AiGenerateSentenceRequest;
import com.kizuna.dto.grammar.AiGenerateSentenceResponse;
import com.kizuna.dto.grammar.GrammarRuleDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Map;

public interface IGrammarService {

    Map<String, Long> getLevelCounts();

    Page<GrammarRuleDto> getGrammarRules(String level, String keyword, Pageable pageable);

    GrammarRuleDto getGrammarDetail(String id);

    AiGenerateSentenceResponse generateAiSentence(AiGenerateSentenceRequest request);
}
