package com.kizuna.features.tutor_search.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO bọc phản hồi phân trang cho danh sách tìm kiếm gia sư.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TutorSearchPageResponse {
    private List<TutorSummaryResponseDTO> content;
    private int pageNumber;
    private int pageSize;
    private long totalElements;
    private int totalPages;
    private boolean last;
}

