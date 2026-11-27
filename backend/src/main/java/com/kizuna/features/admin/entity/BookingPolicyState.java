package com.kizuna.features.admin.entity;
import jakarta.persistence.*;
import lombok.*;
import java.util.*;
import java.time.OffsetDateTime;
@Entity @Table(name="booking_policy_states") @Getter @Setter @NoArgsConstructor
public class BookingPolicyState {
    @Id @Column(name="state_id") private UUID stateId;
     @Column(name="current_revision_id") private UUID currentRevisionId;
    @Version @Column(name="version") private Long version;
}
