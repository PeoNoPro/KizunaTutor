package com.kizuna.features.learning_support.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AiWritingVocabularyDto {
    private String word;      // Từ vựng Kanji/Kana
    private String reading;   // Cách đọc Hiragana
    private String meaning;   // Nghĩa tiếng Việt
}
