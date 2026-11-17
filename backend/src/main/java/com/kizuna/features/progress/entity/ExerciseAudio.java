package com.kizuna.features.progress.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * File nghe của câu hỏi bài làm trực tiếp (bảng exercise_audio, migration 031).
 * Nội dung file nằm trên Cloudflare R2 (storageKey, vd "audio/&lt;uuid&gt;.mp3"); DB không lưu dữ liệu nhị phân.
 * exercise_id NULL = gia sư vừa tải lên chưa lưu bài, hoặc câu hỏi đã bỏ file / bài đã bị xóa (sẽ được dọn).
 */
@Entity
@Table(name = "exercise_audio")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExerciseAudio {

    @Id
    @Column(name = "audio_id", updatable = false, nullable = false)
    private UUID audioId;

    @Column(name = "space_id", nullable = false, updatable = false)
    private UUID spaceId;

    @Column(name = "exercise_id")
    private UUID exerciseId;

    @Column(name = "file_name", nullable = false, length = 255)
    private String fileName;

    @Column(name = "file_size", nullable = false)
    private Long fileSize;

    @Column(name = "content_type", nullable = false, length = 100)
    private String contentType;

    @Column(name = "storage_key", nullable = false, updatable = false, length = 500)
    private String storageKey;

    @Column(name = "created_at", insertable = false, updatable = false)
    private OffsetDateTime createdAt;
}
