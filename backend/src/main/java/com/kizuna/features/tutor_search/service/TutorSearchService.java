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
     * Lấy chi tiết hồ sơ công khai của một gia sư theo UUID (ID gốc).
     */
    TutorSummaryResponseDTO getTutorDetail(UUID tutorId);

    /**
     * Lấy chi tiết hồ sơ công khai của một gia sư theo mã tự tăng (tutor_code).
     */
    TutorSummaryResponseDTO getTutorByCode(Long tutorCode);

    /**
     * Lấy danh sách các tỉnh/thành phố có gia sư thực tế trong hệ thống.
     */
    List<String> getAvailableLocations();

    /**
     * Khung giờ còn trống của gia sư cho từng ngày trong một tuần (tuần hiện tại đến 3 tuần sau).
     * @param weekStart ngày bất kỳ trong tuần cần xem (null = tuần hiện tại)
     */
    com.kizuna.features.tutor_search.dto.TutorWeekScheduleDTO getWeeklySchedule(UUID tutorId, java.time.LocalDate weekStart);
}
