package com.kizuna.controller;

import com.kizuna.dto.kanji.HandwritingSearchDto;
import com.kizuna.dto.kanji.KanjiCardDto;
import com.kizuna.dto.kanji.ScoreResult;
import com.kizuna.dto.kanji.ScoreSubmission;
import com.kizuna.entity.KanjiDictionary;
import com.kizuna.repository.KanjiDictionaryRepository;
import com.kizuna.service.ai.AiStrokeAnalysisService;
import com.kizuna.service.kanji.GradingService;
import com.kizuna.service.kanji.HandwritingSearchService;
import com.kizuna.util.SvgUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/kanji")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class KanjiController {

    private final KanjiDictionaryRepository kanjiRepository;
    private final GradingService gradingService;
    private final AiStrokeAnalysisService aiStrokeAnalysisService;
    private final HandwritingSearchService handwritingSearchService;

    /**
     * Tra cứu danh sách chữ Kanji phân trang theo cấp độ JLPT, từ khóa và số nét
     */
    @GetMapping({"/search", "/list", "/catalog"})
    public ResponseEntity<Map<String, Object>> searchKanji(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String jlpt,
            @RequestParam(required = false) String level,
            @RequestParam(required = false) Short strokeCount,
            @RequestParam(required = false) UUID excludeFolderId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "24") int size) {

        if (page < 0) page = 0;
        if (size <= 0) size = 24;
        if (size > 100) size = 100;

        String targetJlpt = (jlpt != null && !jlpt.isBlank()) ? jlpt : level;
        String jlptParam = (targetJlpt != null && !targetJlpt.isBlank() && !targetJlpt.equalsIgnoreCase("ALL"))
                ? jlpt.trim().toUpperCase()
                : null;
        String kwParam = (keyword != null && !keyword.isBlank()) ? keyword.trim() : null;

        Pageable pageable = PageRequest.of(page, size);
        Page<KanjiDictionary> pagedResult = kanjiRepository.searchKanjiPaginated(kwParam, jlptParam, strokeCount, excludeFolderId, pageable);

        List<KanjiCardDto> items = pagedResult.getContent().stream().map(k -> {
            handwritingSearchService.enrichKanjiMetadata(k);
            return KanjiCardDto.builder()
                    .id(k.getKanjiId())
                    .unicodeHex(k.getUnicodeHex())
                    .kanjiChar(k.getKanjiCharacter())
                    .strokeCount(k.getStrokeCount())
                    .radical(k.getRadical())
                    .hanViet(k.getHanViet() != null ? k.getHanViet() : "HÁN TỰ")
                    .onyomi(k.getOnyomi() != null ? k.getOnyomi() : "-")
                    .kunyomi(k.getKunyomi() != null ? k.getKunyomi() : "-")
                    .meaning(k.getMeaning() != null ? k.getMeaning() : "Chữ Hán hệ thống")
                    .jlpt(k.getJlptLevel() != null ? k.getJlptLevel().name() : (jlptParam != null ? jlptParam : "N5"))
                    .build();
        }).toList();

        Map<String, Object> response = new HashMap<>();
        response.put("items", items);
        response.put("content", items);
        response.put("totalElements", pagedResult.getTotalElements());
        response.put("totalPages", pagedResult.getTotalPages());
        response.put("currentPage", pagedResult.getNumber());
        response.put("number", pagedResult.getNumber());
        response.put("pageSize", pagedResult.getSize());
        response.put("size", pagedResult.getSize());
        response.put("hasNext", pagedResult.hasNext());
        response.put("hasPrev", pagedResult.hasPrevious());

        return ResponseEntity.ok(response);
    }

    @GetMapping("/count")
    public ResponseEntity<Map<String, Object>> countKanji() {
        long count = kanjiRepository.count();
        return ResponseEntity.ok(Map.of(
                "total_kanji", count,
                "message", count > 0 ? "Từ điển Kanji đã sẵn sàng phục vụ." : "Chưa có dữ liệu Kanji trong database."
        ));
    }

    /**
     * Tra cứu chữ Hán theo nét vẽ viết tay thời gian thực (Handwriting Search)
     */
    @PostMapping("/search-by-strokes")
    public ResponseEntity<HandwritingSearchDto.SearchResponse> searchByStrokes(
            @RequestBody HandwritingSearchDto.SearchRequest request) {
        HandwritingSearchDto.SearchResponse response = handwritingSearchService.search(request);
        return ResponseEntity.ok(response);
    }

    /**
     * Chấm điểm và phân tích nét vẽ chữ Hán bằng thuật toán Topo Vector + AI Sensei (Gemini)
     */
    @PostMapping("/grade")
    public ResponseEntity<ScoreResult> gradePractice(@RequestBody ScoreSubmission submission) {
        ScoreResult result = gradingService.evaluate(submission);
        return ResponseEntity.ok(result);
    }

    /**
     * Kiểm tra trạng thái AI (đã cấu hình key ở server chưa)
     */
    @GetMapping("/ai-status")
    public ResponseEntity<Map<String, Object>> getAiStatus() {
        return ResponseEntity.ok(aiStrokeAnalysisService.getAiStatus());
    }

    /**
     * Lấy toàn bộ siêu dữ liệu chi tiết của một chữ Kanji (Hán Việt, Onyomi, Kunyomi, JLPT, Bộ thủ, Nghĩa, SVG)
     */
    @GetMapping("/detail")
    public ResponseEntity<?> getKanjiDetail(
            @RequestParam(name = "char", required = false) String charParam,
            @RequestParam(name = "character", required = false) String characterParam,
            @RequestParam(name = "id", required = false) UUID idParam) {
        String ch = (charParam != null && !charParam.isBlank()) ? charParam.trim()
                : ((characterParam != null && !characterParam.isBlank()) ? characterParam.trim() : null);

        if (ch != null) {
            return getKanjiByCharacter(ch);
        } else if (idParam != null) {
            return kanjiRepository.findById(idParam)
                    .map(k -> {
                        handwritingSearchService.enrichKanjiMetadata(k);
                        if (k.getSvgData() != null) {
                            k.setSvgData(SvgUtils.cleanKanjiVgSvg(k.getSvgData()));
                        }
                        return ResponseEntity.ok(k);
                    })
                    .orElse(ResponseEntity.notFound().build());
        }
        return ResponseEntity.badRequest().body(Map.of("error", "Tham số 'char' hoặc 'id' là bắt buộc"));
    }

    @GetMapping("/{character}")
    public ResponseEntity<?> getKanjiByCharacter(@PathVariable String character) {
        return kanjiRepository.findByKanjiCharacter(character)
                .map(k -> {
                    handwritingSearchService.enrichKanjiMetadata(k);
                    if (k.getSvgData() != null) {
                        k.setSvgData(SvgUtils.cleanKanjiVgSvg(k.getSvgData()));
                    }
                    return ResponseEntity.ok(k);
                })
                .or(() -> {
                    var offlineOpt = handwritingSearchService.getOfflineKanji(character);
                    if (offlineOpt.isPresent()) {
                        var off = offlineOpt.get();
                        KanjiDictionary mock = KanjiDictionary.builder()
                                .kanjiCharacter(character)
                                .hanViet(off.han_viet() != null ? off.han_viet() : "HÁN TỰ")
                                .meaning(off.meaning() != null ? off.meaning() : "")
                                .onyomi(off.onyomi() != null ? off.onyomi() : "-")
                                .kunyomi(off.kunyomi() != null ? off.kunyomi() : "-")
                                .strokeCount((short) 1)
                                .radical("Chưa cập nhật")
                                .build();
                        return java.util.Optional.of(ResponseEntity.ok(mock));
                    }
                    return java.util.Optional.empty();
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping({"/hex/{hex}", "/strokes/{hex}"})
    public ResponseEntity<?> getKanjiByHex(@PathVariable String hex) {
        return kanjiRepository.findByUnicodeHex(hex.toLowerCase())
                .map(k -> {
                    handwritingSearchService.enrichKanjiMetadata(k);
                    if (k.getSvgData() != null) {
                        k.setSvgData(SvgUtils.cleanKanjiVgSvg(k.getSvgData()));
                    }
                    return ResponseEntity.ok(k);
                })
                .orElse(ResponseEntity.notFound().build());
    }
}
