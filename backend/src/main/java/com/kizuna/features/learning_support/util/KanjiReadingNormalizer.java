package com.kizuna.features.learning_support.util;

import java.util.*;
import java.util.regex.Pattern;

/**
 * Tiện ích chuẩn hóa cách đọc Hiragana, Onyomi, Kunyomi và Romaji cho chữ Hán Kanji.
 * Cho phép người học gõ bằng bàn phím tiếng Nhật (Hiragana/Katakana), Romaji hoặc âm Hán Việt.
 */
public final class KanjiReadingNormalizer {

    private static final Pattern REMOVE_PARENS = Pattern.compile("\\([^)]*\\)|（[^）]*）");
    private static final Pattern SPLIT_DELIMITERS = Pattern.compile("[,;、/・\\s]+");

    // Bảng tra cứu chuyển đổi cơ bản giữa Hiragana và Romaji
    private static final Map<String, String> HIRAGANA_TO_ROMAJI = new HashMap<>();
    private static final Map<String, String> ROMAJI_TO_HIRAGANA = new HashMap<>();

    static {
        // Nguyên âm & hàng K
        addMapping("あ", "a"); addMapping("い", "i"); addMapping("う", "u"); addMapping("え", "e"); addMapping("お", "o");
        addMapping("か", "ka"); addMapping("き", "ki"); addMapping("く", "ku"); addMapping("け", "ke"); addMapping("ko", "ko");
        addMapping("が", "ga"); addMapping("ぎ", "gi"); addMapping("ぐ", "gu"); addMapping("げ", "ge"); addMapping("ご", "go");
        // Hàng S, T
        addMapping("さ", "sa"); addMapping("し", "shi"); addMapping("す", "su"); addMapping("せ", "se"); addMapping("そ", "so");
        addMapping("ざ", "za"); addMapping("じ", "ji"); addMapping("ず", "zu"); addMapping("ぜ", "ze"); addMapping("ぞ", "zo");
        addMapping("た", "ta"); addMapping("ち", "chi"); addMapping("つ", "tsu"); addMapping("て", "te"); addMapping("と", "to");
        addMapping("だ", "da"); addMapping("ぢ", "ji"); addMapping("づ", "zu"); addMapping("で", "de"); addMapping("ど", "do");
        // Hàng N, H
        addMapping("な", "na"); addMapping("に", "ni"); addMapping("ぬ", "nu"); addMapping("ね", "ne"); addMapping("の", "no");
        addMapping("は", "ha"); addMapping("ひ", "hi"); addMapping("ふ", "fu"); addMapping("へ", "he"); addMapping("ほ", "ho");
        addMapping("ば", "ba"); addMapping("び", "bi"); addMapping("ぶ", "bu"); addMapping("べ", "be"); addMapping("ぼ", "bo");
        addMapping("ぱ", "pa"); addMapping("ぴ", "pi"); addMapping("ぷ", "pu"); addMapping("ぺ", "pe"); addMapping("ぽ", "po");
        // Hàng M, Y, R, W
        addMapping("ま", "ma"); addMapping("み", "mi"); addMapping("む", "mu"); addMapping("め", "me"); addMapping("も", "mo");
        addMapping("や", "ya"); addMapping("ゆ", "yu"); addMapping("よ", "yo");
        addMapping("ら", "ra"); addMapping("り", "ri"); addMapping("る", "ru"); addMapping("れ", "re"); addMapping("ろ", "ro");
        addMapping("わ", "wa"); addMapping("を", "wo"); addMapping("ん", "n");
        // Âm ghép phổ biến
        addMapping("きゃ", "kya"); addMapping("きゅ", "kyu"); addMapping("きょ", "kyo");
        addMapping("しゃ", "sha"); addMapping("しゅ", "shu"); addMapping("しょ", "sho");
        addMapping("ちゃ", "cha"); addMapping("ちゅ", "chu"); addMapping("ちょ", "cho");
        addMapping("にゃ", "nya"); addMapping("にゅ", "nyu"); addMapping("にょ", "nyo");
        addMapping("ひゃ", "hya"); addMapping("ひゅ", "hyu"); addMapping("ひょ", "hyo");
        addMapping("みゃ", "mya"); addMapping("みゅ", "myu"); addMapping("みょ", "myo");
        addMapping("りゃ", "rya"); addMapping("りゅ", "ryu"); addMapping("りょ", "ryo");
        addMapping("ぎゃ", "gya"); addMapping("ぎゅ", "gyu"); addMapping("ぎょ", "gyo");
        addMapping("じゃ", "ja"); addMapping("じゅ", "ju"); addMapping("じょ", "jo");
        addMapping("びゃ", "bya"); addMapping("びゅ", "byu"); addMapping("びょ", "byo");
        addMapping("ぴゃ", "pya"); addMapping("ぴゅ", "pyu"); addMapping("ぴょ", "pyo");
    }

    private static void addMapping(String hira, String roma) {
        HIRAGANA_TO_ROMAJI.put(hira, roma);
        ROMAJI_TO_HIRAGANA.putIfAbsent(roma, hira);
    }

    private KanjiReadingNormalizer() {
    }

    /**
     * Chuyển một chuỗi ký tự Katakana sang Hiragana tương ứng trong bảng mã Unicode.
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
     * Trích xuất và chuẩn hóa tất cả các cách đọc hợp lệ từ Kunyomi và Onyomi.
     * Tự động bổ sung cả dạng Hiragana, Katakana chuyển đổi và Romaji tương ứng.
     */
    public static List<String> extractNormalizedReadings(String kunyomi, String onyomi, String hanViet) {
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
                String fullWord = token.replace(".", "").replace("-", "").replace("~", "").trim();
                if (!fullWord.isEmpty()) {
                    String hira = katakanaToHiragana(fullWord);
                    resultSet.add(hira);
                    String roma = toRomaji(hira);
                    if (!roma.isEmpty()) resultSet.add(roma);
                }

                if (token.contains(".")) {
                    String stem = token.substring(0, token.indexOf('.')).replace("-", "").trim();
                    if (!stem.isEmpty()) {
                        String hiraStem = katakanaToHiragana(stem);
                        resultSet.add(hiraStem);
                        String romaStem = toRomaji(hiraStem);
                        if (!romaStem.isEmpty()) resultSet.add(romaStem);
                    }
                }
            }
        }

        // 2. Phân tích Onyomi
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
                    String roma = toRomaji(hira);
                    if (!roma.isEmpty()) resultSet.add(roma);
                }
            }
        }

        // 3. Thêm âm Hán Việt không dấu và có dấu (chữ thường) để người dùng gõ linh hoạt
        if (hanViet != null && !hanViet.isBlank()) {
            String cleanHv = hanViet.trim().toLowerCase();
            resultSet.add(cleanHv);
            String unaccented = removeVietnameseAccents(cleanHv);
            if (!unaccented.isEmpty() && !unaccented.equals(cleanHv)) {
                resultSet.add(unaccented);
            }
        }

        return new ArrayList<>(resultSet);
    }

    /**
     * Xác định cách đọc chính đại diện để hiển thị gợi ý trên thẻ bài.
     */
    public static String determinePrimaryReading(String kunyomi, String onyomi) {
        if (kunyomi != null && !kunyomi.isBlank() && !"-".equals(kunyomi.trim())) {
            String cleaned = REMOVE_PARENS.matcher(kunyomi).replaceAll("");
            String[] tokens = SPLIT_DELIMITERS.split(cleaned);
            for (String token : tokens) {
                String trimmed = token.replace("-", "").trim();
                if (!trimmed.isEmpty()) {
                    return katakanaToHiragana(trimmed.replace(".", ""));
                }
            }
        }
        if (onyomi != null && !onyomi.isBlank() && !"-".equals(onyomi.trim())) {
            String cleaned = REMOVE_PARENS.matcher(onyomi).replaceAll("");
            String[] tokens = SPLIT_DELIMITERS.split(cleaned);
            for (String token : tokens) {
                String trimmed = token.replace("-", "").trim();
                if (!trimmed.isEmpty()) {
                    return katakanaToHiragana(trimmed.replace(".", ""));
                }
            }
        }
        return "";
    }

    /**
     * Chuyển đổi một chuỗi Hiragana ngắn sang Romaji
     */
    public static String toRomaji(String hira) {
        if (hira == null || hira.isEmpty()) return "";
        StringBuilder sb = new StringBuilder();
        int i = 0;
        while (i < hira.length()) {
            // Kiểm tra âm ghép 2 ký tự (きゃ, しゃ, v.v.)
            if (i + 1 < hira.length()) {
                String pair = hira.substring(i, i + 2);
                if (HIRAGANA_TO_ROMAJI.containsKey(pair)) {
                    sb.append(HIRAGANA_TO_ROMAJI.get(pair));
                    i += 2;
                    continue;
                }
            }
            // Ký tự đơn
            String single = hira.substring(i, i + 1);
            if (HIRAGANA_TO_ROMAJI.containsKey(single)) {
                sb.append(HIRAGANA_TO_ROMAJI.get(single));
            } else {
                sb.append(single);
            }
            i++;
        }
        return sb.toString();
    }

    /**
     * Xóa dấu tiếng Việt
     */
    public static String removeVietnameseAccents(String text) {
        if (text == null) return "";
        String nfdNormalizedString = java.text.Normalizer.normalize(text, java.text.Normalizer.Form.NFD);
        Pattern pattern = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");
        return pattern.matcher(nfdNormalizedString).replaceAll("").replace("đ", "d").replace("Đ", "D");
    }
}
