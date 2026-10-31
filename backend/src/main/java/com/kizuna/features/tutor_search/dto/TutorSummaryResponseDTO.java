package com.kizuna.features.tutor_search.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * DTO dữ liệu tóm tắt của gia sư hiển thị trên thẻ kết quả tìm kiếm (Tutor Card).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TutorSummaryResponseDTO {
    private UUID tutorId;
    private String fullName;
    private String avatarUrl;
    private String headline;
    private String bio;
    private String highestJlptLevel;
    private Short experienceYears;
    private BigDecimal hourlyRate;
    private BigDecimal avgRating;
    private Integer totalReviews;
    private String location;
    private boolean isFeatured;
    private boolean isVerified;
    private List<String> specialties;
    private String teachingFormat;
}

