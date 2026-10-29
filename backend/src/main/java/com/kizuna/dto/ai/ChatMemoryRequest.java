package com.kizuna.dto.ai;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatMemoryRequest {
    private String learnerId;
    private String tutorId;
    private String message;
    private String role; // "user", "tutor", "assistant"
    private Map<String, Object> metadata;
}
