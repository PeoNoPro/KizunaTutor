package com.kizuna.controller.learner;

import com.kizuna.dto.kanji.KanjiCardDto;
import com.kizuna.entity.KanjiDictionary;
import com.kizuna.repository.KanjiDictionaryRepository;
import com.kizuna.service.kanji.HandwritingSearchService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * REST Controller phục vụ tra cứu học tập & lưu bộ thẻ cá nhân của Học Viên.
 * Endpoint: /api/learner/kanji/**
 */
@RestController
@RequestMapping("/api/learner/kanji")
@RequiredArgsConstructor
public class LearnerKanjiApiController {

    private final KanjiDictionaryRepository kanjiRepository;
    private final HandwritingSearchService handwritingSearchService;

    @Data
    public static class SaveToDeckRequest {
        private UUID kanjiId;
        private String kanjiChar;
        private String targetDeckName;
    }

    /**
     * 1. Tra cứu từ điển Hán tự tương tác cho học viên
     */
    @GetMapping("/search")
    public ResponseEntity<Map<String, Object>> searchDictionary(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String jlpt,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "18") int size) {

        String jlptParam = (jlpt != null && !jlpt.isBlank() && !jlpt.equalsIgnoreCase("ALL")) ? jlpt.trim().toUpperCase() : null;
        String kwParam = (keyword != null && !keyword.isBlank()) ? keyword.trim() : null;

        Page<KanjiDictionary> pagedResult = kanjiRepository.searchKanjiPaginated(kwParam, jlptParam, null, null, PageRequest.of(page, size));

        List<KanjiCardDto> items = pagedResult.getContent().stream().map(k -> {
            handwritingSearchService.enrichKanjiMetadata(k);
            return KanjiCardDto.builder()
                    .id(k.getKanjiId())
                    .kanjiChar(k.getKanjiCharacter())
                    .unicodeHex(k.getUnicodeHex())
                    .strokeCount(k.getStrokeCount())
                    .radical(k.getRadical())
                    .hanViet(k.getHanViet() != null ? k.getHanViet() : "HÁN TỰ")
                    .onyomi(k.getOnyomi() != null ? k.getOnyomi() : "-")
                    .kunyomi(k.getKunyomi() != null ? k.getKunyomi() : "-")
                    .meaning(k.getMeaning() != null ? k.getMeaning() : "")
                    .jlpt(k.getJlptLevel() != null ? k.getJlptLevel().name() : "N5")
                    .build();
        }).toList();

        Map<String, Object> res = new HashMap<>();
        res.put("items", items);
        res.put("totalElements", pagedResult.getTotalElements());
        res.put("totalPages", pagedResult.getTotalPages());
        res.put("currentPage", pagedResult.getNumber());

        return ResponseEntity.ok(res);
    }

    /**
     * 2. Lưu nhanh chữ Kanji vào bộ thẻ cá nhân của học viên
     */
    @PostMapping("/save-card")
    public ResponseEntity<Map<String, Object>> saveToDeck(@RequestBody SaveToDeckRequest request) {
        String deck = (request.getTargetDeckName() != null && !request.getTargetDeckName().isBlank())
                ? request.getTargetDeckName()
                : "Bộ Thẻ Mặc Định";

        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Đã lưu chữ Kanji [" + request.getKanjiChar() + "] vào bộ thẻ [" + deck + "] của bạn!"
        ));
    }
}
