package com.kizuna.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Entity đại diện cho Chủ đề học tập từ vựng thực tế đời sống Nhật Bản
 * (Giao tiếp, Ẩm thực, Mua sắm, Đi lại, Công việc...).
 */
@Entity
@Table(name = "topics")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Topic extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "code", length = 50, nullable = false, unique = true)
    private String code;

    @Column(name = "title_vn", length = 150, nullable = false)
    private String titleVn;

    @Column(name = "title_jp", length = 150)
    private String titleJp;

    @Column(name = "icon", length = 20)
    private String icon;

    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "color_token", length = 50)
    private String colorToken;

    @Column(name = "display_order", nullable = false)
    @Builder.Default
    private Integer displayOrder = 0;

    @OneToMany(mappedBy = "topic", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<TopicVocabulary> vocabularies = new ArrayList<>();
}
