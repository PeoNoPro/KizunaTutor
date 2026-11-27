package com.kizuna.features.tutor_search.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO chứa thông tin buổi học thử đã được đặt trước của gia sư (giờ Việt Nam).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TutorBookedSlotDTO {
    private String date;      // "dd/MM/yyyy" (ví dụ: 13/11/2026)
    private String startTime; // "HH:mm" (ví dụ: 18:00)
    private String endTime;   // "HH:mm" (ví dụ: 18:30)
}

