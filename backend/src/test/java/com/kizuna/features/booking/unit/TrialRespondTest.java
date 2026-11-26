package com.kizuna.features.booking.unit;

import com.kizuna.core.entity.Learner;
import com.kizuna.core.entity.User;
import com.kizuna.core.exception.BadRequestException;
import com.kizuna.core.exception.ConflictException;
import com.kizuna.features.booking.dto.request.RespondTrialBookingRequestDTO;
import com.kizuna.features.booking.entity.ConnectionRequest;
import com.kizuna.features.booking.entity.ConnectionRequestStatus;
import com.kizuna.features.booking.entity.TrialBooking;
import com.kizuna.features.booking.entity.TrialBookingHistory;
import com.kizuna.features.booking.entity.TrialBookingStatus;
import com.kizuna.core.entity.Tutor;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit test FE-07 / UC-07.03 Accept Trial Booking và UC-07.04 Decline Trial Booking
 * - TrialBookingServiceImpl.respondTrialBooking(email, bookingId, {ACCEPT | DECLINE}).
 */
class TrialRespondTest extends TrialUnitTestBase {
    private static final String LINK = "https://meet.google.com/abc-defg-hij";

    private static RespondTrialBookingRequestDTO accept(String link) {
        return RespondTrialBookingRequestDTO.builder().action("ACCEPT").meetingLink(link).build();
    }

    private static RespondTrialBookingRequestDTO decline(String reason) {
        return RespondTrialBookingRequestDTO.builder().action("DECLINE").declineReason(reason).build();
    }

    /** Yêu cầu học thử đang chờ của một học viên khác với cùng gia sư. */
    private TrialBooking otherPending(OffsetDateTime start) {
        User u = User.builder().userId(UUID.randomUUID()).email("khac@test.com").fullName("Hoc Vien B").build();
        Learner l = Learner.builder().learnerId(UUID.randomUUID()).user(u).build();
        ConnectionRequest cr = ConnectionRequest.builder().requestId(UUID.randomUUID()).learner(l).tutor(tutor)
                .status(ConnectionRequestStatus.PENDING).build();
        return TrialBooking.builder().bookingId(UUID.randomUUID()).connectionRequest(cr).learner(l).tutor(tutor)
                .scheduledStartTime(start).scheduledEndTime(start.plusMinutes(30)).bookingStatus(TrialBookingStatus.PENDING).build();
    }

    // ---- UC-07.03 Accept ----

    @Test @DisplayName("UTCID01: Nhận học thử với link hợp lệ -> Confirmed, lưu link, lịch sử, báo học viên; chưa tạo kết nối")
    void utcid01_accept() {
        asTutor();
        TrialBooking b = booking(TrialBookingStatus.PENDING, 48);

        service.respondTrialBooking(TUTOR_EMAIL, BOOKING_ID, accept("  " + LINK + " "));

        assertEquals(TrialBookingStatus.CONFIRMED, b.getBookingStatus());
        assertEquals(LINK, b.getMeetingLink());
        assertTrue(b.getTutorConfirmed());
        assertEquals(ConnectionRequestStatus.PENDING, b.getConnectionRequest().getStatus(), "nhận học thử không tạo kết nối");
        verify(history).save(argThat((TrialBookingHistory h) -> h.getNewStatus() == TrialBookingStatus.CONFIRMED));
        verify(events).publish(contains(":accepted"), eq("TRIAL"), any(), any(), any(), eq(List.of(learnerUser.getUserId())));
    }

    @Test @DisplayName("UTCID02: Yêu cầu chờ khác trùng một phần giờ -> tự hủy, ghi lý do, báo học viên đó")
    void utcid02_cancelsOverlapping() {
        asTutor();
        TrialBooking b = booking(TrialBookingStatus.PENDING, 48);
        TrialBooking overlap = otherPending(b.getScheduledStartTime().plusMinutes(15));
        when(bookings.findByTutorTutorIdAndBookingStatusIn(tutor.getTutorId(), List.of(TrialBookingStatus.PENDING)))
                .thenReturn(List.of(b, overlap));

        service.respondTrialBooking(TUTOR_EMAIL, BOOKING_ID, accept(LINK));

        assertEquals(TrialBookingStatus.CANCELLED, overlap.getBookingStatus());
        assertEquals(ConnectionRequestStatus.CANCELLED, overlap.getConnectionRequest().getStatus());
        verify(history).save(argThat((TrialBookingHistory h) -> h.getTrialBooking() == overlap
                && "Tự động hủy vì gia sư đã nhận học viên khác vào giờ này".equals(h.getChangeReason())));
        verify(events).publish(contains(":taken"), any(), any(), any(), any(), eq(List.of(overlap.getLearner().getUser().getUserId())));
    }

    @Test @DisplayName("UTCID03: Yêu cầu chờ khác ngay sau buổi đã nhận (chạm sát) -> giữ nguyên Pending")
    void utcid03_keepsTouching() {
        asTutor();
        TrialBooking b = booking(TrialBookingStatus.PENDING, 48);
        TrialBooking next = otherPending(b.getScheduledEndTime());
        when(bookings.findByTutorTutorIdAndBookingStatusIn(tutor.getTutorId(), List.of(TrialBookingStatus.PENDING)))
                .thenReturn(List.of(b, next));

        service.respondTrialBooking(TUTOR_EMAIL, BOOKING_ID, accept(LINK));

        assertEquals(TrialBookingStatus.PENDING, next.getBookingStatus());
    }

    @Test @DisplayName("UTCID04: Yêu cầu chờ quá 24 giờ -> 400 đã hết hạn phản hồi")
    void utcid04_expired() {
        asTutor();
        TrialBooking b = booking(TrialBookingStatus.PENDING, 48);
        b.setUpdatedAt(OffsetDateTime.now().minusHours(25));
        var ex = assertThrows(BadRequestException.class, () -> service.respondTrialBooking(TUTOR_EMAIL, BOOKING_ID, accept(LINK)));
        assertEquals("Yêu cầu học thử này đã hết hạn phản hồi. Học viên có thể gửi lại yêu cầu mới.", ex.getMessage());
        assertEquals(TrialBookingStatus.PENDING, b.getBookingStatus());
    }

    @Test @DisplayName("UTCID05: Đã tới giờ học mà còn Pending -> 400 (coi như hết hạn), không xác nhận")
    void utcid05_timePassed() {
        asTutor();
        TrialBooking b = booking(TrialBookingStatus.PENDING, -1);
        assertThrows(BadRequestException.class, () -> service.respondTrialBooking(TUTOR_EMAIL, BOOKING_ID, accept(LINK)));
        assertEquals(TrialBookingStatus.PENDING, b.getBookingStatus());
        verify(bookings, never()).save(any());
    }

    @Test @DisplayName("UTCID06: Trùng buổi học thử đã xác nhận khác của gia sư -> 409")
    void utcid06_tutorOverlap() {
        asTutor();
        booking(TrialBookingStatus.PENDING, 48);
        when(bookings.existsConfirmedTutorOverlap(eq(tutor.getTutorId()), eq(BOOKING_ID), any(), any())).thenReturn(true);
        var ex = assertThrows(ConflictException.class, () -> service.respondTrialBooking(TUTOR_EMAIL, BOOKING_ID, accept(LINK)));
        assertTrue(ex.getMessage().startsWith("Bạn đã nhận một buổi học thử khác trùng khung giờ này."));
        verify(bookings, never()).save(any());
    }

    @Test @DisplayName("UTCID07: Học viên đã có buổi học thử đã xác nhận cùng giờ -> 409")
    void utcid07_learnerOverlap() {
        asTutor();
        booking(TrialBookingStatus.PENDING, 48);
        when(bookings.existsConfirmedLearnerOverlap(eq(learner.getLearnerId()), eq(BOOKING_ID), any(), any())).thenReturn(true);
        var ex = assertThrows(ConflictException.class, () -> service.respondTrialBooking(TUTOR_EMAIL, BOOKING_ID, accept(LINK)));
        assertTrue(ex.getMessage().startsWith("Học viên đã có một buổi học thử khác được xác nhận vào khung giờ này."));
    }

    @Test @DisplayName("UTCID08: Trùng lịch lớp chính thức của gia sư -> 409")
    void utcid08_classClash() {
        asTutor();
        booking(TrialBookingStatus.PENDING, 48);
        when(bookings.findClassScheduleClash(any(), any(), any(), anyInt(), any(), any())).thenReturn(Optional.of("TUTOR"));
        var ex = assertThrows(ConflictException.class, () -> service.respondTrialBooking(TUTOR_EMAIL, BOOKING_ID, accept(LINK)));
        assertTrue(ex.getMessage().startsWith("Buổi học thử trùng với lịch dạy lớp chính thức của bạn."));
    }

    @Test @DisplayName("UTCID09: Trùng buổi học kết nối với học viên khác -> 409")
    void utcid09_sessionClash() {
        asTutor();
        booking(TrialBookingStatus.PENDING, 48);
        when(bookings.existsConnectionSessionClash(any(), any(), anyInt(), any(), any(), anyBoolean(), anyInt())).thenReturn(true);
        var ex = assertThrows(ConflictException.class, () -> service.respondTrialBooking(TUTOR_EMAIL, BOOKING_ID, accept(LINK)));
        assertTrue(ex.getMessage().startsWith("Bạn đã có buổi học kết nối với học viên khác vào khung giờ này."));
    }

    @Test @DisplayName("UTCID10: Không nhập link phòng học -> 400")
    void utcid10_missingLink() {
        asTutor();
        booking(TrialBookingStatus.PENDING, 48);
        var ex = assertThrows(BadRequestException.class, () -> service.respondTrialBooking(TUTOR_EMAIL, BOOKING_ID, accept(" ")));
        assertTrue(ex.getMessage().startsWith("Vui lòng cung cấp đường dẫn phòng học trực tuyến hợp lệ"));
        verify(bookings, never()).save(any());
    }

    @Test @DisplayName("UTCID11: Link không bắt đầu bằng http -> 400")
    void utcid11_linkWithoutHttp() {
        asTutor();
        booking(TrialBookingStatus.PENDING, 48);
        assertThrows(BadRequestException.class, () -> service.respondTrialBooking(TUTOR_EMAIL, BOOKING_ID, accept("meet.google.com/abc")));
        verify(bookings, never()).save(any());
    }

    @Test @DisplayName("UTCID12: Buổi học thử đã được xử lý -> 400 'Buổi học thử này đã được xử lý trước đó'")
    void utcid12_alreadyProcessed() {
        asTutor();
        booking(TrialBookingStatus.CONFIRMED, 48);
        var ex = assertThrows(BadRequestException.class, () -> service.respondTrialBooking(TUTOR_EMAIL, BOOKING_ID, accept(LINK)));
        assertEquals("Buổi học thử này đã được xử lý trước đó", ex.getMessage());
    }

    @Test @DisplayName("UTCID13: Gia sư hết gói hội viên -> 400 yêu cầu đăng ký / gia hạn gói")
    void utcid13_noMembership() {
        asTutor();
        booking(TrialBookingStatus.PENDING, 48);
        when(membership.hasActiveMembership(eq(tutor.getTutorId()), any())).thenReturn(false);
        var ex = assertThrows(BadRequestException.class, () -> service.respondTrialBooking(TUTOR_EMAIL, BOOKING_ID, accept(LINK)));
        assertEquals("Bạn chưa có gói hội viên còn hạn nên chưa thể nhận học viên mới. Vui lòng đăng ký hoặc gia hạn gói.", ex.getMessage());
    }

    @Test @DisplayName("UTCID14: Gia sư xử lý buổi học thử của gia sư khác -> 403")
    void utcid14_otherTutorsBooking() {
        asTutor();
        TrialBooking b = booking(TrialBookingStatus.PENDING, 48);
        b.setTutor(Tutor.builder().tutorId(UUID.randomUUID()).user(User.builder().userId(UUID.randomUUID()).build()).build());
        assertThrows(AccessDeniedException.class, () -> service.respondTrialBooking(TUTOR_EMAIL, BOOKING_ID, accept(LINK)));
        assertEquals(TrialBookingStatus.PENDING, b.getBookingStatus());
    }

    @Test @DisplayName("UTCID15: Hành động không phải ACCEPT/DECLINE -> 400")
    void utcid15_invalidAction() {
        asTutor();
        booking(TrialBookingStatus.PENDING, 48);
        var req = RespondTrialBookingRequestDTO.builder().action("MAYBE").build();
        var ex = assertThrows(BadRequestException.class, () -> service.respondTrialBooking(TUTOR_EMAIL, BOOKING_ID, req));
        assertEquals("Hành động không hợp lệ (chỉ chấp nhận ACCEPT hoặc DECLINE)", ex.getMessage());
    }

    // ---- UC-07.04 Decline ----

    @Test @DisplayName("UTCID16: Từ chối kèm lý do -> Declined, yêu cầu kết nối Declined, lịch sử có lý do, báo học viên")
    void utcid16_decline() {
        asTutor();
        TrialBooking b = booking(TrialBookingStatus.PENDING, 48);

        service.respondTrialBooking(TUTOR_EMAIL, BOOKING_ID, decline("Busy this week"));

        assertEquals(TrialBookingStatus.DECLINED, b.getBookingStatus());
        assertEquals(ConnectionRequestStatus.DECLINED, b.getConnectionRequest().getStatus());
        verify(history).save(argThat((TrialBookingHistory h) -> h.getNewStatus() == TrialBookingStatus.DECLINED
                && "Busy this week".equals(h.getChangeReason())));
        verify(events).publish(contains(":declined"), any(), any(), any(), any(), eq(List.of(learnerUser.getUserId())));
    }

    @Test @DisplayName("UTCID17: Từ chối không cần gói hội viên và không kiểm tra trùng lịch")
    void utcid17_declineNeedsNoMembership() {
        asTutor();
        booking(TrialBookingStatus.PENDING, 48);
        when(membership.hasActiveMembership(any(), any())).thenReturn(false);

        service.respondTrialBooking(TUTOR_EMAIL, BOOKING_ID, decline("No"));

        verify(bookings, never()).existsConfirmedTutorOverlap(any(), any(), any(), any());
    }

    @Test @DisplayName("UTCID18: Từ chối yêu cầu đã xử lý / đã hết hạn -> 400, không đổi")
    void utcid18_declineHandled() {
        asTutor();
        TrialBooking b = booking(TrialBookingStatus.EXPIRED, 48);
        assertThrows(BadRequestException.class, () -> service.respondTrialBooking(TUTOR_EMAIL, BOOKING_ID, decline("No")));
        assertEquals(TrialBookingStatus.EXPIRED, b.getBookingStatus());
    }
}
