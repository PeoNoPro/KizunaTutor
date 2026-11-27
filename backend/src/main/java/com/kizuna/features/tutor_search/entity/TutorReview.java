package com.kizuna.features.tutor_search.entity;



import com.kizuna.core.entity.*;
import com.kizuna.core.entity.BaseEntity;
import com.kizuna.core.entity.User;
import com.kizuna.core.entity.Learner;
import com.kizuna.core.entity.Tutor;
import jakarta.persistence.*;
import lombok.*;
import java.util.UUID;

@Entity
@Table(name = "tutor_reviews")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TutorReview extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "reviewId")
    private UUID reviewId;
}
