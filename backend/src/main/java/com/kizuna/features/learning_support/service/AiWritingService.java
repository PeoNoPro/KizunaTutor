package com.kizuna.features.learning_support.service;

import com.kizuna.features.learning_support.dto.AiWritingCheckRequest;
import com.kizuna.features.learning_support.dto.AiWritingCheckResponse;

import java.util.List;
import java.util.Map;

public interface AiWritingService {

    /**
     * Thẩm định và sửa lỗi bài viết/câu văn tiếng Nhật của học viên bằng AI.
     */
    AiWritingCheckResponse checkWriting(AiWritingCheckRequest request);

    /**
     * Lấy danh sách các câu thử thách & mẫu ngữ pháp gợi ý theo cấp độ JLPT.
     */
    List<Map<String, Object>> getGrammarChallengePrompts(String jlptLevel);
}
