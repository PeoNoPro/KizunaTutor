package com.kizuna.features.learning_support.dto;

import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReadingAnalysisResponse {
    /** Văn bản gốc người dùng nhập */
    private String originalText;

    /** Văn bản đã gắn thẻ Ruby HTML (<ruby>漢字<rt>かんじ</rt></ruby>) */
    private String rubyHtml;

    /** Tóm tắt / Ý chính của đoạn văn bằng tiếng Việt */
    private String overallSummary;

    /** Cấp độ ước tính của toàn bài (N5 - N1) */
    private String estimatedLevel;

    /** Danh sách các cấu trúc ngữ pháp được bóc tách */
    @Builder.Default
    private List<ReadingGrammarItemDto> detectedGrammars = new ArrayList<>();

    /** Danh sách các từ vựng trọng tâm */
    @Builder.Default
    private List<ReadingVocabularyItemDto> vocabularies = new ArrayList<>();

    /** Các câu dịch tham khảo từng câu */
    @Builder.Default
    private List<String> sentenceTranslations = new ArrayList<>();
}
