package com.kizuna.features.learning_support.service;

import com.kizuna.features.learning_support.dto.KanjiCardDto;
import com.kizuna.features.learning_support.entity.KanjiDictionary;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;
import java.util.UUID;

public interface KanjiDictionaryService {
    Page<KanjiCardDto> searchKanji(String keyword, String jlpt, Short strokeCount, Short minStroke, Short maxStroke, Pageable pageable);
    Optional<KanjiDictionary> findByCharacter(String character);
    Optional<KanjiDictionary> findById(UUID id);
}
