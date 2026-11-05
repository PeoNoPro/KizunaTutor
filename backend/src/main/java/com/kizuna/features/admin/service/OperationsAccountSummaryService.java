package com.kizuna.features.admin.service;

import com.kizuna.features.admin.dto.OperationsAccountSummaryResponse;
import com.kizuna.features.admin.repository.OperationsAccountSummaryRepository;
import com.kizuna.core.exception.BusinessException;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;
import java.time.Clock;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class OperationsAccountSummaryService {
    private final OperationsActorResolver actors;
    private final OperationsAccountSummaryRepository accounts;
    private final Clock operationsClock;

    public OperationsAccountSummaryResponse read() {
        actors.requireAdmin();
        try { return accounts.read(OffsetDateTime.now(operationsClock)); }
        catch (DataAccessException exception) {
            throw new BusinessException(503, "Tạm thời chưa tải được thống kê tài khoản. Vui lòng thử lại sau.");
        }
    }

    /** Cơ cấu vai trò/trạng thái lấy tại thời điểm truy vấn; tài khoản mới lấy theo ngày tạo trong kỳ. */
    public Map<String, Object> statistics(LocalDate start, LocalDate end) {
        actors.requireAdmin();
        var now = OffsetDateTime.now(operationsClock);
        var period = OperationsIncidentReportService.period(start, end, now);
        var endExclusive = period.endDate().plusDays(1).atStartOfDay(period.start().toZonedDateTime().getZone()).toOffsetDateTime();
        List<OperationsAccountSummaryRepository.Group> rows;
        try { rows = accounts.statistics(period.start(), endExclusive); }
        catch (DataAccessException exception) {
            throw new BusinessException(503, "Tạm thời chưa tải được thống kê tài khoản. Vui lòng thử lại sau.");
        }
        var created = new HashMap<String, Long>();
        rows.stream().filter(row -> row.kind().equals("created")).forEach(row -> created.put(row.key(), row.total()));
        var days = new ArrayList<Map<String, Object>>();
        for (var day = period.start().toLocalDate(); !day.isAfter(period.endDate()); day = day.plusDays(1)) {
            days.add(Map.of("date", day.toString(), "count", created.getOrDefault(day.toString(), 0L)));
        }
        var result = new LinkedHashMap<String, Object>();
        result.put("startDate", period.start().toLocalDate());
        result.put("endDate", period.endDate());
        result.put("generatedAt", now);
        result.put("roles", labelled(rows, "role", ROLE_LABELS, "Nhóm khác"));
        result.put("statuses", labelled(rows, "status", STATUS_LABELS, "Trạng thái khác"));
        result.put("newAccounts", days);
        return result;
    }

    private List<Map<String, Object>> labelled(List<OperationsAccountSummaryRepository.Group> rows, String kind, Map<String, String> labels, String fallback) {
        var totals = new LinkedHashMap<String, Long>();
        labels.values().forEach(label -> totals.put(label, 0L));
        rows.stream().filter(row -> row.kind().equals(kind)).forEach(row -> totals.merge(labels.getOrDefault(row.key(), fallback), row.total(), Long::sum));
        return totals.entrySet().stream().map(entry -> Map.<String, Object>of("label", entry.getKey(), "count", entry.getValue())).toList();
    }

    private static final Map<String, String> ROLE_LABELS = ordered("ROLE_LEARNER", "Học viên", "ROLE_TUTOR", "Gia sư", "ROLE_CS_STAFF", "Chăm sóc khách hàng",
        "ROLE_ACADEMIC_OFFICER", "Cán bộ học vụ", "ROLE_MANAGER", "Quản lý vận hành", "ROLE_ADMIN", "Quản trị viên hệ thống");
    private static final Map<String, String> STATUS_LABELS = ordered("ACTIVE", "Đang hoạt động", "PENDING", "Chờ kích hoạt", "SUSPENDED", "Tạm khóa",
        "BANNED", "Đã bị khóa", "INACTIVE", "Ngừng hoạt động");
    private static Map<String, String> ordered(String... pairs) {
        var map = new LinkedHashMap<String, String>();
        for (int i = 0; i < pairs.length; i += 2) map.put(pairs[i], pairs[i + 1]);
        return map;
    }
}
