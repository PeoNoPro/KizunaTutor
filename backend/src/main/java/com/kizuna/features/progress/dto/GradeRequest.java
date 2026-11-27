package com.kizuna.features.progress.dto;

import java.math.BigDecimal;

/** Chấm điểm (UC-67). */
public record GradeRequest(BigDecimal grade, String feedback) {
}
