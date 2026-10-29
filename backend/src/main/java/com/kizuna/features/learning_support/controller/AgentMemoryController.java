package com.kizuna.features.learning_support.controller;


import com.kizuna.core.entity.*;
import com.kizuna.features.learning_support.dto.ChatMemoryRequest;
import com.kizuna.features.learning_support.dto.ChatMemoryResponse;
import com.kizuna.features.learning_support.service.impl.AgentMemoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/ai/memory")
@RequiredArgsConstructor
public class AgentMemoryController {

    private final AgentMemoryService agentMemoryService;

    @PostMapping("/chat")
    public ResponseEntity<Map<String, String>> recordChatMemory(@RequestBody ChatMemoryRequest request) {
        agentMemoryService.saveLearnerContext(request);
        return ResponseEntity.ok(Map.of("message", "Ngữ cảnh hội thoại đã được ghi nhận vào trí nhớ AI thành công."));
    }

    @GetMapping("/context/{learnerId}")
    public ResponseEntity<ChatMemoryResponse> getLearnerContext(@PathVariable String learnerId) {
        ChatMemoryResponse response = agentMemoryService.retrieveContext(learnerId);
        return ResponseEntity.ok(response);
    }
}
