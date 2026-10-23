package com.kizuna.service.kanji;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kizuna.dto.kanji.HandwritingSearchDto;
import com.kizuna.dto.kanji.HandwritingSearchDto.MatchItem;
import com.kizuna.dto.kanji.HandwritingSearchDto.SearchRequest;
import com.kizuna.dto.kanji.HandwritingSearchDto.SearchResponse;
import com.kizuna.dto.kanji.HandwritingSearchDto.StrokePoint;
import com.kizuna.entity.KanjiDictionary;
import com.kizuna.repository.KanjiDictionaryRepository;
import com.kizuna.util.SvgUtils;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Service xử lý nhận diện & tra cứu chữ Hán theo nét vẽ viết tay thời gian thực (Real-time Handwriting Kanji Search).
 * Kiến trúc Hybrid:
 * 1. Làm giàu dữ liệu từ kết quả nhận diện của client (Google Input Tools API) với metadata chuẩn trong DB KizunaTutor & Từ điển Offline.
 * 2. Fallback sang Thuật toán Topo Hình học Vector (Internal Stroke Direction & Topology Matching) khi offline.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class HandwritingSearchService {

    private final KanjiDictionaryRepository kanjiRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public record OfflineKanji(String character, String han_viet, String onyomi, String kunyomi, String meaning, String jlpt) {}
    private final Map<String, OfflineKanji> offlineDict = new ConcurrentHashMap<>();

    @PostConstruct
    public void initDictionary() {
        try {
            ClassPathResource res = new ClassPathResource("data/kanji-dictionary-data.json");
            if (res.exists()) {
                try (InputStream is = res.getInputStream()) {
                    List<OfflineKanji> list = objectMapper.readValue(is, new TypeReference<>() {});
                    for (OfflineKanji k : list) {
                        if (k.character() != null) {
                            offlineDict.put(k.character(), k);
                        }
                    }
                    log.info("[HandwritingSearch] Đã nạp thành công {} từ điển Hán-Việt offline.", offlineDict.size());
                }
            }
        } catch (Exception e) {
            log.warn("[HandwritingSearch] Không thể nạp kanji-dictionary-data.json: {}", e.getMessage());
        }
    }

    public Optional<OfflineKanji> getOfflineKanji(String character) {
        if (character == null) return Optional.empty();
        return Optional.ofNullable(offlineDict.get(character.trim()));
    }

    public void enrichKanjiMetadata(KanjiDictionary kanji) {
        if (kanji == null || kanji.getKanjiCharacter() == null) return;
        OfflineKanji off = offlineDict.get(kanji.getKanjiCharacter().trim());
        if (off == null) return;
        if (kanji.getHanViet() == null || kanji.getHanViet().isBlank()) {
            kanji.setHanViet(off.han_viet());
        }
        if (kanji.getOnyomi() == null || kanji.getOnyomi().isBlank()) {
            kanji.setOnyomi(off.onyomi());
        }
        if (kanji.getKunyomi() == null || kanji.getKunyomi().isBlank()) {
            kanji.setKunyomi(off.kunyomi());
        }
        if (kanji.getMeaning() == null || kanji.getMeaning().isBlank()) {
            kanji.setMeaning(off.meaning());
        }
        if (kanji.getJlptLevel() == null && off.jlpt() != null && off.jlpt().matches("^N[1-5]$")) {
            try {
                kanji.setJlptLevel(com.kizuna.entity.JlptLevel.valueOf(off.jlpt()));
            } catch (Exception ignored) {}
        }
    }

    public SearchResponse search(SearchRequest req) {
        int limit = req.getLimit() > 0 ? Math.min(req.getLimit(), 30) : 12;

        // 1. Nếu client truyền lên danh sách candidates từ Google Input Tools Handwriting API
        if (req.getCandidates() != null && !req.getCandidates().isEmpty()) {
            List<MatchItem> matches = enrichCandidates(req.getCandidates(), limit);
            if (!matches.isEmpty()) {
                return new SearchResponse(matches, "GOOGLE_HANDWRITING_ENRICHED");
            }
        }

        // 2. Thuật toán Topo Vector nội bộ (Internal Topology Matching Engine Fallback)
        List<MatchItem> topologyMatches = matchByVectorTopology(req.getStrokes(), req.getStrokeCount(), limit);
        return new SearchResponse(topologyMatches, "INTERNAL_TOPOLOGY_ENGINE");
    }

    /**
     * Tra cứu thông tin đầy đủ cho danh sách các chữ Hán được nhận diện từ Google Input Tools API
     */
    private List<MatchItem> enrichCandidates(List<String> candidates, int limit) {
        List<MatchItem> result = new ArrayList<>();
        Set<String> processedChars = new HashSet<>();

        double score = 0.98;
        for (String ch : candidates) {
            if (ch == null || ch.isBlank() || processedChars.contains(ch)) continue;
            processedChars.add(ch);

            // Chỉ nhận ký tự CJK chữ Hán
            int codePoint = ch.codePointAt(0);
            if (codePoint < 0x4e00 || codePoint > 0x9fff) {
                continue;
            }

            Optional<KanjiDictionary> kanjiOpt = kanjiRepository.findByKanjiCharacter(ch);
            OfflineKanji off = offlineDict.get(ch);

            if (kanjiOpt.isPresent()) {
                KanjiDictionary k = kanjiOpt.get();
                String hanViet = (k.getHanViet() != null && !k.getHanViet().isBlank()) 
                        ? k.getHanViet() 
                        : (off != null && off.han_viet() != null ? off.han_viet() : "HÁN TỰ");
                String meaning = (k.getMeaning() != null && !k.getMeaning().isBlank()) 
                        ? k.getMeaning() 
                        : (off != null && off.meaning() != null ? off.meaning() : "Chữ Hán tra cứu");
                String onyomi = (k.getOnyomi() != null && !k.getOnyomi().isBlank()) 
                        ? k.getOnyomi() 
                        : (off != null && off.onyomi() != null ? off.onyomi() : "-");
                String kunyomi = (k.getKunyomi() != null && !k.getKunyomi().isBlank()) 
                        ? k.getKunyomi() 
                        : (off != null && off.kunyomi() != null ? off.kunyomi() : "-");
                String jlpt = k.getJlptLevel() != null 
                        ? k.getJlptLevel().name() 
                        : (off != null && off.jlpt() != null ? off.jlpt() : "N/A");

                result.add(MatchItem.builder()
                        .character(k.getKanjiCharacter())
                        .unicodeHex(k.getUnicodeHex())
                        .hanViet(hanViet)
                        .meaning(meaning)
                        .totalStrokes(k.getStrokeCount() != null ? (int) k.getStrokeCount() : 0)
                        .onyomi(onyomi)
                        .kunyomi(kunyomi)
                        .jlpt(jlpt)
                        .similarityScore(Math.round(score * 100.0) / 100.0)
                        .svgSolution(SvgUtils.cleanKanjiVgSvg(k.getSvgData()))
                        .build());
            } else if (off != null) {
                // Fallback nếu chữ chưa có trong bảng kanji_dictionary DB nhưng có trong từ điển
                result.add(MatchItem.builder()
                        .character(ch)
                        .unicodeHex(Integer.toHexString(codePoint).toUpperCase())
                        .hanViet(off.han_viet() != null ? off.han_viet() : "HÁN TỰ")
                        .meaning(off.meaning() != null ? off.meaning() : "Chữ Hán tra cứu")
                        .totalStrokes(0)
                        .onyomi(off.onyomi() != null ? off.onyomi() : "-")
                        .kunyomi(off.kunyomi() != null ? off.kunyomi() : "-")
                        .jlpt(off.jlpt() != null ? off.jlpt() : "N/A")
                        .similarityScore(Math.round(score * 100.0) / 100.0)
                        .svgSolution(null)
                        .build());
            } else {
                result.add(MatchItem.builder()
                        .character(ch)
                        .unicodeHex(Integer.toHexString(codePoint).toUpperCase())
                        .hanViet("HÁN TỰ")
                        .meaning("Chữ Hán tra cứu")
                        .totalStrokes(0)
                        .onyomi("-")
                        .kunyomi("-")
                        .jlpt("N/A")
                        .similarityScore(Math.round(score * 100.0) / 100.0)
                        .svgSolution(null)
                        .build());
            }

            score = Math.max(0.40, score - 0.04);
            if (result.size() >= limit) break;
        }

        return result;
    }

    /**
     * Thuật toán so khớp nét vẽ Vector Topology nội bộ khi không dùng API ngoài
     */
    private List<MatchItem> matchByVectorTopology(List<List<StrokePoint>> userStrokes, int strokeCount, int limit) {
        if (userStrokes == null || userStrokes.isEmpty()) {
            return List.of();
        }

        int actualStrokes = strokeCount > 0 ? strokeCount : userStrokes.size();
        short minStrokes = (short) Math.max(1, actualStrokes);
        short maxStrokes = (short) (actualStrokes + 6);

        // Lấy danh sách ứng viên có số nét phù hợp từ cơ sở dữ liệu
        List<KanjiDictionary> candidates = kanjiRepository.findByStrokeCountBetween(
                minStrokes, maxStrokes, PageRequest.of(0, 100)
        );

        if (candidates.isEmpty()) {
            candidates = kanjiRepository.findAll(PageRequest.of(0, 50)).getContent();
        }

        // Tính vector hướng của các nét người dùng đã vẽ
        List<double[]> userVectors = new ArrayList<>();
        for (List<StrokePoint> stroke : userStrokes) {
            if (stroke == null || stroke.size() < 2) continue;
            StrokePoint start = stroke.get(0);
            StrokePoint end = stroke.get(stroke.size() - 1);
            double dx = end.getX() - start.getX();
            double dy = end.getY() - start.getY();
            double len = Math.hypot(dx, dy);
            if (len > 0.001) {
                userVectors.add(new double[]{dx / len, dy / len});
            } else {
                userVectors.add(new double[]{1.0, 0.0});
            }
        }

        if (userVectors.isEmpty()) return List.of();

        // So khớp với từng ứng viên
        List<ScoredKanji> scoredList = new ArrayList<>();
        for (KanjiDictionary k : candidates) {
            List<double[]> stdVectors = extractStandardVectors(k.getSvgData());
            if (stdVectors.isEmpty()) continue;

            int kStrokeCount = k.getStrokeCount() != null ? k.getStrokeCount() : stdVectors.size();
            double matchScore = calculateTopologyScore(userVectors, stdVectors, actualStrokes, kStrokeCount);
            scoredList.add(new ScoredKanji(k, matchScore));
        }

        // Sắp xếp điểm giảm dần
        scoredList.sort((a, b) -> Double.compare(b.score, a.score));

        List<MatchItem> results = new ArrayList<>();
        for (int i = 0; i < Math.min(limit, scoredList.size()); i++) {
            ScoredKanji sk = scoredList.get(i);
            KanjiDictionary k = sk.entity;
            OfflineKanji off = offlineDict.get(k.getKanjiCharacter());

            String hanViet = (k.getHanViet() != null && !k.getHanViet().isBlank()) 
                    ? k.getHanViet() 
                    : (off != null && off.han_viet() != null ? off.han_viet() : "HÁN TỰ");
            String meaning = (k.getMeaning() != null && !k.getMeaning().isBlank()) 
                    ? k.getMeaning() 
                    : (off != null && off.meaning() != null ? off.meaning() : "Chữ Hán tra cứu");
            String onyomi = (k.getOnyomi() != null && !k.getOnyomi().isBlank()) 
                    ? k.getOnyomi() 
                    : (off != null && off.onyomi() != null ? off.onyomi() : "-");
            String kunyomi = (k.getKunyomi() != null && !k.getKunyomi().isBlank()) 
                    ? k.getKunyomi() 
                    : (off != null && off.kunyomi() != null ? off.kunyomi() : "-");
            String jlpt = k.getJlptLevel() != null 
                    ? k.getJlptLevel().name() 
                    : (off != null && off.jlpt() != null ? off.jlpt() : "N/A");

            results.add(MatchItem.builder()
                    .character(k.getKanjiCharacter())
                    .unicodeHex(k.getUnicodeHex())
                    .hanViet(hanViet)
                    .meaning(meaning)
                    .totalStrokes(k.getStrokeCount() != null ? (int) k.getStrokeCount() : 0)
                    .onyomi(onyomi)
                    .kunyomi(kunyomi)
                    .jlpt(jlpt)
                    .similarityScore(Math.round(sk.score * 100.0) / 100.0)
                    .svgSolution(SvgUtils.cleanKanjiVgSvg(k.getSvgData()))
                    .build());
        }

        return results;
    }

    private record ScoredKanji(KanjiDictionary entity, double score) {}

    private double calculateTopologyScore(List<double[]> userVectors, List<double[]> stdVectors, int userStrokeCount, int stdStrokeCount) {
        int compareCount = Math.min(userVectors.size(), stdVectors.size());
        if (compareCount == 0) return 0.0;

        double sumSim = 0.0;
        for (int i = 0; i < compareCount; i++) {
            double[] u = userVectors.get(i);
            double[] s = stdVectors.get(i);
            double dot = u[0] * s[0] + u[1] * s[1];
            // Chuẩn hóa [-1, 1] về [0, 1]
            double sim = (dot + 1.0) / 2.0;
            sumSim += sim;
        }

        double avgDirectionSim = sumSim / compareCount;

        // Phạt nhẹ nếu số nét chênh lệch nhiều
        int strokeDiff = Math.abs(stdStrokeCount - userStrokeCount);
        double strokePenalty = Math.min(0.35, strokeDiff * 0.05);

        return Math.max(0.1, avgDirectionSim - strokePenalty);
    }

    private List<double[]> extractStandardVectors(String rawSvg) {
        List<double[]> vectors = new ArrayList<>();
        if (rawSvg == null || rawSvg.isBlank()) return vectors;

        Pattern pathPattern = Pattern.compile("<path[^>]*\\bd=\"([^\"]+)\"", Pattern.CASE_INSENSITIVE);
        Matcher matcher = pathPattern.matcher(rawSvg);

        while (matcher.find()) {
            String d = matcher.group(1);
            double[] vec = parsePathVector(d);
            if (vec != null) {
                vectors.add(vec);
            }
        }

        return vectors;
    }

    private double[] parsePathVector(String d) {
        if (d == null || d.isBlank()) return null;
        try {
            Matcher m = Pattern.compile("[M|m]\\s*([-\\d.]+)[,\\s]+([-\\d.]+)").matcher(d);
            if (!m.find()) return null;
            double startX = Double.parseDouble(m.group(1));
            double startY = Double.parseDouble(m.group(2));

            double currX = startX;
            double currY = startY;

            Matcher segMatcher = Pattern.compile("([cCsSqQtTlLhHvV])([^a-zA-Z]*)").matcher(d.substring(m.end()));
            while (segMatcher.find()) {
                char cmd = segMatcher.group(1).charAt(0);
                String argsStr = segMatcher.group(2).trim();
                if (argsStr.isEmpty()) continue;

                Matcher numMatcher = Pattern.compile("[-+]?[0-9]*\\.?[0-9]+(?:[eE][-+]?[0-9]+)?").matcher(argsStr);
                List<Double> nums = new ArrayList<>();
                while (numMatcher.find()) {
                    nums.add(Double.parseDouble(numMatcher.group()));
                }

                if ((cmd == 'c' || cmd == 'C') && nums.size() >= 6) {
                    for (int i = 0; i + 5 < nums.size(); i += 6) {
                        currX += nums.get(i + 4);
                        currY += nums.get(i + 5);
                    }
                }
            }

            double dx = currX - startX;
            double dy = currY - startY;
            double len = Math.hypot(dx, dy);
            if (len > 0.001) {
                return new double[]{dx / len, dy / len};
            }
            return new double[]{1.0, 0.0};
        } catch (Exception e) {
            return new double[]{1.0, 0.0};
        }
    }
}
