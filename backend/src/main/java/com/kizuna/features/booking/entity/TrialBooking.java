package com.kizuna.features.booking.entity;

import com.kizuna.core.entity.BaseEntity;
import com.kizuna.core.entity.Learner;
import com.kizuna.core.entity.Tutor;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcType;
import org.hibernate.dialect.PostgreSQLEnumJdbcType;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Entity Buổi Học Thử (TrialBooking)
 * Ánh xạ bảng trial_bookings trong CSDL.
 */
@Entity
@Table(name = "trial_bookings")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TrialBooking extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "booking_id", updatable = false, nullable = false)
    private UUID bookingId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "request_id")
    private ConnectionRequest connectionRequest;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "learner_id", nullable = false)
    private Learner learner;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tutor_id", nullable = false)
    private Tutor tutor;

    @Column(name = "scheduled_start_time", nullable = false)
    private OffsetDateTime scheduledStartTime;

    @Column(name = "scheduled_end_time", nullable = false)
    private OffsetDateTime scheduledEndTime;

    @Column(name = "meeting_link", length = 500)
    private String meetingLink;

    @Enumerated(EnumType.STRING)
    @JdbcType(PostgreSQLEnumJdbcType.class)
    @Column(name = "booking_status", nullable = false)
    private TrialBookingStatus bookingStatus;

    @Column(name = "learner_confirmed", nullable = false)
    @Builder.Default
    private Boolean learnerConfirmed = false;

    @Column(name = "tutor_confirmed", nullable = false)
    @Builder.Default
    private Boolean tutorConfirmed = false;

    @Column(name = "official_arrangement_agreed", nullable = false)
    @Builder.Default
    private Boolean officialArrangementAgreed = false;

    @Column(name = "policy_revision_id")
    private UUID policyRevisionId;

    @Column(name = "policy_duration_minutes")
    private Integer policyDurationMinutes;

    @Column(name = "policy_min_advance_hours")
    private Integer policyMinAdvanceHours;
}
