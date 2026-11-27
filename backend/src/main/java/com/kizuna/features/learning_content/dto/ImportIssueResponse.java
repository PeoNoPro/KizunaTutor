package com.kizuna.features.learning_content.dto;

/** Một dòng lỗi / cảnh báo của tệp. level: ERROR (dòng bị bỏ qua) | WARNING; row đếm từ 1 như Excel. */
public record ImportIssueResponse(String sheet, int row, String level, String message) {

    public static ImportIssueResponse error(String sheet, int row, String message) {
        return new ImportIssueResponse(sheet, row, "ERROR", message);
    }

    public static ImportIssueResponse warning(String sheet, int row, String message) {
        return new ImportIssueResponse(sheet, row, "WARNING", message);
    }
}
