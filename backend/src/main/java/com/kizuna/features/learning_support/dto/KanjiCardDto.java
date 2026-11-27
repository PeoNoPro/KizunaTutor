package com.kizuna.features.learning_support.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KanjiCardDto {
    private UUID id;
    private String kanjiChar;
    private String unicodeHex;
    private Short strokeCount;
    private String radical;
    private String hanViet;
    private String onyomi;
    private String kunyomi;
    private String meaning;
    private String jlpt;
}
