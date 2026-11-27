package com.kizuna.features.tutor_search.entity;

import com.kizuna.core.entity.Tutor;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalTime;
import java.util.UUID;

/**
 * Thực thể lưu trữ khung giờ rảnh giảng dạy của gia sư theo các ngày trong tuần (UC-31).
 * Bảng: tutor_availabilities
 */
@Entity
@Table(name = "tutor_availabilities")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TutorAvailability {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "availability_id")
    private UUID availabilityId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tutor_id", nullable = false)
    private Tutor tutor;

    /**
     * Ngày trong tuần: 1 = Chủ nhật, 2 = Thứ hai, 3 = Thứ ba, 4 = Thứ tư, 5 = Thứ năm, 6 = Thứ sáu, 7 = Thứ bảy
     */
    @Column(name = "day_of_week", nullable = false)
    private Integer dayOfWeek;

    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalTime endTime;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;
}
