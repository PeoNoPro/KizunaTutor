package com.kizuna.features.learning_support.service.impl;

import com.kizuna.features.learning_support.dto.KanjiCardDto;
import com.kizuna.features.learning_support.entity.KanjiDictionary;
import com.kizuna.features.learning_support.repository.KanjiDictionaryRepository;
import com.kizuna.features.learning_support.service.KanjiDictionaryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class KanjiDictionaryServiceImpl implements KanjiDictionaryService {

    private final KanjiDictionaryRepository kanjiRepository;

    @Override
    @Transactional(readOnly = true)
    public Page<KanjiCardDto> searchKanji(String keyword, String jlpt, Short strokeCount, Short minStroke, Short maxStroke, Pageable pageable) {
        String jlptParam = (jlpt != null && !jlpt.isBlank() && !jlpt.equalsIgnoreCase("ALL")) ? jlpt.trim().toUpperCase() : null;
        String kwParam = (keyword != null && !keyword.isBlank()) ? keyword.trim() : null;

        Page<KanjiDictionary> paged;
        if (minStroke != null || maxStroke != null) {
            paged = kanjiRepository.searchKanjiByStrokeRange(kwParam, jlptParam, minStroke, maxStroke, pageable);
        } else {
            paged = kanjiRepository.searchKanjiPaginated(kwParam, jlptParam, strokeCount, pageable);
        }

        return paged.map(this::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<KanjiDictionary> findByCharacter(String character) {
        if (character == null || character.isBlank()) return Optional.empty();
        return kanjiRepository.findByKanjiCharacter(character.trim().substring(0, 1));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<KanjiDictionary> findById(UUID id) {
        return kanjiRepository.findById(id);
    }

    private KanjiCardDto toDto(KanjiDictionary k) {
        return KanjiCardDto.builder()
                .id(k.getKanjiId())
                .kanjiChar(k.getKanjiCharacter())
                .unicodeHex(k.getUnicodeHex())
                .strokeCount(k.getStrokeCount())
                .radical(k.getRadical())
                .hanViet(k.getHanViet() != null ? k.getHanViet() : "HÁN TỰ")
                .onyomi(k.getOnyomi() != null ? k.getOnyomi() : "—")
                .kunyomi(k.getKunyomi() != null ? k.getKunyomi() : "—")
                .meaning(k.getMeaning() != null ? k.getMeaning() : "")
                .jlpt(k.getJlptLevel() != null ? k.getJlptLevel().name() : "N5")
                .build();
    }
}
