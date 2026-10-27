package com.kizuna.entity;

import jakarta.persistence.*;
import lombok.*;
import java.util.UUID;

@Entity
@Table(name = "trial_booking_histories")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TrialBookingHistory extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "historyId")
    private UUID historyId;
}
