package com.kizuna.features.booking.entity;

import com.kizuna.core.entity.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcType;
import org.hibernate.dialect.PostgreSQLEnumJdbcType;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Entity Lịch Sử Thay Đổi Trạng Thái Học Thử (TrialBookingHistory)
 * Ánh xạ bảng trial_booking_histories trong CSDL.
 */
@Entity
@Table(name = "trial_booking_histories")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TrialBookingHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "history_id", updatable = false, nullable = false)
    private UUID historyId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booking_id", nullable = false)
    private TrialBooking trialBooking;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "changed_by_user_id")
    private User changedByUser;

    @Enumerated(EnumType.STRING)
    @JdbcType(PostgreSQLEnumJdbcType.class)
    @Column(name = "previous_status")
    private TrialBookingStatus previousStatus;

    @Enumerated(EnumType.STRING)
    @JdbcType(PostgreSQLEnumJdbcType.class)
    @Column(name = "new_status", nullable = false)
    private TrialBookingStatus newStatus;

    @Column(name = "change_reason", length = 255)
    private String changeReason;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;
}
