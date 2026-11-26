package com.kizuna.features.tutor_search.unit;

import com.kizuna.core.exception.BadRequestException;
import com.kizuna.core.exception.ConflictException;
import com.kizuna.features.tutor_search.dto.CreateAvailabilityRequestDTO;
import com.kizuna.features.tutor_search.dto.TutorAvailabilityDTO;
import com.kizuna.features.tutor_search.entity.TutorAvailability;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit test FE-06 / UC-06.02 Add Availability Slot - TutorScheduleServiceImpl.addAvailability(email, request).
 * Quy tắc: giờ HH:mm bắt buộc, bắt đầu < kết thúc, tối thiểu 30 phút, không trùng ca đang mở cùng ngày.
 */
class AvailabilityAddTest extends AvailabilityUnitTestBase {

    private static CreateAvailabilityRequestDTO slot(int day, String start, String end) {
        return CreateAvailabilityRequestDTO.builder().dayOfWeek(day).startTime(start).endTime(end).build();
    }

    private void noOverlap() {
        when(slots.hasOverlappingSlot(eq(TUTOR_ID), anyInt(), any(), any())).thenReturn(false);
    }

    @Test @DisplayName("TC-06-004: Thêm ca hợp lệ Thứ hai 18:00-20:00 -> lưu ca đang mở")
    void tc06_004_validSlot() {
        loggedInTutor();
        noOverlap();

        TutorAvailabilityDTO result = service.addAvailability(EMAIL, slot(2, "18:00", "20:00"));

        ArgumentCaptor<TutorAvailability> saved = ArgumentCaptor.forClass(TutorAvailability.class);
        verify(slots).save(saved.capture());
        assertEquals(2, saved.getValue().getDayOfWeek());
        assertEquals(LocalTime.of(18, 0), saved.getValue().getStartTime());
        assertEquals(LocalTime.of(20, 0), saved.getValue().getEndTime());
        assertTrue(saved.getValue().getIsActive());
        assertEquals("18:00", result.getStartTime());
    }

    @Test @DisplayName("TC-06-005: Giờ bắt đầu để trống -> 400 'Giờ bắt đầu không được để trống'")
    void tc06_005_emptyTime() {
        loggedInTutor();
        var ex = assertThrows(BadRequestException.class, () -> service.addAvailability(EMAIL, slot(2, " ", "20:00")));
        assertEquals("Giờ bắt đầu không được để trống", ex.getMessage());
        verify(slots, never()).save(any());
    }

    @Test @DisplayName("TC-06-006: Giờ sai định dạng '6pm' -> 400 định dạng HH:mm")
    void tc06_006_wrongFormat() {
        loggedInTutor();
        var ex = assertThrows(BadRequestException.class, () -> service.addAvailability(EMAIL, slot(2, "6pm", "20:00")));
        assertEquals("Giờ bắt đầu không đúng định dạng HH:mm (Ví dụ: 18:30)", ex.getMessage());
        verify(slots, never()).save(any());
    }

    @Test @DisplayName("TC-06-007: Bắt đầu bằng kết thúc 18:00-18:00 (biên) -> 400")
    void tc06_007_startEqualsEnd() {
        loggedInTutor();
        var ex = assertThrows(BadRequestException.class, () -> service.addAvailability(EMAIL, slot(2, "18:00", "18:00")));
        assertTrue(ex.getMessage().contains("phải sớm hơn giờ kết thúc"));
        verify(slots, never()).save(any());
    }

    @Test @DisplayName("TC-06-008: Bắt đầu sau kết thúc 20:00-18:00 -> 400")
    void tc06_008_startAfterEnd() {
        loggedInTutor();
        var ex = assertThrows(BadRequestException.class, () -> service.addAvailability(EMAIL, slot(2, "20:00", "18:00")));
        assertTrue(ex.getMessage().contains("phải sớm hơn giờ kết thúc"));
    }

    @Test @DisplayName("TC-06-009: Ca 29 phút (dưới biên) -> 400 tối thiểu 30 phút")
    void tc06_009_29Minutes() {
        loggedInTutor();
        var ex = assertThrows(BadRequestException.class, () -> service.addAvailability(EMAIL, slot(2, "18:00", "18:29")));
        assertTrue(ex.getMessage().startsWith("Khung giờ rảnh tối thiểu phải từ 30 phút"));
        verify(slots, never()).save(any());
    }

    @Test @DisplayName("TC-06-010: Ca đúng 30 phút (biên) -> lưu được")
    void tc06_010_30Minutes() {
        loggedInTutor();
        noOverlap();
        service.addAvailability(EMAIL, slot(2, "18:00", "18:30"));
        verify(slots).save(any(TutorAvailability.class));
    }

    @Test @DisplayName("TC-06-011: Trùng ca đang mở cùng ngày -> 409")
    void tc06_011_overlap() {
        loggedInTutor();
        when(slots.hasOverlappingSlot(TUTOR_ID, 2, LocalTime.of(19, 0), LocalTime.of(21, 0))).thenReturn(true);

        var ex = assertThrows(ConflictException.class, () -> service.addAvailability(EMAIL, slot(2, "19:00", "21:00")));
        assertTrue(ex.getMessage().contains("bị trùng với một ca rảnh đang mở trong ngày Thứ hai"));
        verify(slots, never()).save(any());
    }

    @Test @DisplayName("TC-06-012: Ca chạm sát ca khác 20:00-21:00 (repository báo không trùng) -> lưu được")
    void tc06_012_touchingSlot() {
        loggedInTutor();
        noOverlap();
        service.addAvailability(EMAIL, slot(2, "20:00", "21:00"));
        verify(slots).hasOverlappingSlot(TUTOR_ID, 2, LocalTime.of(20, 0), LocalTime.of(21, 0));
        verify(slots).save(any(TutorAvailability.class));
    }

    @Test @DisplayName("TC-06-014: Trùng giờ với ca đang tắt -> câu kiểm tra trùng chỉ xét ca đang mở, lưu được")
    void tc06_014_sameTimeAsTurnedOffSlot() throws Exception {
        String jpql = com.kizuna.features.tutor_search.repository.TutorAvailabilityRepository.class
                .getMethod("hasOverlappingSlot", java.util.UUID.class, Integer.class, LocalTime.class, LocalTime.class)
                .getAnnotation(org.springframework.data.jpa.repository.Query.class).value();
        assertTrue(jpql.contains("a.isActive = true"), "Câu kiểm tra trùng phải bỏ qua ca đang tắt");

        loggedInTutor();
        noOverlap();
        service.addAvailability(EMAIL, slot(2, "18:00", "20:00"));
        verify(slots).save(any(TutorAvailability.class));
    }

    @Test @DisplayName("TC-06-013: Cùng giờ nhưng khác ngày -> kiểm tra trùng theo đúng ngày mới, lưu được")
    void tc06_013_otherDay() {
        loggedInTutor();
        when(slots.hasOverlappingSlot(TUTOR_ID, 2, LocalTime.of(18, 0), LocalTime.of(20, 0))).thenReturn(true);
        when(slots.hasOverlappingSlot(TUTOR_ID, 3, LocalTime.of(18, 0), LocalTime.of(20, 0))).thenReturn(false);

        service.addAvailability(EMAIL, slot(3, "18:00", "20:00"));
        verify(slots).save(any(TutorAvailability.class));
    }
}
