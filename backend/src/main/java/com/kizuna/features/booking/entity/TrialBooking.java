package com.kizuna.features.booking.entity;

import com.kizuna.core.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import java.util.UUID;

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
    @Column(name = "booking_id")
    private UUID bookingId;
    @Column(name = "policy_revision_id") private UUID policyRevisionId;
    @Column(name = "policy_duration_minutes") private Integer policyDurationMinutes;
    @Column(name = "policy_min_advance_hours") private Integer policyMinAdvanceHours;
}
