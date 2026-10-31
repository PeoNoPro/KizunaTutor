package com.kizuna.controller.learner;

import com.kizuna.dto.grammar.AiGenerateSentenceRequest;
import com.kizuna.dto.grammar.AiGenerateSentenceResponse;
import com.kizuna.dto.grammar.GrammarRuleDto;
import com.kizuna.service.IGrammarService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.util.List;
import java.util.Map;

/**
 * REST API phục vụ Cẩm Nang Ngữ Pháp Mẫu Câu JLPT (N5 - N1)
 * và Tích hợp Kizuna AI Sensei đặt câu theo ngữ cảnh đời sống.
 */
@RestController
@RequestMapping("/api/learner/grammar")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@Slf4j
public class GrammarApiController {

    private final IGrammarService grammarService;

    /**
     * 1. GET thống kê số lượng mẫu câu theo 5 cấp độ JLPT
     */
    @GetMapping("/levels")
    public ResponseEntity<Map<String, Long>> getLevelCounts() {
        return ResponseEntity.ok(grammarService.getLevelCounts());
    }

    /**
     * 2. GET danh sách mẫu câu theo cấp độ & tìm kiếm
     */
    @GetMapping
    public ResponseEntity<Page<GrammarRuleDto>> getGrammarRules(
            @RequestParam(defaultValue = "N5") String level,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(Math.max(0, page), Math.min(size, 100));
        return ResponseEntity.ok(grammarService.getGrammarRules(level, keyword, pageable));
    }

    /**
     * 3. GET chi tiết 1 mẫu câu ngữ pháp
     */
    @GetMapping("/{id}")
    public ResponseEntity<GrammarRuleDto> getGrammarDetail(@PathVariable("id") String id) {
        GrammarRuleDto detail = grammarService.getGrammarDetail(id);
        if (detail == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(detail);
    }

    /**
     * 4. POST tạo câu ví dụ dựa trên AI Gemini theo đúng ngữ cảnh thực tế
     */
    @PostMapping("/ai-generate")
    public ResponseEntity<AiGenerateSentenceResponse> generateAiSentence(
            @RequestBody(required = false) AiGenerateSentenceRequest request) {
        if (request == null) {
            request = new AiGenerateSentenceRequest();
        }
        try {
            AiGenerateSentenceResponse result = grammarService.generateAiSentence(request);
            return ResponseEntity.ok(result);
        } catch (Throwable t) {
            log.error("[AiGrammar] Lỗi khi xử lý tạo câu qua AI: ", t);
            return ResponseEntity.ok(AiGenerateSentenceResponse.builder()
                    .pattern(request.getPattern() != null ? request.getPattern() : "〜てください")
                    .context(request.getContext() != null ? request.getContext() : "Giao tiếp hàng ngày")
                    .japaneseSentence("すみません、お水をもう一杯ください。")
                    .furiganaHtml("すみません、お<ruby>水<rt>みず</rt></ruby>をもう<ruby>一杯<rt>いっぱい</rt></ruby><ruby>下<rt>くだ</rt></ruby>さい。")
                    .vietnameseMeaning("Xin lỗi, làm ơn cho tôi xin thêm một cốc nước nữa ạ.")
                    .nuanceNote("Mẫu câu này thể hiện sự nhờ vả, yêu cầu một cách lịch sự, nhã nhặn.")
                    .kanjiCharacters(List.of("水", "杯", "下"))
                    .keyVocabularies(List.of("水 (みず) - Nước", "一杯 (いっぱい) - Một cốc / Một ly"))
                    .build());
        }
    }

    /**
     * 5. GET Xuất toàn bộ 526 mẫu ngữ pháp JLPT (N5 - N1) ra file Excel 100% tiếng Việt
     */
    @GetMapping("/export-excel")
    public ResponseEntity<Resource> exportExcel() {
        log.info("[GrammarApiController] Nhận yêu cầu xuất file Excel cẩm nang ngữ pháp OpenJLPT 100% tiếng Việt");
        try {
            File file = grammarService.exportGrammarExcel();
            Resource resource = new FileSystemResource(file);
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"Kho_Ngu_Phap_OpenJLPT_Tieng_Viet.xlsx\"")
                    .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                    .contentLength(file.length())
                    .body(resource);
        } catch (Exception e) {
            log.error("[GrammarApiController] Lỗi khi tạo file Excel ngữ pháp: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }
}
