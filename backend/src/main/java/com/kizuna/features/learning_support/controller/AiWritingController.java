package com.kizuna.features.learning_support.controller;

import com.kizuna.features.learning_support.dto.AiWritingCheckRequest;
import com.kizuna.features.learning_support.dto.AiWritingCheckResponse;
import com.kizuna.features.learning_support.service.AiWritingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/learner/ai-writing")
@RequiredArgsConstructor
public class AiWritingController {

    private final AiWritingService aiWritingService;

    @PostMapping("/check")
    public ResponseEntity<AiWritingCheckResponse> checkWriting(
            @Valid @RequestBody AiWritingCheckRequest request) {
        log.info("Nhận yêu cầu kiểm tra bài viết tiếng Nhật bằng AI: text length = {}",
                request.getText() != null ? request.getText().length() : 0);
        AiWritingCheckResponse response = aiWritingService.checkWriting(request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/prompts")
    public ResponseEntity<List<Map<String, Object>>> getPrompts(
            @RequestParam(defaultValue = "N5") String level) {
        List<Map<String, Object>> prompts = aiWritingService.getGrammarChallengePrompts(level);
        return ResponseEntity.ok(prompts);
    }
}
