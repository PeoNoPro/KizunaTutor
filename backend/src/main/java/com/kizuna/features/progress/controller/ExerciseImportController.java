package com.kizuna.features.progress.controller;

import com.kizuna.features.classroom.dto.ClassroomApiResponse;
import com.kizuna.features.classroom.exception.ClassroomExceptionHandler;
import com.kizuna.features.classroom.util.CurrentActor;
import com.kizuna.features.classroom.util.CurrentUserResolver;
import com.kizuna.features.progress.service.impl.ExerciseImportService;
import com.kizuna.features.progress.service.impl.ExerciseImportService.ImportPreview;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

/**
 * Nhập câu hỏi bài làm trực tiếp từ Excel (FE-07). Yêu cầu header Authorization: Bearer &lt;token&gt;.
 * - GET  .../exercise-import/template: tải file mẫu (.xlsx).
 * - POST .../exercise-import: kiểm tra file, trả bảng xem trước từng dòng (không lưu gì).
 */
@RestController
@RequestMapping("/api/classrooms/{spaceId}/exercise-import")
@RequiredArgsConstructor
@CrossOrigin(origins = {"http://localhost:3000", "http://127.0.0.1:3000"})
public class ExerciseImportController extends ClassroomExceptionHandler {

    private final CurrentUserResolver currentUserResolver;
    private final ExerciseImportService service;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ClassroomApiResponse<ImportPreview> preview(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String auth,
            @PathVariable UUID spaceId,
            @RequestParam(value = "file", required = false) MultipartFile file) {
        CurrentActor actor = currentUserResolver.resolve(auth);
        return ClassroomApiResponse.ok("Đã đọc file Excel.", service.preview(actor, spaceId, file));
    }

    @GetMapping("/template")
    public ResponseEntity<byte[]> template(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String auth,
            @PathVariable UUID spaceId) {
        CurrentActor actor = currentUserResolver.resolve(auth);
        byte[] data = service.template(actor, spaceId);
        ContentDisposition disposition = ContentDisposition.attachment()
                .filename("mau-nhap-cau-hoi-kizuna.xlsx", StandardCharsets.UTF_8).build();
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .header(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS, HttpHeaders.CONTENT_DISPOSITION)
                .header(HttpHeaders.CACHE_CONTROL, "private, no-store")
                .body(data);
    }
}
