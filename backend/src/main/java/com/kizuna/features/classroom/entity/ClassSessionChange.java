package com.kizuna.features.classroom.entity;

import com.kizuna.core.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

/**
 * Thay đổi riêng một buổi học chính thức trong tuần (bảng class_session_changes, migration 015 + 016):
 * - cancelled = false: buổi được dời sang newDate, startTime–endTime (cùng tuần);
 * - cancelled = true: buổi bị hủy (không tính vào khóa, khóa tự bù thêm buổi ở cuối).
 * Buổi gốc = buổi của lịch hằng tuần {@link #schedule} rơi vào ngày {@link #originalDate}. Các tuần khác giữ nguyên.
 */
@Entity
@Table(name = "class_session_changes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClassSessionChange extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "change_id", updatable = false, nullable = false)
    private UUID changeId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "schedule_id", nullable = false, updatable = false)
    private ClassSchedule schedule;

    @Column(name = "original_date", nullable = false, updatable = false)
    private LocalDate originalDate;

    @Column(name = "new_date")
    private LocalDate newDate;

    @Column(name = "start_time")
    private LocalTime startTime;

    @Column(name = "end_time")
    private LocalTime endTime;

    @Column(name = "cancelled", nullable = false)
    private boolean cancelled;
}
