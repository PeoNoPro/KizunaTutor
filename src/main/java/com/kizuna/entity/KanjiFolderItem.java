package com.kizuna.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Entity liên kết giữa Thư mục thẻ Kanji và Từ điển KanjiDictionary.
 * Lưu trữ danh sách các chữ Hán thuộc từng thư mục kèm thứ tự và ghi chú.
 */
@Entity
@Table(name = "kanji_folder_items",
       uniqueConstraints = @UniqueConstraint(columnNames = {"folder_id", "kanji_id"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class KanjiFolderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "item_id", updatable = false, nullable = false)
    private UUID itemId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "folder_id", nullable = false)
    private KanjiFolder folder;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "kanji_id", nullable = false)
    private KanjiDictionary kanji;

    @Column(name = "order_index", nullable = false)
    @Builder.Default
    private Integer orderIndex = 0;

    @Column(name = "notes", length = 255)
    private String notes;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;
}
