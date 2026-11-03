package com.kizuna.features.verification.controller;

import com.kizuna.features.verification.dto.request.RejectVerificationRequest;
import com.kizuna.features.verification.dto.request.RequestInfoRequest;
import com.kizuna.features.verification.dto.request.VerificationQueueQuery;
import com.kizuna.features.verification.dto.response.DecisionResultResponse;
import com.kizuna.features.verification.dto.response.PageResponse;
import com.kizuna.features.verification.dto.response.TutorVerificationResponse;
import com.kizuna.features.verification.dto.response.VerificationHistoryItemResponse;
import com.kizuna.features.verification.dto.response.VerificationQueueItemResponse;
import com.kizuna.features.verification.dto.response.VerificationSummaryResponse;
import com.kizuna.features.verification.service.VerificationReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * API thẩm định hồ sơ gia sư dành cho cán bộ học vụ (FE-03: UC-25…UC-28, UC-84).
 * Chỉ ROLE_ACADEMIC_OFFICER truy cập được (SecurityConfig).
 */
@RestController
@CrossOrigin(origins = {"http://localhost:3000", "http://127.0.0.1:3000"})
@RequestMapping("/api/academic/verifications")
@RequiredArgsConstructor
public class VerificationReviewController {

    private final VerificationReviewService reviewService;

    @GetMapping("/summary")
    public ResponseEntity<VerificationSummaryResponse> getSummary() {
        return ResponseEntity.ok(reviewService.getSummary());
    }

    @GetMapping
    public ResponseEntity<PageResponse<VerificationQueueItemResponse>> getQueue(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String jlptLevel,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate submittedFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate submittedTo,
            @RequestParam(required = false) String sort,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        return ResponseEntity.ok(reviewService.getQueue(VerificationQueueQuery.builder()
                .status(status).keyword(keyword).jlptLevel(jlptLevel)
                .submittedFrom(submittedFrom).submittedTo(submittedTo)
                .sort(sort).page(page).size(size)
                .build()));
    }

    @GetMapping("/{tutorId}")
    public ResponseEntity<TutorVerificationResponse> getDetail(@PathVariable UUID tutorId) {
        return ResponseEntity.ok(reviewService.getDetail(tutorId));
    }

    @GetMapping("/{tutorId}/documents/{documentId}/file")
    public ResponseEntity<Resource> downloadDocument(@PathVariable UUID tutorId, @PathVariable UUID documentId) {
        return DocumentFileResponses.inline(reviewService.loadDocumentFile(tutorId, documentId));
    }

    @GetMapping("/{tutorId}/history")
    public ResponseEntity<List<VerificationHistoryItemResponse>> getHistory(@PathVariable UUID tutorId) {
        return ResponseEntity.ok(reviewService.getHistory(tutorId));
    }

    @PostMapping("/{tutorId}/approve")
    public ResponseEntity<DecisionResultResponse> approve(@PathVariable UUID tutorId) {
        return ResponseEntity.ok(reviewService.approve(tutorId));
    }

    @PostMapping("/{tutorId}/reject")
    public ResponseEntity<DecisionResultResponse> reject(@PathVariable UUID tutorId,
                                                         @RequestBody(required = false) RejectVerificationRequest request) {
        return ResponseEntity.ok(reviewService.reject(tutorId, request));
    }

    @PostMapping("/{tutorId}/request-info")
    public ResponseEntity<DecisionResultResponse> requestInfo(@PathVariable UUID tutorId,
                                                              @RequestBody(required = false) RequestInfoRequest request) {
        return ResponseEntity.ok(reviewService.requestInfo(tutorId, request));
    }
}
