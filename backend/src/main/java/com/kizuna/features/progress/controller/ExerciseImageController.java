package com.kizuna.features.progress.controller;

import com.kizuna.features.classroom.dto.ClassroomApiResponse;
import com.kizuna.features.classroom.exception.ClassroomExceptionHandler;
import com.kizuna.features.classroom.util.CurrentActor;
import com.kizuna.features.classroom.util.CurrentUserResolver;
import com.kizuna.features.progress.dto.ExerciseDtos.ImageContent;
import com.kizuna.features.progress.dto.ExerciseDtos.ImageInfo;
import com.kizuna.features.progress.service.ExerciseImageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Ảnh minh họa cho câu hỏi bài làm trực tiếp (FE-07). Yêu cầu header Authorization: Bearer &lt;token&gt;.
 * Gia sư tải ảnh lên trước, rồi gửi imageId kèm câu hỏi khi lưu bài.
 */
@RestController
@RequestMapping("/api/classrooms/{spaceId}/exercise-images")
@RequiredArgsConstructor
@CrossOrigin(origins = {"http://localhost:3000", "http://127.0.0.1:3000"})
public class ExerciseImageController extends ClassroomExceptionHandler {

    private final CurrentUserResolver currentUserResolver;
    private final ExerciseImageService service;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ClassroomApiResponse<ImageInfo>> upload(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String auth,
            @PathVariable UUID spaceId,
            @RequestParam(value = "file", required = false) MultipartFile file) {
        CurrentActor actor = currentUserResolver.resolve(auth);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ClassroomApiResponse.ok("Đã tải ảnh lên.", service.upload(actor, spaceId, file)));
    }

    /** Nội dung ảnh (trình duyệt tải bằng token rồi hiển thị). Ảnh không đổi theo mã nên cho trình duyệt lưu đệm riêng. */
    @GetMapping("/{imageId}")
    public ResponseEntity<byte[]> content(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String auth,
            @PathVariable UUID spaceId, @PathVariable UUID imageId) {
        CurrentActor actor = currentUserResolver.resolve(auth);
        ImageContent img = service.content(actor, spaceId, imageId);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(img.contentType()))
                .cacheControl(CacheControl.maxAge(1, TimeUnit.HOURS).cachePrivate())
                .header("X-Content-Type-Options", "nosniff")
                .body(img.data());
    }
}
