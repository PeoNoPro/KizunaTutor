package com.kizuna.features.progress.controller;

import com.kizuna.features.classroom.dto.ClassroomApiResponse;
import com.kizuna.features.classroom.util.CurrentActor;
import com.kizuna.features.classroom.util.CurrentUserResolver;
import com.kizuna.features.progress.dto.*;
import com.kizuna.features.progress.service.AssignmentService;
import com.kizuna.features.progress.service.SubmissionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

/**
 * API FE-07 Homework, Assessment & Progress Tracking (UC-63, 64, 66, 67, 95, 96).
 * Mọi endpoint yêu cầu header Authorization: Bearer &lt;token&gt;.
 */
@RestController
@RequestMapping("/api/classrooms/{spaceId}/assignments")
@RequiredArgsConstructor
@CrossOrigin(origins = {"http://localhost:3000", "http://127.0.0.1:3000"})
public class AssignmentController {

    private final CurrentUserResolver currentUserResolver;
    private final AssignmentService assignmentService;
    private final SubmissionService submissionService;

    @GetMapping
    public ClassroomApiResponse<List<AssignmentSummaryResponse>> list(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String auth,
            @PathVariable UUID spaceId) {
        CurrentActor actor = currentUserResolver.resolve(auth);
        return ClassroomApiResponse.ok("Tải danh sách bài tập thành công.", assignmentService.list(actor, spaceId));
    }

    @PostMapping
    public ResponseEntity<ClassroomApiResponse<AssignmentSummaryResponse>> create(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String auth,
            @PathVariable UUID spaceId, @RequestBody AssignmentRequest request) {
        CurrentActor actor = currentUserResolver.resolve(auth);
        return ResponseEntity.status(HttpStatus.CREATED).body(ClassroomApiResponse.ok(
                "Giao bài tập thành công.", assignmentService.create(actor, spaceId, request)));
    }

    @GetMapping("/{assignmentId}")
    public ClassroomApiResponse<AssignmentDetailResponse> detail(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String auth,
            @PathVariable UUID spaceId, @PathVariable UUID assignmentId) {
        CurrentActor actor = currentUserResolver.resolve(auth);
        return ClassroomApiResponse.ok("Tải chi tiết bài tập thành công.",
                assignmentService.detail(actor, spaceId, assignmentId));
    }

    @PutMapping("/{assignmentId}")
    public ClassroomApiResponse<AssignmentSummaryResponse> update(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String auth,
            @PathVariable UUID spaceId, @PathVariable UUID assignmentId, @RequestBody AssignmentRequest request) {
        CurrentActor actor = currentUserResolver.resolve(auth);
        return ClassroomApiResponse.ok("Cập nhật bài tập thành công.",
                assignmentService.update(actor, spaceId, assignmentId, request));
    }

    @PostMapping(value = "/{assignmentId}/submissions", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ClassroomApiResponse<SubmissionResponse>> submit(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String auth,
            @PathVariable UUID spaceId, @PathVariable UUID assignmentId,
            @RequestParam(value = "content", required = false) String content,
            @RequestPart(value = "file", required = false) MultipartFile file) {
        CurrentActor actor = currentUserResolver.resolve(auth);
        SubmissionResponse result = submissionService.submit(actor, spaceId, assignmentId, content, file);
        String message = result.attemptNumber() > 1 ? "Nộp lại bài tập thành công." : "Nộp bài tập thành công.";
        return ResponseEntity.status(HttpStatus.CREATED).body(ClassroomApiResponse.ok(message, result));
    }

    @GetMapping("/{assignmentId}/submissions/{submissionId}/file")
    public ResponseEntity<byte[]> file(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String auth,
            @PathVariable UUID spaceId, @PathVariable UUID assignmentId, @PathVariable UUID submissionId) {
        CurrentActor actor = currentUserResolver.resolve(auth);
        SubmissionFileContent content = submissionService.getFile(actor, spaceId, assignmentId, submissionId);
        ContentDisposition disposition = ContentDisposition.inline()
                .filename(content.fileName(), StandardCharsets.UTF_8)
                .build();
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(content.contentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .header(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS, HttpHeaders.CONTENT_DISPOSITION)
                .header("X-Content-Type-Options", "nosniff")
                .header(HttpHeaders.CACHE_CONTROL, "private, no-store")
                .body(content.data());
    }

    @PutMapping("/{assignmentId}/submissions/{submissionId}/grade")
    public ClassroomApiResponse<SubmissionResponse> grade(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String auth,
            @PathVariable UUID spaceId, @PathVariable UUID assignmentId, @PathVariable UUID submissionId,
            @RequestBody GradeRequest request) {
        CurrentActor actor = currentUserResolver.resolve(auth);
        return ClassroomApiResponse.ok("Đã gửi điểm và nhận xét.",
                submissionService.grade(actor, spaceId, assignmentId, submissionId, request));
    }
}
