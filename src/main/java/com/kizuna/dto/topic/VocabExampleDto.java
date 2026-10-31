package com.kizuna.dto.topic;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO chứa thông tin câu ví dụ thực tế của từ vựng (ngữ liệu Tatoeba)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VocabExampleDto {
    private String ja;
    private String furigana;
    private String en;
    private String vn;
    private Integer tatoebaId;
}
