package com.kizuna.features.classroom.entity;

import com.kizuna.core.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Một buổi học cố định hằng tuần của lớp (bảng class_schedules, migration 012).
 * dayOfWeek: 1 = Thứ Hai ... 7 = Chủ nhật.
 * activeFrom / activeUntil (migration 016): buổi hằng tuần chỉ sinh ra các buổi có giờ bắt đầu trong khoảng này.
 * Xóa / đổi giờ một buổi hằng tuần = kết thúc hiệu lực từ thời điểm đó (activeUntil), các buổi đã diễn ra vẫn được giữ.
 */
@Entity
@Table(name = "class_schedules")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClassSchedule extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "schedule_id", updatable = false, nullable = false)
    private UUID scheduleId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "space_id", nullable = false, updatable = false)
    private LearningSpace space;

    @Column(name = "day_of_week", nullable = false)
    private Integer dayOfWeek;

    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalTime endTime;

    @Column(name = "active_from", nullable = false)
    private OffsetDateTime activeFrom;

    /** null = đang hiệu lực. */
    @Column(name = "active_until")
    private OffsetDateTime activeUntil;
}
