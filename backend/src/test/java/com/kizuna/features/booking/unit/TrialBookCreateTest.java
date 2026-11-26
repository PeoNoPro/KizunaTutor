package com.kizuna.features.booking.unit;

import com.kizuna.features.verification.entity.TutorVerificationStatus;
import com.kizuna.core.exception.BadRequestException;
import com.kizuna.core.exception.ConflictException;
import com.kizuna.features.booking.dto.request.CreateTrialBookingRequestDTO;
import com.kizuna.features.booking.entity.ConnectionRequest;
import com.kizuna.features.booking.entity.ConnectionRequestStatus;
import com.kizuna.features.booking.entity.TrialBooking;
import com.kizuna.features.booking.entity.TrialBookingHistory;
import com.kizuna.features.booking.entity.TrialBookingStatus;
import com.kizuna.features.booking.service.ConnectionSchedulePlanner.BusyKind;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.access.AccessDeniedException;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit test FE-07 / UC-07.01 Book Trial Lesson và UC-07.02 Propose Trial Lesson Outside Availability
 * - TrialBookingServiceImpl.createTrialBooking(email, request).
 */
class TrialBookCreateTest extends TrialUnitTestBase {
    private static final OffsetDateTime START = OffsetDateTime.now().plusDays(2).withNano(0);

    private CreateTrialBookingRequestDTO request(OffsetDateTime start) {
        return CreateTrialBookingRequestDTO.builder().tutorId(tutor.getTutorId()).scheduledStartTime(start)
                .learningGoals("Pass N3").initialMessage("Hello").contactEmail(LEARNER_EMAIL).contactPhone("0912345678")
                .shareContact(true).build();
    }

    private void ready() {
        asLearner();
        when(tutors.findById(tutor.getTutorId())).thenReturn(Optional.of(tutor));
    }

    private void assertNothingSaved() {
        verify(bookings, never()).save(any());
        verify(connections, never()).save(any());
    }

    @Test @DisplayName("TC-07-001: Đặt học thử hợp lệ -> buổi Pending 30 phút online, có yêu cầu kết nối, lịch sử, báo gia sư")
    void tc07_001_validBooking() {
        ready();

        var dto = service.createTrialBooking(LEARNER_EMAIL, request(START));

        ArgumentCaptor<TrialBooking> saved = ArgumentCaptor.forClass(TrialBooking.class);
        verify(bookings).save(saved.capture());
        TrialBooking b = saved.getValue();
        assertEquals(TrialBookingStatus.PENDING, b.getBookingStatus());
        assertEquals("ONLINE", b.getLearningMode());
        assertEquals(30, Duration.between(b.getScheduledStartTime(), b.getScheduledEndTime()).toMinutes());
        assertFalse(b.getOffSchedule());
        assertEquals(ConnectionRequestStatus.PENDING, b.getConnectionRequest().getStatus());
        assertEquals("0912345678", b.getConnectionRequest().getContactPhone());
        verify(history).save(argThat((TrialBookingHistory h) -> h.getNewStatus() == TrialBookingStatus.PENDING));
        verify(events).publish(contains(":created"), eq("TRIAL"), any(), any(), any(), eq(List.of(tutorUser.getUserId())));
        assertNotNull(dto.getBookingId());
    }

    @Test @DisplayName("TC-07-001: Thời lượng khác 30 phút -> 400 'Thời lượng buổi học thử là 30 phút'")
    void tc07_001_durationFixed() {
        ready();
        CreateTrialBookingRequestDTO req = request(START);
        req.setDurationMinutes(60);
        var ex = assertThrows(BadRequestException.class, () -> service.createTrialBooking(LEARNER_EMAIL, req));
        assertEquals("Thời lượng buổi học thử là 30 phút", ex.getMessage());
        assertNothingSaved();
    }

    @Test @DisplayName("TC-07-002: Chưa đồng ý chia sẻ liên hệ -> 400 yêu cầu đồng ý")
    void tc07_002_noConsent() {
        ready();
        CreateTrialBookingRequestDTO req = request(START);
        req.setShareContact(false);
        var ex = assertThrows(BadRequestException.class, () -> service.createTrialBooking(LEARNER_EMAIL, req));
        assertTrue(ex.getMessage().startsWith("Vui lòng đồng ý chia sẻ hồ sơ"));
        verify(bookings, never()).save(any());
    }

    @Test @DisplayName("TC-07-003: Số điện thoại liên hệ sai -> 400 'Số điện thoại không hợp lệ...'")
    void tc07_003_invalidPhone() {
        ready();
        CreateTrialBookingRequestDTO req = request(START);
        req.setContactPhone("12345");
        var ex = assertThrows(BadRequestException.class, () -> service.createTrialBooking(LEARNER_EMAIL, req));
        assertEquals("Số điện thoại không hợp lệ. Vui lòng nhập 10 số, bắt đầu bằng 0.", ex.getMessage());
    }

    @Test @DisplayName("TC-07-003: Email liên hệ sai -> 400 'Email liên hệ không hợp lệ.'")
    void tc07_003_invalidEmail() {
        ready();
        CreateTrialBookingRequestDTO req = request(START);
        req.setContactEmail("abc");
        var ex = assertThrows(BadRequestException.class, () -> service.createTrialBooking(LEARNER_EMAIL, req));
        assertEquals("Email liên hệ không hợp lệ.", ex.getMessage());
    }

    @Test @DisplayName("TC-07-004/020: Giờ bắt đầu trong quá khứ -> 400 'Thời gian bắt đầu học phải ở tương lai'")
    void tc07_004_pastStart() {
        ready();
        var ex = assertThrows(BadRequestException.class,
                () -> service.createTrialBooking(LEARNER_EMAIL, request(OffsetDateTime.now().minusHours(1))));
        assertEquals("Thời gian bắt đầu học phải ở tương lai", ex.getMessage());
        assertNothingSaved();
    }

    @Test @DisplayName("TC-07-005: Đã có buổi học thử đang mở với gia sư này -> 409")
    void tc07_005_openTrialExists() {
        ready();
        when(bookings.existsByLearnerLearnerIdAndTutorTutorIdAndBookingStatusIn(eq(learner.getLearnerId()), eq(tutor.getTutorId()), any()))
                .thenReturn(true);
        var ex = assertThrows(ConflictException.class, () -> service.createTrialBooking(LEARNER_EMAIL, request(START)));
        assertTrue(ex.getMessage().startsWith("Bạn đang có một yêu cầu học thử với gia sư này."));
        assertNothingSaved();
    }

    @Test @DisplayName("TC-07-006: Đang chờ 2 gia sư (biên trên) -> yêu cầu thứ 3 bị chặn 409")
    void tc07_006_thirdPending() {
        ready();
        when(bookings.countByLearnerLearnerIdAndBookingStatus(learner.getLearnerId(), TrialBookingStatus.PENDING)).thenReturn(2L);
        var ex = assertThrows(ConflictException.class, () -> service.createTrialBooking(LEARNER_EMAIL, request(START)));
        assertTrue(ex.getMessage().startsWith("Bạn đang chờ 2 gia sư phản hồi"));
        assertNothingSaved();
    }

    @Test @DisplayName("TC-07-007: Đang chờ 1 gia sư -> yêu cầu thứ 2 vẫn được tạo")
    void tc07_007_secondPendingAllowed() {
        ready();
        when(bookings.countByLearnerLearnerIdAndBookingStatus(learner.getLearnerId(), TrialBookingStatus.PENDING)).thenReturn(1L);
        service.createTrialBooking(LEARNER_EMAIL, request(START));
        verify(bookings).save(any(TrialBooking.class));
    }

    @Test @DisplayName("TC-07-008/035: Gia sư vừa từ chối trong 24 giờ -> 409 chờ 24 giờ")
    void tc07_008_declinedRecently() {
        ready();
        when(bookings.existsByLearnerLearnerIdAndTutorTutorIdAndBookingStatusAndUpdatedAtAfter(
                eq(learner.getLearnerId()), eq(tutor.getTutorId()), eq(TrialBookingStatus.DECLINED), any())).thenReturn(true);
        var ex = assertThrows(ConflictException.class, () -> service.createTrialBooking(LEARNER_EMAIL, request(START)));
        assertTrue(ex.getMessage().startsWith("Gia sư vừa từ chối yêu cầu học thử của bạn. Bạn có thể gửi lại sau 24 giờ"));
    }

    @Test @DisplayName("TC-07-009: Mốc kiểm tra từ chối là 24 giờ trước -> lần từ chối cũ hơn không chặn")
    void tc07_009_cooldownWindow() {
        ready();
        service.createTrialBooking(LEARNER_EMAIL, request(START));

        ArgumentCaptor<OffsetDateTime> since = ArgumentCaptor.forClass(OffsetDateTime.class);
        verify(bookings).existsByLearnerLearnerIdAndTutorTutorIdAndBookingStatusAndUpdatedAtAfter(
                eq(learner.getLearnerId()), eq(tutor.getTutorId()), eq(TrialBookingStatus.DECLINED), since.capture());
        long hours = Duration.between(since.getValue(), OffsetDateTime.now()).toMinutes();
        assertTrue(Math.abs(hours - 24 * 60) <= 1, "mốc phải là 24 giờ trước, thực tế " + hours + " phút");
        verify(bookings).save(any(TrialBooking.class));
    }

    @Test @DisplayName("TC-07-010: Giờ ngoài lịch rảnh và không chọn 'Đề xuất giờ khác' -> 400")
    void tc07_010_outsideAvailability() {
        ready();
        when(bookings.isWithinAvailability(any(), anyInt(), any(), any())).thenReturn(false);
        var ex = assertThrows(BadRequestException.class, () -> service.createTrialBooking(LEARNER_EMAIL, request(START)));
        assertTrue(ex.getMessage().startsWith("Giờ bạn chọn nằm ngoài lịch rảnh của gia sư."));
        assertNothingSaved();
    }

    @Test @DisplayName("TC-07-011: Gia sư đã bận giờ đó -> 409 'Giờ này đã kín: ...'")
    void tc07_011_tutorBusy() {
        ready();
        busyAt(START, BusyKind.TRIAL);
        var ex = assertThrows(ConflictException.class, () -> service.createTrialBooking(LEARNER_EMAIL, request(START)));
        assertEquals("Giờ này đã kín: gia sư đã có lịch học thử. Vui lòng chọn giờ khác.", ex.getMessage());
        assertNothingSaved();
    }

    @Test @DisplayName("TC-07-012: Học viên đã có buổi đã xác nhận với gia sư khác giờ đó -> 409")
    void tc07_012_learnerBusy() {
        ready();
        busyAt(START, BusyKind.LEARNER_TRIAL);
        var ex = assertThrows(ConflictException.class, () -> service.createTrialBooking(LEARNER_EMAIL, request(START)));
        assertTrue(ex.getMessage().contains("bạn đã có buổi học thử với gia sư khác vào giờ này"));
    }

    @Test @DisplayName("TC-07-013: Gia sư chưa được duyệt -> 400 chưa sẵn sàng nhận học viên")
    void tc07_013_notApproved() {
        ready();
        tutor.setVerificationStatus(TutorVerificationStatus.PENDING);
        var ex = assertThrows(BadRequestException.class, () -> service.createTrialBooking(LEARNER_EMAIL, request(START)));
        assertTrue(ex.getMessage().startsWith("Gia sư hiện chưa sẵn sàng nhận học viên mới."));
        assertNothingSaved();
    }

    @Test @DisplayName("TC-07-013: Gia sư hết gói hội viên -> 400 'Gia sư hiện chưa sẵn sàng nhận học viên mới. Vui lòng chọn gia sư khác.'")
    void tc07_013_noMembership() {
        ready();
        when(membership.hasActiveMembership(eq(tutor.getTutorId()), any())).thenReturn(false);
        var ex = assertThrows(BadRequestException.class, () -> service.createTrialBooking(LEARNER_EMAIL, request(START)));
        assertEquals("Gia sư hiện chưa sẵn sàng nhận học viên mới. Vui lòng chọn gia sư khác.", ex.getMessage());
    }

    @Test @DisplayName("TC-07-014: Tài khoản gia sư đặt học thử -> 403")
    void tc07_014_tutorCannotBook() {
        asTutor();
        var ex = assertThrows(AccessDeniedException.class, () -> service.createTrialBooking(TUTOR_EMAIL, request(START)));
        assertEquals("Chỉ tài khoản học viên mới có thể đặt lịch học thử", ex.getMessage());
    }

    @Test @DisplayName("TC-07-015: Yêu cầu chờ quá hạn của chính học viên được chuyển Hết hạn trước khi kiểm tra giới hạn")
    void tc07_015_expiresOwnStaleRequests() {
        ready();
        TrialBooking stale = booking(TrialBookingStatus.PENDING, 1);
        when(bookings.findStalePendingOfLearnerLocked(eq(learner.getLearnerId()), eq(TrialBookingStatus.PENDING), any(), any()))
                .thenReturn(List.of(stale));

        service.createTrialBooking(LEARNER_EMAIL, request(START));

        assertEquals(TrialBookingStatus.EXPIRED, stale.getBookingStatus());
        assertEquals(ConnectionRequestStatus.EXPIRED, stale.getConnectionRequest().getStatus());
    }

    // ---- UC-07.02 Đề xuất giờ ngoài lịch rảnh ----

    @Test @DisplayName("TC-07-016: Đề xuất giờ ngoài lịch rảnh -> lưu Pending, đánh dấu ngoài lịch rảnh")
    void tc07_016_proposeOffSchedule() {
        ready();
        when(bookings.isWithinAvailability(any(), anyInt(), any(), any())).thenReturn(false);
        CreateTrialBookingRequestDTO req = request(START);
        req.setOffSchedule(true);

        service.createTrialBooking(LEARNER_EMAIL, req);

        verify(bookings).save(argThat((TrialBooking b) -> b.getOffSchedule() && b.getBookingStatus() == TrialBookingStatus.PENDING));
    }

    @Test @DisplayName("TC-07-017: Đã đề xuất 2 lần trong 7 ngày với gia sư này -> lần thứ 3 bị chặn 409")
    void tc07_017_proposalLimit() {
        ready();
        when(bookings.isWithinAvailability(any(), anyInt(), any(), any())).thenReturn(false);
        when(bookings.countOffScheduleSince(eq(learner.getLearnerId()), eq(tutor.getTutorId()), any())).thenReturn(2L);
        CreateTrialBookingRequestDTO req = request(START);
        req.setOffSchedule(true);

        var ex = assertThrows(ConflictException.class, () -> service.createTrialBooking(LEARNER_EMAIL, req));
        assertTrue(ex.getMessage().startsWith("Bạn chỉ có thể đề xuất giờ ngoài lịch rảnh tối đa 2 lần mỗi tuần"));
        assertNothingSaved();
    }

    @Test @DisplayName("TC-07-017: Đã đề xuất 1 lần -> lần thứ 2 vẫn được")
    void tc07_017_secondProposalAllowed() {
        ready();
        when(bookings.isWithinAvailability(any(), anyInt(), any(), any())).thenReturn(false);
        when(bookings.countOffScheduleSince(eq(learner.getLearnerId()), eq(tutor.getTutorId()), any())).thenReturn(1L);
        CreateTrialBookingRequestDTO req = request(START);
        req.setOffSchedule(true);

        service.createTrialBooking(LEARNER_EMAIL, req);
        verify(bookings).save(any(TrialBooking.class));
    }

    @Test @DisplayName("TC-07-019: Giờ đề xuất trùng lịch bận của gia sư -> 409")
    void tc07_019_proposalBusy() {
        ready();
        when(bookings.isWithinAvailability(any(), anyInt(), any(), any())).thenReturn(false);
        busyAt(START, BusyKind.TUTOR_CLASS);
        CreateTrialBookingRequestDTO req = request(START);
        req.setOffSchedule(true);

        var ex = assertThrows(ConflictException.class, () -> service.createTrialBooking(LEARNER_EMAIL, req));
        assertTrue(ex.getMessage().startsWith("Giờ này đã kín"));
        verify(connections, never()).save(any(ConnectionRequest.class));
    }
}
