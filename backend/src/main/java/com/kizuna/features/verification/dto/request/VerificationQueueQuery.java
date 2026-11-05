package com.kizuna.features.verification.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

/**
 * Bộ lọc hàng đợi hồ sơ (UC-25, FR-011, FR-012).
 * status: PENDING (mặc định) | ADDITIONAL_INFO_REQUIRED | APPROVED | REJECTED | ALL (mọi hồ sơ đã từng nộp).
 * sort: oldest (mặc định, cũ nhất trước) | newest.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VerificationQueueQuery {
    private String status;
    private String keyword;
    private String jlptLevel;
    private LocalDate submittedFrom;
    private LocalDate submittedTo;
    private String sort;
    private Integer page;
    private Integer size;
}
