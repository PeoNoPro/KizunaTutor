package com.kizuna.features.payment.service;

import org.springframework.stereotype.Component;

import java.time.LocalDate;

/**
 * Tính thời hạn gói (FR-008, quyết định cộng nối 2026-11-04 — srs-gaps G-03).
 * Bắt đầu = hôm nay nếu không còn gói hiệu lực; ngược lại = ngày sau ngày hết hạn xa nhất.
 * Hết hạn = bắt đầu + số tháng − 1 ngày.
 */
@Component
public class MembershipPeriodCalculator {

    public record Period(LocalDate startDate, LocalDate endDate) {
    }

    public Period calculate(LocalDate today, LocalDate latestActiveEnd, int durationMonths) {
        if (today == null || durationMonths <= 0) {
            throw new IllegalArgumentException("Thời hạn gói không hợp lệ");
        }
        LocalDate start = today;
        if (latestActiveEnd != null && !latestActiveEnd.isBefore(today)) {
            start = latestActiveEnd.plusDays(1);
        }
        return new Period(start, start.plusMonths(durationMonths).minusDays(1));
    }
}
