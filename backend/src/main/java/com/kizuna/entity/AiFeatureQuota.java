package com.kizuna.entity;

import jakarta.persistence.*;
import lombok.*;
import java.util.UUID;

@Entity
@Table(name = "ai_feature_quotas")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AiFeatureQuota extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "quotaId")
    private UUID quotaId;
}
