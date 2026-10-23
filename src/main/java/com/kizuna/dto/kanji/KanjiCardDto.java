package com.kizuna.dto.kanji;

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
    private String unicodeHex;
    private String kanjiChar;
    private Short strokeCount;
    private String radical;
    private String hanViet;
    private String onyomi;
    private String kunyomi;
    private String meaning;
    private String jlpt;
}
