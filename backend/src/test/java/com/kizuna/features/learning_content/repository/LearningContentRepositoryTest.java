package com.kizuna.features.learning_content.repository;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Liên kết từ vựng – Kanji chỉ được ghép đúng từng cặp (từ, cách đọc) đã thêm (review PR):
 * hai mảng gửi xuống câu SQL phải song song theo dòng, không gộp trùng riêng từng mảng.
 */
class LearningContentRepositoryTest {

    @Test
    void capTuVaCachDocGiuDungThuTuTheoDong() {
        String[][] pairs = LearningContentRepository.wordReadingPairs(List.of(
                Map.of("word", "相", "reading", "あい"),
                Map.of("word", "間", "reading", "かん"),
                Map.of("word", "相", "reading", "そう")));

        assertThat(pairs[0]).containsExactly("相", "間", "相");
        assertThat(pairs[1]).containsExactly("あい", "かん", "そう");
    }

    @Test
    void khongGopTrungRiengTungMang_tranhTaoToHopKhongCoTrongTep() {
        // Nếu gộp trùng riêng từng mảng, độ dài hai mảng lệch nhau và mất quan hệ từng cặp.
        String[][] pairs = LearningContentRepository.wordReadingPairs(List.of(
                Map.of("word", "相", "reading", "あい"),
                Map.of("word", "相", "reading", "そう")));

        assertThat(pairs[0]).hasSameSizeAs(pairs[1]).hasSize(2);
    }
}
