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
import com.kizuna.features.booking.dto.request.RespondConnectionRequestDTO;
import com.kizuna.features.booking.dto.response.ConnectionRequestResponseDTO;
import com.kizuna.features.booking.entity.*;
import com.kizuna.features.booking.repository.ConnectionRequestRepository;
import com.kizuna.features.booking.repository.ConnectionRequestSlotRepository;
import com.kizuna.features.booking.repository.TrialBookingHistoryRepository;
import com.kizuna.features.booking.repository.TrialBookingRepository;
import com.kizuna.features.booking.service.ConnectionBusyLoader;
import com.kizuna.features.booking.service.ConnectionSchedulePlanner.BusyIndex;
import com.kizuna.features.booking.service.ConnectionSchedulePlanner.BusyKind;
import com.kizuna.features.booking.service.ConnectionSchedulePlanner.DatedBusy;
import com.kizuna.features.booking.service.ConnectionSchedulePlanner.WeeklyBusy;
import com.kizuna.features.booking.service.impl.ConnectionRequestServiceImpl;
import com.kizuna.features.tutor_search.service.TutorScheduleService;
import com.kizuna.features.verification.entity.TutorVerificationStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.AccessDeniedException;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ConnectionRequestServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private LearnerRepository learnerRepository;
    @Mock private TutorRepository tutorRepository;
    @Mock private ConnectionRequestRepository connectionRequestRepository;
    @Mock private ConnectionRequestSlotRepository slotRepository;
    @Mock private TrialBookingRepository trialBookingRepository;
    @Mock private TrialBookingHistoryRepository trialBookingHistoryRepository;
    @Mock private TutorScheduleService tutorScheduleService;
    @Mock private JdbcTemplate jdbcTemplate;
    @Mock private ConnectionBusyLoader busyLoader;

    @InjectMocks
    private ConnectionRequestServiceImpl service;

    private User learnerUser;
    private Learner learner;
    private User tutorUser;
    private Tutor tutor;
    private UUID tutorId;

    @BeforeEach
    void setUp() {
        tutorId = UUID.randomUUID();
        learnerUser = User.builder().userId(UUID.randomUUID()).email("learner@test.com").fullName("Hoc Vien")
                .role(Role.builder().roleName("ROLE_LEARNER").build()).build();
        learner = Learner.builder().learnerId(UUID.randomUUID()).user(learnerUser).build();
        tutorUser = User.builder().userId(UUID.randomUUID()).email("tutor@test.com").fullName("Yamada Sensei")
                .role(Role.builder().roleName("ROLE_TUTOR").build()).build();
        tutor = Tutor.builder().tutorId(tutorId).tutorCode(10001L).user(tutorUser)
                .verificationStatus(TutorVerificationStatus.APPROVED).build();

        lenient().when(learnerRepository.save(any(Learner.class))).thenAnswer(inv -> inv.getArgument(0));
        lenient().when(connectionRequestRepository.save(any(ConnectionRequest.class))).thenAnswer(inv -> {
            ConnectionRequest cr = inv.getArgument(0);
            if (cr.getRequestId() == null) cr.setRequestId(UUID.randomUUID());
            return cr;
        });
        lenient().when(busyLoader.load(any(), any(), any(), any(), any(), any())).thenReturn(BusyIndex.EMPTY);
        lenient().when(trialBookingRepository.findClassScheduleClash(any(), any(), anyInt(), any(), any()))
                .thenReturn(Optional.empty());
    }

    private void stubBusy(BusyIndex busy) {
        when(busyLoader.load(any(), any(), any(), any(), any(), any())).thenReturn(busy);
    }

    private void stubLearnerAndTutor() {
        when(userRepository.findByEmail("learner@test.com")).thenReturn(Optional.of(learnerUser));
        when(tutorRepository.findById(tutorId)).thenReturn(Optional.of(tutor));
        when(learnerRepository.findByUser(learnerUser)).thenReturn(Optional.of(learner));
    }

    private static ConnectionSlotRequestDTO slot(int dow, String start, int minutes) {
        return ConnectionSlotRequestDTO.builder().dayOfWeek(dow).startTime(LocalTime.parse(start)).durationMinutes(minutes).build();
    }

    private CreateConnectionRequestDTO request(ConnectionSlotRequestDTO... slots) {
        return CreateConnectionRequestDTO.builder().tutorId(tutorId).slots(List.of(slots))
                .currentLevel("N4").learningGoals("Thi JLPT N3").initialMessage("Em chào thầy").build();
    }

    @Test
    @DisplayName("Gửi yêu cầu kết nối trực tiếp thành công - lưu các ca hằng tuần, trạng thái chờ gia sư")
    void create_success() {
        stubLearnerAndTutor();
        when(tutorScheduleService.isSlotAvailable(eq(tutorId), anyInt(), any(), any())).thenReturn(true);

        ConnectionRequestResponseDTO res = service.createDirectRequest("learner@test.com",
                request(slot(2, "19:00", 90), slot(5, "19:00", 60)));

        ArgumentCaptor<ConnectionRequest> captor = ArgumentCaptor.forClass(ConnectionRequest.class);
        verify(connectionRequestRepository).save(captor.capture());
        ConnectionRequest saved = captor.getValue();
        assertEquals(ConnectionRequestType.DIRECT, saved.getRequestType());
        assertEquals(ConnectionRequestStatus.PENDING, saved.getStatus());
        assertEquals(2, saved.getSlots().size());
        assertEquals(LocalTime.of(20, 30), saved.getSlots().get(0).getEndTime());

        assertEquals("PENDING", res.getStatus());
        assertEquals("Chờ gia sư xác nhận", res.getStatusLabel());
        assertEquals("N4", res.getCurrentLevel());
        assertEquals("Thi JLPT N3", res.getLearningGoals());
        assertEquals("Em chào thầy", res.getInitialMessage());
        assertEquals("Thứ hai", res.getSlots().get(0).getDayName());
        assertEquals(90, res.getSlots().get(0).getDurationMinutes());
    }

    @Test
    @DisplayName("Ca nằm ngoài lịch rảnh của gia sư thì bị từ chối")
    void create_outsideAvailability() {
        stubLearnerAndTutor();
        when(tutorScheduleService.isSlotAvailable(eq(tutorId), anyInt(), any(), any())).thenReturn(false);

        BadRequestException ex = assertThrows(BadRequestException.class,
                () -> service.createDirectRequest("learner@test.com", request(slot(2, "19:00", 60))));
        assertTrue(ex.getMessage().contains("ngoài lịch rảnh"));
        verify(connectionRequestRepository, never()).save(any());
    }

    @Test
    @DisplayName("Ca đã bị học viên khác nhận (đã khóa) thì bị từ chối")
    void create_lockedSlot() {
        stubLearnerAndTutor();
        when(tutorScheduleService.isSlotAvailable(eq(tutorId), anyInt(), any(), any())).thenReturn(true);
        stubBusy(new BusyIndex(List.of(new WeeklyBusy(2, LocalTime.of(19, 0), LocalTime.of(20, 0), BusyKind.TUTOR_COURSE)), List.of()));

        ConflictException ex = assertThrows(ConflictException.class,
                () -> service.createDirectRequest("learner@test.com", request(slot(2, "19:00", 60))));
        assertTrue(ex.getMessage().contains("gia sư đã có buổi học với học viên khác"));
        verify(connectionRequestRepository, never()).save(any());
    }

    @Test
    @DisplayName("Ca trùng buổi học thử của học viên khác từ ngày bắt đầu trở đi thì bị từ chối")
    void create_trialClash() {
        stubLearnerAndTutor();
        when(tutorScheduleService.isSlotAvailable(eq(tutorId), anyInt(), any(), any())).thenReturn(true);
        // Học viên khác có buổi học thử vào Thứ Sáu của tuần thứ 3 tính từ hôm nay → 1 trong 8 buổi bị kín
        LocalDate today = LocalDate.now(ZoneId.of("Asia/Ho_Chi_Minh"));
        LocalDate friday = today.with(java.time.temporal.TemporalAdjusters.next(java.time.DayOfWeek.FRIDAY)).plusWeeks(2);
        stubBusy(new BusyIndex(List.of(), List.of(new DatedBusy(friday, LocalTime.of(18, 0), LocalTime.of(18, 30), BusyKind.TRIAL))));

        ConflictException ex = assertThrows(ConflictException.class,
                () -> service.createDirectRequest("learner@test.com", request(slot(6, "18:00", 60))));
        assertTrue(ex.getMessage().contains("gia sư đã có lịch học thử"));
        assertTrue(ex.getMessage().contains(friday.format(java.time.format.DateTimeFormatter.ofPattern("dd/MM"))));
        verify(connectionRequestRepository, never()).save(any());
    }

    @Test
    @DisplayName("Lưu đủ số buổi cụ thể; ngày bắt đầu trong quá khứ được đưa về hôm nay")
    void create_savesSessions() {
        stubLearnerAndTutor();
        when(tutorScheduleService.isSlotAvailable(eq(tutorId), anyInt(), any(), any())).thenReturn(true);
        LocalDate today = LocalDate.now(ZoneId.of("Asia/Ho_Chi_Minh"));

        CreateConnectionRequestDTO req = request(slot(2, "19:00", 60), slot(5, "19:00", 60));
        req.setStartDate(today.minusDays(10));
        req.setTotalSessions(5);
        ConnectionRequestResponseDTO res = service.createDirectRequest("learner@test.com", req);

        assertEquals(5, res.getTotalSessions());
        assertEquals(5, res.getSessions().size());
        assertFalse(res.getStartDate().isBefore(today));
        assertTrue(res.getSessions().stream().allMatch(s -> s.getDayOfWeek() == 2 || s.getDayOfWeek() == 5));
    }

    @Test
    @DisplayName("Số buổi vượt quá 12 thì bị từ chối")
    void create_tooManySessions() {
        stubLearnerAndTutor();
        CreateConnectionRequestDTO req = request(slot(2, "19:00", 60));
        req.setTotalSessions(13);
        BadRequestException ex = assertThrows(BadRequestException.class,
                () -> service.createDirectRequest("learner@test.com", req));
        assertTrue(ex.getMessage().contains("12"));
    }

    @Test
    @DisplayName("Xem trước: báo buổi bị kín và gợi ý tuần bắt đầu không vướng")
    void preview_reportsConflictAndSuggestsWeek() {
        when(userRepository.findByEmail("learner@test.com")).thenReturn(Optional.of(learnerUser));
        when(learnerRepository.findByUser(learnerUser)).thenReturn(Optional.of(learner));
        when(tutorRepository.existsById(tutorId)).thenReturn(true);
        LocalDate today = LocalDate.now(ZoneId.of("Asia/Ho_Chi_Minh"));
        LocalDate nextMonday = today.with(java.time.temporal.TemporalAdjusters.next(java.time.DayOfWeek.MONDAY));
        LocalDate nextWed = nextMonday.plusDays(2);
        stubBusy(new BusyIndex(List.of(), List.of(new DatedBusy(nextWed, LocalTime.of(19, 0), LocalTime.of(19, 30), BusyKind.TRIAL))));

        var res = service.preview("learner@test.com", com.kizuna.features.booking.dto.request.ConnectionPreviewRequestDTO.builder()
                .tutorId(tutorId).slots(List.of(slot(4, "19:00", 60))).startDate(today).totalSessions(4).build());

        assertEquals(1, res.getConflictCount());
        var busySession = res.getSessions().stream().filter(s -> !s.isAvailable()).findFirst().orElseThrow();
        assertEquals(nextWed, busySession.getDate());
        assertEquals("gia sư đã có lịch học thử", busySession.getReason());
        // Bắt đầu từ tuần sau tuần bị kín thì cả 4 buổi đều trống
        assertEquals(nextMonday.plusWeeks(1), res.getSuggestedStartWeek());
    }

    @Test
    @DisplayName("Ngày bắt đầu học vượt quá 4 tuần được xem thì bị từ chối")
    void create_startDateTooFar() {
        stubLearnerAndTutor();
        CreateConnectionRequestDTO req = request(slot(2, "19:00", 60));
        req.setStartDate(LocalDate.now(ZoneId.of("Asia/Ho_Chi_Minh")).plusWeeks(5));
        BadRequestException ex = assertThrows(BadRequestException.class,
                () -> service.createDirectRequest("learner@test.com", req));
        assertTrue(ex.getMessage().contains("4 tuần"));
    }

    @Test
    @DisplayName("Thời lượng ca không thuộc 60/90/120 phút thì bị từ chối")
    void create_invalidDuration() {
        stubLearnerAndTutor();
        BadRequestException ex = assertThrows(BadRequestException.class,
                () -> service.createDirectRequest("learner@test.com", request(slot(2, "19:00", 45))));
        assertTrue(ex.getMessage().contains("60, 90 hoặc 120"));
    }

    @Test
    @DisplayName("Hai ca cùng ngày chồng giờ nhau thì bị từ chối")
    void create_overlappingSlots() {
        stubLearnerAndTutor();
        BadRequestException ex = assertThrows(BadRequestException.class,
                () -> service.createDirectRequest("learner@test.com", request(slot(3, "18:00", 120), slot(3, "19:00", 60))));
        assertTrue(ex.getMessage().contains("chồng giờ"));
    }

    @Test
    @DisplayName("Đã có yêu cầu kết nối đang chờ với gia sư này thì không gửi thêm")
    void create_duplicatePending() {
        stubLearnerAndTutor();
        when(connectionRequestRepository.existsByLearnerLearnerIdAndTutorTutorIdAndRequestTypeAndStatus(
                learner.getLearnerId(), tutorId, ConnectionRequestType.DIRECT, ConnectionRequestStatus.PENDING)).thenReturn(true);

        ConflictException ex = assertThrows(ConflictException.class,
                () -> service.createDirectRequest("learner@test.com", request(slot(2, "19:00", 60))));
        assertTrue(ex.getMessage().contains("đang chờ gia sư phản hồi"));
    }

    private ConnectionRequest pendingRequest() {
        ConnectionRequest cr = ConnectionRequest.builder()
                .requestId(UUID.randomUUID()).learner(learner).tutor(tutor)
                .requestType(ConnectionRequestType.DIRECT).status(ConnectionRequestStatus.PENDING).build();
        cr.getSlots().add(ConnectionRequestSlot.builder().connectionRequest(cr).dayOfWeek(2)
                .startTime(LocalTime.of(19, 0)).endTime(LocalTime.of(20, 30)).build());
        return cr;
    }

    @Test
    @DisplayName("Gia sư đồng ý - kết nối ACCEPTED và hủy các buổi học thử còn mở giữa hai người")
    void respond_accept() {
        ConnectionRequest cr = pendingRequest();
        TrialBooking openTrial = TrialBooking.builder().bookingId(UUID.randomUUID()).learner(learner).tutor(tutor)
                .scheduledStartTime(OffsetDateTime.now().plusDays(1)).scheduledEndTime(OffsetDateTime.now().plusDays(1).plusMinutes(30))
                .bookingStatus(TrialBookingStatus.PENDING).build();
        when(userRepository.findByEmail("tutor@test.com")).thenReturn(Optional.of(tutorUser));
        when(tutorRepository.findByUser(tutorUser)).thenReturn(Optional.of(tutor));
        when(connectionRequestRepository.findById(cr.getRequestId())).thenReturn(Optional.of(cr));
        when(trialBookingRepository.findByTutorTutorIdAndLearnerLearnerIdAndBookingStatusIn(any(), any(), any()))
                .thenReturn(List.of(openTrial));

        ConnectionRequestResponseDTO res = service.respond("tutor@test.com", cr.getRequestId(),
                RespondConnectionRequestDTO.builder().action("ACCEPT").message("Hẹn em thứ hai").build());

        assertEquals(ConnectionRequestStatus.ACCEPTED, cr.getStatus());
        assertNotNull(cr.getRespondedAt());
        assertEquals("Đã kết nối", res.getStatusLabel());
        assertEquals("Hẹn em thứ hai", res.getResponseMessage());
        assertEquals(TrialBookingStatus.CANCELLED, openTrial.getBookingStatus());
    }

    @Test
    @DisplayName("Gia sư đồng ý nhưng ca đã được nhận cho học viên khác thì báo trùng")
    void respond_accept_slotTakenMeanwhile() {
        ConnectionRequest cr = pendingRequest();
        when(userRepository.findByEmail("tutor@test.com")).thenReturn(Optional.of(tutorUser));
        when(tutorRepository.findByUser(tutorUser)).thenReturn(Optional.of(tutor));
        when(connectionRequestRepository.findById(cr.getRequestId())).thenReturn(Optional.of(cr));
        when(slotRepository.existsLockedSlotOverlap(eq(tutorId), eq(cr.getRequestId()), eq(2), any(), any())).thenReturn(true);

        assertThrows(ConflictException.class, () -> service.respond("tutor@test.com", cr.getRequestId(),
                RespondConnectionRequestDTO.builder().action("ACCEPT").build()));
        assertEquals(ConnectionRequestStatus.PENDING, cr.getStatus());
    }

    @Test
    @DisplayName("Gia sư đồng ý nhưng một buổi cụ thể trùng buổi học thử đã xác nhận thì báo rõ buổi nào")
    void respond_accept_sessionConflict() {
        ConnectionRequest cr = pendingRequest();
        LocalDate day = LocalDate.now(ZoneId.of("Asia/Ho_Chi_Minh")).plusDays(9);
        cr.getSessions().add(ConnectionRequestSession.builder().connectionRequest(cr).sessionDate(day)
                .startTime(LocalTime.of(19, 0)).endTime(LocalTime.of(20, 30)).build());
        when(userRepository.findByEmail("tutor@test.com")).thenReturn(Optional.of(tutorUser));
        when(tutorRepository.findByUser(tutorUser)).thenReturn(Optional.of(tutor));
        when(connectionRequestRepository.findById(cr.getRequestId())).thenReturn(Optional.of(cr));
        when(busyLoader.load(eq(tutorId), eq(learner.getLearnerId()), eq(cr.getRequestId()), any(), any(),
                eq(ConnectionBusyLoader.Mode.TUTOR_ACCEPT)))
                .thenReturn(new BusyIndex(List.of(), List.of(new DatedBusy(day, LocalTime.of(20, 0), LocalTime.of(20, 30), BusyKind.TRIAL))));

        ConflictException ex = assertThrows(ConflictException.class, () -> service.respond("tutor@test.com", cr.getRequestId(),
                RespondConnectionRequestDTO.builder().action("ACCEPT").build()));
        assertTrue(ex.getMessage().contains("bạn đã có lịch học thử"));
        assertEquals(ConnectionRequestStatus.PENDING, cr.getStatus());
    }

    @Test
    @DisplayName("Gia sư từ chối - lưu lý do, trạng thái DECLINED")
    void respond_decline() {
        ConnectionRequest cr = pendingRequest();
        when(userRepository.findByEmail("tutor@test.com")).thenReturn(Optional.of(tutorUser));
        when(tutorRepository.findByUser(tutorUser)).thenReturn(Optional.of(tutor));
        when(connectionRequestRepository.findById(cr.getRequestId())).thenReturn(Optional.of(cr));

        ConnectionRequestResponseDTO res = service.respond("tutor@test.com", cr.getRequestId(),
                RespondConnectionRequestDTO.builder().action("DECLINE").message("Thầy đã kín lịch").build());

        assertEquals(ConnectionRequestStatus.DECLINED, cr.getStatus());
        assertEquals("Thầy đã kín lịch", res.getResponseMessage());
    }

    @Test
    @DisplayName("Gia sư khác không được phản hồi yêu cầu không phải của mình")
    void respond_wrongTutor() {
        ConnectionRequest cr = pendingRequest();
        Tutor other = Tutor.builder().tutorId(UUID.randomUUID()).user(tutorUser).build();
        when(userRepository.findByEmail("tutor@test.com")).thenReturn(Optional.of(tutorUser));
        when(tutorRepository.findByUser(tutorUser)).thenReturn(Optional.of(other));
        when(connectionRequestRepository.findById(cr.getRequestId())).thenReturn(Optional.of(cr));

        assertThrows(AccessDeniedException.class, () -> service.respond("tutor@test.com", cr.getRequestId(),
                RespondConnectionRequestDTO.builder().action("ACCEPT").build()));
    }

    @Test
    @DisplayName("Học viên hủy yêu cầu đang chờ")
    void cancel_pending() {
        ConnectionRequest cr = pendingRequest();
        when(userRepository.findByEmail("learner@test.com")).thenReturn(Optional.of(learnerUser));
        when(connectionRequestRepository.findById(cr.getRequestId())).thenReturn(Optional.of(cr));

        ConnectionRequestResponseDTO res = service.cancel("learner@test.com", cr.getRequestId());
        assertEquals("CANCELLED", res.getStatus());
    }
}
