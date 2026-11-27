package com.kizuna.features.learning_support.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.io.InputStream;
import java.util.*;

/**
 * REST API cung cấp kho dữ liệu Ngữ pháp OpenJLPT (/api/learner/grammar).
 * Ưu tiên truy xuất bảng grammars trên PostgreSQL nếu có, hoặc fallback nạp từ classpath data/grammars.json.
 */
@Slf4j
@RestController
@RequestMapping("/api/learner/grammar")
@RequiredArgsConstructor
public class GrammarController {

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    private volatile List<Map<String, Object>> cachedFallbackList;

    private List<Map<String, Object>> loadGrammarSource() {
        // 1. Thử truy vấn bảng grammars trong PostgreSQL nếu tồn tại
        try {
            List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                    "SELECT id, grammar_code AS \"grammarCode\", jlpt_level AS \"jlptLevel\", pattern, romaji, meaning, formation, notes, example_sentence AS \"exampleSentence\", example_furigana AS \"exampleFurigana\", example_meaning AS \"exampleMeaning\", extended_examples AS \"extendedExamples\", tags FROM grammars ORDER BY id ASC"
            );
            if (!rows.isEmpty()) {
                return rows;
            }
        } catch (Exception ignored) {
            // Bảng grammars chưa tồn tại trong schema hiện tại -> nạp từ JSON đồng bộ đi kèm
        }

        if (cachedFallbackList == null) {
            synchronized (this) {
                if (cachedFallbackList == null) {
                    try (InputStream is = new ClassPathResource("data/grammars.json").getInputStream()) {
                        cachedFallbackList = objectMapper.readValue(is, new TypeReference<>() {});
                    } catch (Exception ex) {
                        log.error("Không thể nạp classpath data/grammars.json: {}", ex.getMessage(), ex);
                        cachedFallbackList = List.of();
                    }
                }
            }
        }
        return cachedFallbackList;
    }

    @GetMapping
    public ResponseEntity<Map<String, Object>> getGrammars(
            @RequestParam(defaultValue = "ALL") String level,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "1000") int size) {

        List<Map<String, Object>> all = loadGrammarSource();
        String normLevel = level != null ? level.trim().toUpperCase() : "ALL";
        String kw = keyword != null ? keyword.trim().toLowerCase() : "";

        List<Map<String, Object>> filtered = all.stream().filter(item -> {
            if (!"ALL".equals(normLevel) && !normLevel.isBlank()) {
                String itemLvl = String.valueOf(item.getOrDefault("jlptLevel", item.getOrDefault("level", ""))).toUpperCase();
                if (!itemLvl.equals(normLevel)) return false;
            }
            if (!kw.isBlank()) {
                String combined = (
                        item.getOrDefault("pattern", "") + " " +
                        item.getOrDefault("romaji", "") + " " +
                        item.getOrDefault("meaning", "") + " " +
                        item.getOrDefault("grammarCode", "") + " " +
                        item.getOrDefault("tags", "")
                ).toLowerCase();
                return combined.contains(kw);
            }
            return true;
        }).toList();

        int safeSize = Math.max(1, Math.min(size, 2000));
        int safePage = Math.max(0, page);
        int fromIdx = Math.min(safePage * safeSize, filtered.size());
        int toIdx = Math.min(fromIdx + safeSize, filtered.size());

        List<Map<String, Object>> pageItems = filtered.subList(fromIdx, toIdx);
        int totalPages = (int) Math.ceil((double) filtered.size() / safeSize);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("content", pageItems);
        response.put("totalElements", filtered.size());
        response.put("totalPages", totalPages);
        response.put("currentPage", safePage);

        return ResponseEntity.ok()
                .cacheControl(CacheControl.noCache())
                .body(response);
    }

    @GetMapping("/{idOrCode}")
    public ResponseEntity<?> getGrammarDetail(@PathVariable String idOrCode) {
        String key = idOrCode.trim();
        return loadGrammarSource().stream()
                .filter(item -> key.equals(String.valueOf(item.get("grammarCode")))
                        || key.equals(String.valueOf(item.get("id"))))
                .findFirst()
                .<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
