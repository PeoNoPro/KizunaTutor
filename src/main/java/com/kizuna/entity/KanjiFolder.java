package com.kizuna.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Entity lưu trữ thông tin Thư mục / Bộ thẻ học tập Kanji của Người dùng.
 * Cho phép phân loại chữ Kanji theo chủ đề, cấp độ JLPT hoặc mục tiêu cá nhân.
 */
@Entity
@Table(name = "kanji_folders")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class KanjiFolder extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "folder_id", updatable = false, nullable = false)
    private UUID folderId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "folder_name", length = 100, nullable = false)
    private String folderName;

    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "color", length = 30)
    @Builder.Default
    private String color = "#EF4444";

    @Column(name = "is_default", nullable = false)
    @Builder.Default
    private Boolean isDefault = false;

    @OneToMany(mappedBy = "folder", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<KanjiFolderItem> items = new ArrayList<>();
}
