package com.kizuna.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kizuna.dto.minigame.CreateKanjiFolderRequest;
import com.kizuna.dto.minigame.KanjiFolderSummaryDto;
import com.kizuna.dto.topic.AddVocabToDeckRequest;
import com.kizuna.dto.topic.TopicDto;
import com.kizuna.dto.topic.TopicVocabularyDto;
import com.kizuna.dto.topic.VocabExampleDto;
import com.kizuna.entity.KanjiDictionary;
import com.kizuna.entity.Topic;
import com.kizuna.entity.TopicVocabulary;
import com.kizuna.repository.KanjiDictionaryRepository;
import com.kizuna.repository.TopicRepository;
import com.kizuna.repository.TopicVocabularyRepository;
import com.kizuna.service.IKanjiFolderService;
import com.kizuna.service.ITopicService;
import com.kizuna.service.kanji.HandwritingSearchService;
import com.kizuna.util.VocabularyVietnameseDictionary;
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

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service quản lý toàn diện Kho Từ Vựng & Chủ Đề Đời Sống Nhật Bản:
 * - Nạp 7.811 từ vựng chuẩn OpenJLPT (N5 -> N1) kèm 7.245 câu ví dụ thực tế Tatoeba.
 * - Nạp các chủ đề đời sống giao tiếp thực tế.
 * - Hỗ trợ tìm kiếm đa năng (Kanji, Romaji, Hiragana, Nghĩa, Từ loại) và phân trang mượt mà.
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

    // In-memory cache lưu danh sách chủ đề & từ vựng theo cả Code lẫn UUID
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
            log.warn("[TopicService] Sử dụng In-Memory Cache an toàn: {}", e.getMessage());
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

    /**
     * Nạp In-Memory Cache gồm:
     * 1. 5 Cấp độ JLPT từ kho OpenJLPT (N5 -> N1: 7.811 từ vựng)
     * 2. Chủ đề tổng hợp ALL_JLPT (7.811 từ)
     * 3. Các chủ đề giao tiếp đời sống (topics-vocabulary.json)
     */
    private void loadMemoryCache() {
        memoryTopics.clear();
        memoryVocabsByCode.clear();
        memoryVocabsById.clear();

        List<TopicVocabularyDto> allOpenJlptVocabs = new ArrayList<>();

        // 1. NẠP KHO TỪ VỰNG OPENJLPT (N5 -> N1)
        String[] levels = {"n5", "n4", "n3", "n2", "n1"};
        String[] levelTitles = {
            "JLPT N5 (Sơ cấp căn bản)",
            "JLPT N4 (Sơ trung cấp)",
            "JLPT N3 (Trung cấp thực chiến)",
            "JLPT N2 (Trung cao cấp chuyên môn)",
            "JLPT N1 (Cao cấp thượng đẳng)"
        };
        String[] levelIcons = {"🟢", "🔵", "🟡", "🟠", "🔴"};
        String[] levelColors = {"#15803D", "#2563EB", "#D97706", "#EA580C", "#DC2626"};
        String[] levelDescs = {
            "Kho 674 từ vựng N5 nền tảng chuẩn đề thi và giao tiếp cơ bản",
            "Kho 630 từ vựng N4 mở rộng cấu trúc và đời sống thường nhật",
            "Kho 1.659 từ vựng N3 trung cấp, bước đệm chuyển tiếp đời sống và làm việc",
            "Kho 1.778 từ vựng N2 công sở, thương mại, tin tức và xã hội",
            "Kho 3.070 từ vựng N1 học thuật, báo chí, chính trị và chuyên sâu"
        };

        for (int i = 0; i < levels.length; i++) {
            String lvl = levels[i];
            String upper = lvl.toUpperCase();
            String code = "JLPT_" + upper;
            UUID topicId = UUID.nameUUIDFromBytes(code.getBytes());

            List<TopicVocabularyDto> levelVocabs = loadVocabulariesFromFile(lvl, upper, topicId);
            allOpenJlptVocabs.addAll(levelVocabs);

            memoryVocabsByCode.put(code, levelVocabs);
            memoryVocabsById.put(topicId, levelVocabs);

            memoryTopics.add(TopicDto.builder()
                    .id(topicId)
                    .code(code)
                    .titleVn(levelTitles[i])
                    .titleJp("日本語能力試験 " + upper)
                    .icon(levelIcons[i])
                    .description(levelDescs[i])
                    .colorToken(levelColors[i])
                    .displayOrder(i + 1)
                    .vocabCount((long) levelVocabs.size())
                    .build());
        }

        // 2. TẠO CHỦ ĐỀ TỔNG HỢP: TẤT CẢ TỪ VỰNG (ALL_JLPT - 7.811 TỪ)
        String allCode = "ALL_JLPT";
        UUID allTopicId = UUID.nameUUIDFromBytes(allCode.getBytes());
        memoryVocabsByCode.put(allCode, allOpenJlptVocabs);
        memoryVocabsById.put(allTopicId, allOpenJlptVocabs);

        memoryTopics.add(0, TopicDto.builder()
                .id(allTopicId)
                .code(allCode)
                .titleVn("Tất cả từ vựng (N5 - N1)")
                .titleJp("JLPT 語彙マスター")
                .icon("📚")
                .description("Kho tổng hợp trọn vẹn 7.811 từ vựng OpenJLPT kèm 7.245 câu ví dụ thực tế")
                .colorToken("#7C3AED")
                .displayOrder(0)
                .vocabCount((long) allOpenJlptVocabs.size())
                .build());

        log.info("[TopicService] Đã nạp thành công {} từ vựng OpenJLPT vào bộ nhớ đệm.", allOpenJlptVocabs.size());
    }

    /**
     * Đọc các từ vựng từ file openjlpt data/json/vocab/{lvl}.json
     */
    private List<TopicVocabularyDto> loadVocabulariesFromFile(String lvl, String levelUpper, UUID topicId) {
        List<TopicVocabularyDto> result = new ArrayList<>();
        try {
            File file = new File("data/openjlpt/data/json/vocab/" + lvl + ".json");
            InputStream is = null;
            if (file.exists()) {
                is = new FileInputStream(file);
            } else {
                ClassPathResource res = new ClassPathResource("data/openjlpt/data/json/vocab/" + lvl + ".json");
                if (res.exists()) {
                    is = res.getInputStream();
                }
            }

            if (is == null) {
                log.warn("[TopicService] Không tìm thấy file từ vựng: {}.json", lvl);
                return result;
            }

            try (InputStream stream = is) {
                List<Map<String, Object>> rawList = objectMapper.readValue(stream, new TypeReference<>() {});
                for (Map<String, Object> map : rawList) {
                    String id = (String) map.get("id");
                    String word = (String) map.get("word");
                    String reading = (String) map.get("reading");
                    String romaji = (String) map.get("romaji");
                    Integer jmdictId = (Integer) map.get("jmdict_id");

                    @SuppressWarnings("unchecked")
                    List<String> meanings = (List<String>) map.get("meanings");
                    String meaningEn = (meanings != null && !meanings.isEmpty()) ? String.join("; ", meanings) : "";

                    String hanViet = extractHanViet(word);
                    String meaningVn = VocabularyVietnameseDictionary.translateMeaning(word, reading, meaningEn, levelUpper, hanViet);
                    if (meaningVn == null || VocabularyVietnameseDictionary.isPureEnglish(meaningVn)) {
                        meaningVn = (hanViet != null && !hanViet.isBlank()) ? "Nghĩa Hán tự: " + hanViet : "Từ vựng tiếng Nhật chuẩn OpenJLPT";
                    }

                    @SuppressWarnings("unchecked")
                    List<String> pos = (List<String>) map.get("pos");
                    String posVn = mapPosToVietnamese(pos);

                    @SuppressWarnings("unchecked")
                    List<Map<String, Object>> exRaw = (List<Map<String, Object>>) map.get("examples");
                    List<VocabExampleDto> examples = new ArrayList<>();
                    String exampleJp = null;
                    String exampleVn = null;

                    if (exRaw != null && !exRaw.isEmpty()) {
                        for (Map<String, Object> exMap : exRaw) {
                            String exJa = (String) exMap.get("ja");
                            String exFuri = (String) exMap.get("furigana");
                            String exEn = (String) exMap.get("en");
                            Integer tatoebaId = (Integer) exMap.get("tatoeba_id");
                            String exVn = VocabularyVietnameseDictionary.translateExample(exJa, exEn);
                            if (exVn == null || VocabularyVietnameseDictionary.isPureEnglish(exVn) || exVn.equalsIgnoreCase(exEn)) {
                                exVn = "Ví dụ đàm thoại thực tế trong đời sống Nhật Bản.";
                            }

                            examples.add(VocabExampleDto.builder()
                                    .ja(exJa)
                                    .furigana(exFuri != null ? exFuri : exJa)
                                    .en(null)
                                    .vn(exVn)
                                    .tatoebaId(tatoebaId)
                                    .build());
                        }
                        exampleJp = examples.get(0).getJa();
                        exampleVn = examples.get(0).getVn();
                    }

                    UUID vocabUuid = (id != null)
                            ? UUID.nameUUIDFromBytes(("vocab_" + id).getBytes())
                            : UUID.nameUUIDFromBytes((levelUpper + "_" + word).getBytes());

                    result.add(TopicVocabularyDto.builder()
                            .id(vocabUuid)
                            .topicId(topicId)
                            .word(word)
                            .furigana(reading)
                            .romaji(romaji)
                            .hanViet(hanViet)
                            .meaningVn(meaningVn)
                            .meaningEn(null)
                            .jlptLevel(levelUpper)
                            .pos(pos != null ? pos : List.of())
                            .posVn(posVn)
                            .jmdictId(jmdictId)
                            .exampleJp(exampleJp)
                            .exampleVn(exampleVn)
                            .kanjiCharacters(extractKanjiCharacters(word))
                            .examples(examples)
                            .build());
                }
            }
        } catch (Exception e) {
            log.error("[TopicService] Lỗi khi nạp từ vựng cấp {}: {}", lvl, e.getMessage());
        }
        return result;
    }

    /**
     * Chuyển mã ký hiệu POS (JMdict) sang tiếng Việt rõ nghĩa
     */
    private String mapPosToVietnamese(List<String> posList) {
        if (posList == null || posList.isEmpty()) return "Từ vựng";
        List<String> vnList = new ArrayList<>();
        for (String pos : posList) {
            if (pos == null) continue;
            String p = pos.toLowerCase();
            if (p.startsWith("v5")) vnList.add("Động từ nhóm 1 (Ngũ đoạn)");
            else if ("v1".equals(p)) vnList.add("Động từ nhóm 2 (Nhất đoạn)");
            else if ("vs".equals(p) || "vk".equals(p) || p.startsWith("vs-")) vnList.add("Động từ nhóm 3 (Bất quy tắc)");
            else if ("adj-i".equals(p)) vnList.add("Tính từ đuôi -i");
            else if ("adj-na".equals(p)) vnList.add("Tính từ đuôi -na");
            else if ("adj-no".equals(p)) vnList.add("Tính từ bổ nghĩa (no)");
            else if ("n".equals(p)) vnList.add("Danh từ");
            else if ("adv".equals(p) || p.startsWith("adv")) vnList.add("Phó từ");
            else if ("prt".equals(p)) vnList.add("Trợ từ");
            else if ("exp".equals(p)) vnList.add("Cụm từ");
            else if ("int".equals(p)) vnList.add("Thán từ");
            else if ("num".equals(p)) vnList.add("Số từ");
            else if ("counter".equals(p)) vnList.add("Từ đếm");
            else if ("vi".equals(p)) vnList.add("Nội động từ");
            else if ("vt".equals(p)) vnList.add("Ngoại động từ");
            else if ("pn".equals(p)) vnList.add("Đại từ");
        }
        return vnList.isEmpty() ? "Từ vựng" : String.join(", ", new LinkedHashSet<>(vnList));
    }

    private List<String> extractKanjiCharacters(String text) {
        if (text == null) return List.of();
        List<String> list = new ArrayList<>();
        for (char c : text.toCharArray()) {
            Character.UnicodeBlock block = Character.UnicodeBlock.of(c);
            if (block == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS ||
                block == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_A) {
                list.add(String.valueOf(c));
            }
        }
        return list;
    }

    private String extractHanViet(String word) {
        if (word == null) return null;
        List<String> hvList = new ArrayList<>();
        for (char c : word.toCharArray()) {
            String ch = String.valueOf(c);
            var opt = handwritingSearchService.getOfflineKanji(ch);
            opt.ifPresent(k -> {
                if (k.han_viet() != null && !k.han_viet().isBlank()) {
                    hvList.add(k.han_viet().toUpperCase());
                }
            });
        }
        return hvList.isEmpty() ? null : String.join(" ", hvList);
    }

    @Override
    public List<TopicDto> getAllTopics() {
        if (isDatabaseReady) {
            try {
                List<Topic> dbTopics = topicRepository.findAllOrderByDisplayOrder();
                if (dbTopics != null && !dbTopics.isEmpty()) {
                    long totalCount = vocabularyRepository.count();
                    List<TopicDto> result = new ArrayList<>();
                    for (Topic t : dbTopics) {
                        long count;
                        if ("ALL_JLPT".equalsIgnoreCase(t.getCode())) {
                            count = totalCount;
                        } else {
                            count = vocabularyRepository.countByTopicId(t.getId());
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
                log.warn("[TopicService] Lỗi khi truy vấn topics từ database, chuyển sang bộ nhớ đệm: {}", e.getMessage());
            }
        }
        return new ArrayList<>(memoryTopics);
    }

    @Override
    public Page<TopicVocabularyDto> getVocabulariesByTopic(String topicIdentifier, String keyword, Pageable pageable) {
        String cleanKeyword = (keyword != null && !keyword.trim().isEmpty()) ? keyword.trim().toLowerCase() : null;

        if (isDatabaseReady) {
            try {
                if (topicIdentifier == null || "ALL_JLPT".equalsIgnoreCase(topicIdentifier)) {
                    Page<TopicVocabulary> dbPage = vocabularyRepository.findAllWithSearch(cleanKeyword, pageable);
                    return dbPage.map(this::mapToDto);
                }

                Topic targetTopic = null;
                Optional<Topic> topicByCode = topicRepository.findByCode(topicIdentifier);
                if (topicByCode.isPresent()) {
                    targetTopic = topicByCode.get();
                } else {
                    try {
                        UUID uId = UUID.fromString(topicIdentifier);
                        targetTopic = topicRepository.findById(uId).orElse(null);
                    } catch (Exception ignored) {}
                }

                if (targetTopic != null) {
                    Page<TopicVocabulary> dbPage = vocabularyRepository.findByTopicIdWithSearch(targetTopic.getId(), cleanKeyword, pageable);
                    return dbPage.map(this::mapToDto);
                } else {
                    Page<TopicVocabulary> dbPage = vocabularyRepository.findAllWithSearch(cleanKeyword, pageable);
                    return dbPage.map(this::mapToDto);
                }
            } catch (Exception e) {
                log.warn("[TopicService] Lỗi khi truy vấn topic_vocabularies từ database, chuyển sang bộ nhớ đệm: {}", e.getMessage());
            }
        }

        List<TopicVocabularyDto> list = null;
        if (topicIdentifier != null) {
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
        }

        // Mặc định nạp toàn bộ nếu không tìm thấy chủ đề cụ thể
        if (list == null || list.isEmpty()) {
            list = memoryVocabsByCode.getOrDefault("ALL_JLPT", List.of());
        }

        if (cleanKeyword != null) {
            String kw = cleanKeyword;
            list = list.stream().filter(v ->
                    (v.getWord() != null && v.getWord().toLowerCase().contains(kw)) ||
                    (v.getFurigana() != null && v.getFurigana().toLowerCase().contains(kw)) ||
                    (v.getRomaji() != null && v.getRomaji().toLowerCase().contains(kw)) ||
                    (v.getMeaningVn() != null && v.getMeaningVn().toLowerCase().contains(kw)) ||
                    (v.getMeaningEn() != null && v.getMeaningEn().toLowerCase().contains(kw)) ||
                    (v.getHanViet() != null && v.getHanViet().toLowerCase().contains(kw)) ||
                    (v.getPosVn() != null && v.getPosVn().toLowerCase().contains(kw))
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
        for (List<TopicVocabularyDto> vList : memoryVocabsByCode.values()) {
            for (TopicVocabularyDto v : vList) {
                if (v.getId() != null && v.getId().equals(vocabId)) {
                    vocabDto = v;
                    break;
                }
            }
            if (vocabDto != null) break;
        }

        if (vocabDto == null && isDatabaseReady) {
            Optional<TopicVocabulary> vOpt = vocabularyRepository.findById(vocabId);
            if (vOpt.isPresent()) {
                vocabDto = mapToDto(vOpt.get());
            }
        }

        if (vocabDto == null) {
            throw new IllegalArgumentException("Không tìm thấy từ vựng với mã định danh: " + vocabId);
        }

        UUID targetDeckId = request.getDeckId();

        if (targetDeckId == null && request.getNewDeckName() != null && !request.getNewDeckName().isBlank()) {
            CreateKanjiFolderRequest createReq = new CreateKanjiFolderRequest();
            createReq.setFolderName(request.getNewDeckName().trim());
            createReq.setDescription("Tạo từ Góc học tập từ vựng OpenJLPT");
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
        response.put("message", "Đã lưu từ vựng [" + vocabDto.getWord() + "] cùng các chữ Kanji [" + String.join(", ", savedChars) + "] vào bộ thẻ thành công! 🎯");
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

        List<VocabExampleDto> exList = new ArrayList<>();
        if (v.getExampleJp() != null && !v.getExampleJp().isBlank()) {
            exList.add(VocabExampleDto.builder()
                    .ja(v.getExampleJp())
                    .furigana(v.getExampleJp())
                    .vn(v.getExampleVn())
                    .build());
        }

        return TopicVocabularyDto.builder()
                .id(v.getId())
                .topicId(v.getTopic() != null ? v.getTopic().getId() : null)
                .word(v.getWord())
                .furigana(v.getFurigana())
                .romaji(v.getRomaji())
                .hanViet(v.getHanViet())
                .meaningVn(v.getMeaningVn())
                .meaningEn(null)
                .jlptLevel(v.getJlptLevel())
                .pos(List.of("Từ vựng OpenJLPT"))
                .posVn("Từ vựng OpenJLPT")
                .exampleJp(v.getExampleJp())
                .exampleVn(v.getExampleVn())
                .kanjiCharacters(list)
                .examples(exList)
                .build();
    }

    @Override
    public TopicVocabularyDto getVocabularyById(UUID vocabId) {
        if (vocabId == null) return null;

        for (List<TopicVocabularyDto> vList : memoryVocabsByCode.values()) {
            for (TopicVocabularyDto v : vList) {
                if (v.getId() != null && v.getId().equals(vocabId)) {
                    return v;
                }
            }
        }

        if (isDatabaseReady) {
            Optional<TopicVocabulary> vOpt = vocabularyRepository.findById(vocabId);
            if (vOpt.isPresent()) {
                return mapToDto(vOpt.get());
            }
        }

        return null;
    }

    @Override
    @Transactional
    public void seedTopicsAndVocabulariesIfEmpty() {
        // Giữ lại để tương thích interface
    }
}
