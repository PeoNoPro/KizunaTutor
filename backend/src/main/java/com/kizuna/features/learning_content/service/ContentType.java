package com.kizuna.features.learning_content.service;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

/**
 * Ba loại học liệu cán bộ học vụ nhập từ Excel, mỗi loại ghi vào bảng đã có sẵn:
 * KANJI → kanji_dictionary (+ kanjis), VOCABULARY → vocabularies (+ vocabulary_kanjis), GRAMMAR → grammars.
 * Cột được nhận theo tên tiêu đề (không theo vị trí) nên tệp chỉ cần "định dạng tương tự".
 */
public enum ContentType {

    KANJI("Kanji", List.of(
            Field.of("character", "Kanji", true, 1, "kanji", "chu kanji", "chu han", "han tu"),
            Field.of("hanViet", "Hán Việt", false, 50, "han viet", "am han viet"),
            Field.of("onyomi", "Onyomi", false, 150, "onyomi", "am on"),
            Field.of("kunyomi", "Kunyomi", false, 300, "kunyomi", "am kun"),
            Field.of("meaning", "Nghĩa tiếng Việt", true, 200, "nghia tieng viet", "nghia", "y nghia", "y nghia tieng viet"),
            Field.of("jlpt", "JLPT", false, 2, "jlpt", "cap do jlpt", "cap do"),
            Field.of("strokes", "Số nét", false, 3, "so net", "tong so net", "stroke count", "strokes"))),

    VOCABULARY("Từ vựng", List.of(
            Field.of("word", "Từ vựng", true, 255, "tu vung", "tu"),
            Field.of("reading", "Cách đọc", false, 255, "cach doc"),
            Field.of("romaji", "Phiên âm Romaji", false, 255, "phien am romaji", "romaji"),
            Field.of("hanViet", "Âm Hán Việt", false, 255, "am han viet", "han viet"),
            Field.of("partOfSpeech", "Từ loại", false, 100, "tu loai"),
            Field.of("meaning", "Nghĩa tiếng Việt", true, 0, "nghia tieng viet", "nghia"),
            Field.of("exampleSentence", "Câu ví dụ", false, 0, "cau vi du"),
            Field.of("exampleFurigana", "Câu ví dụ Furigana", false, 0, "cau vi du furigana"),
            Field.of("exampleMeaning", "Dịch nghĩa câu ví dụ", false, 0, "dich nghia cau vi du"),
            Field.of("jmdictId", "Mã JMdict ID", false, 50, "ma jmdict id", "jmdict id", "ma jmdict"),
            Field.of("jlpt", "Cấp độ JLPT", false, 2, "cap do jlpt", "jlpt", "cap do"))),

    GRAMMAR("Ngữ pháp", List.of(
            Field.of("code", "Mã định danh (ID)", true, 50, "ma dinh danh", "ma ngu phap", "id"),
            Field.of("pattern", "Mẫu cấu trúc ngữ pháp", true, 255, "mau cau truc ngu phap", "mau ngu phap", "cau truc"),
            Field.of("romaji", "Phiên âm Romaji", false, 255, "phien am romaji", "romaji"),
            Field.of("meaning", "Ý nghĩa tiếng Việt", true, 0, "y nghia tieng viet", "y nghia", "nghia tieng viet"),
            Field.of("formation", "Công thức kết hợp", false, 0, "cong thuc ket hop", "cong thuc"),
            Field.of("notes", "Giải thích & Ghi chú", false, 0, "giai thich ghi chu", "ghi chu"),
            Field.of("exampleSentence", "Câu ví dụ tiêu biểu", false, 0, "cau vi du tieu bieu", "cau vi du"),
            Field.of("exampleFurigana", "Câu ví dụ Furigana", false, 0, "cau vi du furigana"),
            Field.of("exampleMeaning", "Dịch nghĩa câu ví dụ", false, 0, "dich nghia cau vi du"),
            Field.of("extendedExamples", "Các câu ví dụ mở rộng", false, 0, "cac cau vi du mo rong", "vi du mo rong"),
            Field.of("tags", "Thẻ phân loại (Tags)", false, 255, "the phan loai", "tags", "the"),
            Field.of("jlpt", "Cấp độ JLPT", false, 2, "cap do jlpt", "jlpt", "cap do")));

    private final String label;
    private final List<Field> fields;

    ContentType(String label, List<Field> fields) {
        this.label = label;
        this.fields = fields;
    }

    public String label() {
        return label;
    }

    public List<Field> fields() {
        return fields;
    }

    public List<Field> requiredFields() {
        return fields.stream().filter(Field::required).toList();
    }

    /** Khóa nhận diện trùng lặp: Kanji theo chữ, từ vựng theo (từ, cách đọc), ngữ pháp theo mã định danh. */
    public String keyOf(java.util.Map<String, String> values) {
        return switch (this) {
            case KANJI -> values.get("character");
            case VOCABULARY -> values.get("word") + "\u0000" + values.get("reading");
            case GRAMMAR -> values.get("code").toLowerCase(Locale.ROOT);
        };
    }

    /** Mô tả khóa cho thông báo lỗi. */
    public String describeKey(java.util.Map<String, String> values) {
        return switch (this) {
            case KANJI -> "chữ " + values.get("character");
            case VOCABULARY -> "từ " + values.get("word") + " (" + values.get("reading") + ")";
            case GRAMMAR -> "mã " + values.get("code");
        };
    }

    /** Cách ghi một mục đã thêm vào lịch sử nhập. */
    public String describeItem(java.util.Map<String, String> values) {
        return switch (this) {
            case KANJI -> values.get("character");
            case VOCABULARY -> values.get("word").equals(values.get("reading"))
                    ? values.get("word") : values.get("word") + " (" + values.get("reading") + ")";
            case GRAMMAR -> values.get("code") + " · " + values.get("pattern");
        };
    }

    /** kanji | vocabulary | grammar trên đường dẫn API. */
    public static Optional<ContentType> fromPath(String value) {
        if (value == null) return Optional.empty();
        return switch (value.trim().toLowerCase(Locale.ROOT)) {
            case "kanji" -> Optional.of(KANJI);
            case "vocabulary", "vocabularies" -> Optional.of(VOCABULARY);
            case "grammar", "grammars" -> Optional.of(GRAMMAR);
            default -> Optional.empty();
        };
    }

    /**
     * Một cột của tệp. maxLength = 0 nghĩa là cột TEXT không giới hạn;
     * aliases là tên tiêu đề đã chuẩn hóa (bỏ dấu, chữ thường, bỏ phần trong ngoặc).
     */
    public record Field(String name, String header, boolean required, int maxLength, Set<String> aliases) {
        static Field of(String name, String header, boolean required, int maxLength, String... aliases) {
            return new Field(name, header, required, maxLength, Set.of(aliases));
        }
    }
}
