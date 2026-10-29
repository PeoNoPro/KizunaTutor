package com.kizuna.util;

import java.util.*;
import java.util.regex.Pattern;

/**
 * Tiện ích chuẩn hóa cách đọc Hiragana và Onyomi/Kunyomi cho Chữ Hán Kanji.
 * Chuyển đổi toàn bộ Katakana sang Hiragana, làm sạch Okurigana (dấu chấm, dấu gạch nối)
 * để tạo ra tập hợp các chuỗi Hiragana hợp lệ phục vụ kiểm tra gõ phím trong Minigame.
 */
public final class KanjiReadingNormalizer {

    private static final Pattern REMOVE_PARENS = Pattern.compile("\\([^)]*\\)|（[^）]*）");
    private static final Pattern SPLIT_DELIMITERS = Pattern.compile("[,;、/・\\s]+");

    private KanjiReadingNormalizer() {
    }

    /**
     * Chuyển một chuỗi ký tự Katakana sang Hiragana tương ứng trong Unicode.
     * Dải Katakana: 0x30A1 - 0x30F6 tương ứng Hiragana: 0x3041 - 0x3096 (độ lệch 0x60).
     */
    public static String katakanaToHiragana(String text) {
        if (text == null || text.isBlank()) {
            return "";
        }
        StringBuilder sb = new StringBuilder(text.length());
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c >= '\u30A1' && c <= '\u30F6') {
                sb.append((char) (c - 0x60));
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    /**
     * Trích xuất và chuẩn hóa tất cả các cách đọc Hiragana hợp lệ từ Onyomi và Kunyomi.
     * Người chơi gõ bất kỳ cách đọc nào trong danh sách này đều được tính là gõ đúng.
     *
     * @param kunyomi Chuỗi Kunyomi gốc từ DB (ví dụ: "い.く, ゆ.く, おこな.う")
     * @param onyomi  Chuỗi Onyomi gốc từ DB (ví dụ: "コウ, ギョウ")
     * @return Danh sách các chuỗi Hiragana duy nhất, không rỗng
     */
    public static List<String> extractNormalizedReadings(String kunyomi, String onyomi) {
        Set<String> resultSet = new LinkedHashSet<>();

        // 1. Phân tích Kunyomi
        if (kunyomi != null && !kunyomi.isBlank() && !"-".equals(kunyomi.trim())) {
            String cleaned = REMOVE_PARENS.matcher(kunyomi).replaceAll("");
            String[] tokens = SPLIT_DELIMITERS.split(cleaned);
            for (String raw : tokens) {
                String token = raw.trim();
                if (token.isEmpty() || "-".equals(token)) {
                    continue;
                }
                // Dạng đầy đủ khi bỏ dấu chấm okurigana: "い.く" -> "いく"
                String fullWord = token.replace(".", "").replace("-", "").replace("~", "").trim();
                if (!fullWord.isEmpty()) {
                    resultSet.add(katakanaToHiragana(fullWord));
                }

                // Dạng thân từ (trước dấu chấm okurigana): "い.く" -> "い" (chỉ thêm nếu độ dài >= 1)
                if (token.contains(".")) {
                    String stem = token.substring(0, token.indexOf('.')).replace("-", "").trim();
                    if (!stem.isEmpty() && stem.length() >= 1) {
                        resultSet.add(katakanaToHiragana(stem));
                    }
                }
            }
        }

        // 2. Phân tích Onyomi (thường là Katakana: "コウ, ギョウ")
        if (onyomi != null && !onyomi.isBlank() && !"-".equals(onyomi.trim())) {
            String cleaned = REMOVE_PARENS.matcher(onyomi).replaceAll("");
            String[] tokens = SPLIT_DELIMITERS.split(cleaned);
            for (String raw : tokens) {
                String token = raw.trim().replace(".", "").replace("-", "");
                if (token.isEmpty()) {
                    continue;
                }
                String hira = katakanaToHiragana(token);
                if (!hira.isEmpty()) {
                    resultSet.add(hira);
                }
            }
        }

        return new ArrayList<>(resultSet);
    }

    /**
     * Xác định cách đọc chính đại diện (Primary Reading) ưu tiên để hiển thị gợi ý
     * cho người chơi trên giao diện Minigame.
     */
    public static String determinePrimaryReading(String kunyomi, String onyomi, List<String> readings) {
        if (readings != null && !readings.isEmpty()) {
            // Nếu có cách đọc Kunyomi hoàn chỉnh (ví dụ: "いく"), ưu tiên cách đọc đầu tiên
            return readings.get(0);
        }
        if (kunyomi != null && !kunyomi.isBlank() && !"-".equals(kunyomi.trim())) {
            String first = kunyomi.split("[,;、/・\\s]+")[0].replace(".", "").replace("-", "").trim();
            if (!first.isEmpty()) {
                return katakanaToHiragana(first);
            }
        }
        if (onyomi != null && !onyomi.isBlank() && !"-".equals(onyomi.trim())) {
            String first = onyomi.split("[,;、/・\\s]+")[0].replace(".", "").replace("-", "").trim();
            if (!first.isEmpty()) {
                return katakanaToHiragana(first);
            }
        }
        return "";
    }
}
