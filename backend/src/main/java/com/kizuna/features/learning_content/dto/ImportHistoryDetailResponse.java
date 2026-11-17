package com.kizuna.features.learning_content.dto;

import java.util.List;

/** Chi tiết một lần nhập: các mục đã thêm (tối đa 2.000 mục được lưu). */
public record ImportHistoryDetailResponse(ImportHistoryResponse item, List<String> addedItems, boolean addedTruncated) {
}
