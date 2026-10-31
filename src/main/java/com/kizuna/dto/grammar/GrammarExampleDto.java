package com.kizuna.dto.grammar;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GrammarExampleDto {
    private String ja;
    private String furigana;
    private String en;
    private String vn;
}
