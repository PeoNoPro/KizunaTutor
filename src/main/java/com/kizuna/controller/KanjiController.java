package com.kizuna.controller;

import com.kizuna.dto.kanji.HandwritingSearchDto;
import com.kizuna.dto.kanji.ScoreResult;
import com.kizuna.dto.kanji.ScoreSubmission;
import com.kizuna.entity.KanjiDictionary;
import com.kizuna.repository.KanjiDictionaryRepository;
import com.kizuna.service.ai.AiStrokeAnalysisService;
import com.kizuna.service.kanji.GradingService;
import com.kizuna.service.kanji.HandwritingSearchService;
import com.kizuna.util.SvgUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/kanji")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class KanjiController {

    private final KanjiDictionaryRepository kanjiRepository;
    private final GradingService gradingService;
    private final AiStrokeAnalysisService aiStrokeAnalysisService;
    private final HandwritingSearchService handwritingSearchService;

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

    @GetMapping("/{character}")
    public ResponseEntity<?> getKanjiByCharacter(@PathVariable String character) {
        return kanjiRepository.findByKanjiCharacter(character)
                .map(k -> {
                    if (k.getSvgData() != null) {
                        k.setSvgData(SvgUtils.cleanKanjiVgSvg(k.getSvgData()));
                    }
                    return ResponseEntity.ok(k);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/hex/{hex}")
    public ResponseEntity<?> getKanjiByHex(@PathVariable String hex) {
        return kanjiRepository.findByUnicodeHex(hex.toLowerCase())
                .map(k -> {
                    if (k.getSvgData() != null) {
                        k.setSvgData(SvgUtils.cleanKanjiVgSvg(k.getSvgData()));
                    }
                    return ResponseEntity.ok(k);
                })
                .orElse(ResponseEntity.notFound().build());
    }
}
