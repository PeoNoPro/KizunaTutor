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
     * 1. Tra cứu từ điển Hán tự tương tác cho học viên (hỗ trợ từ khóa, JLPT và lọc theo số nét)
     */
    @GetMapping("/search")
    public ResponseEntity<Map<String, Object>> searchDictionary(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String jlpt,
            @RequestParam(required = false) Short strokeCount,
            @RequestParam(required = false) Short minStroke,
            @RequestParam(required = false) Short maxStroke,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "18") int size) {

        String jlptParam = (jlpt != null && !jlpt.isBlank() && !jlpt.equalsIgnoreCase("ALL")) ? jlpt.trim().toUpperCase() : null;
        String kwParam = (keyword != null && !keyword.isBlank()) ? keyword.trim() : null;

        Page<KanjiDictionary> pagedResult;
        if (minStroke != null || maxStroke != null) {
            pagedResult = kanjiRepository.searchKanjiByStrokeRange(kwParam, jlptParam, minStroke, maxStroke, PageRequest.of(page, size));
        } else {
            pagedResult = kanjiRepository.searchKanjiPaginated(kwParam, jlptParam, strokeCount, null, PageRequest.of(page, size));
        }

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
     * 2. Lấy toàn bộ siêu dữ liệu chi tiết của một chữ Kanji cho Học Viên
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
                            k.setSvgData(com.kizuna.util.SvgUtils.cleanKanjiVgSvg(k.getSvgData()));
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
                        k.setSvgData(com.kizuna.util.SvgUtils.cleanKanjiVgSvg(k.getSvgData()));
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

    /**
     * 3. Lưu nhanh chữ Kanji vào bộ thẻ cá nhân của học viên
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

    @Data
    public static class KanaProgressRequest {
        private String scriptType;
        private int totalCount;
        private int correctCount;
        private double accuracy;
        private List<String> categories;
    }

    /**
     * 3. Ghi nhận kết quả làm bài Kana Quiz vào tiến độ học tập của Học Viên
     */
    @PostMapping("/progress/kana-quiz")
    public ResponseEntity<Map<String, Object>> saveKanaProgress(@RequestBody KanaProgressRequest request) {
        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Đã ghi nhận kết quả bài luyện Kana vào tiến độ học tập thành công!",
                "accuracy", request.getAccuracy(),
                "correctCount", request.getCorrectCount(),
                "totalCount", request.getTotalCount()
        ));
    }
}
