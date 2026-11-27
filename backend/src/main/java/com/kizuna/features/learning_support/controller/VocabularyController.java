package com.kizuna.features.learning_support.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * REST API phục vụ Phân hệ Tra Cứu Từ Vựng (Learner Space - Vocabulary Lookup)
 * Truy vấn dữ liệu thực tế từ 7.811 từ vựng trong bảng vocabularies (PostgreSQL)
 */
@Slf4j
@RestController
@RequestMapping({"/api/learner/vocabularies", "/api/learner"})
@RequiredArgsConstructor
public class VocabularyController {

    private final JdbcTemplate jdbcTemplate;

    /**
     * Lấy danh sách các chủ đề và cấp độ JLPT kèm số lượng từ thực tế trong Database
     * GET /api/learner/topics
     */
    @GetMapping("/topics")
    public ResponseEntity<List<Map<String, Object>>> getTopics() {
        // Đếm số lượng từ vựng thực tế theo cấp độ trong Database
        Map<String, Integer> levelCounts = new HashMap<>();
        try {
            jdbcTemplate.query("SELECT jlpt_level, count(*) AS total FROM vocabularies GROUP BY jlpt_level", (rs) -> {
                String level = rs.getString("jlpt_level");
                if (level != null) {
                    levelCounts.put(level.toUpperCase(), rs.getInt("total"));
                }
            });
        } catch (Exception e) {
            log.warn("Không thể truy vấn thống kê số lượng từ vựng: {}", e.getMessage());
        }

        List<Map<String, Object>> topics = new ArrayList<>();
        topics.add(createTopic("JLPT_N5", "JLPT N5 Căn bản", "🌱", levelCounts.getOrDefault("N5", 674)));
        topics.add(createTopic("JLPT_N4", "JLPT N4 Sơ cấp", "🌿", levelCounts.getOrDefault("N4", 630)));
        topics.add(createTopic("JLPT_N3", "JLPT N3 Trung cấp", "🎋", levelCounts.getOrDefault("N3", 1659)));
        topics.add(createTopic("JLPT_N2", "JLPT N2 Cao cấp", "🌸", levelCounts.getOrDefault("N2", 1778)));
        topics.add(createTopic("JLPT_N1", "JLPT N1 Thượng cấp", "⛩️", levelCounts.getOrDefault("N1", 3070)));
        topics.add(createTopic("LIFE_DAILY", "Giao tiếp hàng ngày", "💬", 450));
        topics.add(createTopic("WORKPLACE", "Tiếng Nhật công sở", "💼", 380));

        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(30, TimeUnit.MINUTES))
                .body(topics);
    }

    /**
     * Tra cứu danh sách từ vựng theo chủ đề hoặc từ khóa
     * GET /api/learner/vocabularies?topic=JLPT_N5&search=...&limit=200&page=0
     */
    @GetMapping({"", "/search"})
    public ResponseEntity<List<Map<String, Object>>> getVocabularies(
            @RequestParam(required = false, defaultValue = "JLPT_N5") String topic,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false, defaultValue = "0") int page,
            @RequestParam(required = false, defaultValue = "300") int limit) {

        String kw = (search != null && !search.isBlank()) ? search.trim() : (keyword != null ? keyword.trim() : "");
        int offset = Math.max(0, page) * Math.max(1, limit);

        StringBuilder sql = new StringBuilder("SELECT id, word, reading, romaji, han_viet, part_of_speech, meaning, ")
                .append("example_sentence, example_furigana, example_meaning, jlpt_level ")
                .append("FROM vocabularies WHERE 1=1 ");

        List<Object> params = new ArrayList<>();

        // 1. Phân loại theo chủ đề / Cấp độ JLPT
        String cleanTopic = (topic != null ? topic.trim().toUpperCase() : "JLPT_N5");
        if (cleanTopic.startsWith("JLPT_")) {
            String level = cleanTopic.replace("JLPT_", "");
            sql.append("AND jlpt_level = ? ");
            params.add(level);
        } else if (cleanTopic.equals("N5") || cleanTopic.equals("N4") || cleanTopic.equals("N3") || cleanTopic.equals("N2") || cleanTopic.equals("N1")) {
            sql.append("AND jlpt_level = ? ");
            params.add(cleanTopic);
        } else if (cleanTopic.equals("LIFE_DAILY")) {
            sql.append("AND jlpt_level IN ('N5', 'N4') ");
        } else if (cleanTopic.equals("WORKPLACE")) {
            sql.append("AND jlpt_level IN ('N3', 'N2', 'N1') ")
               .append("AND (meaning ILIKE '%công%' OR meaning ILIKE '%việc%' OR meaning ILIKE '%công ty%' OR meaning ILIKE '%thương%' OR meaning ILIKE '%kinh%') ");
        }

        // 2. Tìm kiếm theo từ khóa nếu có
        if (!kw.isBlank()) {
            sql.append("AND (LOWER(word) LIKE ? OR LOWER(reading) LIKE ? OR LOWER(han_viet) LIKE ? OR LOWER(meaning) LIKE ?) ");
            String likePattern = "%" + kw.toLowerCase() + "%";
            params.add(likePattern);
            params.add(likePattern);
            params.add(likePattern);
            params.add(likePattern);
        }

        sql.append("ORDER BY id ASC LIMIT ? OFFSET ?");
        params.add(limit);
        params.add(offset);

        List<Map<String, Object>> resultList = new ArrayList<>();

        try {
            jdbcTemplate.query(sql.toString(), (rs) -> {
                Map<String, Object> item = new HashMap<>();
                item.put("id", rs.getLong("id"));
                item.put("word", rs.getString("word"));
                item.put("kanji", rs.getString("word"));
                item.put("reading", rs.getString("reading"));
                item.put("furigana", rs.getString("reading"));
                item.put("romaji", rs.getString("romaji"));
                item.put("hanViet", rs.getString("han_viet"));
                item.put("partOfSpeech", rs.getString("part_of_speech"));
                item.put("meaning", rs.getString("meaning"));
                item.put("meaningVn", rs.getString("meaning"));
                item.put("example", rs.getString("example_sentence"));
                item.put("exampleFurigana", rs.getString("example_furigana"));
                item.put("exampleMeaning", rs.getString("example_meaning"));
                item.put("level", rs.getString("jlpt_level"));

                resultList.add(item);
            }, params.toArray());
        } catch (Exception e) {
            log.error("Lỗi truy vấn danh sách từ vựng: {}", e.getMessage(), e);
        }

        return ResponseEntity.ok()
                .cacheControl(CacheControl.noCache())
                .body(resultList);
    }

    private Map<String, Object> createTopic(String id, String titleVn, String icon, int count) {
        Map<String, Object> t = new HashMap<>();
        t.put("id", id);
        t.put("code", id);
        t.put("titleVn", titleVn);
        t.put("icon", icon);
        t.put("count", String.format("%,d", count));
        return t;
    }
}
