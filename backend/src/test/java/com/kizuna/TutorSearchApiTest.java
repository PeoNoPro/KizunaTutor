package com.kizuna;

import com.kizuna.features.tutor_search.dto.TutorSearchCriteriaDTO;
import com.kizuna.features.tutor_search.dto.TutorSearchPageResponse;
import com.kizuna.features.tutor_search.dto.TutorSummaryResponseDTO;
import com.kizuna.features.tutor_search.service.TutorSearchService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
public class TutorSearchApiTest {

    @Autowired
    private TutorSearchService tutorSearchService;

    @Test
    public void testSearchTutorsWithEmptyCriteria() {
        TutorSearchCriteriaDTO criteria = TutorSearchCriteriaDTO.builder()
                .page(0)
                .size(10)
                .build();

        TutorSearchPageResponse response = tutorSearchService.searchTutors(criteria);
        assertNotNull(response);
        assertNotNull(response.getContent());
        assertFalse(response.getContent().isEmpty());
        assertNotNull(response.getContent().get(0).getTutorCode());
    }

    @Test
    public void testGetFeaturedTutors() {
        List<TutorSummaryResponseDTO> featured = tutorSearchService.getFeaturedTutors();
        assertNotNull(featured);
        assertFalse(featured.isEmpty());
    }

    @Test
    public void testGetTutorByCode_Success() {
        TutorSummaryResponseDTO tutor = tutorSearchService.getTutorByCode(10001L);
        assertNotNull(tutor, "Phải tìm thấy gia sư với mã 10001");
        assertEquals(10001L, tutor.getTutorCode());
        assertNotNull(tutor.getTutorId());
    }

    @Test
    public void testGetTutorByCode_NotFound() {
        TutorSummaryResponseDTO tutor = tutorSearchService.getTutorByCode(99999L);
        assertNull(tutor, "Không tìm thấy gia sư với mã không tồn tại");
    }
}
