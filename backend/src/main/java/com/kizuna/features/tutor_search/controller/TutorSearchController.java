package com.kizuna.features.tutor_search.controller;

import com.kizuna.features.tutor_search.dto.TutorSearchCriteriaDTO;
import com.kizuna.features.tutor_search.dto.TutorSearchPageResponse;
import com.kizuna.features.tutor_search.dto.TutorSummaryResponseDTO;
import com.kizuna.features.tutor_search.service.TutorSearchService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * REST API Controller phục vụ tìm kiếm và khám phá gia sư (FE-04).
 */
@RestController
@RequestMapping("/api/tutors")
@RequiredArgsConstructor
@CrossOrigin(origins = {"http://localhost:3000", "http://127.0.0.1:3000"})
public class TutorSearchController {

    private final TutorSearchService tutorSearchService;

    /**
     * Tìm kiếm và lọc danh sách gia sư đa tiêu chí.
     * Hỗ trợ cả GET /api/tutors và GET /api/tutors/search
     */
    @GetMapping({"", "/search"})
    public ResponseEntity<TutorSearchPageResponse> searchTutors(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String jlptLevel,
            @RequestParam(required = false) String goal,
            @RequestParam(required = false) String format,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(required = false) String schedule,
            @RequestParam(defaultValue = "rating") String sortBy,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        TutorSearchCriteriaDTO criteria = TutorSearchCriteriaDTO.builder()
                .keyword(keyword)
                .jlptLevel(jlptLevel)
                .goal(goal)
                .format(format)
                .minPrice(minPrice)
                .maxPrice(maxPrice)
                .schedule(schedule)
                .sortBy(sortBy)
                .page(page)
                .size(size)
                .build();

        TutorSearchPageResponse response = tutorSearchService.searchTutors(criteria);
        return ResponseEntity.ok(response);
    }

    /**
     * Lấy danh sách gia sư tiêu biểu (Featured Tutors) có đánh giá cao nhất.
     * GET /api/tutors/featured
     */
    @GetMapping("/featured")
    public ResponseEntity<List<TutorSummaryResponseDTO>> getFeaturedTutors() {
        List<TutorSummaryResponseDTO> list = tutorSearchService.getFeaturedTutors();
        return ResponseEntity.ok(list);
    }

    /**
     * Xem thông tin chi tiết một gia sư theo ID.
     * GET /api/tutors/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<TutorSummaryResponseDTO> getTutorDetail(@PathVariable UUID id) {
        TutorSummaryResponseDTO detail = tutorSearchService.getTutorDetail(id);
        return ResponseEntity.ok(detail);
    }
}

