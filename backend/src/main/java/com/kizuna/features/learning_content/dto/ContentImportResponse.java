package com.kizuna.features.learning_content.dto;

import java.util.List;

/**
 * Kết quả kiểm tra (dryRun = true) hoặc nhập thật. created là số sẽ thêm khi chỉ kiểm tra;
 * skippedExisting là số mục đã có (luôn giữ nguyên).
 */
public record ContentImportResponse(
        String type,
        String typeLabel,
        String fileName,
        boolean dryRun,
        List<String> sheets,
        int totalRows,
        int created,
        int skippedExisting,
        int duplicatesInFile,
        int invalidRows,
        List<ImportIssueResponse> issues,
        boolean issuesTruncated,
        String message) {
}
