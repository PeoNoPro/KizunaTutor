package com.kizuna.features.progress.service;

import com.kizuna.features.classroom.util.CurrentActor;
import com.kizuna.features.progress.dto.ExerciseAiReviewResponse;

import java.util.List;
import java.util.UUID;

/**
 * FE-15: AI gợi ý chấm câu tự luận (không có đáp án) của bài làm trực tiếp. AI chỉ gợi ý;
 * điểm chính thức vẫn do gia sư lưu bằng chức năng chấm của FE-07.
 */
public interface ExerciseAiReviewService {

    /** Bộ xử lý nền: chấm các câu đang chờ AI; trả số câu máy này đã xử lý. */
    int processPending();

    /** Gợi ý từng câu của một lượt — chỉ gia sư của lớp. */
    List<ExerciseAiReviewResponse> list(CurrentActor actor, UUID spaceId, UUID exerciseId, UUID attemptId);

    /** Cho AI chấm lại các câu "chưa chấm được" / bị kẹt — gia sư được chấm điểm lớp này. */
    List<ExerciseAiReviewResponse> retry(CurrentActor actor, UUID spaceId, UUID exerciseId, UUID attemptId);
}
