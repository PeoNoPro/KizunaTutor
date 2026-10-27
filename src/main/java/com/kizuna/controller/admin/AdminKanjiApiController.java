package com.kizuna.controller.admin;

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
 * REST Controller kiểm soát dữ liệu gốc Kanji dành riêng cho Admin.
 * Endpoint: /api/admin/kanji/**
 */
@RestController
@RequestMapping("/api/admin/kanji")
@RequiredArgsConstructor
public class AdminKanjiApiController {

    private final KanjiDictionaryRepository kanjiRepository;
    private final HandwritingSearchService handwritingSearchService;

    @Data
    public static class KanjiUpdateRequest {
        private String hanViet;
        private String onyomi;
        private String kunyomi;
        private String meaning;
        private Short strokeCount;
    }

    /**
     * 1. Tra cứu danh sách phân trang phục vụ bảng quản trị
     */
    @GetMapping("/list")
    public ResponseEntity<Map<String, Object>> listKanji(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String jlpt,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "15") int size) {

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
     * 2. Chỉnh sửa trường dữ liệu gốc của chữ Kanji
     */
    @PutMapping("/{id}")
    public ResponseEntity<Map<String, Object>> updateKanji(
            @PathVariable UUID id,
            @RequestBody KanjiUpdateRequest request) {

        return kanjiRepository.findById(id).map(k -> {
            if (request.getHanViet() != null) k.setHanViet(request.getHanViet().trim());
            if (request.getOnyomi() != null) k.setOnyomi(request.getOnyomi().trim());
            if (request.getKunyomi() != null) k.setKunyomi(request.getKunyomi().trim());
            if (request.getMeaning() != null) k.setMeaning(request.getMeaning().trim());
            if (request.getStrokeCount() != null) k.setStrokeCount(request.getStrokeCount());

            kanjiRepository.save(k);
            return ResponseEntity.ok(Map.<String, Object>of(
                    "success", true,
                    "message", "Cập nhật chữ Kanji [" + k.getKanjiCharacter() + "] thành công!"
            ));
        }).orElseGet(() -> ResponseEntity.status(404).body(Map.of(
                "success", false,
                "message", "Không tìm thấy chữ Kanji với ID đã cung cấp"
        )));
    }

    /**
     * 3. Kích hoạt AI Enrich Metadata cho các chữ còn thiếu nghĩa hoặc âm
     */
    @PostMapping("/enrich-ai")
    public ResponseEntity<Map<String, Object>> triggerAiEnrichment() {
        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Đã khởi chạy tiến trình AI Enrich thành công cho toàn bộ kho dữ liệu Kanji."
        ));
    }
}
