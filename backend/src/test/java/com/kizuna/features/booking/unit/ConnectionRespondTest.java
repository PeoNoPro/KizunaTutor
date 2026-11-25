package com.kizuna.features.booking.unit;

import com.kizuna.core.exception.BadRequestException;
import com.kizuna.core.exception.ConflictException;
import com.kizuna.features.booking.dto.request.RespondConnectionRequestDTO;
import com.kizuna.features.booking.dto.request.RespondConnectionRequestDTO.SlotDuration;
import com.kizuna.features.booking.entity.ConnectionRequest;
import com.kizuna.features.booking.entity.ConnectionRequestSlot;
import com.kizuna.features.booking.entity.ConnectionRequestStatus;
import com.kizuna.features.booking.entity.TrialBooking;
import com.kizuna.features.booking.entity.TrialBookingStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;

import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit test FE-08 / UC-08.04 Accept Connection Request và UC-08.05 Decline Connection Request
 * - ConnectionRequestServiceImpl.respond(email, requestId, {ACCEPT + độ dài từng ca | DECLINE + lời nhắn}).
 */
class ConnectionRespondTest extends ConnectionUnitTestBase {

    /** Đồng ý với độ dài lần lượt cho từng ca của yêu cầu (null = không gửi độ dài cho ca đó). */
    private static RespondConnectionRequestDTO accept(ConnectionRequest cr, Integer... minutes) {
        List<SlotDuration> d = new java.util.ArrayList<>();
        for (int i = 0; i < minutes.length; i++) {
            if (minutes[i] != null) d.add(new SlotDuration(cr.getSlots().get(i).getSlotId(), minutes[i]));
        }
        return RespondConnectionRequestDTO.builder().action("ACCEPT").message("Hẹn gặp em").slotDurations(d).build();
    }

    private static RespondConnectionRequestDTO decline(String message) {
        return RespondConnectionRequestDTO.builder().action("DECLINE").message(message).build();
    }

    // ---- UC-08.04 Accept ----

    @Test @DisplayName("TC-08-024: Đồng ý với độ dài 60 / 90 / 120 phút -> Accepted, giờ kết thúc từng ca được điền, báo học viên")
    void tc08_024_accept() {
        asTutor();
        ConnectionRequest cr = request(ConnectionRequestStatus.PENDING, 2, 2, "18:00", 4, "18:00", 6, "18:00");

        service.respond(TUTOR_EMAIL, REQUEST_ID, accept(cr, 60, 90, 120));

        assertEquals(ConnectionRequestStatus.ACCEPTED, cr.getStatus());
        assertEquals("Hẹn gặp em", cr.getResponseMessage());
        assertEquals(List.of(LocalTime.of(19, 0), LocalTime.of(19, 30), LocalTime.of(20, 0)),
                cr.getSlots().stream().map(ConnectionRequestSlot::getEndTime).toList());
        verify(events).publish(contains(":accepted"), eq("CONNECTION"), any(), any(), any(), eq(List.of(learnerUser.getUserId())));
    }

    @Test @DisplayName("TC-08-024: Đồng ý -> hủy các buổi học thử còn mở giữa hai người")
    void tc08_024_cancelsOpenTrials() {
        asTutor();
        ConnectionRequest cr = request(ConnectionRequestStatus.PENDING, 2, 2, "18:00");
        TrialBooking open = TrialBooking.builder().bookingId(UUID.randomUUID()).learner(learner).tutor(tutor)
                .scheduledStartTime(OffsetDateTime.now().plusDays(1)).bookingStatus(TrialBookingStatus.CONFIRMED).build();
        when(trials.findByTutorTutorIdAndLearnerLearnerIdAndBookingStatusIn(eq(tutor.getTutorId()), eq(learner.getLearnerId()), any()))
                .thenReturn(List.of(open));

        service.respond(TUTOR_EMAIL, REQUEST_ID, accept(cr, 60));

        assertEquals(TrialBookingStatus.CANCELLED, open.getBookingStatus());
    }

    @Test @DisplayName("TC-08-025: Không chọn độ dài cho ca -> 400 'Vui lòng chọn độ dài cho ca Thứ hai 18:00'")
    void tc08_025_missingLength() {
        asTutor();
        ConnectionRequest cr = request(ConnectionRequestStatus.PENDING, 2, 2, "18:00");
        var ex = assertThrows(BadRequestException.class, () -> service.respond(TUTOR_EMAIL, REQUEST_ID, accept(cr, (Integer) null)));
        assertEquals("Vui lòng chọn độ dài cho ca Thứ hai 18:00", ex.getMessage());
        assertEquals(ConnectionRequestStatus.PENDING, cr.getStatus());
    }

    @Test @DisplayName("TC-08-026: Độ dài 45 phút (không thuộc 60/90/120) -> 400")
    void tc08_026_invalidLength() {
        asTutor();
        ConnectionRequest cr = request(ConnectionRequestStatus.PENDING, 2, 2, "18:00");
        var ex = assertThrows(BadRequestException.class, () -> service.respond(TUTOR_EMAIL, REQUEST_ID, accept(cr, 45)));
        assertEquals("Độ dài ca Thứ hai 18:00 chỉ có thể là 60, 90 hoặc 120 phút", ex.getMessage());
    }

    @Test @DisplayName("TC-08-027: Ca 22:30 + 120 phút kết thúc sau 24:00 -> 400; ca 22:30 + 60 phút được nhận")
    void tc08_027_endsAfterMidnight() {
        asTutor();
        ConnectionRequest cr = request(ConnectionRequestStatus.PENDING, 2, 2, "22:30");
        var ex = assertThrows(BadRequestException.class, () -> service.respond(TUTOR_EMAIL, REQUEST_ID, accept(cr, 120)));
        assertEquals("Ca Thứ hai 22:30 phải kết thúc trước 24:00", ex.getMessage());

        service.respond(TUTOR_EMAIL, REQUEST_ID, accept(cr, 60));
        assertEquals(ConnectionRequestStatus.ACCEPTED, cr.getStatus());
    }

    @Test @DisplayName("TC-08-027: Ca 23:00 + 60 phút kết thúc đúng 24:00 (biên) -> 400 (phải kết thúc trước 24:00)")
    void tc08_027_endsExactlyMidnight() {
        asTutor();
        ConnectionRequest cr = request(ConnectionRequestStatus.PENDING, 2, 2, "23:00");
        assertThrows(BadRequestException.class, () -> service.respond(TUTOR_EMAIL, REQUEST_ID, accept(cr, 60)));
    }

    @Test @DisplayName("TC-08-028: Hai ca cùng ngày chồng giờ (18:00 dài 120 phút và 19:00) -> 400")
    void tc08_028_overlappingSlots() {
        asTutor();
        ConnectionRequest cr = request(ConnectionRequestStatus.PENDING, 2, 2, "18:00", 2, "19:00");
        var ex = assertThrows(BadRequestException.class, () -> service.respond(TUTOR_EMAIL, REQUEST_ID, accept(cr, 120, 60)));
        assertTrue(ex.getMessage().startsWith("Ca Thứ hai 18:00") && ex.getMessage().contains("bị chồng giờ nhau"), ex.getMessage());
    }

    @Test @DisplayName("TC-08-029: Ca đã được nhận cho học viên khác -> 409, yêu cầu vẫn Pending")
    void tc08_029_slotTaken() {
        asTutor();
        ConnectionRequest cr = request(ConnectionRequestStatus.PENDING, 2, 2, "18:00");
        when(slots.existsLockedSlotOverlap(eq(tutor.getTutorId()), eq(REQUEST_ID), eq(2), any(), any())).thenReturn(true);
        var ex = assertThrows(ConflictException.class, () -> service.respond(TUTOR_EMAIL, REQUEST_ID, accept(cr, 60)));
        assertTrue(ex.getMessage().contains("đã được bạn nhận cho học viên khác"));
        assertEquals(ConnectionRequestStatus.PENDING, cr.getStatus());
    }

    @Test @DisplayName("TC-08-029: Ca trùng buổi học thử gia sư đã nhận với học viên khác -> 409")
    void tc08_029_trialClash() {
        asTutor();
        ConnectionRequest cr = request(ConnectionRequestStatus.PENDING, 2, 2, "18:00");
        when(trials.existsTrialClashFrom(eq(tutor.getTutorId()), any(), any(), anyInt(), any(), any())).thenReturn(true);
        var ex = assertThrows(ConflictException.class, () -> service.respond(TUTOR_EMAIL, REQUEST_ID, accept(cr, 60)));
        assertTrue(ex.getMessage().contains("trùng buổi học thử bạn đã nhận"));
    }

    @Test @DisplayName("TC-08-030: Yêu cầu tạo quá 24 giờ -> 400 đã hết hạn")
    void tc08_030_expired() {
        asTutor();
        ConnectionRequest cr = request(ConnectionRequestStatus.PENDING, 25, 2, "18:00");
        var ex = assertThrows(BadRequestException.class, () -> service.respond(TUTOR_EMAIL, REQUEST_ID, accept(cr, 60)));
        assertEquals("Yêu cầu kết nối này đã hết hạn phản hồi. Học viên có thể gửi lại yêu cầu mới.", ex.getMessage());
    }

    @Test @DisplayName("TC-08-030 / TC-08-034: Yêu cầu đã được xử lý -> 400 (cả khi đồng ý và từ chối)")
    void tc08_030_alreadyHandled() {
        asTutor();
        ConnectionRequest cr = request(ConnectionRequestStatus.ACCEPTED, 2, 2, "18:00");
        assertThrows(BadRequestException.class, () -> service.respond(TUTOR_EMAIL, REQUEST_ID, accept(cr, 60)));
        var ex = assertThrows(BadRequestException.class, () -> service.respond(TUTOR_EMAIL, REQUEST_ID, decline("No")));
        assertEquals("Yêu cầu kết nối này đã được xử lý trước đó", ex.getMessage());
    }

    @Test @DisplayName("TC-08-031: Gia sư hết gói hội viên -> 400 yêu cầu đăng ký / gia hạn gói")
    void tc08_031_noMembership() {
        asTutor();
        ConnectionRequest cr = request(ConnectionRequestStatus.PENDING, 2, 2, "18:00");
        when(membership.hasActiveMembership(eq(tutor.getTutorId()), any())).thenReturn(false);
        var ex = assertThrows(BadRequestException.class, () -> service.respond(TUTOR_EMAIL, REQUEST_ID, accept(cr, 60)));
        assertEquals("Bạn chưa có gói hội viên còn hạn nên chưa thể nhận học viên mới. Vui lòng đăng ký hoặc gia hạn gói.", ex.getMessage());
    }

    @Test @DisplayName("TC-08-024: Gia sư xử lý yêu cầu của gia sư khác -> 403")
    void tc08_024_otherTutor() {
        asTutor();
        ConnectionRequest cr = request(ConnectionRequestStatus.PENDING, 2, 2, "18:00");
        cr.setTutor(com.kizuna.core.entity.Tutor.builder().tutorId(UUID.randomUUID()).build());
        assertThrows(AccessDeniedException.class, () -> service.respond(TUTOR_EMAIL, REQUEST_ID, accept(cr, 60)));
    }

    // ---- UC-08.05 Decline ----

    @Test @DisplayName("TC-08-032: Từ chối kèm lời nhắn -> Declined, lưu lời nhắn, báo học viên")
    void tc08_032_decline() {
        asTutor();
        ConnectionRequest cr = request(ConnectionRequestStatus.PENDING, 2, 2, "18:00");

        service.respond(TUTOR_EMAIL, REQUEST_ID, decline("Schedule full"));

        assertEquals(ConnectionRequestStatus.DECLINED, cr.getStatus());
        assertEquals("Schedule full", cr.getResponseMessage());
        assertNotNull(cr.getRespondedAt(), "mốc từ chối dùng cho quy tắc chờ 24 giờ");
        verify(events).publish(contains(":declined"), any(), any(), any(), any(), eq(List.of(learnerUser.getUserId())));
    }

    @Test @DisplayName("TC-08-033: Lời nhắn 1000 ký tự hợp lệ; 1001 ký tự -> 'Lời nhắn tối đa 1000 ký tự'")
    void tc08_033_messageBoundary() {
        String m1000 = "a".repeat(1000), m1001 = "a".repeat(1001);
        assertFalse(hasError(decline(m1000), "message", "Lời nhắn tối đa 1000 ký tự"));
        assertTrue(hasError(decline(m1001), "message", "Lời nhắn tối đa 1000 ký tự"));
    }

    @Test @DisplayName("TC-08-032: Hành động không hợp lệ -> 400")
    void tc08_032_invalidAction() {
        asTutor();
        request(ConnectionRequestStatus.PENDING, 2, 2, "18:00");
        var req = RespondConnectionRequestDTO.builder().action("MAYBE").build();
        var ex = assertThrows(BadRequestException.class, () -> service.respond(TUTOR_EMAIL, REQUEST_ID, req));
        assertEquals("Hành động không hợp lệ. Vui lòng chọn đồng ý hoặc từ chối.", ex.getMessage());
    }

}
