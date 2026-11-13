package com.kizuna.features.learning_support.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReadingGrammarItemDto {
    /** Mẫu ngữ pháp chuẩn, ví dụ: 〜ことにしている, 〜ても */
    private String pattern;

    /** Mã định danh ngữ pháp (nếu khớp được với kho Kizuna, ví dụ N3_012) */
    private String grammarCode;

    /** Cấp độ JLPT (N5, N4, N3, N2, N1) */
    private String jlptLevel;

    /** Ý nghĩa tiếng Việt */
    private String meaning;

    /** Cụm từ hoặc mệnh đề được trích xuất từ văn bản gốc */
    private String matchedText;

    /** Giải thích cách dùng ngữ cảnh trong câu */
    private String explanation;
}
