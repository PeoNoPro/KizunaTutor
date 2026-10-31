package com.kizuna.dto.topic;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TopicDto {
    private UUID id;
    private String code;
    private String titleVn;
    private String titleJp;
    private String icon;
    private String description;
    private String colorToken;
    private Integer displayOrder;
    private Long vocabCount;
}
