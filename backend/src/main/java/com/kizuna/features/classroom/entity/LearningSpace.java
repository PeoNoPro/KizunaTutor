package com.kizuna.features.classroom.entity;



import com.kizuna.core.entity.*;
import com.kizuna.core.entity.BaseEntity;
import com.kizuna.core.entity.User;
import com.kizuna.core.entity.Learner;
import com.kizuna.core.entity.Tutor;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcType;
import org.hibernate.dialect.PostgreSQLEnumJdbcType;

import java.util.UUID;

/**
 * Entity Không Gian Học Tập (Learning Space) kết nối giữa Gia Sư và Học Viên.
 * Chỉ học viên nằm trong Learning Space có status = ACTIVE mới được mở khóa
 * phân hệ "Học tập & Luyện tập" chuyên sâu (Shodo Canvas, Bộ Thẻ, Minigame).
 */
@Entity
@Table(name = "learning_spaces")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LearningSpace extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "space_id", updatable = false, nullable = false)
    private UUID spaceId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tutor_id", nullable = false)
    private Tutor tutor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "learner_id", nullable = false)
    private Learner learner;

    @Column(name = "title", length = 150, nullable = false)
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", columnDefinition = "learning_space_status_enum", nullable = false)
    @JdbcType(PostgreSQLEnumJdbcType.class)
    @Builder.Default
    private LearningSpaceStatus status = LearningSpaceStatus.ACTIVE;
}
