package com.kizuna.features.progress.controller;

import com.kizuna.features.classroom.dto.ClassroomApiResponse;
import com.kizuna.features.classroom.exception.ClassroomExceptionHandler;
import com.kizuna.features.classroom.util.CurrentActor;
import com.kizuna.features.classroom.util.CurrentUserResolver;
import com.kizuna.features.progress.dto.*;
import com.kizuna.features.progress.service.AssignmentAttachmentService;
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
public class AssignmentController extends ClassroomExceptionHandler {

    private final CurrentUserResolver currentUserResolver;
    private final AssignmentService assignmentService;
    private final SubmissionService submissionService;
    private final AssignmentAttachmentService attachmentService;

    @GetMapping
    public ClassroomApiResponse<List<AssignmentSummaryResponse>> list(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String auth,
            @PathVariable UUID spaceId) {
        CurrentActor actor = currentUserResolver.resolve(auth);
        return ClassroomApiResponse.ok("Tải danh sách bài tập thành công.", assignmentService.list(actor, spaceId));
    }

    /**
     * Giao bài tập (multipart): title, description, dueDate (ISO, tùy chọn), totalPoints (tùy chọn), nhiều phần "file".
     * Mọi thứ gửi trong một lần, kiểm tra hết; lỗi bất kỳ (kể cả một tệp) → không lưu gì.
     */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ClassroomApiResponse<AssignmentSummaryResponse>> create(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String auth,
            @PathVariable UUID spaceId,
            @RequestParam(value = "title", required = false) String title,
            @RequestParam(value = "description", required = false) String description,
            @RequestParam(value = "dueDate", required = false)
            @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE_TIME)
            java.time.OffsetDateTime dueDate,
            @RequestParam(value = "totalPoints", required = false) java.math.BigDecimal totalPoints,
            @RequestPart(value = "file", required = false) List<MultipartFile> files) {
        CurrentActor actor = currentUserResolver.resolve(auth);
        AssignmentRequest request = new AssignmentRequest(title, description, dueDate, totalPoints);
        return ResponseEntity.status(HttpStatus.CREATED).body(ClassroomApiResponse.ok(
                "Giao bài tập thành công.", assignmentService.create(actor, spaceId, request, files)));
    }

    @GetMapping("/{assignmentId}")
    public ClassroomApiResponse<AssignmentDetailResponse> detail(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String auth,
            @PathVariable UUID spaceId, @PathVariable UUID assignmentId) {
        CurrentActor actor = currentUserResolver.resolve(auth);
        return ClassroomApiResponse.ok("Tải chi tiết bài tập thành công.",
                assignmentService.detail(actor, spaceId, assignmentId));
    }

    /** Sửa bài tập (multipart) như POST, kèm "removeFileIds" (tệp gỡ) và nhiều phần "file" (tệp thêm), trong một lần. */
    @PutMapping(value = "/{assignmentId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ClassroomApiResponse<AssignmentSummaryResponse> update(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String auth,
            @PathVariable UUID spaceId, @PathVariable UUID assignmentId,
            @RequestParam(value = "title", required = false) String title,
            @RequestParam(value = "description", required = false) String description,
            @RequestParam(value = "dueDate", required = false)
            @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE_TIME)
            java.time.OffsetDateTime dueDate,
            @RequestParam(value = "totalPoints", required = false) java.math.BigDecimal totalPoints,
            @RequestParam(value = "removeFileIds", required = false) List<UUID> removeFileIds,
            @RequestPart(value = "file", required = false) List<MultipartFile> files) {
        CurrentActor actor = currentUserResolver.resolve(auth);
        AssignmentRequest request = new AssignmentRequest(title, description, dueDate, totalPoints);
        return ClassroomApiResponse.ok("Cập nhật bài tập thành công.",
                assignmentService.update(actor, spaceId, assignmentId, request, files, removeFileIds));
    }

    // ===== Tệp đề bài: lưu cùng lúc với giao/sửa bài tập; ở đây chỉ còn API mở tệp =====

    @GetMapping("/{assignmentId}/files/{fileId}")
    public ResponseEntity<byte[]> assignmentFile(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String auth,
            @PathVariable UUID spaceId, @PathVariable UUID assignmentId, @PathVariable UUID fileId) {
        CurrentActor actor = currentUserResolver.resolve(auth);
        return fileResponse(attachmentService.getFile(actor, spaceId, assignmentId, fileId));
    }

    // ===== Bài nộp: mỗi học viên một bài nộp, sửa thay vì nộp lại =====

    @PostMapping(value = "/{assignmentId}/submissions", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ClassroomApiResponse<SubmissionResponse>> submit(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String auth,
            @PathVariable UUID spaceId, @PathVariable UUID assignmentId,
            @RequestParam(value = "content", required = false) String content,
            @RequestPart(value = "file", required = false) List<MultipartFile> files) {
        CurrentActor actor = currentUserResolver.resolve(auth);
        return ResponseEntity.status(HttpStatus.CREATED).body(ClassroomApiResponse.ok("Nộp bài tập thành công.",
                submissionService.submit(actor, spaceId, assignmentId, content, files)));
    }

    /** Sửa bài đã nộp: nội dung mới, các tệp thêm ("file"), id các tệp cần gỡ ("removeFileIds"). */
    @PutMapping(value = "/{assignmentId}/submissions/{submissionId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ClassroomApiResponse<SubmissionResponse> updateSubmission(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String auth,
            @PathVariable UUID spaceId, @PathVariable UUID assignmentId, @PathVariable UUID submissionId,
            @RequestParam(value = "content", required = false) String content,
            @RequestParam(value = "removeFileIds", required = false) List<UUID> removeFileIds,
            @RequestPart(value = "file", required = false) List<MultipartFile> files) {
        CurrentActor actor = currentUserResolver.resolve(auth);
        return ClassroomApiResponse.ok("Cập nhật bài nộp thành công.",
                submissionService.update(actor, spaceId, assignmentId, submissionId, content, files, removeFileIds));
    }

    @GetMapping("/{assignmentId}/submissions/{submissionId}/files/{fileId}")
    public ResponseEntity<byte[]> submissionFile(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String auth,
            @PathVariable UUID spaceId, @PathVariable UUID assignmentId, @PathVariable UUID submissionId,
            @PathVariable UUID fileId) {
        CurrentActor actor = currentUserResolver.resolve(auth);
        return fileResponse(submissionService.getFile(actor, spaceId, assignmentId, submissionId, fileId));
    }

    /** Trả nội dung tệp (không bọc JSON), mở trực tiếp trên trình duyệt, không lưu cache. */
    private static ResponseEntity<byte[]> fileResponse(SubmissionFileContent content) {
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
