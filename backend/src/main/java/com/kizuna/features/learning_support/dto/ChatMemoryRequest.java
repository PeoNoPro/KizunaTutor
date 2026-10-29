package com.kizuna.features.learning_support.dto;


import com.kizuna.core.entity.*;
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
