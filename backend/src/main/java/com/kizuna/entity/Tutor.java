package com.kizuna.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcType;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.dialect.PostgreSQLEnumJdbcType;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Entity Gia Sư (Tutor) của nền tảng KizunaTutor.
 * Liên kết 1-1 với thực thể User vai trò ROLE_TUTOR.
 */
@Entity
@Table(name = "tutors")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Tutor {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "tutor_id", updatable = false, nullable = false)
    private UUID tutorId;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(name = "headline", length = 200)
    private String headline;

    @Column(name = "bio", columnDefinition = "TEXT")
    private String bio;

    @Column(name = "experience_years", nullable = false)
    @Builder.Default
    private Short experienceYears = 0;

    @Column(name = "hourly_rate", precision = 12, scale = 2, nullable = false)
    @Builder.Default
    private BigDecimal hourlyRate = BigDecimal.ZERO;

    @Column(name = "highest_jlpt_level", length = 10)
    private String highestJlptLevel;

    @Column(name = "avg_rating", precision = 3, scale = 2, nullable = false)
    @Builder.Default
    private BigDecimal avgRating = BigDecimal.ZERO;

    @Column(name = "total_reviews", nullable = false)
    @Builder.Default
    private Integer totalReviews = 0;

    @Enumerated(EnumType.STRING)
    @Column(name = "verification_status", columnDefinition = "tutor_verification_status_enum", nullable = false)
    @JdbcType(PostgreSQLEnumJdbcType.class)
    @Builder.Default
    private TutorVerificationStatus verificationStatus = TutorVerificationStatus.APPROVED;

    @Enumerated(EnumType.STRING)
    @Column(name = "membership_status", columnDefinition = "tutor_membership_status_enum", nullable = false)
    @JdbcType(PostgreSQLEnumJdbcType.class)
    @Builder.Default
    private TutorMembershipStatus membershipStatus = TutorMembershipStatus.PREMIUM;

    @Column(name = "is_featured", nullable = false)
    @Builder.Default
    private Boolean isFeatured = false;

    @Enumerated(EnumType.STRING)
    @Column(name = "tutor_state", length = 30, nullable = false)
    @Builder.Default
    private TutorState tutorState = TutorState.DRAFT_PROFILE;

    @Column(name = "phone", length = 20)
    private String phone;

    @Column(name = "date_of_birth")
    private java.time.LocalDate dateOfBirth;

    @Column(name = "address", length = 255)
    private String address;

    @Column(name = "certificate_urls", columnDefinition = "TEXT")
    private String certificateUrls;

    @Column(name = "verification_feedback", columnDefinition = "TEXT")
    private String verificationFeedback;

    @Column(name = "submitted_at")
    private OffsetDateTime submittedAt;

    @Column(name = "verified_at")
    private OffsetDateTime verifiedAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;
}
