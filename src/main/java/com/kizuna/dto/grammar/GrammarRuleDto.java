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
public class GrammarRuleDto {
    private String id;
    private String pattern;
    private String romaji;
    private String level; // N5, N4, N3, N2, N1
    private String meaning;
    private String meaningVn;
    private String formation;
    private String notes;
    private List<String> tags;
    private List<GrammarExampleDto> examples;
}
