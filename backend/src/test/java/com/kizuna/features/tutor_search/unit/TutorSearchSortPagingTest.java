package com.kizuna.features.tutor_search.unit;

import com.kizuna.features.tutor_search.dto.TutorSearchCriteriaDTO;
import com.kizuna.features.tutor_search.dto.TutorSearchPageResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit test FE-05 / UC-05.01 Search, UC-05.03 Sort - TutorSearchServiceImpl.searchTutors(criteria).
 * Kiểm tra thứ tự sắp xếp và phân trang gửi xuống repository, và cách trả trang kết quả.
 * Điều kiện hiển thị và bộ lọc (Specification) chạy trên DB nên kiểm tra ở test tích hợp, không ở đây.
 */
class TutorSearchSortPagingTest extends TutorSearchUnitTestBase {

    @SuppressWarnings("unchecked")
    private Pageable search(TutorSearchCriteriaDTO criteria) {
        when(repo.findAll(any(Specification.class), any(Pageable.class)))
                .thenAnswer(inv -> new PageImpl<>(List.of(), (Pageable) inv.getArgument(1), 0));
        service.searchTutors(criteria);
        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        verify(repo).findAll(any(Specification.class), pageable.capture());
        return pageable.getValue();
    }

    private static TutorSearchCriteriaDTO sortBy(String sort) {
        TutorSearchCriteriaDTO c = new TutorSearchCriteriaDTO();
        c.setSortBy(sort);
        return c;
    }

    @Test @DisplayName("UTCID01: Mặc định sắp xếp theo điểm đánh giá giảm dần, rồi số lượt đánh giá")
    void utcid01_defaultRating() {
        Pageable p = search(new TutorSearchCriteriaDTO());
        assertEquals(Sort.by(Sort.Direction.DESC, "avgRating").and(Sort.by(Sort.Direction.DESC, "totalReviews")), p.getSort());
    }

    @Test @DisplayName("UTCID02: Điểm bằng nhau -> tiêu chí phụ là số lượt đánh giá giảm dần")
    void utcid02_tieBreakByReviews() {
        Sort.Order second = search(sortBy("rating")).getSort().toList().get(1);
        assertEquals("totalReviews", second.getProperty());
        assertTrue(second.isDescending());
    }

    @Test @DisplayName("UTCID03: Nhiều kinh nghiệm nhất -> experienceYears giảm dần")
    void utcid03_experience() {
        assertEquals(Sort.by(Sort.Direction.DESC, "experienceYears"), search(sortBy("exp")).getSort());
    }

    @Test @DisplayName("UTCID04: Giá thấp đến cao -> hourlyRate tăng dần")
    void utcid04_priceAsc() {
        assertEquals(Sort.by(Sort.Direction.ASC, "hourlyRate"), search(sortBy("price_asc")).getSort());
    }

    @Test @DisplayName("UTCID05: Giá cao đến thấp -> hourlyRate giảm dần")
    void utcid05_priceDesc() {
        assertEquals(Sort.by(Sort.Direction.DESC, "hourlyRate"), search(sortBy("price_desc")).getSort());
    }

    @Test @DisplayName("UTCID06: Mới nhất -> hiện sắp theo experienceYears giảm dần (giống 'nhiều kinh nghiệm', theo code hiện tại)")
    void utcid06_newest() {
        assertEquals(Sort.by(Sort.Direction.DESC, "experienceYears"), search(sortBy("newest")).getSort());
    }

    @Test @DisplayName("UTCID07: Giá trị sắp xếp lạ -> sắp theo điểm đánh giá")
    void utcid07_unknownSort() {
        assertEquals("avgRating", search(sortBy("abc")).getSort().toList().get(0).getProperty());
    }

    @Test @DisplayName("UTCID08: Không chọn sắp xếp (null) -> sắp theo điểm đánh giá")
    void utcid08_nullSort() {
        assertEquals("avgRating", search(sortBy(null)).getSort().toList().get(0).getProperty());
    }

    @Test @DisplayName("UTCID09: Mặc định trang 0, 10 kết quả mỗi trang")
    void utcid09_defaultPaging() {
        Pageable p = search(new TutorSearchCriteriaDTO());
        assertEquals(0, p.getPageNumber());
        assertEquals(10, p.getPageSize());
    }

    @Test @DisplayName("UTCID10: Trang âm hoặc size 0 -> trang 0, size 1 (không lỗi)")
    void utcid10_safePaging() {
        TutorSearchCriteriaDTO c = new TutorSearchCriteriaDTO();
        c.setPage(-3);
        c.setSize(0);
        Pageable p = search(c);
        assertEquals(0, p.getPageNumber());
        assertEquals(1, p.getPageSize());
    }

    @Test @DisplayName("UTCID11: 11 kết quả, trang 2 -> trả số trang, tổng và cờ trang cuối từ repository")
    @SuppressWarnings("unchecked")
    void utcid11_pageMetadata() {
        Page<com.kizuna.core.entity.Tutor> page = new PageImpl<>(List.of(), PageRequest.of(1, 10), 11);
        when(repo.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);
        TutorSearchCriteriaDTO c = new TutorSearchCriteriaDTO();
        c.setPage(1);

        TutorSearchPageResponse res = service.searchTutors(c);

        assertEquals(1, res.getPageNumber());
        assertEquals(11, res.getTotalElements());
        assertEquals(2, res.getTotalPages());
        assertTrue(res.isLast());
    }

    @Test @DisplayName("UTCID12: Không có gia sư khớp -> danh sách rỗng, tổng 0")
    void utcid12_noResult() {
        search(new TutorSearchCriteriaDTO());
        TutorSearchPageResponse res = service.searchTutors(new TutorSearchCriteriaDTO());
        assertTrue(res.getContent().isEmpty());
        assertEquals(0, res.getTotalElements());
    }

    @Test @DisplayName("UTCID13: Có kết quả -> thẻ gia sư có tên, tiêu đề, JLPT, điểm, học phí")
    @SuppressWarnings("unchecked")
    void utcid13_cardContent() {
        when(repo.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(tutor(TUTOR_ID, "Nguyen Van A", "Hà Nội - Cầu Giấy")), PageRequest.of(0, 10), 1));

        var card = service.searchTutors(new TutorSearchCriteriaDTO()).getContent().get(0);

        assertEquals("Nguyen Van A", card.getFullName());
        assertEquals("JLPT teacher", card.getHeadline());
        assertEquals("N1", card.getHighestJlptLevel());
        assertEquals(0, new java.math.BigDecimal("4.5").compareTo(card.getAvgRating()));
        assertEquals(0, new java.math.BigDecimal("200000").compareTo(card.getHourlyRate()));
    }
}
