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
public class AddVocabToDeckRequest {
    private UUID deckId;
    private String newDeckName; // Nếu muốn tạo deck mới ngay tại chỗ
}
