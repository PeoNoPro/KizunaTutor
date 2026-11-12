package com.kizuna.features.learning_content.dto;

import java.util.List;

/** Số mục hiện có và cột cần có của một loại học liệu. */
public record ContentTypeSummaryResponse(String type, String label, long total, List<String> requiredColumns,
                                         List<String> optionalColumns) {
}
