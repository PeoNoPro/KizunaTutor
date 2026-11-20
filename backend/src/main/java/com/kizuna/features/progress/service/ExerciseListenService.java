package com.kizuna.features.progress.service;

import com.kizuna.features.classroom.util.CurrentActor;
import com.kizuna.features.progress.dto.ExerciseDtos.ListenGrant;
import com.kizuna.features.progress.dto.ExerciseDtos.ListenStatus;

import java.util.List;
import java.util.UUID;

/**
 * Giới hạn số lần nghe của câu nghe (FE-07), đếm ở máy chủ theo từng lượt làm bài.
 * Học viên đang làm bài chỉ lấy được link nghe của câu có giới hạn qua {@link #listen}; mỗi lần cấp link tính 1 lần nghe.
 */
public interface ExerciseListenService {

    /** Số lần đã nghe các câu nghe của một lượt làm bài (học viên của lượt). */
    List<ListenStatus> status(CurrentActor actor, UUID spaceId, UUID attemptId);

    /** Cấp link nghe cho một lần nghe; hết lượt, hết giờ hoặc bài đã nộp thì báo lỗi. */
    ListenGrant listen(CurrentActor actor, UUID spaceId, UUID attemptId, UUID questionId, boolean resume);

    /**
     * File nghe thuộc câu có giới hạn số lần nghe và học viên chưa nộp lượt nào / đang làm bài
     * → không được lấy link trực tiếp (phải qua {@link #listen}). Đã nộp xong thì xem lại thoải mái.
     */
    boolean requiresCounting(UUID learnerId, UUID exerciseId, UUID audioId);
}
