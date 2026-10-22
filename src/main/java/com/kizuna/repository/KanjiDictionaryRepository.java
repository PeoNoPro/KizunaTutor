package com.kizuna.repository;

import com.kizuna.entity.KanjiDictionary;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
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
}
