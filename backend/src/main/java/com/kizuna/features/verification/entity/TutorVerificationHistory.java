package com.kizuna.features.verification.entity;

import com.kizuna.core.entity.Tutor;
import com.kizuna.core.entity.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcType;
import org.hibernate.dialect.PostgreSQLEnumJdbcType;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Mục lịch sử thẩm định gia sư. Chỉ ghi thêm, không sửa hoặc xóa.
 * Ghi chú nội bộ (notes) không bao giờ hiển thị cho gia sư.
 */
@Entity
@Table(name = "tutor_verification_histories")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TutorVerificationHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "history_id", updatable = false, nullable = false)
    private UUID historyId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tutor_id", nullable = false, updatable = false)
    private Tutor tutor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "officer_id", updatable = false)
    private User officer;

    @Enumerated(EnumType.STRING)
    @Column(name = "action", columnDefinition = "tutor_verif_action_enum", nullable = false, updatable = false)
    @JdbcType(PostgreSQLEnumJdbcType.class)
    private TutorVerifAction action;

    @Column(name = "notes", columnDefinition = "TEXT", updatable = false)
    private String internalNote;

    @Column(name = "feedback", columnDefinition = "TEXT", updatable = false)
    private String feedback;

    @Enumerated(EnumType.STRING)
    @Column(name = "reason_code", length = 50, updatable = false)
    private RejectionReason reasonCode;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;
}
