package com.kizuna.dto.minigame;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KanjiFolderSummaryDto {
    private UUID folderId;
    private String folderName;
    private String description;
    private String color;
    private Boolean isDefault;
    private long kanjiCount;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}
