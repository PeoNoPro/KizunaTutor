package com.kizuna.features.classroom.entity;

import com.kizuna.core.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

/**
 * Buổi học gia sư thêm riêng trong một tuần (bảng class_extra_sessions, migration 016).
 * Không ảnh hưởng các tuần khác; được tính vào tổng số buổi của khóa.
 */
@Entity
@Table(name = "class_extra_sessions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClassExtraSession extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "extra_id", updatable = false, nullable = false)
    private UUID extraId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "space_id", nullable = false, updatable = false)
    private LearningSpace space;

    @Column(name = "session_date", nullable = false)
    private LocalDate sessionDate;

    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalTime endTime;
}
