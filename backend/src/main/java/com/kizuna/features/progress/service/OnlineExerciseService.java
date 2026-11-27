package com.kizuna.features.progress.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.kizuna.features.classroom.util.CurrentActor;
import com.kizuna.features.progress.dto.ExerciseDtos.AttemptView;
import com.kizuna.features.progress.dto.ExerciseDtos.ExerciseDetail;
import com.kizuna.features.progress.dto.ExerciseDtos.ExerciseRequest;
import com.kizuna.features.progress.dto.ExerciseDtos.ExerciseSummary;
import com.kizuna.features.progress.dto.ExerciseDtos.GradeAttemptRequest;

import java.util.List;
import java.util.UUID;

/** Bài làm trực tiếp trên web (FE-07): gia sư soạn đề, học viên làm, hệ thống / gia sư chấm. */
public interface OnlineExerciseService {

    /** Gia sư thấy mọi bài (kể cả nháp); học viên chỉ thấy bài đã giao và đã tới giờ mở. */
    List<ExerciseSummary> list(CurrentActor actor, UUID spaceId);

    ExerciseDetail detail(CurrentActor actor, UUID spaceId, UUID exerciseId);

    ExerciseDetail create(CurrentActor actor, UUID spaceId, ExerciseRequest request);

    ExerciseDetail update(CurrentActor actor, UUID spaceId, UUID exerciseId, ExerciseRequest request);

    void delete(CurrentActor actor, UUID spaceId, UUID exerciseId);

    /** Học viên bắt đầu lượt mới, hoặc nhận lại lượt đang làm dở. */
    AttemptView start(CurrentActor actor, UUID spaceId, UUID exerciseId);

    /** Gia sư: mọi lượt (kèm đáp án). Học viên: các lượt của mình. */
    List<AttemptView> attempts(CurrentActor actor, UUID spaceId, UUID exerciseId);

    AttemptView attempt(CurrentActor actor, UUID spaceId, UUID exerciseId, UUID attemptId);

    /** Học viên tự lưu câu trả lời một câu trong lúc làm bài. */
    void saveAnswer(CurrentActor actor, UUID spaceId, UUID exerciseId, UUID attemptId, UUID questionId, JsonNode response);

    /** Học viên nộp bài (đã xác nhận). Nộp rồi không sửa được. */
    AttemptView submit(CurrentActor actor, UUID spaceId, UUID exerciseId, UUID attemptId);

    /** Gia sư chấm câu tự luận / sửa điểm từng câu của một lượt. */
    AttemptView grade(CurrentActor actor, UUID spaceId, UUID exerciseId, UUID attemptId, GradeAttemptRequest request);
}
