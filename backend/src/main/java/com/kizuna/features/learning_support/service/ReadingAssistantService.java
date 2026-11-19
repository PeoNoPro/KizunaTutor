package com.kizuna.features.learning_support.service;

import com.kizuna.features.learning_support.dto.ReadingAnalysisRequest;
import com.kizuna.features.learning_support.dto.ReadingAnalysisResponse;

public interface ReadingAssistantService {
    /**
     * Phân tích, bóc tách cấu trúc ngữ pháp, từ vựng và furigana cho đoạn văn bản tiếng Nhật.
     */
    ReadingAnalysisResponse analyzeReading(ReadingAnalysisRequest request);
}
