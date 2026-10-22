package com.kizuna.dto.kanji;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * Các DTO phục vụ tính năng "Tra cứu chữ Hán bằng nét vẽ viết tay thời gian thực" (Handwriting Kanji Search).
 */
public class HandwritingSearchDto {

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class StrokePoint {
        private double x;
        private double y;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class SearchRequest {
        private int strokeCount;
        @Builder.Default
        private List<List<StrokePoint>> strokes = new ArrayList<>();
        @Builder.Default
        private List<String> candidates = new ArrayList<>();
        @Builder.Default
        private int limit = 12;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class MatchItem {
        private String character;
        private String unicodeHex;
        private String hanViet;
        private String meaning;
        private Integer totalStrokes;
        private String onyomi;
        private String kunyomi;
        private String jlpt;
        private double similarityScore;
        private String svgSolution;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class SearchResponse {
        @Builder.Default
        private List<MatchItem> matches = new ArrayList<>();
        @Builder.Default
        private String sourceEngine = "HYBRID";
        private int totalFound;

        public SearchResponse(List<MatchItem> matches, String sourceEngine) {
            this.matches = matches != null ? matches : new ArrayList<>();
            this.sourceEngine = sourceEngine;
            this.totalFound = this.matches.size();
        }
    }
}
