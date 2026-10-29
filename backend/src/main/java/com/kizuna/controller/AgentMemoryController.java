package com.kizuna.controller;

import com.kizuna.dto.ai.ChatMemoryRequest;
import com.kizuna.dto.ai.ChatMemoryResponse;
import com.kizuna.service.ai.AgentMemoryService;
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
