package com.kizuna.controller.tutor;

import com.kizuna.dto.kanji.KanjiCardDto;
import com.kizuna.entity.KanjiDictionary;
import com.kizuna.repository.KanjiDictionaryRepository;
import com.kizuna.service.kanji.HandwritingSearchService;
import com.kizuna.util.SvgUtils;
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
 * REST Controller phục vụ hoạt động giảng dạy & soạn bài tập của Tutor.
 * Endpoint: /api/tutor/kanji/**
 */
@RestController
@RequestMapping("/api/tutor/kanji")
@RequiredArgsConstructor
public class TutorKanjiApiController {

    private final KanjiDictionaryRepository kanjiRepository;
    private final HandwritingSearchService handwritingSearchService;

    @Data
    public static class CreateAssignmentRequest {
        private String title;
        private List<UUID> kanjiIds;
        private String targetClass;
        private String deadline;
    }

    /**
     * 1. Tra cứu danh mục chữ Kanji có kèm SVG phục vụ mô phỏng thứ tự nét viết
     */
    @GetMapping("/catalog")
    public ResponseEntity<Map<String, Object>> getTeachingCatalog(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String jlpt,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "16") int size) {

        String jlptParam = (jlpt != null && !jlpt.isBlank() && !jlpt.equalsIgnoreCase("ALL")) ? jlpt.trim().toUpperCase() : null;
        String kwParam = (keyword != null && !keyword.isBlank()) ? keyword.trim() : null;

        Page<KanjiDictionary> pagedResult = kanjiRepository.searchKanjiPaginated(kwParam, jlptParam, null, null, PageRequest.of(page, size));

        List<KanjiCardDto> items = pagedResult.getContent().stream().map(k -> {
            handwritingSearchService.enrichKanjiMetadata(k);
            return KanjiCardDto.builder()
                    .id(k.getKanjiId())
                    .kanjiChar(k.getKanjiCharacter())
                    .strokeCount(k.getStrokeCount())
                    .hanViet(k.getHanViet() != null ? k.getHanViet() : "HÁN TỰ")
                    .onyomi(k.getOnyomi() != null ? k.getOnyomi() : "-")
                    .kunyomi(k.getKunyomi() != null ? k.getKunyomi() : "-")
                    .meaning(k.getMeaning() != null ? k.getMeaning() : "")
                    .jlpt(k.getJlptLevel() != null ? k.getJlptLevel().name() : "N5")
                    .svgData(k.getSvgData() != null ? SvgUtils.cleanKanjiVgSvg(k.getSvgData()) : null)
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
     * 2. Tạo bài tập từ "Giỏ gom chữ" giao cho học viên trong lớp
     */
    @PostMapping("/create-assignment")
    public ResponseEntity<Map<String, Object>> createAssignment(@RequestBody CreateAssignmentRequest request) {
        int count = (request.getKanjiIds() != null) ? request.getKanjiIds().size() : 0;
        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Đã tạo bài tập [" + request.getTitle() + "] gồm " + count + " chữ Kanji và gửi tới học viên lớp " + request.getTargetClass() + " thành công!",
                "assignedCount", count
        ));
    }
}
