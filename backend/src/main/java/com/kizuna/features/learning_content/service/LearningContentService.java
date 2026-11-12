package com.kizuna.features.learning_content.service;

import com.kizuna.core.dto.PageResponse;
import com.kizuna.features.learning_content.dto.ContentImportResponse;
import com.kizuna.features.learning_content.dto.ContentSummaryResponse;
import com.kizuna.features.learning_content.dto.ImportHistoryDetailResponse;
import com.kizuna.features.learning_content.dto.ImportHistoryResponse;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;
import java.util.UUID;

/**
 * FE-13: cán bộ học vụ xem kho học liệu (Kanji, từ vựng, ngữ pháp), nhập mục mới từ Excel và xem lịch sử nhập.
 * typePath: kanji | vocabulary | grammar. Mục đã có trong kho luôn giữ nguyên.
 */
public interface LearningContentService {

    long MAX_FILE_BYTES = 15L * 1024 * 1024;

    /** Số mục hiện có và cột cần có của từng loại. */
    ContentSummaryResponse summary();

    /** sort: LEVEL (N5 → N1) | NEWEST; jlpt: N1..N5 hoặc trống. */
    PageResponse<Map<String, Object>> listItems(String typePath, String keyword, String jlpt, String sort,
                                                int page, int size);

    /** dryRun = true chỉ kiểm tra, không ghi; false thì thêm mục mới và ghi lịch sử trong một giao dịch. */
    ContentImportResponse importFile(String typePath, MultipartFile file, boolean dryRun, UUID officerId,
                                     String officerEmail);

    /** typePath trống / ALL = mọi loại. */
    PageResponse<ImportHistoryResponse> history(String typePath, int page, int size);

    ImportHistoryDetailResponse historyDetail(UUID id);
}
