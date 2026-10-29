package com.kizuna.features.classroom.dto;


import com.kizuna.core.entity.*;
import com.kizuna.features.learning_support.dto.KanjiCardDto;
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
public class KanjiFolderSummaryDto {
    private UUID folderId;
    private String folderName;
    private String description;
    private String color;
    private Boolean isDefault;
    private long kanjiCount;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;

    @Builder.Default
    private List<KanjiCardDto> items = new ArrayList<>();

    @Builder.Default
    private List<String> sampleChars = new ArrayList<>();
}
