package com.kizuna.features.learning_support.controller;

import com.kizuna.features.learning_support.dto.KanjiCardDto;
import com.kizuna.features.learning_support.entity.KanjiDictionary;
import com.kizuna.features.learning_support.service.KanjiDictionaryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping({"/api/learner/kanji", "/api/kanji"})
@RequiredArgsConstructor
public class KanjiDictionaryController {

    private final KanjiDictionaryService kanjiService;

    @GetMapping("/search")
    public ResponseEntity<Map<String, Object>> searchDictionary(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String jlpt,
            @RequestParam(required = false) Short strokeCount,
            @RequestParam(required = false) Short minStroke,
            @RequestParam(required = false) Short maxStroke,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Page<KanjiCardDto> pagedResult = kanjiService.searchKanji(
                keyword, jlpt, strokeCount, minStroke, maxStroke, PageRequest.of(page, size)
        );

        Map<String, Object> res = new HashMap<>();
        res.put("items", pagedResult.getContent());
        res.put("totalElements", pagedResult.getTotalElements());
        res.put("totalPages", pagedResult.getTotalPages());
        res.put("currentPage", pagedResult.getNumber());

        return ResponseEntity.ok()
                .cacheControl(CacheControl.noCache())
                .body(res);
    }

    @GetMapping("/detail")
    public ResponseEntity<?> getKanjiDetail(
            @RequestParam(name = "char", required = false) String charParam,
            @RequestParam(name = "id", required = false) UUID idParam) {

        if (charParam != null && !charParam.isBlank()) {
            return kanjiService.findByCharacter(charParam)
                    .map(ResponseEntity::ok)
                    .orElse(ResponseEntity.notFound().build());
        }

        if (idParam != null) {
            return kanjiService.findById(idParam)
                    .map(ResponseEntity::ok)
                    .orElse(ResponseEntity.notFound().build());
        }

        return ResponseEntity.badRequest().body(Map.of("message", "Vui lòng cung cấp tham số 'char' hoặc 'id'"));
    }

    @GetMapping("/{character}")
    public ResponseEntity<KanjiDictionary> getKanjiByChar(@PathVariable String character) {
        return kanjiService.findByCharacter(character)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Tra cứu chữ Hán theo danh sách ứng viên nét vẽ hoặc số nét (hỗ trợ cả online Google OHR và fallback offline DB).
     * POST /api/kanji/search-by-strokes & /api/learner/kanji/search-by-strokes
     */
    @PostMapping("/search-by-strokes")
    public ResponseEntity<Map<String, Object>> searchByStrokes(@RequestBody Map<String, Object> body) {
        int strokeCountInt = body.get("strokeCount") instanceof Number n ? n.intValue() : 0;
        int limit = body.get("limit") instanceof Number n ? Math.min(Math.max(n.intValue(), 1), 30) : 15;

        List<String> candidates = new ArrayList<>();
        if (body.get("candidates") instanceof List<?> rawList) {
            for (Object obj : rawList) {
                if (obj != null && !String.valueOf(obj).isBlank()) {
                    candidates.add(String.valueOf(obj).trim());
                }
            }
        }

        List<Map<String, Object>> matches = new ArrayList<>();
        if (!candidates.isEmpty()) {
            int max = Math.min(candidates.size(), limit);
            for (int i = 0; i < max; i++) {
                String ch = candidates.get(i);
                var opt = kanjiService.findByCharacter(ch);
                Map<String, Object> m = new LinkedHashMap<>();
                if (opt.isPresent()) {
                    KanjiDictionary kd = opt.get();
                    m.put("character", kd.getKanjiCharacter());
                    m.put("kanjiChar", kd.getKanjiCharacter());
                    m.put("hanViet", kd.getHanViet() != null ? kd.getHanViet() : "HÁN TỰ");
                    m.put("meaning", kd.getMeaning() != null ? kd.getMeaning() : "Chữ Hán");
                    m.put("totalStrokes", kd.getStrokeCount());
                    m.put("strokeCount", kd.getStrokeCount());
                    m.put("jlpt", kd.getJlptLevel() != null ? kd.getJlptLevel().name() : "N/A");
                    m.put("onyomi", kd.getOnyomi() != null ? kd.getOnyomi() : "-");
                    m.put("kunyomi", kd.getKunyomi() != null ? kd.getKunyomi() : "-");
                    m.put("radical", kd.getRadical() != null ? kd.getRadical() : "");
                } else {
                    m.put("character", ch);
                    m.put("kanjiChar", ch);
                    m.put("hanViet", "HÁN TỰ");
                    m.put("meaning", "Chữ Hán nhận diện");
                    m.put("totalStrokes", strokeCountInt > 0 ? strokeCountInt : 1);
                    m.put("strokeCount", strokeCountInt > 0 ? strokeCountInt : 1);
                    m.put("jlpt", "N/A");
                    m.put("onyomi", "-");
                    m.put("kunyomi", "-");
                    m.put("radical", "");
                }
                m.put("similarityScore", String.format(java.util.Locale.US, "%.2f", Math.max(0.1, 1.0 - i * 0.05)));
                matches.add(m);
            }
        } else if (strokeCountInt > 0) {
            Page<KanjiCardDto> byStroke = kanjiService.searchKanji(
                    null, null, (short) strokeCountInt, null, null, PageRequest.of(0, limit)
            );
            int idx = 0;
            for (KanjiCardDto dto : byStroke.getContent()) {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("character", dto.getKanjiChar());
                m.put("kanjiChar", dto.getKanjiChar());
                m.put("hanViet", dto.getHanViet() != null ? dto.getHanViet() : "HÁN TỰ");
                m.put("meaning", dto.getMeaning() != null ? dto.getMeaning() : "Chữ Hán");
                m.put("totalStrokes", dto.getStrokeCount());
                m.put("strokeCount", dto.getStrokeCount());
                m.put("jlpt", dto.getJlpt() != null ? dto.getJlpt() : "N/A");
                m.put("onyomi", dto.getOnyomi() != null ? dto.getOnyomi() : "-");
                m.put("kunyomi", dto.getKunyomi() != null ? dto.getKunyomi() : "-");
                m.put("radical", dto.getRadical() != null ? dto.getRadical() : "");
                m.put("similarityScore", String.format(java.util.Locale.US, "%.2f", Math.max(0.1, 1.0 - idx * 0.04)));
                matches.add(m);
                idx++;
            }
        }

        return ResponseEntity.ok(Map.of("matches", matches));
    }
}
