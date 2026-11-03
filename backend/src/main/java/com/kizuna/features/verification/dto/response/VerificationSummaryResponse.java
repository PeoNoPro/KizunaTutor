package com.kizuna.features.verification.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

/**
 * Số liệu bảng điều khiển thẩm định (III.3.1.1, FR-010). Tất cả đếm trực tiếp từ DB.
 */
@Getter
@Builder
@AllArgsConstructor
public class VerificationSummaryResponse {
    private long totalTutors;
    private long notSubmittedCount;
    private long pendingCount;
    private long pendingNewToday;
    private long pendingOver24h;
    private long needsInfoCount;
    private long approvedCount;
    private long rejectedCount;
    private long approvedThisMonth;
    private long rejectedThisMonth;
}
