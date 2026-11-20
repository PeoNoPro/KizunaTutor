package com.kizuna.features.progress.service.impl;

import com.kizuna.features.progress.service.ExerciseAiReviewService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * FE-15: mỗi phút chấm các câu tự luận đang chờ AI. Tắt bằng app.exercise-ai.enabled=false.
 * Bảng gợi ý chưa được tạo (chưa chạy migration 036) thì chỉ cảnh báo một lần, không làm phiền log.
 */
@Slf4j
@Component
public class ExerciseAiGradingWorker {

    private final ExerciseAiReviewService service;
    private final boolean enabled;
    private boolean warnedMissingTable;

    public ExerciseAiGradingWorker(ExerciseAiReviewService service,
                                   @Value("${app.exercise-ai.enabled:true}") boolean enabled) {
        this.service = service;
        this.enabled = enabled;
    }

    @Scheduled(initialDelayString = "${app.exercise-ai.initial-delay-ms:30000}",
            fixedDelayString = "${app.exercise-ai.poll-ms:60000}")
    public void run() {
        if (!enabled) return;
        try {
            int processed = service.processPending();
            warnedMissingTable = false;
            if (processed > 0) log.info("AI đã xử lý {} câu tự luận", processed);
        } catch (DataAccessException e) {
            if (!warnedMissingTable) {
                log.warn("AI chấm bài tạm dừng: chưa đọc được bảng exercise_ai_reviews (đã chạy migration 036 chưa?) - {}",
                        e.getClass().getSimpleName());
                warnedMissingTable = true;
            }
        } catch (RuntimeException e) {
            log.warn("AI chấm bài gặp lỗi: {}", e.getClass().getSimpleName());
        }
    }
}
