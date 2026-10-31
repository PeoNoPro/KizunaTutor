package com.kizuna.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

/**
 * Entity lưu trữ từ vựng theo từng chủ đề đời sống, kết nối với KanjiDictionary
 * và các phân hệ Flashcards, Practice Canvas.
 */
@Entity
@Table(name = "topic_vocabularies")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TopicVocabulary extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "topic_id", nullable = false)
    private Topic topic;

    @Column(name = "word", length = 100, nullable = false)
    private String word;

    @Column(name = "furigana", length = 150)
    private String furigana;

    @Column(name = "romaji", length = 150)
    private String romaji;

    @Column(name = "han_viet", length = 150)
    private String hanViet;

    @Column(name = "meaning_vn", length = 500, nullable = false)
    private String meaningVn;

    @Column(name = "jlpt_level", length = 20)
    private String jlptLevel;

    @Column(name = "example_jp", length = 500)
    private String exampleJp;

    @Column(name = "example_vn", length = 500)
    private String exampleVn;

    /**
     * Danh sách chữ Kanji cấu thành từ vựng, lưu dạng chuỗi phân cách bởi dấu phẩy (vd: "日,本")
     * hoặc chuỗi JSON để frontend dễ tách chữ và gửi sang Shodo practice-canvas.
     */
    @Column(name = "kanji_characters", length = 100)
    private String kanjiCharacters;
}
