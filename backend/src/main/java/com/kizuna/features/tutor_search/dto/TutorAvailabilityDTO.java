package com.kizuna.features.tutor_search.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * DTO chứa thông tin khung giờ và ngày rảnh giảng dạy của gia sư (UC-31).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TutorAvailabilityDTO {
    private UUID availabilityId;
    private Integer dayOfWeek;
    private String dayName;
    private String startTime;
    private String endTime;
}

