package com.kizuna.features.tutor_search.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * DTO đại diện cho một đánh giá nhận xét từ học viên dành cho gia sư.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TutorReviewDTO {
    private UUID reviewId;
    private String learnerName;
    private String learnerAvatarUrl;
    private String courseName;
    private BigDecimal rating;
    private String comment;
    private String createdAt;
    private String tag;
}

