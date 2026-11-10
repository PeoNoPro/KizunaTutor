package com.kizuna.features.progress.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.kizuna.features.classroom.dto.ClassroomApiResponse;
import com.kizuna.features.classroom.exception.ClassroomExceptionHandler;
import com.kizuna.features.classroom.util.CurrentActor;
import com.kizuna.features.classroom.util.CurrentUserResolver;
import com.kizuna.features.progress.dto.ExerciseDtos.*;
import com.kizuna.features.progress.service.OnlineExerciseService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Bài làm trực tiếp trên web (FE-07). Mọi endpoint yêu cầu header Authorization: Bearer &lt;token&gt;.
 * Gia sư: soạn / sửa / xóa đề, xem bài làm, chấm. Học viên: bắt đầu / làm tiếp, tự lưu, nộp, xem kết quả.
 */
@RestController
@RequestMapping("/api/classrooms/{spaceId}/exercises")
@RequiredArgsConstructor
@CrossOrigin(origins = {"http://localhost:3000", "http://127.0.0.1:3000"})
public class OnlineExerciseController extends ClassroomExceptionHandler {

    private final CurrentUserResolver currentUserResolver;
    private final OnlineExerciseService service;

    @GetMapping
    public ClassroomApiResponse<List<ExerciseSummary>> list(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String auth,
            @PathVariable UUID spaceId) {
        CurrentActor actor = currentUserResolver.resolve(auth);
        return ClassroomApiResponse.ok("Tải danh sách bài làm trực tiếp thành công.", service.list(actor, spaceId));
    }

    @PostMapping
    public ResponseEntity<ClassroomApiResponse<ExerciseDetail>> create(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String auth,
            @PathVariable UUID spaceId, @RequestBody ExerciseRequest request) {
        CurrentActor actor = currentUserResolver.resolve(auth);
        ExerciseDetail saved = service.create(actor, spaceId, request);
        String message = "PUBLISHED".equals(saved.summary().status()) ? "Giao bài làm trực tiếp thành công." : "Đã lưu nháp.";
        return ResponseEntity.status(HttpStatus.CREATED).body(ClassroomApiResponse.ok(message, saved));
    }

    @GetMapping("/{exerciseId}")
    public ClassroomApiResponse<ExerciseDetail> detail(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String auth,
            @PathVariable UUID spaceId, @PathVariable UUID exerciseId) {
        CurrentActor actor = currentUserResolver.resolve(auth);
        return ClassroomApiResponse.ok("Tải bài làm trực tiếp thành công.", service.detail(actor, spaceId, exerciseId));
    }

    @PutMapping("/{exerciseId}")
    public ClassroomApiResponse<ExerciseDetail> update(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String auth,
            @PathVariable UUID spaceId, @PathVariable UUID exerciseId, @RequestBody ExerciseRequest request) {
        CurrentActor actor = currentUserResolver.resolve(auth);
        return ClassroomApiResponse.ok("Đã lưu bài làm trực tiếp.", service.update(actor, spaceId, exerciseId, request));
    }

    @DeleteMapping("/{exerciseId}")
    public ClassroomApiResponse<Void> delete(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String auth,
            @PathVariable UUID spaceId, @PathVariable UUID exerciseId) {
        CurrentActor actor = currentUserResolver.resolve(auth);
        service.delete(actor, spaceId, exerciseId);
        return ClassroomApiResponse.ok("Đã xóa bài làm trực tiếp.", null);
    }

    /** Học viên bắt đầu lượt mới hoặc nhận lại lượt đang làm dở. */
    @PostMapping("/{exerciseId}/attempts")
    public ClassroomApiResponse<AttemptView> start(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String auth,
            @PathVariable UUID spaceId, @PathVariable UUID exerciseId) {
        CurrentActor actor = currentUserResolver.resolve(auth);
        return ClassroomApiResponse.ok("Bắt đầu làm bài.", service.start(actor, spaceId, exerciseId));
    }

    @GetMapping("/{exerciseId}/attempts")
    public ClassroomApiResponse<List<AttemptView>> attempts(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String auth,
            @PathVariable UUID spaceId, @PathVariable UUID exerciseId) {
        CurrentActor actor = currentUserResolver.resolve(auth);
        return ClassroomApiResponse.ok("Tải các lượt làm bài thành công.", service.attempts(actor, spaceId, exerciseId));
    }

    @GetMapping("/{exerciseId}/attempts/{attemptId}")
    public ClassroomApiResponse<AttemptView> attempt(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String auth,
            @PathVariable UUID spaceId, @PathVariable UUID exerciseId, @PathVariable UUID attemptId) {
        CurrentActor actor = currentUserResolver.resolve(auth);
        return ClassroomApiResponse.ok("Tải lượt làm bài thành công.", service.attempt(actor, spaceId, exerciseId, attemptId));
    }

    /** Tự lưu câu trả lời một câu (body = JSON câu trả lời theo dạng câu). */
    @PutMapping("/{exerciseId}/attempts/{attemptId}/answers/{questionId}")
    public ClassroomApiResponse<Void> saveAnswer(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String auth,
            @PathVariable UUID spaceId, @PathVariable UUID exerciseId, @PathVariable UUID attemptId,
            @PathVariable UUID questionId, @RequestBody(required = false) JsonNode response) {
        CurrentActor actor = currentUserResolver.resolve(auth);
        service.saveAnswer(actor, spaceId, exerciseId, attemptId, questionId, response);
        return ClassroomApiResponse.ok("Đã lưu.", null);
    }

    @PostMapping("/{exerciseId}/attempts/{attemptId}/submit")
    public ClassroomApiResponse<AttemptView> submit(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String auth,
            @PathVariable UUID spaceId, @PathVariable UUID exerciseId, @PathVariable UUID attemptId) {
        CurrentActor actor = currentUserResolver.resolve(auth);
        return ClassroomApiResponse.ok("Nộp bài thành công.", service.submit(actor, spaceId, exerciseId, attemptId));
    }

    @PutMapping("/{exerciseId}/attempts/{attemptId}/grade")
    public ClassroomApiResponse<AttemptView> grade(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String auth,
            @PathVariable UUID spaceId, @PathVariable UUID exerciseId, @PathVariable UUID attemptId,
            @RequestBody GradeAttemptRequest request) {
        CurrentActor actor = currentUserResolver.resolve(auth);
        return ClassroomApiResponse.ok("Đã lưu điểm.", service.grade(actor, spaceId, exerciseId, attemptId, request));
    }
}
