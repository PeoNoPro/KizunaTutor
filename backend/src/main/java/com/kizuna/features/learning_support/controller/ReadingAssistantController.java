package com.kizuna.features.learning_support.controller;

import com.kizuna.features.learning_support.dto.ReadingAnalysisRequest;
import com.kizuna.features.learning_support.dto.ReadingAnalysisResponse;
import com.kizuna.features.learning_support.service.ReadingAssistantService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping({"/api/learner/reading", "/api/reading"})
@RequiredArgsConstructor
public class ReadingAssistantController {

    private final ReadingAssistantService readingService;

    /**
     * POST /api/learner/reading/analyze & /api/reading/analyze
     * Bóc tách đoạn văn bản tiếng Nhật: Furigana, cấu trúc ngữ pháp, từ vựng và tóm tắt ý chính.
     */
    @PostMapping("/analyze")
    public ResponseEntity<ReadingAnalysisResponse> analyzeReading(@Valid @RequestBody ReadingAnalysisRequest request) {
        log.info("Yêu cầu bóc tách đọc hiểu: text length={}, targetJlpt={}",
                request.getText() != null ? request.getText().length() : 0,
                request.getTargetJlpt());

        ReadingAnalysisResponse response = readingService.analyzeReading(request);
        return ResponseEntity.ok(response);
    }
}
