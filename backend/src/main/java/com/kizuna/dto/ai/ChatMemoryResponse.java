package com.kizuna.dto.ai;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatMemoryResponse {
    private String status;
    private String memoryId;
    private String summary;
    private List<String> relevantMemories;
    private Map<String, Object> details;
}
