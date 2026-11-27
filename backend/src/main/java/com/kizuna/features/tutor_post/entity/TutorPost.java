package com.kizuna.features.tutor_post.entity;

import com.kizuna.core.entity.Tutor;
import com.kizuna.core.entity.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcType;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.util.List;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.dialect.PostgreSQLEnumJdbcType;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Một lần gửi bài đăng của gia sư (bảng tutor_posts — data-model FE-09 §6 + migration 015).
 * Bài hiển thị cho học viên là dòng PUBLISHED mới nhất của gia sư; mỗi gia sư có tối đa một dòng PENDING_REVIEW.
 */
@Entity
@Table(name = "tutor_posts")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TutorPost {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "post_id", updatable = false, nullable = false)
    private UUID postId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tutor_id", nullable = false, updatable = false)
    private Tutor tutor;

    @Column(name = "title", length = 150, nullable = false)
    private String title;

    @Column(name = "content", columnDefinition = "TEXT", nullable = false)
    private String content;

    @Column(name = "specialties", length = 255)
    private String specialties;

    @Column(name = "teaching_format", length = 20)
    private String teachingFormat;

    /** Học phí thấp nhất trong bảng giá (trang Tìm gia sư lọc / sắp xếp và hiện "Từ ..." theo cột này). */
    @Column(name = "hourly_rate", precision = 12, scale = 2)
    private BigDecimal hourlyRate;

    /** Bảng học phí theo môn học / cấp độ (migration 017). Bài cũ chưa có bảng giá thì null. */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "price_items", columnDefinition = "jsonb")
    private List<PostPriceItem> priceItems;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", columnDefinition = "tutor_post_status_enum", nullable = false)
    @JdbcType(PostgreSQLEnumJdbcType.class)
    private TutorPostStatus status;

    @Column(name = "rejection_reason", columnDefinition = "TEXT")
    private String rejectionReason;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewed_by")
    private User reviewedBy;

    @Column(name = "reviewed_at")
    private OffsetDateTime reviewedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    /** Thời điểm gửi gần nhất (gửi lại khi đang chờ duyệt sẽ cập nhật cột này). */
    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;
}
