package com.kizuna.features.learning_support.controller;

import com.kizuna.features.learning_support.dto.KanjiCardDto;
import com.kizuna.features.learning_support.entity.KanjiDictionary;
import com.kizuna.features.learning_support.service.KanjiDictionaryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
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

        return ResponseEntity.ok(res);
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
}
