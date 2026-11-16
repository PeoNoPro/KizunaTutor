package com.kizuna.features.admin.service;

import com.kizuna.core.exception.BusinessException;
import com.kizuna.features.admin.repository.OperationsDashboardRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;

import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.*;

/**
 * Tổng quan Admin (bổ sung 2026-11-12): tỷ lệ tăng giảm của tài khoản mới (kỳ đang xem so với kỳ liền trước cùng độ dài)
 * (Tình hình yêu cầu hỗ trợ chuyển sang Tổng quan Quản lý: GET /api/v1/tickets/overview.)
 */
@Service
@RequiredArgsConstructor
public class OperationsDashboardService {
    private static final ZoneId ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final Set<String> STAFF = Set.of("ROLE_ADMIN", "ROLE_MANAGER", "ROLE_CS_STAFF", "ROLE_ACADEMIC_OFFICER");
    private final OperationsActorResolver actors;
    private final OperationsDashboardRepository dashboard;
    private final Clock operationsClock;

    public Map<String, Object> overview(LocalDate start, LocalDate end) {
        actors.requireAdmin();
        var period = OperationsIncidentReportService.period(start, end, OffsetDateTime.now(operationsClock));
        var currentStart = period.start().toLocalDate();
        long days = ChronoUnit.DAYS.between(currentStart, period.endDate()) + 1;
        var previousStart = currentStart.minusDays(days);
        try {
            var groups = new LinkedHashMap<String, long[]>();
            for (var key : List.of("total", "ROLE_TUTOR", "ROLE_LEARNER", "staff")) groups.put(key, new long[2]);
            for (var row : dashboard.newAccounts(previousStart.atStartOfDay(ZONE).toOffsetDateTime(), period.start(),
                    period.endDate().plusDays(1).atStartOfDay(ZONE).toOffsetDateTime())) {
                add(groups.get("total"), row);
                if (groups.containsKey(row.role())) add(groups.get(row.role()), row);
                if (STAFF.contains(row.role())) add(groups.get("staff"), row);
            }
            var trends = new LinkedHashMap<String, Object>();
            groups.forEach((key, value) -> trends.put(key, trend(value[0], value[1])));
            var result = new LinkedHashMap<String, Object>();
            result.put("startDate", currentStart); result.put("endDate", period.endDate());
            result.put("previousStartDate", previousStart); result.put("previousEndDate", currentStart.minusDays(1));
            result.put("newAccounts", trends);
            return result;
        } catch (DataAccessException exception) {
            throw new BusinessException(503, "Tạm thời chưa tải được số liệu tổng quan. Vui lòng thử lại sau.");
        }
    }

    private static void add(long[] target, OperationsDashboardRepository.NewAccounts row) { target[0] += row.current(); target[1] += row.previous(); }

    /** Kỳ trước bằng 0 thì không tính phần trăm (null) để giao diện ghi "Mới" thay vì +∞%. */
    static Map<String, Object> trend(long current, long previous) {
        var m = new LinkedHashMap<String, Object>();
        m.put("current", current); m.put("previous", previous);
        m.put("changePercent", previous == 0 ? null : Math.round((current - previous) * 1000.0 / previous) / 10.0);
        return m;
    }
}
