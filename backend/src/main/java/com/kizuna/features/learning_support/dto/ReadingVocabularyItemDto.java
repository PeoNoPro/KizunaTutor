package com.kizuna.features.learning_support.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReadingVocabularyItemDto {
    /** Từ vựng chữ Hán hoặc từ nguyên bản (ví dụ: 毎朝, 散歩) */
    private String kanji;

    /** Cách đọc Kana/Hiragana (ví dụ: まいあさ, さんぽ) */
    private String reading;

    /** Âm Hán Việt (ví dụ: MỖI TRIỀU, TẢN BỘ) */
    private String hanViet;

    /** Nghĩa tiếng Việt */
    private String meaning;

    /** Cấp độ JLPT (N5, N4, N3, N2, N1) */
    private String jlpt;

    /** Từ loại (Danh từ, Động từ nhóm 1, Tính từ -i, ...) */
    private String wordType;
}
