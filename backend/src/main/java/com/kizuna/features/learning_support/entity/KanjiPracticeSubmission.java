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
@Table(name = "kanji_practice_submissions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class KanjiPracticeSubmission extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "practiceId")
    private UUID practiceId;
}
