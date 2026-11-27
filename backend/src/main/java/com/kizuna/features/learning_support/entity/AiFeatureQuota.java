package com.kizuna.features.learning_support.entity;



import com.kizuna.core.entity.*;
import com.kizuna.core.entity.BaseEntity;
import com.kizuna.core.entity.User;
import com.kizuna.core.entity.Learner;
import com.kizuna.core.entity.Tutor;
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
