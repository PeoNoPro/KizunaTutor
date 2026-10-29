package com.kizuna.seeder;

import com.kizuna.entity.KanjiDictionary;
import com.kizuna.repository.KanjiDictionaryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
@RequiredArgsConstructor
@Slf4j
public class KanjiDataSeeder implements CommandLineRunner {

    private final KanjiDictionaryRepository kanjiRepository;

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

    @Override
    public void run(String... args) throws Exception {
        if (!seederEnabled) {
            log.info("[KanjiSeeder] Seeder đang bị tắt qua cấu hình (app.seeder.kanji.enabled=false).");
            return;
        }

        // Kiểm tra xem bảng đã có dữ liệu chưa
        long count = kanjiRepository.count();
        if (count > 0) {
            log.info("[KanjiSeeder] Bảng kanji_dictionary đã có {} bản ghi. Bỏ qua quá trình nạp dữ liệu.", count);
            return;
        }

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

                // Lọc các ký tự CJK Hán tự chuẩn (thông dụng từ 0x4E00 đến 0x9FAF)
                // và các bộ thủ / ký tự mở rộng BMP (<= 0xFFFF) để vừa vặn CHAR(1)
                if (codePoint < 0x3400 || codePoint > 0xFFFF) {
                    skipped++;
                    continue;
                }

                String kanjiChar = String.valueOf((char) codePoint);
                String svgContent = Files.readString(file.toPath(), StandardCharsets.UTF_8);

                // Đếm số nét bằng số thẻ <path
                short strokeCount = (short) countMatches(PATH_PATTERN, svgContent);
                if (strokeCount <= 0) {
                    strokeCount = 1;
                }

                // Trích xuất bộ thủ
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

                // Lưu theo từng lô để tối ưu hiệu năng
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

        // Lưu phần còn lại của batch
        if (!batch.isEmpty()) {
            kanjiRepository.saveAll(batch);
            totalSaved += batch.size();
            batch.clear();
        }

        log.info("[KanjiSeeder] HOÀN TẤT! Đã nạp thành công {} chữ Kanji vào bảng kanji_dictionary (Bỏ qua: {} file).", totalSaved, skipped);
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
