package com.kizuna.features.learning_content.controller;

import com.kizuna.core.dto.ApiResponse;
import com.kizuna.core.dto.PageResponse;
import com.kizuna.core.entity.User;
import com.kizuna.features.learning_content.dto.ContentImportResponse;
import com.kizuna.features.learning_content.dto.ContentSummaryResponse;
import com.kizuna.features.learning_content.dto.ImportHistoryDetailResponse;
import com.kizuna.features.learning_content.dto.ImportHistoryResponse;
import com.kizuna.features.learning_content.service.LearningContentService;
import com.kizuna.features.payment.service.CurrentUserAccessor;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;
import java.util.UUID;

/**
 * FE-13: cán bộ học vụ xem kho học liệu, nhập từ Excel và xem lịch sử nhập. Chỉ ROLE_ACADEMIC_OFFICER (SecurityConfig).
 * Hợp đồng: specs/013-academic-content-import/contracts/content-import-api.md. Mọi phản hồi bọc ApiResponse.
 * type: kanji | vocabulary | grammar
 */
@RestController
@CrossOrigin(origins = {"http://localhost:3000", "http://127.0.0.1:3000"})
@RequestMapping("/api/academic/content-imports")
@RequiredArgsConstructor
public class ContentImportController {

    private final LearningContentService service;
    private final CurrentUserAccessor currentUser;

    @GetMapping("/summary")
    public ResponseEntity<ApiResponse<ContentSummaryResponse>> summary() {
        return ResponseEntity.ok(ApiResponse.ok(service.summary()));
    }

    @GetMapping("/{type}/items")
    public ResponseEntity<ApiResponse<PageResponse<Map<String, Object>>>> items(
            @PathVariable String type,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String jlpt,
            @RequestParam(defaultValue = "LEVEL") String sort,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(ApiResponse.ok(service.listItems(type, keyword, jlpt, sort, page, size)));
    }

    @PostMapping(value = "/{type}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<ContentImportResponse>> importFile(
            @PathVariable String type,
            @RequestPart(value = "file", required = false) MultipartFile file,
            @RequestParam(defaultValue = "true") boolean dryRun) {
        User officer = currentUser.requireUser();
        ContentImportResponse result = service.importFile(type, file, dryRun, officer.getUserId(), officer.getEmail());
        return ResponseEntity.ok(ApiResponse.ok(result.message(), result));
    }

    @GetMapping("/history")
    public ResponseEntity<ApiResponse<PageResponse<ImportHistoryResponse>>> history(
            @RequestParam(required = false) String type,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(ApiResponse.ok(service.history(type, page, size)));
    }

    @GetMapping("/history/{id}")
    public ResponseEntity<ApiResponse<ImportHistoryDetailResponse>> historyDetail(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(service.historyDetail(id)));
    }
}
