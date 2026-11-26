package com.kizuna.features.booking.unit;

import com.kizuna.core.exception.BadRequestException;
import com.kizuna.core.exception.ConflictException;
import com.kizuna.features.booking.dto.request.ConnectionSlotRequestDTO;
import com.kizuna.features.booking.dto.request.CreateConnectionRequestDTO;
import com.kizuna.features.booking.entity.ConnectionRequest;
import com.kizuna.features.booking.entity.ConnectionRequestStatus;
import com.kizuna.features.booking.entity.ConnectionRequestType;
import com.kizuna.features.verification.entity.TutorVerificationStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit test FE-08 / UC-08.02 Send Direct Connection Request - ConnectionRequestServiceImpl.createDirectRequest(email, request).
 * Học viên chọn thứ + giờ bắt đầu (mốc 30 phút, trước 23:00, không trùng); yêu cầu chỉ là đề nghị, không giữ chỗ.
 */
class ConnectionCreateTest extends ConnectionUnitTestBase {

    private static ConnectionSlotRequestDTO slot(int day, String start) {
        return ConnectionSlotRequestDTO.builder().dayOfWeek(day).startTime(LocalTime.parse(start)).build();
    }

    private CreateConnectionRequestDTO request(String mode, String location, ConnectionSlotRequestDTO... s) {
        return CreateConnectionRequestDTO.builder().tutorId(tutor.getTutorId()).learningMode(mode).locationDetails(location)
                .learningGoals("JLPT N3").contactEmail(LEARNER_EMAIL).contactPhone("0912345678").shareContact(true)
                .slots(new ArrayList<>(List.of(s))).build();
    }

    private CreateConnectionRequestDTO valid() {
        return request("ONLINE", null, slot(2, "18:00"), slot(4, "18:00"));
    }

    private void ready() {
        asLearner();
        when(tutors.findById(tutor.getTutorId())).thenReturn(Optional.of(tutor));
    }

    @Test @DisplayName("UTCID01: Gửi yêu cầu hợp lệ -> Pending, loại DIRECT, lưu ca + liên hệ, báo gia sư")
    void utcid01_valid() {
        ready();

        service.createDirectRequest(LEARNER_EMAIL, valid());

        ArgumentCaptor<ConnectionRequest> saved = ArgumentCaptor.forClass(ConnectionRequest.class);
        verify(requests).save(saved.capture());
        ConnectionRequest cr = saved.getValue();
        assertEquals(ConnectionRequestStatus.PENDING, cr.getStatus());
        assertEquals(ConnectionRequestType.DIRECT, cr.getRequestType());
        assertEquals("ONLINE", cr.getLearningMode());
        assertEquals(2, cr.getSlots().size());
        assertNull(cr.getSlots().get(0).getEndTime(), "độ dài ca do gia sư chọn khi đồng ý");
        assertEquals("0912345678", cr.getContactPhone());
        verify(events).publish(contains(":created"), eq("CONNECTION"), any(), any(), any(), eq(List.of(tutorUser.getUserId())));
    }

    @Test @DisplayName("UTCID02: Không chọn ca nào -> 400 'Vui lòng chọn ít nhất một ca học hằng tuần'")
    void utcid02_noSlot() {
        ready();
        var ex = assertThrows(BadRequestException.class, () -> service.createDirectRequest(LEARNER_EMAIL, request("ONLINE", null)));
        assertEquals("Vui lòng chọn ít nhất một ca học hằng tuần", ex.getMessage());
        assertTrue(hasError(request("ONLINE", null), "slots", "Vui lòng chọn ít nhất một ca học hằng tuần"));
        verify(requests, never()).save(any());
    }

    @Test @DisplayName("UTCID03: Chưa đồng ý chia sẻ liên hệ -> 400")
    void utcid03_noConsent() {
        ready();
        CreateConnectionRequestDTO req = valid();
        req.setShareContact(false);
        var ex = assertThrows(BadRequestException.class, () -> service.createDirectRequest(LEARNER_EMAIL, req));
        assertTrue(ex.getMessage().startsWith("Vui lòng đồng ý chia sẻ hồ sơ"));
        verify(requests, never()).save(any());
    }

    @Test @DisplayName("UTCID04: Số điện thoại liên hệ sai -> 400")
    void utcid04_invalidPhone() {
        ready();
        CreateConnectionRequestDTO req = valid();
        req.setContactPhone("12345");
        assertThrows(BadRequestException.class, () -> service.createDirectRequest(LEARNER_EMAIL, req));
    }

    @Test @DisplayName("UTCID05: Học trực tiếp mà không nhập khu vực -> 400")
    void utcid05_offlineNoArea() {
        ready();
        tutorTeaches("BOTH");
        var ex = assertThrows(BadRequestException.class,
                () -> service.createDirectRequest(LEARNER_EMAIL, request("OFFLINE", " ", slot(2, "18:00"))));
        assertEquals("Vui lòng nhập khu vực / địa điểm học khi chọn học trực tiếp.", ex.getMessage());
    }

    @Test @DisplayName("UTCID06: Học trực tiếp có khu vực, gia sư dạy cả hai hình thức -> lưu OFFLINE kèm khu vực")
    void utcid06_offlineWithArea() {
        ready();
        tutorTeaches("BOTH");
        service.createDirectRequest(LEARNER_EMAIL, request("OFFLINE", "Cầu Giấy", slot(2, "18:00")));
        verify(requests).save(argThat((ConnectionRequest cr) -> "OFFLINE".equals(cr.getLearningMode())
                && "Cầu Giấy".equals(cr.getLocationDetails())));
    }

    @Test @DisplayName("UTCID07: Giờ bắt đầu 18:15 (không phải giờ chẵn / giờ rưỡi) -> 400")
    void utcid07_notHalfHour() {
        ready();
        var ex = assertThrows(BadRequestException.class,
                () -> service.createDirectRequest(LEARNER_EMAIL, request("ONLINE", null, slot(2, "18:15"))));
        assertEquals("Giờ bắt đầu ca học phải là giờ chẵn hoặc giờ rưỡi", ex.getMessage());
    }

    @Test @DisplayName("UTCID08: Giờ bắt đầu 22:30 -> hợp lệ, lưu yêu cầu")
    void utcid08_start2230() {
        ready();
        service.createDirectRequest(LEARNER_EMAIL, request("ONLINE", null, slot(2, "22:30")));
        verify(requests).save(any(ConnectionRequest.class));
    }

    @Test @DisplayName("UTCID09: Giờ bắt đầu 23:30 -> 400 'Ca học phải bắt đầu trước 23:00'")
    void utcid09_start2330() {
        ready();
        var ex = assertThrows(BadRequestException.class,
                () -> service.createDirectRequest(LEARNER_EMAIL, request("ONLINE", null, slot(2, "23:30"))));
        assertEquals("Ca học phải bắt đầu trước 23:00", ex.getMessage());
        verify(requests, never()).save(any());
    }

    @Test @DisplayName("UTCID10: Giờ bắt đầu đúng 23:00 (biên) -> code hiện tại vẫn nhận (spec ghi 'trước 23:00')")
    void utcid10_exactly23() {
        ready();
        assertDoesNotThrow(() -> service.createDirectRequest(LEARNER_EMAIL, request("ONLINE", null, slot(2, "23:00"))));
    }

    @Test @DisplayName("UTCID11: Chọn cùng một ca hai lần -> 400 'Ca Thứ hai 18:00 bị chọn trùng'")
    void utcid11_duplicateSlot() {
        ready();
        var ex = assertThrows(BadRequestException.class,
                () -> service.createDirectRequest(LEARNER_EMAIL, request("ONLINE", null, slot(2, "18:00"), slot(2, "18:00"))));
        assertEquals("Ca Thứ hai 18:00 bị chọn trùng", ex.getMessage());
    }

    @Test @DisplayName("UTCID12: Giờ bắt đầu ngoài lịch rảnh -> 400 nêu rõ ca")
    void utcid12_outsideAvailability() {
        ready();
        when(schedule.isSlotAvailable(eq(tutor.getTutorId()), eq(1), any(), any())).thenReturn(false);
        var ex = assertThrows(BadRequestException.class,
                () -> service.createDirectRequest(LEARNER_EMAIL, request("ONLINE", null, slot(1, "06:00"))));
        assertEquals("Giờ Chủ nhật 06:00 nằm ngoài lịch rảnh của gia sư. Vui lòng chọn giờ khác.", ex.getMessage());
    }

    @Test @DisplayName("UTCID13: Ca đã được gia sư nhận cho học viên khác -> 409")
    void utcid13_slotTaken() {
        ready();
        when(slots.existsLockedSlotOverlap(eq(tutor.getTutorId()), any(), eq(2), any(), any())).thenReturn(true);
        var ex = assertThrows(ConflictException.class,
                () -> service.createDirectRequest(LEARNER_EMAIL, request("ONLINE", null, slot(2, "18:00"))));
        assertEquals("Giờ Thứ hai 18:00 gia sư đã nhận học viên khác. Vui lòng chọn giờ khác.", ex.getMessage());
    }

    @Test @DisplayName("UTCID14: Gia sư chỉ dạy online, học viên chọn trực tiếp -> 400 'Gia sư này chỉ dạy online...'")
    void utcid14_onlineOnlyTutor() {
        ready();
        tutorTeaches("ONLINE");
        var ex = assertThrows(BadRequestException.class,
                () -> service.createDirectRequest(LEARNER_EMAIL, request("OFFLINE", "Cầu Giấy", slot(2, "18:00"))));
        assertEquals("Gia sư này chỉ dạy online. Vui lòng chọn học online.", ex.getMessage());
    }

    @Test @DisplayName("UTCID15: Gia sư chỉ dạy trực tiếp, học viên chọn online -> 400 'Gia sư này chỉ dạy trực tiếp...'")
    void utcid15_offlineOnlyTutor() {
        ready();
        tutorTeaches("OFFLINE");
        var ex = assertThrows(BadRequestException.class, () -> service.createDirectRequest(LEARNER_EMAIL, valid()));
        assertEquals("Gia sư này chỉ dạy trực tiếp. Vui lòng chọn học trực tiếp.", ex.getMessage());
    }

    @Test @DisplayName("UTCID16: Đã có yêu cầu đang chờ với gia sư này -> 409")
    void utcid16_pendingExists() {
        ready();
        when(requests.existsByLearnerLearnerIdAndTutorTutorIdAndRequestTypeAndStatus(learner.getLearnerId(), tutor.getTutorId(),
                ConnectionRequestType.DIRECT, ConnectionRequestStatus.PENDING)).thenReturn(true);
        var ex = assertThrows(ConflictException.class, () -> service.createDirectRequest(LEARNER_EMAIL, valid()));
        assertEquals("Bạn đã gửi yêu cầu kết nối tới gia sư này và đang chờ gia sư phản hồi.", ex.getMessage());
    }

    @Test @DisplayName("UTCID17: Đang có khóa học kết nối với gia sư này -> 409")
    void utcid17_alreadyConnected() {
        ready();
        when(requests.existsActiveDirectConnection(learner.getLearnerId(), tutor.getTutorId())).thenReturn(true);
        var ex = assertThrows(ConflictException.class, () -> service.createDirectRequest(LEARNER_EMAIL, valid()));
        assertEquals("Bạn đang có khóa học kết nối với gia sư này. Hãy vào lớp học để trao đổi lịch học.", ex.getMessage());
    }

    @Test @DisplayName("UTCID18: Gia sư vừa từ chối trong 24 giờ -> 409")
    void utcid18_declinedRecently() {
        ready();
        when(requests.existsByLearnerLearnerIdAndTutorTutorIdAndRequestTypeAndStatusAndRespondedAtAfter(eq(learner.getLearnerId()),
                eq(tutor.getTutorId()), eq(ConnectionRequestType.DIRECT), eq(ConnectionRequestStatus.DECLINED), any())).thenReturn(true);
        var ex = assertThrows(ConflictException.class, () -> service.createDirectRequest(LEARNER_EMAIL, valid()));
        assertTrue(ex.getMessage().startsWith("Gia sư vừa từ chối yêu cầu kết nối của bạn. Bạn có thể gửi lại sau 24 giờ"));
    }

    @Test @DisplayName("UTCID19: Ca chỉ có yêu cầu đang chờ của học viên khác -> vẫn gửi được (không giữ chỗ)")
    void utcid19_pendingDoesNotReserve() {
        ready();
        service.createDirectRequest(LEARNER_EMAIL, valid());
        verify(slots, atLeastOnce()).existsLockedSlotOverlap(any(), any(), anyInt(), any(), any());
        verify(trials, never()).existsTrialClashFrom(any(), any(), any(), anyInt(), any(), any());
        verify(requests).save(any(ConnectionRequest.class));
    }

    @Test @DisplayName("UTCID20: Gia sư chưa được duyệt -> 400 chưa sẵn sàng nhận học viên")
    void utcid20_tutorNotApproved() {
        ready();
        tutor.setVerificationStatus(TutorVerificationStatus.PENDING);
        var ex = assertThrows(BadRequestException.class, () -> service.createDirectRequest(LEARNER_EMAIL, valid()));
        assertTrue(ex.getMessage().startsWith("Gia sư hiện chưa sẵn sàng nhận học viên mới."));
        verify(requests, never()).save(any());
    }

    @Test @DisplayName("UTCID21: Gia sư hết gói hội viên -> 400 'Gia sư hiện chưa sẵn sàng nhận học viên mới. Vui lòng chọn gia sư khác.'")
    void utcid21_tutorNoMembership() {
        ready();
        when(membership.hasActiveMembership(eq(tutor.getTutorId()), any())).thenReturn(false);
        var ex = assertThrows(BadRequestException.class, () -> service.createDirectRequest(LEARNER_EMAIL, valid()));
        assertEquals("Gia sư hiện chưa sẵn sàng nhận học viên mới. Vui lòng chọn gia sư khác.", ex.getMessage());
    }
}
