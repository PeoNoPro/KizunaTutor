package com.kizuna.core.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;

import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.*;

/** Lỗi ràng buộc chống trùng lịch (schedule_entries, SQLSTATE 23P01) được đổi thành thông báo 409 dễ hiểu. */
class ScheduleOverlapMessageTest {

    private static Exception wrapped(String constraint) {
        SQLException sql = new SQLException(
                "ERROR: conflicting key value violates exclusion constraint \"" + constraint + "\"", "23P01");
        return new DataIntegrityViolationException("could not execute statement", new RuntimeException(sql));
    }

    @Test
    @DisplayName("Gia sư trùng lịch → nhắc tải lại lịch và chọn giờ khác")
    void tutorOverlap() {
        assertEquals("Giờ này vừa có lịch khác. Vui lòng tải lại lịch và chọn giờ khác.",
                GlobalExceptionHandler.scheduleOverlapMessage(wrapped("ex_schedule_entries_tutor_overlap")));
    }

    @Test
    @DisplayName("Học viên trùng lịch của chính mình → báo đã có buổi học khác")
    void learnerOverlap() {
        assertEquals("Bạn đã có một buổi học khác trùng giờ này. Vui lòng chọn giờ khác.",
                GlobalExceptionHandler.scheduleOverlapMessage(wrapped("ex_schedule_entries_learner_overlap")));
    }

    @Test
    @DisplayName("Lỗi khác không bị nhận nhầm là trùng lịch")
    void otherErrorsIgnored() {
        assertNull(GlobalExceptionHandler.scheduleOverlapMessage(
                new DataIntegrityViolationException("x", new SQLException("duplicate key", "23505"))));
        assertNull(GlobalExceptionHandler.scheduleOverlapMessage(new IllegalStateException("boom")));
    }
}
