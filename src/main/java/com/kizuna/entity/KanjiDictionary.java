package com.kizuna.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcType;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.dialect.PostgreSQLEnumJdbcType;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "kanji_dictionary")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class KanjiDictionary {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "kanji_id", updatable = false, nullable = false)
    private UUID kanjiId;

    @Column(name = "kanji_character", length = 1, nullable = false, unique = true)
    private String kanjiCharacter;

    @Column(name = "unicode_hex", length = 5, nullable = false, unique = true)
    private String unicodeHex;

    @Column(name = "radical", length = 4)
    private String radical;

    @Column(name = "stroke_count", nullable = false)
    private Short strokeCount;

    @Column(name = "han_viet", length = 50)
    private String hanViet;

    @Column(name = "onyomi", length = 150)
    private String onyomi;

    @Column(name = "kunyomi", length = 300)
    private String kunyomi;

    @Column(name = "meaning", length = 200)
    private String meaning;

    @Enumerated(EnumType.STRING)
    @Column(name = "jlpt_level", columnDefinition = "jlpt_level_enum")
    @JdbcType(PostgreSQLEnumJdbcType.class)
    private JlptLevel jlptLevel;

    @Column(name = "svg_data", columnDefinition = "TEXT")
    private String svgData;

    @Column(name = "stroke_order_svg_url", length = 500)
    private String strokeOrderSvgUrl;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @Transient
    public String getCharacter() {
        return kanjiCharacter;
    }

    @Transient
    public String getKanji() {
        return kanjiCharacter;
    }

    @Transient
    public String getKanjiChar() {
        return kanjiCharacter;
    }

    @Transient
    public String getJlpt() {
        return jlptLevel != null ? jlptLevel.name() : "N5";
    }

    @Transient
    public Integer getTotalStrokes() {
        return strokeCount != null ? strokeCount.intValue() : 1;
    }
}
