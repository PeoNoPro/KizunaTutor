package com.kizuna.features.booking;

import com.kizuna.features.booking.service.ConnectionBusyLoader;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowCallbackHandler;

import java.time.LocalDate;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** Câu query lịch bận ghép chuỗi: số dấu "?" phải khớp số tham số, và có đủ phần lịch đã xác nhận của học viên với gia sư khác. */
class ConnectionBusyLoaderTest {

    @Test
    @DisplayName("Số tham số khớp số dấu ?, có nhánh lịch của học viên với gia sư khác (LA, LW, LT), chỉ tính lịch đã chấp nhận")
    void placeholdersMatchArguments() {
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        new ConnectionBusyLoader(jdbc).load(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                LocalDate.now(), LocalDate.now().plusDays(30));

        ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Object[]> args = ArgumentCaptor.forClass(Object[].class);
        verify(jdbc).query(sql.capture(), any(RowCallbackHandler.class), args.capture());

        long placeholders = sql.getValue().chars().filter(c -> c == '?').count();
        assertEquals(placeholders, args.getValue().length);
        assertTrue(sql.getValue().contains("'LA'"));
        assertTrue(sql.getValue().contains("'LW'"));
        assertTrue(sql.getValue().contains("'LT'"));
        // Chỉ lịch ĐÃ XÁC NHẬN của học viên với gia sư khác (cách 1)
        assertTrue(sql.getValue().contains("tb.learner_id = ? AND tb.tutor_id <> ?"));
        assertTrue(sql.getValue().contains("tb.booking_status IN ('CONFIRMED', 'RESCHEDULED') "
                + "  AND CAST(tb.scheduled_start_time AT TIME ZONE 'Asia/Ho_Chi_Minh' AS date) BETWEEN ? AND ?"));
        // Buổi học thử của học viên khác đã chốt gồm cả buổi gia sư tự dời (RESCHEDULED); yêu cầu đang chờ không tính
        assertTrue(sql.getValue().contains("AND tb.booking_status IN ('CONFIRMED', 'RESCHEDULED') UNION ALL"));
        assertFalse(sql.getValue().contains("'PENDING'"));
    }
}
