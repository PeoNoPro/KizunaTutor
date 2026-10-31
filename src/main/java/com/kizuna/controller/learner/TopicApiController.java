package com.kizuna.controller.learner;

import com.kizuna.dto.topic.AddVocabToDeckRequest;
import com.kizuna.dto.topic.TopicDto;
import com.kizuna.dto.topic.TopicVocabularyDto;
import com.kizuna.service.ITopicService;
import com.kizuna.util.VocabExcelGenerator;
import lombok.RequiredArgsConstructor;
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
import java.util.UUID;

/**
 * REST Controller phục vụ các endpoint Học Từ Vựng Theo Chủ Đề & OpenJLPT:
 * 1. GET  /api/learner/topics: Lấy danh sách các chủ đề & cấp độ JLPT
 * 2. GET  /api/learner/topics/{topicIdentifier}/vocabularies: Lấy từ vựng theo ID hoặc mã code
 * 3. POST /api/learner/topics/vocabularies/{vocabId}/add-to-deck: Lưu từ vựng vào bộ thẻ
 * 4. GET  /api/learner/topics/export-excel: Tải về toàn bộ 7.811 từ vựng file Excel chuẩn 100% tiếng Việt
 */
@RestController
@RequestMapping("/api/learner/topics")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class TopicApiController {

    private final ITopicService topicService;

    /**
     * 1. GET danh sách tất cả chủ đề học tập
     */
    @GetMapping
    public ResponseEntity<List<TopicDto>> getTopics() {
        List<TopicDto> topics = topicService.getAllTopics();
        return ResponseEntity.ok(topics);
    }

    /**
     * 2. GET danh sách từ vựng theo chủ đề (hỗ trợ cả UUID lẫn code như JLPT_N5, DAILY_COMMUNICATION...)
     */
    @GetMapping("/{topicIdentifier}/vocabularies")
    public ResponseEntity<Page<TopicVocabularyDto>> getVocabularies(
            @PathVariable("topicIdentifier") String topicIdentifier,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "30") int size) {
        String activeQuery = (keyword != null && !keyword.isBlank()) ? keyword : search;
        Pageable pageable = PageRequest.of(Math.max(0, page), Math.min(size, 100));
        Page<TopicVocabularyDto> vocabPage = topicService.getVocabulariesByTopic(topicIdentifier, activeQuery, pageable);
        return ResponseEntity.ok(vocabPage);
    }

    /**
     * Lấy chi tiết một từ vựng theo ID (hỗ trợ hiển thị trang chi tiết riêng)
     */
    @GetMapping("/vocabularies/{vocabId}")
    public ResponseEntity<TopicVocabularyDto> getVocabularyDetail(@PathVariable("vocabId") UUID vocabId) {
        TopicVocabularyDto dto = topicService.getVocabularyById(vocabId);
        if (dto == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(dto);
    }

    /**
     * 3. POST lưu từ vựng kèm danh sách chữ Kanji vào bộ thẻ Flashcard
     */
    @PostMapping("/vocabularies/{vocabId}/add-to-deck")
    public ResponseEntity<Map<String, Object>> addVocabToDeck(
            @PathVariable("vocabId") UUID vocabId,
            @RequestParam(required = false) UUID userId,
            @RequestBody AddVocabToDeckRequest request) {
        Map<String, Object> result = topicService.addVocabularyToDeck(userId, vocabId, request);
        return ResponseEntity.ok(result);
    }

    /**
     * 4. GET xuất toàn bộ 7.811 từ vựng OpenJLPT ra file Excel (.xlsx) chuẩn 100% tiếng Việt
     */
    @GetMapping("/export-excel")
    public ResponseEntity<Resource> exportExcel() {
        String filePath = "Kho_Tu_Vung_OpenJLPT_Full_Tieng_Viet.xlsx";
        File file = new File(filePath);
        if (!file.exists() || file.length() < 500000) {
            file = VocabExcelGenerator.generateExcel(filePath);
        }
        Resource resource = new FileSystemResource(file);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"Kho_Tu_Vung_OpenJLPT_Full_Tieng_Viet.xlsx\"")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(resource);
    }
}
