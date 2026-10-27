package com.kizuna.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Entity Gói Thành Viên Gia Sư (Membership Plan - UC 71, 72)
 */
@Entity
@Table(name = "membership_plans")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MembershipPlan {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "plan_id", updatable = false, nullable = false)
    private UUID planId;

    @Column(name = "plan_name", length = 100, nullable = false)
    private String planName;

    @Column(name = "duration_months", nullable = false)
    private Integer durationMonths;

    @Column(name = "price", precision = 12, scale = 2, nullable = false)
    private BigDecimal price;

    @Column(name = "features_json", columnDefinition = "JSONB")
    private String featuresJson;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    public String getName() {
        return planName;
    }

    public String getCode() {
        return "PLAN_" + (durationMonths != null ? durationMonths : 1) + "M";
    }

    public String getDescription() {
        return "Kích hoạt " + (durationMonths != null ? durationMonths : 1) + " tháng quyền giảng dạy & không gian Kizuna";
    }
}
