package com.kizuna.dto.minigame;

import com.kizuna.dto.kanji.KanjiCardDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * DTO kết quả nạp ngẫu nhiên chữ Kanji vào bộ thẻ
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AddRandomKanjiResponse {
    private boolean success;
    private int addedCount;
    private long totalFolderCount;
    private String message;

    @Builder.Default
    private List<KanjiCardDto> addedItems = new ArrayList<>();
}
