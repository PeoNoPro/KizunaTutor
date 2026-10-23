package com.kizuna.repository;

import com.kizuna.entity.KanjiDictionary;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface KanjiDictionaryRepository extends JpaRepository<KanjiDictionary, UUID> {

    Optional<KanjiDictionary> findByKanjiCharacter(String kanjiCharacter);

    Optional<KanjiDictionary> findByUnicodeHex(String unicodeHex);

    boolean existsByUnicodeHex(String unicodeHex);

    boolean existsByKanjiCharacter(String kanjiCharacter);

    List<KanjiDictionary> findByStrokeCountBetween(Short min, Short max, Pageable pageable);

    @Query(value = "SELECT * FROM kanji_dictionary k WHERE " +
           "(:jlpt IS NULL OR :jlpt = '' OR :jlpt = 'ALL' OR CAST(k.jlpt_level AS text) = :jlpt) AND " +
           "(:strokeCount IS NULL OR k.stroke_count = :strokeCount) AND " +
           "(:keyword IS NULL OR :keyword = '' OR " +
           " k.kanji_character ILIKE CONCAT('%', :keyword, '%') OR " +
           " k.han_viet ILIKE CONCAT('%', :keyword, '%') OR " +
           " k.meaning ILIKE CONCAT('%', :keyword, '%') OR " +
           " k.onyomi ILIKE CONCAT('%', :keyword, '%') OR " +
           " k.kunyomi ILIKE CONCAT('%', :keyword, '%') OR " +
           " k.unicode_hex ILIKE CONCAT('%', :keyword, '%')) " +
           "ORDER BY k.stroke_count ASC, k.unicode_hex ASC",
           countQuery = "SELECT count(*) FROM kanji_dictionary k WHERE " +
           "(:jlpt IS NULL OR :jlpt = '' OR :jlpt = 'ALL' OR CAST(k.jlpt_level AS text) = :jlpt) AND " +
           "(:strokeCount IS NULL OR k.stroke_count = :strokeCount) AND " +
           "(:keyword IS NULL OR :keyword = '' OR " +
           " k.kanji_character ILIKE CONCAT('%', :keyword, '%') OR " +
           " k.han_viet ILIKE CONCAT('%', :keyword, '%') OR " +
           " k.meaning ILIKE CONCAT('%', :keyword, '%') OR " +
           " k.onyomi ILIKE CONCAT('%', :keyword, '%') OR " +
           " k.kunyomi ILIKE CONCAT('%', :keyword, '%') OR " +
           " k.unicode_hex ILIKE CONCAT('%', :keyword, '%'))",
           nativeQuery = true)
    Page<KanjiDictionary> searchKanjiPaginated(
            @Param("keyword") String keyword,
            @Param("jlpt") String jlpt,
            @Param("strokeCount") Short strokeCount,
            Pageable pageable
    );
}
