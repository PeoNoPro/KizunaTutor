package com.kizuna.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kizuna.dto.minigame.CreateKanjiFolderRequest;
import com.kizuna.dto.minigame.KanjiFolderSummaryDto;
import com.kizuna.dto.topic.AddVocabToDeckRequest;
import com.kizuna.dto.topic.TopicDto;
import com.kizuna.dto.topic.TopicVocabularyDto;
import com.kizuna.entity.KanjiDictionary;
import com.kizuna.entity.Topic;
import com.kizuna.entity.TopicVocabulary;
import com.kizuna.repository.KanjiDictionaryRepository;
import com.kizuna.repository.TopicRepository;
import com.kizuna.repository.TopicVocabularyRepository;
import com.kizuna.service.IKanjiFolderService;
import com.kizuna.service.ITopicService;
import com.kizuna.service.kanji.HandwritingSearchService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service quản lý chủ đề & từ vựng đời sống Nhật Bản.
 * Hỗ trợ tra cứu linh hoạt theo UUID hoặc theo mã Code (DAILY_COMMUNICATION, FOOD_DINING...).
 * Đồng thời đồng bộ dữ liệu vào cơ sở dữ liệu nếu bảng tồn tại và có In-Memory Cache dự phòng.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TopicServiceImpl implements ITopicService {

    private final TopicRepository topicRepository;
    private final TopicVocabularyRepository vocabularyRepository;
    private final IKanjiFolderService folderService;
    private final KanjiDictionaryRepository kanjiRepository;
    private final HandwritingSearchService handwritingSearchService;
    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    // Map lưu trữ theo cả UUID lẫn mã Code dạng String
    private final List<TopicDto> memoryTopics = new ArrayList<>();
    private final Map<String, List<TopicVocabularyDto>> memoryVocabsByCode = new ConcurrentHashMap<>();
    private final Map<UUID, List<TopicVocabularyDto>> memoryVocabsById = new ConcurrentHashMap<>();
    private boolean isDatabaseReady = false;

    @PostConstruct
    public void init() {
        loadMemoryCache();

        try {
            ensureTablesExist();
            seedTopicsAndVocabulariesIfEmpty();
            isDatabaseReady = true;
            log.info("[TopicService] Database tables verified & connected successfully.");
        } catch (Exception e) {
            log.warn("[TopicService] Không thể kết nối hoặc khởi tạo bảng trên DB (lỗi: {}). Sử dụng In-Memory Cache an toàn.", e.getMessage());
            isDatabaseReady = false;
        }
    }

    private void ensureTablesExist() {
        try {
            jdbcTemplate.execute(
                "CREATE TABLE IF NOT EXISTS topics (" +
                "    id UUID PRIMARY KEY DEFAULT gen_random_uuid()," +
                "    code VARCHAR(50) NOT NULL UNIQUE," +
                "    title_vn VARCHAR(150) NOT NULL," +
                "    title_jp VARCHAR(150)," +
                "    icon VARCHAR(20)," +
                "    description VARCHAR(500)," +
                "    color_token VARCHAR(50)," +
                "    display_order INTEGER NOT NULL DEFAULT 0," +
                "    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP," +
                "    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP" +
                ")"
            );

            jdbcTemplate.execute(
                "CREATE TABLE IF NOT EXISTS topic_vocabularies (" +
                "    id UUID PRIMARY KEY DEFAULT gen_random_uuid()," +
                "    topic_id UUID NOT NULL REFERENCES topics(id) ON DELETE CASCADE ON UPDATE CASCADE," +
                "    word VARCHAR(100) NOT NULL," +
                "    furigana VARCHAR(150)," +
                "    romaji VARCHAR(150)," +
                "    han_viet VARCHAR(150)," +
                "    meaning_vn VARCHAR(500) NOT NULL," +
                "    jlpt_level VARCHAR(20)," +
                "    example_jp VARCHAR(500)," +
                "    example_vn VARCHAR(500)," +
                "    kanji_characters VARCHAR(100)," +
                "    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP," +
                "    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP" +
                ")"
            );
        } catch (Exception e) {
            log.warn("[TopicService] Lỗi khi tạo bảng topics/topic_vocabularies: {}", e.getMessage());
            throw e;
        }
    }

    private void loadMemoryCache() {
        try {
            ClassPathResource res = new ClassPathResource("data/topics-vocabulary.json");
            if (!res.exists()) return;

            try (InputStream is = res.getInputStream()) {
                List<Map<String, Object>> list = objectMapper.readValue(is, new TypeReference<>() {});
                memoryTopics.clear();
                memoryVocabsByCode.clear();
                memoryVocabsById.clear();

                for (Map<String, Object> item : list) {
                    String code = (String) item.get("code");
                    UUID topicId = UUID.nameUUIDFromBytes(code.getBytes());
                    @SuppressWarnings("unchecked")
                    List<Map<String, Object>> vocabs = (List<Map<String, Object>>) item.get("vocabularies");

                    memoryTopics.add(TopicDto.builder()
                            .id(topicId)
                            .code(code)
                            .titleVn((String) item.get("titleVn"))
                            .titleJp((String) item.get("titleJp"))
                            .icon((String) item.get("icon"))
                            .description((String) item.get("description"))
                            .colorToken((String) item.get("colorToken"))
                            .displayOrder((Integer) item.get("displayOrder"))
                            .vocabCount(vocabs != null ? (long) vocabs.size() : 0L)
                            .build());

                    List<TopicVocabularyDto> vList = new ArrayList<>();
                    if (vocabs != null) {
                        for (Map<String, Object> vMap : vocabs) {
                            @SuppressWarnings("unchecked")
                            List<String> kanjiChars = (List<String>) vMap.get("kanjiCharacters");
                            String word = (String) vMap.get("word");
                            UUID vId = UUID.nameUUIDFromBytes((code + "_" + word).getBytes());

                            vList.add(TopicVocabularyDto.builder()
                                    .id(vId)
                                    .topicId(topicId)
                                    .word(word)
                                    .furigana((String) vMap.get("furigana"))
                                    .romaji((String) vMap.get("romaji"))
                                    .hanViet((String) vMap.get("hanViet"))
                                    .meaningVn((String) vMap.get("meaningVn"))
                                    .jlptLevel((String) vMap.get("jlptLevel"))
                                    .exampleJp((String) vMap.get("exampleJp"))
                                    .exampleVn((String) vMap.get("exampleVn"))
                                    .kanjiCharacters(kanjiChars != null ? kanjiChars : List.of())
                                    .build());
                        }
                    }
                    memoryVocabsByCode.put(code, vList);
                    memoryVocabsById.put(topicId, vList);
                }
                log.info("[TopicService] Đã nạp sẵn {} chủ đề vào bộ nhớ đệm In-Memory.", memoryTopics.size());
            }
        } catch (Exception e) {
            log.error("[TopicService] Lỗi khi nạp memory cache: {}", e.getMessage());
        }
    }

    @Override
    public List<TopicDto> getAllTopics() {
        if (isDatabaseReady) {
            try {
                List<Topic> topics = topicRepository.findAllOrderByDisplayOrder();
                if (!topics.isEmpty()) {
                    List<TopicDto> result = new ArrayList<>();
                    for (Topic t : topics) {
                        long count = vocabularyRepository.countByTopicId(t.getId());
                        if (count == 0 && memoryVocabsByCode.containsKey(t.getCode())) {
                            count = memoryVocabsByCode.get(t.getCode()).size();
                        }
                        result.add(TopicDto.builder()
                                .id(t.getId())
                                .code(t.getCode())
                                .titleVn(t.getTitleVn())
                                .titleJp(t.getTitleJp())
                                .icon(t.getIcon())
                                .description(t.getDescription())
                                .colorToken(t.getColorToken())
                                .displayOrder(t.getDisplayOrder())
                                .vocabCount(count)
                                .build());
                    }
                    return result;
                }
            } catch (Exception e) {
                log.warn("Truy vấn DB thất bại, fallback sang bộ nhớ đệm: {}", e.getMessage());
            }
        }
        return new ArrayList<>(memoryTopics);
    }

    @Override
    public Page<TopicVocabularyDto> getVocabulariesByTopic(String topicIdentifier, String keyword, Pageable pageable) {
        String cleanKeyword = (keyword != null && !keyword.trim().isEmpty()) ? keyword.trim() : null;

        // 1. Thử truy vấn qua Database nếu DB sẵn sàng
        if (isDatabaseReady) {
            try {
                UUID targetId = null;
                try {
                    targetId = UUID.fromString(topicIdentifier);
                } catch (IllegalArgumentException notUuid) {
                    Optional<Topic> tOpt = topicRepository.findByCode(topicIdentifier);
                    if (tOpt.isPresent()) {
                        targetId = tOpt.get().getId();
                    }
                }

                if (targetId != null) {
                    Page<TopicVocabulary> page = vocabularyRepository.findByTopicIdWithSearch(targetId, cleanKeyword, pageable);
                    if (page != null && page.getTotalElements() > 0) {
                        return page.map(this::mapToDto);
                    }
                }
            } catch (Exception e) {
                log.warn("Truy vấn DB từ vựng thất bại, fallback sang bộ nhớ đệm: {}", e.getMessage());
            }
        }

        // 2. Fallback tìm kiếm linh hoạt trong Memory Cache:
        // Tìm theo mã code (DAILY_COMMUNICATION...), theo UUID chuỗi, hoặc tìm topic trong memoryTopics
        List<TopicVocabularyDto> list = null;
        if (memoryVocabsByCode.containsKey(topicIdentifier)) {
            list = memoryVocabsByCode.get(topicIdentifier);
        } else {
            try {
                UUID uId = UUID.fromString(topicIdentifier);
                if (memoryVocabsById.containsKey(uId)) {
                    list = memoryVocabsById.get(uId);
                }
            } catch (Exception ignored) {}
        }

        // Nếu vẫn chưa thấy (ví dụ truyền topicId sinh ngẫu nhiên từ DB), map qua code của topic đó
        if (list == null || list.isEmpty()) {
            if (isDatabaseReady) {
                try {
                    UUID uId = UUID.fromString(topicIdentifier);
                    Optional<Topic> t = topicRepository.findById(uId);
                    if (t.isPresent() && memoryVocabsByCode.containsKey(t.get().getCode())) {
                        list = memoryVocabsByCode.get(t.get().getCode());
                    }
                } catch (Exception ignored) {}
            }
        }

        if (list == null) {
            list = List.of();
        }

        if (cleanKeyword != null) {
            String kw = cleanKeyword.toLowerCase();
            list = list.stream().filter(v ->
                    (v.getWord() != null && v.getWord().toLowerCase().contains(kw)) ||
                    (v.getMeaningVn() != null && v.getMeaningVn().toLowerCase().contains(kw)) ||
                    (v.getRomaji() != null && v.getRomaji().toLowerCase().contains(kw)) ||
                    (v.getHanViet() != null && v.getHanViet().toLowerCase().contains(kw))
            ).toList();
        }

        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), list.size());
        List<TopicVocabularyDto> subList = (start <= end && start < list.size()) ? list.subList(start, end) : List.of();
        return new PageImpl<>(subList, pageable, list.size());
    }

    @Override
    @Transactional
    public Map<String, Object> addVocabularyToDeck(UUID userId, UUID vocabId, AddVocabToDeckRequest request) {
        Map<String, Object> response = new HashMap<>();

        TopicVocabularyDto vocabDto = null;
        if (isDatabaseReady) {
            Optional<TopicVocabulary> vOpt = vocabularyRepository.findById(vocabId);
            if (vOpt.isPresent()) {
                vocabDto = mapToDto(vOpt.get());
            }
        }

        if (vocabDto == null) {
            for (List<TopicVocabularyDto> vList : memoryVocabsByCode.values()) {
                for (TopicVocabularyDto v : vList) {
                    if (v.getId().equals(vocabId)) {
                        vocabDto = v;
                        break;
                    }
                }
                if (vocabDto != null) break;
            }
        }

        if (vocabDto == null) {
            throw new IllegalArgumentException("Không tìm thấy từ vựng với id: " + vocabId);
        }

        UUID targetDeckId = request.getDeckId();

        if (targetDeckId == null && request.getNewDeckName() != null && !request.getNewDeckName().isBlank()) {
            CreateKanjiFolderRequest createReq = new CreateKanjiFolderRequest();
            createReq.setFolderName(request.getNewDeckName().trim());
            createReq.setDescription("Tạo từ Góc học tập đời sống");
            createReq.setColor("#15803D");
            KanjiFolderSummaryDto created = folderService.createFolder(userId, createReq);
            targetDeckId = created.getFolderId();
            response.put("createdNewDeck", true);
            response.put("newDeckName", created.getFolderName());
        }

        if (targetDeckId == null) {
            throw new IllegalArgumentException("Vui lòng chọn bộ thẻ hoặc nhập tên bộ thẻ mới để lưu!");
        }

        List<String> kanjis = vocabDto.getKanjiCharacters() != null ? vocabDto.getKanjiCharacters() : List.of();
        int addedCount = 0;
        List<String> savedChars = new ArrayList<>();

        for (String ch : kanjis) {
            try {
                UUID kanjiId = ensureKanjiExists(ch, vocabDto);
                folderService.addKanjiToFolder(targetDeckId, kanjiId);
                addedCount++;
                savedChars.add(ch);
            } catch (Exception e) {
                log.warn("Chữ [{}] có thể đã có trong bộ thẻ: {}", ch, e.getMessage());
            }
        }

        response.put("success", true);
        response.put("deckId", targetDeckId);
        response.put("word", vocabDto.getWord());
        response.put("savedChars", savedChars);
        response.put("addedCount", addedCount);
        response.put("message", "Đã lưu từ vựng [" + vocabDto.getWord() + "] cùng các chữ Kanji [" + String.join(", ", savedChars) + "] vào bộ thẻ thành công! 🌸");
        return response;
    }

    private UUID ensureKanjiExists(String kanjiChar, TopicVocabularyDto vocab) {
        Optional<KanjiDictionary> kOpt = kanjiRepository.findByKanjiCharacter(kanjiChar);
        if (kOpt.isPresent()) {
            return kOpt.get().getKanjiId();
        }

        var offlineOpt = handwritingSearchService.getOfflineKanji(kanjiChar);
        String hanViet = offlineOpt.map(HandwritingSearchService.OfflineKanji::han_viet).orElse(vocab.getHanViet() != null ? vocab.getHanViet() : "HÁN TỰ");
        String meaning = offlineOpt.map(HandwritingSearchService.OfflineKanji::meaning).orElse(vocab.getMeaningVn());
        String onyomi = offlineOpt.map(HandwritingSearchService.OfflineKanji::onyomi).orElse("-");
        String kunyomi = offlineOpt.map(HandwritingSearchService.OfflineKanji::kunyomi).orElse("-");

        int codePoint = kanjiChar.codePointAt(0);
        String unicodeHex = Integer.toHexString(codePoint).toUpperCase();

        KanjiDictionary newKanji = KanjiDictionary.builder()
                .kanjiCharacter(kanjiChar)
                .unicodeHex(unicodeHex)
                .hanViet(hanViet)
                .meaning(meaning)
                .onyomi(onyomi)
                .kunyomi(kunyomi)
                .strokeCount((short) 4)
                .build();

        KanjiDictionary saved = kanjiRepository.save(newKanji);
        return saved.getKanjiId();
    }

    private TopicVocabularyDto mapToDto(TopicVocabulary v) {
        List<String> list = new ArrayList<>();
        if (v.getKanjiCharacters() != null && !v.getKanjiCharacters().isBlank()) {
            String[] parts = v.getKanjiCharacters().split("[,|;\\s]+");
            for (String p : parts) {
                if (!p.isBlank()) list.add(p.trim());
            }
        }

        return TopicVocabularyDto.builder()
                .id(v.getId())
                .topicId(v.getTopic() != null ? v.getTopic().getId() : null)
                .word(v.getWord())
                .furigana(v.getFurigana())
                .romaji(v.getRomaji())
                .hanViet(v.getHanViet())
                .meaningVn(v.getMeaningVn())
                .jlptLevel(v.getJlptLevel())
                .exampleJp(v.getExampleJp())
                .exampleVn(v.getExampleVn())
                .kanjiCharacters(list)
                .build();
    }

    @Override
    @Transactional
    public void seedTopicsAndVocabulariesIfEmpty() {
        try {
            ClassPathResource res = new ClassPathResource("data/topics-vocabulary.json");
            if (!res.exists()) return;

            try (InputStream is = res.getInputStream()) {
                List<Map<String, Object>> list = objectMapper.readValue(is, new TypeReference<>() {});
                for (Map<String, Object> item : list) {
                    String code = (String) item.get("code");
                    Optional<Topic> existingOpt = topicRepository.findByCode(code);
                    Topic currentTopic = existingOpt.orElse(null);

                    if (currentTopic == null) {
                        Topic topic = Topic.builder()
                                .code(code)
                                .titleVn((String) item.get("titleVn"))
                                .titleJp((String) item.get("titleJp"))
                                .icon((String) item.get("icon"))
                                .description((String) item.get("description"))
                                .colorToken((String) item.get("colorToken"))
                                .displayOrder((Integer) item.get("displayOrder"))
                                .build();
                        currentTopic = topicRepository.save(topic);
                    }

                    // Kiểm tra xem chủ đề này đã có từ vựng trong DB chưa
                    long existingVocabCount = vocabularyRepository.countByTopicId(currentTopic.getId());
                    if (existingVocabCount == 0) {
                        @SuppressWarnings("unchecked")
                        List<Map<String, Object>> vocabs = (List<Map<String, Object>>) item.get("vocabularies");
                        if (vocabs != null) {
                            for (Map<String, Object> vMap : vocabs) {
                                @SuppressWarnings("unchecked")
                                List<String> kanjiChars = (List<String>) vMap.get("kanjiCharacters");
                                String kanjiStr = kanjiChars != null ? String.join(",", kanjiChars) : "";

                                TopicVocabulary vocab = TopicVocabulary.builder()
                                        .topic(currentTopic)
                                        .word((String) vMap.get("word"))
                                        .furigana((String) vMap.get("furigana"))
                                        .romaji((String) vMap.get("romaji"))
                                        .hanViet((String) vMap.get("hanViet"))
                                        .meaningVn((String) vMap.get("meaningVn"))
                                        .jlptLevel((String) vMap.get("jlptLevel"))
                                        .exampleJp((String) vMap.get("exampleJp"))
                                        .exampleVn((String) vMap.get("exampleVn"))
                                        .kanjiCharacters(kanjiStr)
                                        .build();
                                vocabularyRepository.save(vocab);
                            }
                        }
                    }
                }
                log.info("[TopicService] Đã đồng bộ từ vựng cho toàn bộ chủ đề vào Database.");
            }
        } catch (Exception e) {
            log.warn("[TopicService] Đồng bộ seeder database: {}", e.getMessage());
        }
    }
}
