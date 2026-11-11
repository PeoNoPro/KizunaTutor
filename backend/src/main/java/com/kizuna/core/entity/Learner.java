package com.kizuna.core.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Entity Học Viên (Learner) của nền tảng KizunaTutor.
 * Liên kết 1-1 với thực thể User vai trò ROLE_LEARNER.
 */
@Entity
@Table(name = "learners")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Learner {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "learner_id", updatable = false, nullable = false)
    private UUID learnerId;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(name = "current_level", length = 10)
    private String currentLevel;

    @Column(name = "target_level", length = 10)
    private String targetLevel;

    @Column(name = "learning_goals", columnDefinition = "TEXT")
    private String learningGoals;

    /** Tỉnh/Thành phố (database/migrations/017_learner_address.sql). */
    @Column(name = "province", length = 100)
    private String province;

    /** Địa chỉ chi tiết: số nhà, đường, phường/xã, quận/huyện. Chỉ học viên xem được. */
    @Column(name = "address", length = 255)
    private String address;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;
}
