package com.kizuna.features.learning_support.service.impl;


import com.kizuna.core.entity.*;
import com.kizuna.features.learning_support.dto.ChatMemoryRequest;
import com.kizuna.features.learning_support.dto.ChatMemoryResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
@Slf4j
public class AgentMemoryService {

    private final RestClient restClient;

    public AgentMemoryService(@Value("${app.agent-memory.base-url:http://localhost:8081}") String baseUrl) {
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .build();
    }

    /**
     * Lưu ngữ cảnh trao đổi bài học của học viên vào Agent Memory Hub
     */
    public void saveLearnerContext(ChatMemoryRequest request) {
        try {
            this.restClient.post()
                    .uri("/api/v1/memory/chat")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .toBodilessEntity();
            log.info("[AgentMemory] Đã lưu ngữ cảnh hội thoại cho học viên: {}", request.getLearnerId());
        } catch (Exception e) {
            log.warn("[AgentMemory] Không thể kết nối với Agent Memory container (đang hoạt động ở chế độ fallback): {}", e.getMessage());
        }
    }

    /**
     * Truy xuất các ký ức / ngữ cảnh học tập liên quan của học viên
     */
    public ChatMemoryResponse retrieveContext(String learnerId) {
        try {
            return this.restClient.get()
                    .uri("/api/v1/memory/chat/{learnerId}", learnerId)
                    .retrieve()
                    .body(ChatMemoryResponse.class);
        } catch (Exception e) {
            log.warn("[AgentMemory] Không thể lấy trí nhớ hội thoại cho học viên {}: {}", learnerId, e.getMessage());
            return ChatMemoryResponse.builder()
                    .status("FALLBACK")
                    .summary("Agent Memory service chưa khởi chạy hoặc đang offline.")
                    .build();
        }
    }
}
