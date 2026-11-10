package com.kizuna.features.learning_support.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * REST API lưu trữ Agent Memory dài hạn cho Sensei AI (/memory/append).
 * Đóng vai trò dịch vụ nội bộ dự phòng trên cổng 8085 khi microservice cổng 8081 chưa được bật riêng.
 */
@Slf4j
@RestController
@RequestMapping("/memory")
@RequiredArgsConstructor
public class AgentMemoryController {

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;
    private volatile boolean tableInitialized = false;

    private void ensureTable() {
        if (tableInitialized) return;
        synchronized (this) {
            if (tableInitialized) return;
            jdbcTemplate.execute("""
                    CREATE TABLE IF NOT EXISTS learner_agent_memories (
                        id BIGSERIAL PRIMARY KEY,
                        user_key VARCHAR(255) NOT NULL,
                        feature VARCHAR(64) NOT NULL,
                        target_char VARCHAR(64),
                        score INTEGER DEFAULT 0,
                        feedback TEXT,
                        context_json TEXT,
                        created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
                    )
                    """);
            tableInitialized = true;
        }
    }

    @PostMapping("/append")
    public ResponseEntity<Map<String, Object>> appendMemory(@RequestBody Map<String, Object> body) {
        ensureTable();
        String userKey = body.get("userId") != null ? String.valueOf(body.get("userId")) : "learner-guest";
        String feature = body.get("feature") != null ? String.valueOf(body.get("feature")) : "shodo";
        String targetChar = body.get("targetChar") != null ? String.valueOf(body.get("targetChar")) : null;
        int score = body.get("score") instanceof Number n ? n.intValue() : 0;
        String feedback = body.get("feedback") != null ? String.valueOf(body.get("feedback")) : "";

        String contextJson = "{}";
        try {
            contextJson = objectMapper.writeValueAsString(body.getOrDefault("context", Map.of()));
        } catch (Exception ignored) {}

        try {
            jdbcTemplate.update(
                    """
                    INSERT INTO learner_agent_memories (user_key, feature, target_char, score, feedback, context_json, created_at)
                    VALUES (?, ?, ?, ?, ?, ?, NOW())
                    """,
                    userKey, feature, targetChar, score, feedback, contextJson
            );
        } catch (Exception ex) {
            log.warn("Không thể ghi learner_agent_memories: {}", ex.getMessage());
        }

        return ResponseEntity.ok(Map.of(
                "status", 200,
                "success", true,
                "message", "Đã ghi nhận tiến độ học tập vào bộ nhớ Sensei AI.",
                "data", body
        ));
    }

    @GetMapping("/recent")
    public ResponseEntity<List<Map<String, Object>>> getRecent(
            @RequestParam(defaultValue = "learner-guest") String userId,
            @RequestParam(defaultValue = "shodo") String feature) {
        ensureTable();
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                """
                SELECT id, user_key AS "userId", feature, target_char AS "targetChar", score, feedback, created_at AS "timestamp"
                FROM learner_agent_memories
                WHERE user_key = ? AND feature = ?
                ORDER BY created_at DESC
                LIMIT 20
                """,
                userId, feature
        );
        return ResponseEntity.ok(rows);
    }
}
