package com.kizuna.dto.topic;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TopicVocabularyDto {
    private UUID id;
    private UUID topicId;
    private String word;
    private String furigana;
    private String romaji;
    private String hanViet;
    private String meaningVn;
    private String jlptLevel;
    private String exampleJp;
    private String exampleVn;
    private List<String> kanjiCharacters;
}
