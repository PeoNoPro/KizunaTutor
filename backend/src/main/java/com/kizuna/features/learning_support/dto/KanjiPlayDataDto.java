package com.kizuna.features.learning_support.dto;


import com.kizuna.core.entity.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * DTO dữ liệu phục vụ minigame bắn chữ rơi Kanji Rain.
 * Chứa mặt chữ, âm Hán Việt, ý nghĩa và danh sách các cách đọc Hiragana đã được chuẩn hóa.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KanjiPlayDataDto {
    private UUID kanjiId;
    private String character;
    private String hanViet;
    private String meaning;
    private String jlptLevel;
    private Short strokeCount;
    private String onyomi;
    private String kunyomi;

    /**
     * Cách đọc đại diện được gợi ý bên dưới mặt chữ (ví dụ: "い.く" -> "いく" hoặc "こう")
     */
    private String primaryReading;

    /**
     * Danh sách tất cả các cách đọc Hiragana hợp lệ mà người chơi gõ đều được chấp nhận là ĐÚNG.
     * Đã convert toàn bộ Katakana sang Hiragana và bỏ các ký tự dấu okurigana như dấu chấm '.', dấu '-'.
     */
    @Builder.Default
    private List<String> readings = new ArrayList<>();
}
