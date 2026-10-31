package com.kizuna.features.tutor_search.service;

import com.kizuna.features.tutor_search.dto.TutorSearchCriteriaDTO;
import com.kizuna.features.tutor_search.dto.TutorSearchPageResponse;
import com.kizuna.features.tutor_search.dto.TutorSummaryResponseDTO;

import java.util.List;
import java.util.UUID;

/**
 * Interface nghiệp vụ tìm kiếm và khám phá gia sư (FE-04).
 */
public interface TutorSearchService {

    /**
     * Tìm kiếm và lọc danh sách gia sư có phân trang.
     */
    TutorSearchPageResponse searchTutors(TutorSearchCriteriaDTO criteria);

    /**
     * Lấy danh sách 6 gia sư tiêu biểu (Featured Tutors) có điểm đánh giá cao nhất.
     */
    List<TutorSummaryResponseDTO> getFeaturedTutors();

    /**
     * Lấy chi tiết hồ sơ công khai của một gia sư theo ID.
     */
    TutorSummaryResponseDTO getTutorDetail(UUID tutorId);
}

