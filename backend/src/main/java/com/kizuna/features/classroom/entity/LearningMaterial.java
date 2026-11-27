package com.kizuna.features.classroom.entity;

import com.kizuna.core.entity.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Entity Tài Liệu Học Tập (FE-06) thuộc một Learning Space.
 * Ánh xạ đúng bảng learning_materials (không có updated_at nên không kế thừa BaseEntity).
 * Nội dung tệp nằm ở cột file_data (BYTEA) và cố ý KHÔNG ánh xạ ở đây để danh sách không tải dữ liệu nhị phân;
 * đọc/ghi qua LearningMaterialRepository. fileUrl lưu tên tệp gốc đã làm sạch (dùng khi mở/tải về).
 */
@Entity
@Table(name = "learning_materials")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LearningMaterial {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "material_id", updatable = false, nullable = false)
    private UUID materialId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "space_id", nullable = false)
    private LearningSpace space;

    @Column(name = "title", length = 150, nullable = false)
    private String title;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "file_url", length = 500, nullable = false)
    private String fileUrl;

    @Column(name = "file_type", length = 50)
    private String fileType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "uploaded_by")
    private User uploadedBy;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;
}
