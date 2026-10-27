package com.kizuna.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcType;
import org.hibernate.dialect.PostgreSQLEnumJdbcType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Entity Quản lý Gói Thành Viên của Gia Sư (Tutor Membership - UC 73, 75, 77)
 */
@Entity
@Table(name = "tutor_memberships")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TutorMembership {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "membership_id", updatable = false, nullable = false)
    private UUID membershipId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tutor_id", nullable = false)
    private Tutor tutor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "plan_id", nullable = false)
    private MembershipPlan plan;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", columnDefinition = "tutor_membership_state_enum", nullable = false)
    @JdbcType(PostgreSQLEnumJdbcType.class)
    @Builder.Default
    private TutorMembershipState status = TutorMembershipState.ACTIVE;

    @Column(name = "amount", precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(name = "transaction_ref", length = 100)
    private String transactionRef;

    @Column(name = "payment_status", length = 30)
    @Builder.Default
    private String paymentStatus = "SUCCESS";

    @Column(name = "payment_gateway", length = 50)
    @Builder.Default
    private String paymentGateway = "VNPay";

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;
}
