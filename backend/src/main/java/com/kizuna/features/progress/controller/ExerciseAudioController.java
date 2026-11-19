package com.kizuna.features.progress.controller;

import com.kizuna.features.classroom.dto.ClassroomApiResponse;
import com.kizuna.features.classroom.exception.ClassroomExceptionHandler;
import com.kizuna.features.classroom.util.CurrentActor;
import com.kizuna.features.classroom.util.CurrentUserResolver;
import com.kizuna.features.progress.dto.ExerciseDtos.AudioInfo;
import com.kizuna.features.progress.dto.ExerciseDtos.AudioUrl;
import com.kizuna.features.progress.dto.ExerciseDtos.ListenGrant;
import com.kizuna.features.progress.dto.ExerciseDtos.ListenRequest;
import com.kizuna.features.progress.dto.ExerciseDtos.ListenStatus;
import com.kizuna.features.progress.service.ExerciseAudioService;
import com.kizuna.features.progress.service.ExerciseListenService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

/**
 * File nghe cho câu hỏi bài làm trực tiếp (FE-07), lưu trên Cloudflare R2. Yêu cầu header Authorization: Bearer &lt;token&gt;.
 * Gia sư tải file lên trước, rồi gửi audioId kèm câu hỏi khi lưu bài. Nghe: lấy link tạm rồi phát trực tiếp từ R2.
 */
@RestController
@RequestMapping("/api/classrooms/{spaceId}/exercise-audio")
@RequiredArgsConstructor
@CrossOrigin(origins = {"http://localhost:3000", "http://127.0.0.1:3000"})
public class ExerciseAudioController extends ClassroomExceptionHandler {

    private final CurrentUserResolver currentUserResolver;
    private final ExerciseAudioService service;
    private final ExerciseListenService listenService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ClassroomApiResponse<AudioInfo>> upload(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String auth,
            @PathVariable UUID spaceId,
            @RequestParam(value = "file", required = false) MultipartFile file) {
        CurrentActor actor = currentUserResolver.resolve(auth);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ClassroomApiResponse.ok("Đã tải file nghe lên.", service.upload(actor, spaceId, file)));
    }

    /** Link nghe tạm (hết hạn sau expiresInSeconds giây). */
    @GetMapping("/{audioId}/url")
    public ClassroomApiResponse<AudioUrl> url(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String auth,
            @PathVariable UUID spaceId, @PathVariable UUID audioId) {
        CurrentActor actor = currentUserResolver.resolve(auth);
        return ClassroomApiResponse.ok("Tải link nghe thành công.", service.url(actor, spaceId, audioId));
    }

    /** Học viên: số lần đã nghe các câu nghe của lượt đang làm (máy chủ đếm). */
    @GetMapping("/listen")
    public ClassroomApiResponse<List<ListenStatus>> listenStatus(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String auth,
            @PathVariable UUID spaceId, @RequestParam UUID attemptId) {
        CurrentActor actor = currentUserResolver.resolve(auth);
        return ClassroomApiResponse.ok("Tải số lần nghe thành công.", listenService.status(actor, spaceId, attemptId));
    }

    /** Học viên bắt đầu một lần nghe trong lúc làm bài: máy chủ cộng 1 lần nghe rồi cấp link tạm. */
    @PostMapping("/listen")
    public ClassroomApiResponse<ListenGrant> listen(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String auth,
            @PathVariable UUID spaceId, @RequestBody(required = false) ListenRequest request) {
        CurrentActor actor = currentUserResolver.resolve(auth);
        ListenRequest r = request == null ? new ListenRequest(null, null, false) : request;
        return ClassroomApiResponse.ok("Tải link nghe thành công.",
                listenService.listen(actor, spaceId, r.attemptId(), r.questionId(), Boolean.TRUE.equals(r.resume())));
    }
}
