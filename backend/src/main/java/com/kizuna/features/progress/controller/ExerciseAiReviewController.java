package com.kizuna.features.progress.controller;

import com.kizuna.features.classroom.dto.ClassroomApiResponse;
import com.kizuna.features.classroom.exception.ClassroomExceptionHandler;
import com.kizuna.features.classroom.util.CurrentActor;
import com.kizuna.features.classroom.util.CurrentUserResolver;
import com.kizuna.features.progress.dto.ExerciseAiReviewResponse;
import com.kizuna.features.progress.service.ExerciseAiReviewService;
import com.kizuna.features.progress.service.impl.ExerciseAiReviewServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * FE-15: gợi ý chấm của AI cho câu tự luận trong một lượt làm bài trực tiếp. Chỉ gia sư của lớp.
 * Điểm chính thức vẫn lưu bằng PUT .../grade của FE-07.
 */
@RestController
@RequestMapping("/api/classrooms/{spaceId}/exercises/{exerciseId}/attempts/{attemptId}/ai-reviews")
@RequiredArgsConstructor
@CrossOrigin(origins = {"http://localhost:3000", "http://127.0.0.1:3000"})
public class ExerciseAiReviewController extends ClassroomExceptionHandler {

    private final CurrentUserResolver currentUserResolver;
    private final ExerciseAiReviewService service;

    @GetMapping
    public ClassroomApiResponse<List<ExerciseAiReviewResponse>> list(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String auth,
            @PathVariable UUID spaceId, @PathVariable UUID exerciseId, @PathVariable UUID attemptId) {
        CurrentActor actor = currentUserResolver.resolve(auth);
        return ClassroomApiResponse.ok(ExerciseAiReviewServiceImpl.MSG_LIST,
                service.list(actor, spaceId, exerciseId, attemptId));
    }

    @PostMapping("/retry")
    public ClassroomApiResponse<List<ExerciseAiReviewResponse>> retry(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String auth,
            @PathVariable UUID spaceId, @PathVariable UUID exerciseId, @PathVariable UUID attemptId) {
        CurrentActor actor = currentUserResolver.resolve(auth);
        return ClassroomApiResponse.ok(ExerciseAiReviewServiceImpl.MSG_RETRY,
                service.retry(actor, spaceId, exerciseId, attemptId));
    }
}
