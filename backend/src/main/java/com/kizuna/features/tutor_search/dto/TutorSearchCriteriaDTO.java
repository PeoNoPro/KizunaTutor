package com.kizuna.features.tutor_search.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * DTO chứa các tiêu chí tìm kiếm và lọc gia sư (FE-04).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TutorSearchCriteriaDTO {
    private String keyword;
    private String jlptLevel;
    private String goal;
    private String format;
    private BigDecimal minPrice;
    private BigDecimal maxPrice;
    private String schedule;
    
    @Builder.Default
    private String sortBy = "rating";
    
    @Builder.Default
    private int page = 0;
    
    @Builder.Default
    private int size = 10;
}

