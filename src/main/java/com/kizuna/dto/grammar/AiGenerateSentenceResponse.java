package com.kizuna.dto.grammar;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiGenerateSentenceResponse {
    private String pattern;
    private String context;
    private String japaneseSentence;
    private String furiganaHtml;
    private String vietnameseMeaning;
    private String nuanceNote;
    private List<String> kanjiCharacters;
    private List<String> keyVocabularies;
}
