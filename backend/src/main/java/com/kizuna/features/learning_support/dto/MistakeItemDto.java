package com.kizuna.features.learning_support.dto;


import com.kizuna.core.entity.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MistakeItemDto {
    private UUID kanjiId;
    private String kanjiCharacter;
    private String reading;
    private Integer mistakeCount;
    private String typedInput;
}
