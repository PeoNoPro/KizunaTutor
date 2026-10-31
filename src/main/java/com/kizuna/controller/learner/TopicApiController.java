package com.kizuna.controller.learner;

import com.kizuna.dto.topic.AddVocabToDeckRequest;
import com.kizuna.dto.topic.TopicDto;
import com.kizuna.dto.topic.TopicVocabularyDto;
import com.kizuna.service.ITopicService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * REST Controller phục vụ các endpoint Học Từ Vựng Theo Chủ Đề Đời Sống:
 * 1. GET  /api/learner/topics: Lấy danh sách các chủ đề
 * 2. GET  /api/learner/topics/{topicIdentifier}/vocabularies: Lấy từ vựng theo ID hoặc mã code chủ đề
 * 3. POST /api/learner/topics/vocabularies/{vocabId}/add-to-deck: Lưu từ vựng vào bộ thẻ
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
     * 2. GET danh sách từ vựng theo chủ đề (hỗ trợ cả UUID lẫn code như DAILY_COMMUNICATION)
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
}
