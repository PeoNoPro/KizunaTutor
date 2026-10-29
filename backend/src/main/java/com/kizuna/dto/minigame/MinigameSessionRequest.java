package com.kizuna.dto.minigame;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MinigameSessionRequest {

    private UUID folderId;

    @NotNull(message = "Thời gian chơi không được để trống")
    private Integer durationSeconds;

    private Integer targetDurationSeconds;

    @NotNull(message = "Số từ gõ đúng không được để trống")
    private Integer wordsTypedCount;

    @Builder.Default
    private Integer totalMistakes = 0;

    @NotNull(message = "Độ chính xác không được để trống")
    private Double accuracy;

    @Builder.Default
    private Integer score = 0;

    @NotNull(message = "Trạng thái thắng/thua không được để trống")
    private Boolean isWon;

    private UUID failedKanjiId;

    private String failedKanjiCharacter;

    @Builder.Default
    private List<MistakeItemDto> mistakes = new ArrayList<>();
}
