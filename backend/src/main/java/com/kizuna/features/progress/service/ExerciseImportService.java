package com.kizuna.features.progress.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.kizuna.features.classroom.util.CurrentActor;
import com.kizuna.features.progress.entity.ExerciseQuestionType;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/** Nhập câu hỏi bài làm trực tiếp từ Excel (FE-07): xem trước file và tải file mẫu. */
public interface ExerciseImportService {

    /**
     * Một dòng đã đọc: ok = hợp lệ (question có dữ liệu chuẩn hóa), ngược lại error là lý do.
     * listening = câu nghe (cần thêm file nghe trên trình soạn trước khi giao bài); maxPlays null = không giới hạn.
     */
    record ImportRow(int row, boolean ok, String error, ExerciseQuestionType type, String prompt,
                     BigDecimal points, JsonNode data, boolean listening, Integer maxPlays) {
    }

    record ImportPreview(int total, int valid, int invalid, BigDecimal validPoints, List<ImportRow> rows) {
    }

    /** Đọc và kiểm tra file Excel của gia sư lớp; không lưu gì. */
    ImportPreview preview(CurrentActor actor, UUID spaceId, MultipartFile file);

    /** File Excel mẫu (.xlsx) kèm ví dụ và hướng dẫn. */
    byte[] template(CurrentActor actor, UUID spaceId);
}
