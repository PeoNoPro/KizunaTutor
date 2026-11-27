package com.kizuna.features.tutor_post.controller;

import com.kizuna.features.payment.service.CurrentUserAccessor;
import com.kizuna.features.tutor_post.dto.TutorPostDtos.RejectPostRequest;
import com.kizuna.features.tutor_post.dto.TutorPostDtos.ReviewItemView;
import com.kizuna.features.tutor_post.dto.TutorPostDtos.ReviewSummaryView;
import com.kizuna.features.tutor_post.service.TutorPostService;
import com.kizuna.features.verification.dto.response.PageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/** Cán bộ học vụ duyệt bài đăng của gia sư. Chỉ ROLE_ACADEMIC_OFFICER (SecurityConfig). */
@RestController
@CrossOrigin(origins = {"http://localhost:3000", "http://127.0.0.1:3000"})
@RequestMapping("/api/academic/posts")
@RequiredArgsConstructor
public class PostReviewController {

    private final TutorPostService service;
    private final CurrentUserAccessor currentUser;

    @GetMapping("/summary")
    public ResponseEntity<ReviewSummaryView> summary() {
        return ResponseEntity.ok(service.summary());
    }

    @GetMapping
    public ResponseEntity<PageResponse<ReviewItemView>> list(@RequestParam(required = false) String status,
                                                             @RequestParam(required = false) String keyword,
                                                             @RequestParam(required = false) Integer page,
                                                             @RequestParam(required = false) Integer size) {
        return ResponseEntity.ok(service.list(status, keyword, page, size));
    }

    @PostMapping("/{postId}/approve")
    public ResponseEntity<Map<String, Object>> approve(@PathVariable UUID postId) {
        return ResponseEntity.ok(result(TutorPostService.MSG_APPROVED, service.approve(postId, currentUser.requireUser())));
    }

    @PostMapping("/{postId}/reject")
    public ResponseEntity<Map<String, Object>> reject(@PathVariable UUID postId,
                                                      @RequestBody(required = false) RejectPostRequest request) {
        return ResponseEntity.ok(result(TutorPostService.MSG_REJECTED, service.reject(postId, currentUser.requireUser(), request)));
    }

    @PostMapping("/{postId}/request-info")
    public ResponseEntity<Map<String, Object>> requestInfo(@PathVariable UUID postId,
                                                           @RequestBody(required = false) RejectPostRequest request) {
        return ResponseEntity.ok(result(TutorPostService.MSG_INFO_REQUESTED,
                service.requestInfo(postId, currentUser.requireUser(), request)));
    }

    private static Map<String, Object> result(String message, ReviewItemView post) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("message", message);
        body.put("post", post);
        return body;
    }
}
