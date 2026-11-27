package com.kizuna.features.verification.entity;

import com.kizuna.core.entity.Tutor;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcType;
import org.hibernate.dialect.PostgreSQLEnumJdbcType;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Giấy tờ xác minh của gia sư (SRS: TutorApplicationDocument).
 * Bảng tutor_credentials không có created_at/updated_at nên không kế thừa BaseEntity.
 */
@Entity
@Table(name = "tutor_credentials")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TutorCredential {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "credential_id", updatable = false, nullable = false)
    private UUID credentialId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tutor_id", nullable = false)
    private Tutor tutor;

    @Convert(converter = CredentialTypeConverter.class)
    @Column(name = "credential_type", length = 50, nullable = false)
    private CredentialType credentialType;

    @Column(name = "title", length = 150, nullable = false)
    private String title;

    /**
     * Khóa tệp ngẫu nhiên trong kho lưu trữ; không bao giờ trả ra API.
     */
    @Column(name = "file_url", length = 500, nullable = false)
    private String storageKey;

    @Enumerated(EnumType.STRING)
    @Column(name = "verified_status", columnDefinition = "tutor_credential_status_enum", nullable = false)
    @JdbcType(PostgreSQLEnumJdbcType.class)
    @Builder.Default
    private TutorCredentialStatus verifiedStatus = TutorCredentialStatus.PENDING;

    @Column(name = "rejection_reason", columnDefinition = "TEXT")
    private String rejectionReason;

    @Column(name = "uploaded_at", nullable = false)
    private OffsetDateTime uploadedAt;

    @PrePersist
    void onCreate() {
        if (uploadedAt == null) {
            uploadedAt = OffsetDateTime.now();
        }
    }
}
