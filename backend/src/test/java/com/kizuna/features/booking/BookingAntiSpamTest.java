package com.kizuna.features.booking;

import com.kizuna.core.entity.Learner;
import com.kizuna.core.entity.Role;
import com.kizuna.core.entity.Tutor;
import com.kizuna.core.entity.User;
import com.kizuna.core.exception.BadRequestException;
import com.kizuna.core.exception.ConflictException;
import com.kizuna.core.repository.LearnerRepository;
import com.kizuna.core.repository.TutorRepository;
import com.kizuna.core.repository.UserRepository;
import com.kizuna.features.booking.dto.request.ConnectionSlotRequestDTO;
import com.kizuna.features.booking.dto.request.CreateConnectionRequestDTO;
import com.kizuna.features.booking.dto.request.CreateTrialBookingRequestDTO;
import com.kizuna.features.booking.dto.request.RescheduleTrialBookingRequestDTO;
import com.kizuna.features.booking.dto.request.RespondTrialBookingRequestDTO;
import com.kizuna.features.booking.dto.response.TrialBookingResponseDTO;
import com.kizuna.features.booking.entity.*;
import com.kizuna.features.booking.repository.ConnectionRequestRepository;
import com.kizuna.features.booking.repository.ConnectionRequestSlotRepository;
import com.kizuna.features.booking.repository.TrialBookingHistoryRepository;
import com.kizuna.features.booking.repository.TrialBookingRepository;
import com.kizuna.features.booking.service.impl.ConnectionRequestServiceImpl;
import com.kizuna.features.booking.service.impl.TrialBookingServiceImpl;
import com.kizuna.features.tutor_search.service.TutorScheduleService;
import com.kizuna.features.verification.entity.TutorVerificationStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** Chống spam: 1 yêu cầu học thử đang mở / cặp, chờ sau khi bị từ chối, tự hết hạn, đổi lịch quay về chờ duyệt. */
@ExtendWith(MockitoExtension.class)
class BookingAntiSpamTest {

    @Mock private UserRepository userRepository;
    @Mock private LearnerRepository learnerRepository;
    @Mock private TutorRepository tutorRepository;
    @Mock private ConnectionRequestRepository connectionRequestRepository;
    @Mock private ConnectionRequestSlotRepository slotRepository;
    @Mock private TrialBookingRepository trialBookingRepository;
    @Mock private TrialBookingHistoryRepository trialBookingHistoryRepository;
    @Mock private TutorScheduleService tutorScheduleService;
    @Mock private JdbcTemplate jdbcTemplate;
    @Mock private com.kizuna.features.booking.service.ConnectionBusyLoader busyLoader;

    private TrialBookingServiceImpl trialService;
    private ConnectionRequestServiceImpl connectionService;

    private User learnerUser;
    private Learner learner;
    private User tutorUser;
    private Tutor tutor;
    private UUID tutorId;

    @BeforeEach
    void setUp() {
        trialService = new TrialBookingServiceImpl(userRepository, learnerRepository, tutorRepository,
                connectionRequestRepository, trialBookingRepository, trialBookingHistoryRepository, jdbcTemplate, busyLoader);
        org.mockito.Mockito.lenient().when(trialBookingRepository.isWithinAvailability(org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.anyInt(), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any()))
                .thenReturn(true);
        connectionService = new ConnectionRequestServiceImpl(userRepository, learnerRepository, tutorRepository,
                connectionRequestRepository, slotRepository, trialBookingRepository, trialBookingHistoryRepository,
                tutorScheduleService, jdbcTemplate, busyLoader);
        org.mockito.Mockito.lenient().when(busyLoader.load(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any()))
                .thenReturn(com.kizuna.features.booking.service.ConnectionSchedulePlanner.BusyIndex.EMPTY);

        tutorId = UUID.randomUUID();
        learnerUser = User.builder().userId(UUID.randomUUID()).email("learner@test.com").fullName("Hoc Vien")
                .role(Role.builder().roleName("ROLE_LEARNER").build()).build();
        learner = Learner.builder().learnerId(UUID.randomUUID()).user(learnerUser).build();
        tutorUser = User.builder().userId(UUID.randomUUID()).email("tutor@test.com").fullName("Yamada Sensei")
                .role(Role.builder().roleName("ROLE_TUTOR").build()).build();
        tutor = Tutor.builder().tutorId(tutorId).tutorCode(10001L).user(tutorUser)
                .verificationStatus(TutorVerificationStatus.APPROVED).build();
        lenient().when(learnerRepository.save(any(Learner.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    private void stubLearnerCreate() {
        when(userRepository.findByEmail("learner@test.com")).thenReturn(Optional.of(learnerUser));
        when(learnerRepository.findByUser(learnerUser)).thenReturn(Optional.of(learner));
        when(tutorRepository.findById(tutorId)).thenReturn(Optional.of(tutor));
    }

    private CreateTrialBookingRequestDTO trialRequest() {
        return CreateTrialBookingRequestDTO.builder().tutorId(tutorId)
                .scheduledStartTime(OffsetDateTime.now().plusDays(2)).durationMinutes(30).build();
    }

    private TrialBooking trial(TrialBookingStatus status, OffsetDateTime start) {
        TrialBooking tb = TrialBooking.builder().bookingId(UUID.randomUUID()).learner(learner).tutor(tutor)
                .scheduledStartTime(start).scheduledEndTime(start.plusMinutes(30)).bookingStatus(status).build();
        tb.setUpdatedAt(OffsetDateTime.now());
        return tb;
    }

    // ------------------------------------------------------------------ học thử

    @Test
    @DisplayName("Đang có 1 yêu cầu học thử mở với gia sư thì không gửi thêm được")
    void trial_blockWhenOpenRequestExists() {
        stubLearnerCreate();
        when(trialBookingRepository.existsByLearnerLearnerIdAndTutorTutorIdAndBookingStatusIn(
                eq(learner.getLearnerId()), eq(tutorId), anyCollection())).thenReturn(true);

        ConflictException ex = assertThrows(ConflictException.class,
                () -> trialService.createTrialBooking("learner@test.com", trialRequest()));
        assertTrue(ex.getMessage().contains("đang có một yêu cầu học thử với gia sư này"));
        verify(trialBookingRepository, never()).save(any());
    }

    @Test
    @DisplayName("Đang chờ 2 gia sư phản hồi học thử thì không gửi thêm yêu cầu học thử thứ 3 được")
    void trial_blockWhenTwoPendingElsewhere() {
        stubLearnerCreate();
        when(trialBookingRepository.countByLearnerLearnerIdAndBookingStatus(learner.getLearnerId(), TrialBookingStatus.PENDING))
                .thenReturn(2L);

        ConflictException ex = assertThrows(ConflictException.class,
                () -> trialService.createTrialBooking("learner@test.com", trialRequest()));
        assertTrue(ex.getMessage().contains("đang chờ 2 gia sư"));
        verify(trialBookingRepository, never()).save(any());
    }

    @Test
    @DisplayName("Vừa bị gia sư từ chối trong 24 giờ thì chưa gửi lại được")
    void trial_blockDuringDeclineCooldown() {
        stubLearnerCreate();
        when(trialBookingRepository.existsByLearnerLearnerIdAndTutorTutorIdAndBookingStatusAndUpdatedAtAfter(
                eq(learner.getLearnerId()), eq(tutorId), eq(TrialBookingStatus.DECLINED), any())).thenReturn(true);

        ConflictException ex = assertThrows(ConflictException.class,
                () -> trialService.createTrialBooking("learner@test.com", trialRequest()));
        assertTrue(ex.getMessage().contains("vừa từ chối"));
        verify(trialBookingRepository, never()).save(any());
    }

    @Test
    @DisplayName("Yêu cầu quá hạn chuyển sang Đã hết hạn và yêu cầu kết nối đi kèm cũng hết hạn")
    void trial_expireStalePending() {
        TrialBooking reachedStart = trial(TrialBookingStatus.PENDING, OffsetDateTime.now().minusMinutes(5));
        ConnectionRequest cr = ConnectionRequest.builder().requestId(UUID.randomUUID()).learner(learner).tutor(tutor)
                .status(ConnectionRequestStatus.PENDING).build();
        reachedStart.setConnectionRequest(cr);
        when(trialBookingRepository.findStalePending(eq(TrialBookingStatus.PENDING), any(), any()))
                .thenReturn(List.of(reachedStart));

        int count = trialService.expireStalePendingTrials();

        assertEquals(1, count);
        assertEquals(TrialBookingStatus.EXPIRED, reachedStart.getBookingStatus());
        assertEquals(ConnectionRequestStatus.EXPIRED, cr.getStatus());
        verify(trialBookingHistoryRepository).save(argThat(h ->
                h.getNewStatus() == TrialBookingStatus.EXPIRED && h.getChangeReason().contains("tới giờ học")));
    }

    @Test
    @DisplayName("L1 - Gia sư không nhận được buổi học thử khi học viên đã có buổi học (khóa) với gia sư khác cùng giờ")
    void trial_acceptBlockedByLearnerCourseWithOtherTutor() {
        java.time.ZoneId vn = java.time.ZoneId.of("Asia/Ho_Chi_Minh");
        OffsetDateTime start = java.time.LocalDate.now(vn).plusDays(3).atTime(19, 0).atZone(vn).toOffsetDateTime();
        TrialBooking pending = trial(TrialBookingStatus.PENDING, start);
        when(userRepository.findByEmail("tutor@test.com")).thenReturn(Optional.of(tutorUser));
        when(tutorRepository.findByUser(tutorUser)).thenReturn(Optional.of(tutor));
        when(trialBookingRepository.findById(pending.getBookingId())).thenReturn(Optional.of(pending));
        when(busyLoader.load(eq(tutorId), eq(learner.getLearnerId()), any(), any(), any()))
                .thenReturn(new com.kizuna.features.booking.service.ConnectionSchedulePlanner.BusyIndex(List.of(), List.of(
                        new com.kizuna.features.booking.service.ConnectionSchedulePlanner.DatedBusy(start.atZoneSameInstant(vn).toLocalDate(),
                                LocalTime.of(18, 30), LocalTime.of(20, 0),
                                com.kizuna.features.booking.service.ConnectionSchedulePlanner.BusyKind.LEARNER_COURSE))));

        ConflictException ex = assertThrows(ConflictException.class, () -> trialService.respondTrialBooking(
                "tutor@test.com", pending.getBookingId(),
                RespondTrialBookingRequestDTO.builder().action("ACCEPT").meetingLink("https://meet.google.com/abc").build()));
        assertTrue(ex.getMessage().contains("học viên đã có buổi học với gia sư khác"));
        assertEquals(TrialBookingStatus.PENDING, pending.getBookingStatus());
    }

    @Test
    @DisplayName("Gia sư không đồng ý được yêu cầu đã quá 48 giờ chưa phản hồi")
    void trial_respondToStaleRequestRejected() {
        TrialBooking stale = trial(TrialBookingStatus.PENDING, OffsetDateTime.now().plusDays(5));
        stale.setUpdatedAt(OffsetDateTime.now().minusHours(49));
        when(userRepository.findByEmail("tutor@test.com")).thenReturn(Optional.of(tutorUser));
        when(tutorRepository.findByUser(tutorUser)).thenReturn(Optional.of(tutor));
        when(trialBookingRepository.findById(stale.getBookingId())).thenReturn(Optional.of(stale));

        BadRequestException ex = assertThrows(BadRequestException.class, () -> trialService.respondTrialBooking(
                "tutor@test.com", stale.getBookingId(),
                RespondTrialBookingRequestDTO.builder().action("ACCEPT").meetingLink("https://meet.google.com/abc").build()));
        assertTrue(ex.getMessage().contains("hết hạn"));
        assertEquals(TrialBookingStatus.PENDING, stale.getBookingStatus());
    }

    @Test
    @DisplayName("Học viên đổi lịch buổi đã xác nhận thì quay về Chờ gia sư xác nhận")
    void trial_learnerRescheduleGoesBackToPending() {
        TrialBooking confirmed = trial(TrialBookingStatus.CONFIRMED, OffsetDateTime.now().plusDays(3));
        confirmed.setTutorConfirmed(true);
        when(userRepository.findByEmail("learner@test.com")).thenReturn(Optional.of(learnerUser));
        when(trialBookingRepository.findById(confirmed.getBookingId())).thenReturn(Optional.of(confirmed));
        when(trialBookingRepository.findClassScheduleClash(any(), any(), any(), anyInt(), any(), any())).thenReturn(Optional.empty());
        when(trialBookingRepository.save(any(TrialBooking.class))).thenAnswer(inv -> inv.getArgument(0));

        TrialBookingResponseDTO res = trialService.rescheduleTrialBooking("learner@test.com", confirmed.getBookingId(),
                RescheduleTrialBookingRequestDTO.builder().newScheduledStartTime(OffsetDateTime.now().plusDays(4)).build());

        assertEquals("PENDING", res.getBookingStatus());
        assertEquals(Boolean.FALSE, confirmed.getTutorConfirmed());
    }

    @Test
    @DisplayName("L3 - Học viên đổi lịch sang giờ ngoài lịch rảnh bị tính vào giới hạn 2 đề xuất/tuần")
    void trial_learnerOffScheduleRescheduleCountsTowardsLimit() {
        TrialBooking pending = trial(TrialBookingStatus.PENDING, OffsetDateTime.now().plusDays(3));
        when(userRepository.findByEmail("learner@test.com")).thenReturn(Optional.of(learnerUser));
        when(trialBookingRepository.findById(pending.getBookingId())).thenReturn(Optional.of(pending));
        when(trialBookingRepository.findClassScheduleClash(any(), any(), any(), anyInt(), any(), any())).thenReturn(Optional.empty());
        when(trialBookingRepository.isWithinAvailability(any(), anyInt(), any(), any())).thenReturn(false); // giờ mới ngoài lịch rảnh
        when(trialBookingRepository.countOffScheduleSince(eq(learner.getLearnerId()), eq(tutorId), any())).thenReturn(1L);
        when(trialBookingRepository.countOffScheduleReschedulesSince(eq(learner.getLearnerId()), eq(tutorId), any())).thenReturn(1L);

        ConflictException ex = assertThrows(ConflictException.class, () -> trialService.rescheduleTrialBooking("learner@test.com",
                pending.getBookingId(), RescheduleTrialBookingRequestDTO.builder().newScheduledStartTime(OffsetDateTime.now().plusDays(4)).build()));
        assertTrue(ex.getMessage().contains("ngoài lịch rảnh tối đa"));
        verify(trialBookingRepository, never()).save(any());
    }

    @Test
    @DisplayName("L3 - Học viên đổi lịch sang giờ ngoài lịch rảnh (chưa tới giới hạn): lưu dấu đề xuất trong lịch sử")
    void trial_learnerOffScheduleRescheduleIsMarkedInHistory() {
        TrialBooking pending = trial(TrialBookingStatus.PENDING, OffsetDateTime.now().plusDays(3));
        when(userRepository.findByEmail("learner@test.com")).thenReturn(Optional.of(learnerUser));
        when(trialBookingRepository.findById(pending.getBookingId())).thenReturn(Optional.of(pending));
        when(trialBookingRepository.findClassScheduleClash(any(), any(), any(), anyInt(), any(), any())).thenReturn(Optional.empty());
        when(trialBookingRepository.isWithinAvailability(any(), anyInt(), any(), any())).thenReturn(false);
        when(trialBookingRepository.save(any(TrialBooking.class))).thenAnswer(inv -> inv.getArgument(0));

        trialService.rescheduleTrialBooking("learner@test.com", pending.getBookingId(),
                RescheduleTrialBookingRequestDTO.builder().newScheduledStartTime(OffsetDateTime.now().plusDays(4)).build());

        assertEquals(Boolean.TRUE, pending.getOffSchedule());
        verify(trialBookingHistoryRepository).save(argThat(h -> h.getChangeReason().startsWith(
                TrialBookingServiceImpl.OFF_SCHEDULE_RESCHEDULE_MARK)));
    }

    @Test
    @DisplayName("L3 - Gia sư tự dời lịch sang giờ học viên đã có khóa với gia sư khác thì bị chặn")
    void trial_tutorRescheduleBlockedByLearnerCourseWithOtherTutor() {
        java.time.ZoneId vn = java.time.ZoneId.of("Asia/Ho_Chi_Minh");
        TrialBooking confirmed = trial(TrialBookingStatus.CONFIRMED, OffsetDateTime.now().plusDays(3));
        OffsetDateTime newStart = java.time.LocalDate.now(vn).plusDays(5).atTime(19, 0).atZone(vn).toOffsetDateTime();
        when(userRepository.findByEmail("tutor@test.com")).thenReturn(Optional.of(tutorUser));
        when(trialBookingRepository.findById(confirmed.getBookingId())).thenReturn(Optional.of(confirmed));
        when(trialBookingRepository.findClassScheduleClash(any(), any(), any(), anyInt(), any(), any())).thenReturn(Optional.empty());
        when(busyLoader.load(eq(tutorId), eq(learner.getLearnerId()), any(), any(), any()))
                .thenReturn(new com.kizuna.features.booking.service.ConnectionSchedulePlanner.BusyIndex(List.of(), List.of(
                        new com.kizuna.features.booking.service.ConnectionSchedulePlanner.DatedBusy(newStart.atZoneSameInstant(vn).toLocalDate(),
                                LocalTime.of(19, 0), LocalTime.of(20, 30),
                                com.kizuna.features.booking.service.ConnectionSchedulePlanner.BusyKind.LEARNER_COURSE))));

        assertThrows(ConflictException.class, () -> trialService.rescheduleTrialBooking("tutor@test.com",
                confirmed.getBookingId(), RescheduleTrialBookingRequestDTO.builder().newScheduledStartTime(newStart).build()));
        assertEquals(TrialBookingStatus.CONFIRMED, confirmed.getBookingStatus());
    }

    // ------------------------------------------------------------------ kết nối trực tiếp

    @Test
    @DisplayName("L4 - Yêu cầu kết nối đang chờ mà buổi đầu đã bắt đầu thì tự hết hạn ngay (không đợi đủ 24 giờ)")
    void direct_expireWhenFirstSessionStarted() {
        ConnectionRequest started = ConnectionRequest.builder().requestId(UUID.randomUUID()).learner(learner).tutor(tutor)
                .requestType(ConnectionRequestType.DIRECT).status(ConnectionRequestStatus.PENDING)
                .createdAt(OffsetDateTime.now().minusHours(3)).build();
        when(connectionRequestRepository.findByRequestTypeAndStatusAndCreatedAtBefore(any(), any(), any())).thenReturn(List.of());
        when(connectionRequestRepository.findPendingDirectWithStartedSession()).thenReturn(List.of(started));

        assertEquals(1, connectionService.expireStaleDirectRequests());
        assertEquals(ConnectionRequestStatus.EXPIRED, started.getStatus());
        assertTrue(started.getResponseMessage().contains("buổi học đầu tiên"));
    }

    @Test
    @DisplayName("Kết nối trực tiếp: vừa bị từ chối trong 24 giờ thì chưa gửi lại được")
    void direct_blockDuringDeclineCooldown() {
        stubLearnerCreate();
        when(connectionRequestRepository.existsByLearnerLearnerIdAndTutorTutorIdAndRequestTypeAndStatusAndRespondedAtAfter(
                eq(learner.getLearnerId()), eq(tutorId), eq(ConnectionRequestType.DIRECT),
                eq(ConnectionRequestStatus.DECLINED), any())).thenReturn(true);

        CreateConnectionRequestDTO req = CreateConnectionRequestDTO.builder().tutorId(tutorId)
                .slots(List.of(ConnectionSlotRequestDTO.builder().dayOfWeek(2).startTime(LocalTime.of(19, 0)).durationMinutes(60).build()))
                .build();
        ConflictException ex = assertThrows(ConflictException.class,
                () -> connectionService.createDirectRequest("learner@test.com", req));
        assertTrue(ex.getMessage().contains("vừa từ chối"));
        verify(connectionRequestRepository, never()).save(any());
    }

    @Test
    @DisplayName("Kết nối trực tiếp: đang có khóa kết nối còn buổi với gia sư thì không gửi thêm được")
    void direct_blockWhenActiveConnectionExists() {
        stubLearnerCreate();
        when(connectionRequestRepository.existsActiveDirectConnection(learner.getLearnerId(), tutorId)).thenReturn(true);

        CreateConnectionRequestDTO req = CreateConnectionRequestDTO.builder().tutorId(tutorId)
                .slots(List.of(ConnectionSlotRequestDTO.builder().dayOfWeek(2).startTime(LocalTime.of(19, 0)).durationMinutes(60).build()))
                .build();
        ConflictException ex = assertThrows(ConflictException.class,
                () -> connectionService.createDirectRequest("learner@test.com", req));
        assertTrue(ex.getMessage().contains("đang có khóa học kết nối"));
        verify(connectionRequestRepository, never()).save(any());
    }

    @Test
    @DisplayName("Kết nối trực tiếp: buổi học thử đã nhận / đã học xong không bị coi là đã kết nối")
    void direct_acceptedTrialDoesNotBlock() {
        stubLearnerCreate();
        // Chỉ có yêu cầu học thử ACCEPTED (không có khóa kết nối nào) → không bị chặn ở bước "đã kết nối"
        when(connectionRequestRepository.existsActiveDirectConnection(learner.getLearnerId(), tutorId)).thenReturn(false);
        when(tutorScheduleService.isSlotAvailable(any(), anyInt(), any(), any())).thenReturn(false);

        CreateConnectionRequestDTO req = CreateConnectionRequestDTO.builder().tutorId(tutorId)
                .slots(List.of(ConnectionSlotRequestDTO.builder().dayOfWeek(2).startTime(LocalTime.of(19, 0)).durationMinutes(60).build()))
                .build();
        // Đi qua bước kiểm tra kết nối, dừng ở bước kiểm tra lịch rảnh (do stub ở trên)
        BadRequestException ex = assertThrows(BadRequestException.class,
                () -> connectionService.createDirectRequest("learner@test.com", req));
        assertTrue(ex.getMessage().contains("ngoài lịch rảnh"));
    }

    @Test
    @DisplayName("Kết nối trực tiếp: quá 5 ngày gia sư chưa phản hồi thì tự hết hạn")
    void direct_expireStale() {
        ConnectionRequest cr = ConnectionRequest.builder().requestId(UUID.randomUUID()).learner(learner).tutor(tutor)
                .requestType(ConnectionRequestType.DIRECT).status(ConnectionRequestStatus.PENDING)
                .createdAt(OffsetDateTime.now().minusDays(6)).build();
        when(connectionRequestRepository.findByRequestTypeAndStatusAndCreatedAtBefore(
                eq(ConnectionRequestType.DIRECT), eq(ConnectionRequestStatus.PENDING), any())).thenReturn(List.of(cr));

        assertEquals(1, connectionService.expireStaleDirectRequests());
        assertEquals(ConnectionRequestStatus.EXPIRED, cr.getStatus());
        assertNotNull(cr.getRespondedAt());
    }
}
