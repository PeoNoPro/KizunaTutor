package com.kizuna.features.learning_support.dto;

import lombok.*;

import java.util.ArrayList;
import java.util.List;

/**
 * DTO đại diện cho câu hỏi Đọc hiểu (Dokkai) sinh bởi AI kèm các phương án trắc nghiệm,
 * đáp án đúng, dẫn chứng trích dẫn trong bài và giải thích phân tích bẫy.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReadingComprehensionQuestionDto {
    /** Định danh câu hỏi (ví dụ: q_0, q_1) */
    private String id;

    /** Loại câu hỏi: MAIN_IDEA (Ý chính), REASON (Lý do), DETAIL (Chi tiết), REFERENCE (Từ chỉ định) */
    private String questionType;

    /** Nội dung câu hỏi (tiếng Nhật, có thể kèm tiếng Việt với N5/N4) */
    private String question;

    /** Danh sách 4 phương án trắc nghiệm */
    @Builder.Default
    private List<String> options = new ArrayList<>();

    /** Vị trí phương án đúng (0, 1, 2, 3) */
    private Integer correctOptionIndex;

    /** Trích dẫn câu văn làm bằng chứng trực tiếp trong bài đọc */
    private String evidenceQuote;

    /** Lời giải thích phân tích bẫy và lý do đáp án đúng bằng tiếng Việt */
    private String explanation;
}
