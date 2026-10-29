package com.kizuna.seeder;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kizuna.entity.KanjiDictionary;
import com.kizuna.repository.KanjiDictionaryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
@RequiredArgsConstructor
@Slf4j
public class KanjiDataSeeder implements CommandLineRunner {

    private final KanjiDictionaryRepository kanjiRepository;
    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    @Value("${app.seeder.kanji.enabled:true}")
    private boolean seederEnabled;

    @Value("${app.seeder.kanji.batch-size:500}")
    private int batchSize;

    // Pattern nhận diện file chuẩn: 5 ký tự hex (ví dụ: 04e00.svg)
    private static final Pattern SVG_FILE_PATTERN = Pattern.compile("^[0-9a-fA-F]{5}\\.svg$");

    // Pattern tìm thẻ <path (mỗi path tương ứng 1 nét vẽ trong KanjiVG)
    private static final Pattern PATH_PATTERN = Pattern.compile("<path\\b");

    // Pattern trích xuất bộ thủ (radical) từ thuộc tính kvg:element gắn liền với kvg:radical
    private static final Pattern RADICAL_PATTERN_1 = Pattern.compile("kvg:element=\"([^\"]+)\"[^>]*kvg:radical=");
    private static final Pattern RADICAL_PATTERN_2 = Pattern.compile("kvg:radical=[^>]*kvg:element=\"([^\"]+)\"");

    public record OfflineKanjiData(
            String character,
            String han_viet,
            String onyomi,
            String kunyomi,
            String meaning,
            String jlpt
    ) {}

    @Override
    public void run(String... args) throws Exception {
        if (!seederEnabled) {
            log.info("[KanjiSeeder] Seeder đang bị tắt qua cấu hình (app.seeder.kanji.enabled=false).");
            return;
        }

        // 1. Kiểm tra và nạp ký tự SVG từ KanjiVG nếu bảng hoàn toàn trống
        long count = kanjiRepository.count();
        if (count == 0) {
            seedSvgFromKanjiVg();
        } else {
            log.info("[KanjiSeeder] Bảng kanji_dictionary đã có {} bản ghi nét vẽ SVG.", count);
        }

        // 2. Kiểm tra xem dữ liệu JLPT và Hán-Việt đã được backfill vào database chưa
        try {
            Integer jlptCount = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM kanji_dictionary WHERE jlpt_level IS NOT NULL", Integer.class);
            if (jlptCount == null || jlptCount < 5000) {
                log.info("[KanjiSeeder] Phát hiện chỉ có {} bản ghi có cấp độ JLPT. Bắt đầu backfill toàn bộ dữ liệu từ điển...", jlptCount != null ? jlptCount : 0);
                syncMetadata();
            } else {
                log.info("[KanjiSeeder] Đã có {} bản ghi có cấp độ JLPT chuẩn xác trong PostgreSQL.", jlptCount);
            }
        } catch (Exception e) {
            log.warn("[KanjiSeeder] Lỗi khi kiểm tra số lượng JLPT, thử chạy đồng bộ metadata: {}", e.getMessage());
            syncMetadata();
        }
    }

    /**
     * Đồng bộ toàn bộ siêu dữ liệu: JLPT, Âm Hán Việt, Onyomi, Kunyomi, Nghĩa vào PostgreSQL
     */
    public int syncMetadata() {
        log.info("[KanjiSeeder] Đang đồng bộ siêu dữ liệu từ điển Hán-Việt & JLPT từ kanji-dictionary-data.json...");
        try {
            // Mở rộng độ dài cột nếu cần thiết
            try {
                jdbcTemplate.execute("ALTER TABLE kanji_dictionary ALTER COLUMN onyomi TYPE VARCHAR(150)");
                jdbcTemplate.execute("ALTER TABLE kanji_dictionary ALTER COLUMN kunyomi TYPE VARCHAR(300)");
            } catch (Exception e) {
                log.debug("[KanjiSeeder] Bỏ qua ALTER COLUMN nếu đã tồn tại hoặc bảng đã được cấu hình: {}", e.getMessage());
            }

            ClassPathResource res = new ClassPathResource("data/kanji-dictionary-data.json");
            if (!res.exists()) {
                log.warn("[KanjiSeeder] Không tìm thấy resource data/kanji-dictionary-data.json!");
                return 0;
            }

            List<OfflineKanjiData> list;
            try (InputStream is = res.getInputStream()) {
                list = objectMapper.readValue(is, new TypeReference<>() {});
            }

            if (list == null || list.isEmpty()) {
                log.warn("[KanjiSeeder] File kanji-dictionary-data.json rỗng!");
                return 0;
            }

            log.info("[KanjiSeeder] Tìm thấy {} bản ghi siêu dữ liệu trong JSON. Bắt đầu batch update vào PostgreSQL...", list.size());

            String sql = "UPDATE kanji_dictionary SET " +
                         "jlpt_level = CAST(? AS jlpt_level_enum), " +
                         "han_viet = COALESCE(?, han_viet), " +
                         "onyomi = COALESCE(?, onyomi), " +
                         "kunyomi = COALESCE(?, kunyomi), " +
                         "meaning = COALESCE(?, meaning), " +
                         "updated_at = CURRENT_TIMESTAMP " +
                         "WHERE kanji_character = ?";

            int batchChunkSize = 500;
            int totalUpdated = 0;

            for (int i = 0; i < list.size(); i += batchChunkSize) {
                int end = Math.min(i + batchChunkSize, list.size());
                List<OfflineKanjiData> chunk = list.subList(i, end);

                int[] results = jdbcTemplate.batchUpdate(sql, new BatchPreparedStatementSetter() {
                    @Override
                    public void setValues(PreparedStatement ps, int idx) throws SQLException {
                        OfflineKanjiData item = chunk.get(idx);

                        String jlpt = (item.jlpt() != null && item.jlpt().matches("^N[1-5]$")) ? item.jlpt() : "N5";
                        ps.setString(1, jlpt);
                        ps.setString(2, safeTruncate(item.han_viet(), 50));
                        ps.setString(3, safeTruncate(item.onyomi(), 150));
                        ps.setString(4, safeTruncate(item.kunyomi(), 300));
                        ps.setString(5, safeTruncate(item.meaning(), 200));
                        ps.setString(6, item.character());
                    }

                    @Override
                    public int getBatchSize() {
                        return chunk.size();
                    }
                });

                for (int r : results) {
                    if (r > 0) totalUpdated++;
                }
            }

            log.info("[KanjiSeeder] HOÀN TẤT ĐỒNG BỘ SIÊU DỮ LIỆU! Đã cập nhật JLPT, Hán Việt, On/Kun, Nghĩa cho {} chữ Kanji.", totalUpdated);
            return totalUpdated;
        } catch (Exception e) {
            log.error("[KanjiSeeder] Lỗi trong quá trình đồng bộ siêu dữ liệu: {}", e.getMessage(), e);
            return -1;
        }
    }

    private void seedSvgFromKanjiVg() {
        log.info("[KanjiSeeder] Bắt đầu quét dữ liệu SVG từ KanjiVG...");

        // Tìm kiếm thư mục chứa file svg
        Path kanjiPath = resolveKanjiDirectory();
        if (kanjiPath == null || !Files.exists(kanjiPath)) {
            log.warn("[KanjiSeeder] Không tìm thấy thư mục SVG nét chữ Kanji. Đã kiểm tra 'data/kanjivg/kanji' và '../kanjivg/kanji'.");
            return;
        }

        File[] svgFiles = kanjiPath.toFile().listFiles((dir, name) -> SVG_FILE_PATTERN.matcher(name).matches());
        if (svgFiles == null || svgFiles.length == 0) {
            log.warn("[KanjiSeeder] Không tìm thấy file .svg hợp lệ nào trong thư mục: {}", kanjiPath);
            return;
        }

        log.info("[KanjiSeeder] Tìm thấy {} file SVG nét chữ Hán chuẩn. Bắt đầu phân tích và lưu vào PostgreSQL...", svgFiles.length);

        List<KanjiDictionary> batch = new ArrayList<>(batchSize);
        int totalSaved = 0;
        int skipped = 0;

        for (File file : svgFiles) {
            try {
                String fileName = file.getName();
                String unicodeHex = fileName.substring(0, 5).toLowerCase();
                int codePoint = Integer.parseInt(unicodeHex, 16);

                if (codePoint < 0x3400 || codePoint > 0xFFFF) {
                    skipped++;
                    continue;
                }

                String kanjiChar = String.valueOf((char) codePoint);
                String svgContent = Files.readString(file.toPath(), StandardCharsets.UTF_8);

                short strokeCount = (short) countMatches(PATH_PATTERN, svgContent);
                if (strokeCount <= 0) {
                    strokeCount = 1;
                }

                String radical = extractRadical(svgContent);

                KanjiDictionary kanji = KanjiDictionary.builder()
                        .kanjiCharacter(kanjiChar)
                        .unicodeHex(unicodeHex)
                        .strokeCount(strokeCount)
                        .radical(radical)
                        .svgData(svgContent)
                        .strokeOrderSvgUrl("/static/kanjivg/" + fileName)
                        .build();

                batch.add(kanji);

                if (batch.size() >= batchSize) {
                    kanjiRepository.saveAll(batch);
                    totalSaved += batch.size();
                    log.info("[KanjiSeeder] Đã lưu {} chữ Kanji...", totalSaved);
                    batch.clear();
                }

            } catch (Exception e) {
                log.error("[KanjiSeeder] Lỗi khi xử lý file {}: {}", file.getName(), e.getMessage());
            }
        }

        if (!batch.isEmpty()) {
            kanjiRepository.saveAll(batch);
            totalSaved += batch.size();
            batch.clear();
        }

        log.info("[KanjiSeeder] HOÀN TẤT! Đã nạp thành công {} chữ Kanji vào bảng kanji_dictionary (Bỏ qua: {} file).", totalSaved, skipped);
    }

    private String safeTruncate(String s, int max) {
        if (s == null) return null;
        String trimmed = s.trim();
        return trimmed.length() > max ? trimmed.substring(0, max) : trimmed;
    }

    private Path resolveKanjiDirectory() {
        List<Path> candidatePaths = List.of(
                Path.of("data", "kanjivg", "kanji"),
                Path.of("..", "kanjivg", "kanji"),
                Path.of("kanji")
        );

        for (Path path : candidatePaths) {
            if (Files.exists(path) && Files.isDirectory(path)) {
                return path;
            }
        }
        return null;
    }

    private int countMatches(Pattern pattern, String text) {
        Matcher matcher = pattern.matcher(text);
        int count = 0;
        while (matcher.find()) {
            count++;
        }
        return count;
    }

    private String extractRadical(String svgContent) {
        Matcher m1 = RADICAL_PATTERN_1.matcher(svgContent);
        if (m1.find()) {
            return m1.group(1);
        }
        Matcher m2 = RADICAL_PATTERN_2.matcher(svgContent);
        if (m2.find()) {
            return m2.group(1);
        }
        return null;
    }
}
