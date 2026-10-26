package com.kizuna.dto.minigame;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MinigameSessionResponse {
    private UUID sessionId;
    private UUID folderId;
    private String folderName;
    private Integer durationSeconds;
    private Integer targetDurationSeconds;
    private Integer wordsTypedCount;
    private Integer totalMistakes;
    private Double accuracy;
    private Integer score;
    private Boolean isWon;
    private UUID failedKanjiId;
    private String failedKanjiCharacter;
    private OffsetDateTime playedAt;
    private String titleRank; // Danh hiệu đạt được (Kanji Apprentice, Master, v.v.)

    @Builder.Default
    private List<MistakeItemDto> topMistakes = new ArrayList<>();
}
