package com.kizuna;

import com.kizuna.features.tutor_search.dto.TutorSearchCriteriaDTO;
import com.kizuna.features.tutor_search.dto.TutorSearchPageResponse;
import com.kizuna.features.tutor_search.dto.TutorSummaryResponseDTO;
import com.kizuna.features.tutor_search.service.TutorSearchService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotNull;

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
        System.out.println("Search result count: " + response.getTotalElements());
    }

    @Test
    public void testGetFeaturedTutors() {
        List<TutorSummaryResponseDTO> featured = tutorSearchService.getFeaturedTutors();
        assertNotNull(featured);
        System.out.println("Featured tutors count: " + featured.size());
    }
}

