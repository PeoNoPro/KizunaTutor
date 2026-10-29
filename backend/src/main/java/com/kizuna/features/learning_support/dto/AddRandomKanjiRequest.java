package com.kizuna.features.learning_support.dto;


import com.kizuna.core.entity.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO yêu cầu nạp ngẫu nhiên chữ Kanji vào bộ thẻ theo cấp độ JLPT
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AddRandomKanjiRequest {
    /**
     * Cấp độ JLPT ("N5", "N4", "N3", "N2", "N1", "ALL")
     */
    private String jlpt;

    /**
     * Số lượng chữ cần nạp ngẫu nhiên (mặc định 10)
     */
    @Builder.Default
    private Integer quantity = 10;
}
