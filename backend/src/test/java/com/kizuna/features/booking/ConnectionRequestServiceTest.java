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
        lenient().when(busyLoader.load(any(), any(), any(), any(), any())).thenReturn(BusyIndex.EMPTY);
        lenient().when(trialBookingRepository.findClassScheduleClash(any(), any(), any(), anyInt(), any(), any()))
                .thenReturn(Optional.empty());
    }

    private void stubBusy(BusyIndex busy) {
        when(busyLoader.load(any(), any(), any(), any(), any())).thenReturn(busy);
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
        return CreateConnectionRequestDTO.builder().contactPhone("0912345678").shareContact(true).tutorId(tutorId).slots(List.of(slots))
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
        // Học viên chỉ gửi giờ bắt đầu: độ dài ca (giờ kết thúc) để gia sư chọn khi đồng ý
        assertNull(saved.getSlots().get(0).getEndTime());

        assertEquals("PENDING", res.getStatus());
        assertEquals("Chờ gia sư xác nhận", res.getStatusLabel());
        assertEquals("N4", res.getCurrentLevel());
        assertEquals("Thi JLPT N3", res.getLearningGoals());
        assertEquals("Em chào thầy", res.getInitialMessage());
        assertEquals("Thứ hai", res.getSlots().get(0).getDayName());
        assertNull(res.getSlots().get(0).getDurationMinutes());
        assertNull(res.getSlots().get(0).getEndTime());
    }

    @Test
    @DisplayName("Học viên chọn trùng cùng một giờ bắt đầu thì báo lỗi")
    void create_duplicateStart() {
        stubLearnerAndTutor();
        BadRequestException ex = assertThrows(BadRequestException.class,
                () -> service.createDirectRequest("learner@test.com", request(slot(2, "19:00", 60), slot(2, "19:00", 90))));
        assertTrue(ex.getMessage().contains("bị chọn trùng"));
    }

    private ConnectionRequest pendingStartOnlyRequest() {
        ConnectionRequest cr = ConnectionRequest.builder()
                .requestId(UUID.randomUUID()).learner(learner).tutor(tutor)
                .requestType(ConnectionRequestType.DIRECT).status(ConnectionRequestStatus.PENDING).build();
        cr.getSlots().add(ConnectionRequestSlot.builder().slotId(UUID.randomUUID()).connectionRequest(cr).dayOfWeek(2)
                .startTime(LocalTime.of(19, 0)).build());
        cr.getSlots().add(ConnectionRequestSlot.builder().slotId(UUID.randomUUID()).connectionRequest(cr).dayOfWeek(2)
                .startTime(LocalTime.of(20, 0)).build());
        when(userRepository.findByEmail("tutor@test.com")).thenReturn(Optional.of(tutorUser));
        when(tutorRepository.findByUser(tutorUser)).thenReturn(Optional.of(tutor));
        when(connectionRequestRepository.findLockedById(cr.getRequestId())).thenReturn(Optional.of(cr));
        return cr;
    }

    private static RespondConnectionRequestDTO acceptWith(ConnectionRequest cr, Integer... minutes) {
        List<RespondConnectionRequestDTO.SlotDuration> list = new java.util.ArrayList<>();
        for (int i = 0; i < minutes.length; i++) {
            list.add(new RespondConnectionRequestDTO.SlotDuration(cr.getSlots().get(i).getSlotId(), minutes[i]));
        }
        return RespondConnectionRequestDTO.builder().action("ACCEPT").slotDurations(list).build();
    }

    @Test
    @DisplayName("Gia sư đồng ý phải chọn độ dài cho ca chưa có giờ kết thúc")
    void respond_accept_requiresTutorDuration() {
        ConnectionRequest cr = pendingStartOnlyRequest();
        BadRequestException ex = assertThrows(BadRequestException.class, () -> service.respond("tutor@test.com",
                cr.getRequestId(), RespondConnectionRequestDTO.builder().action("ACCEPT").build()));
        assertTrue(ex.getMessage().contains("chọn độ dài"));
        assertEquals(ConnectionRequestStatus.PENDING, cr.getStatus());
    }

    @Test
    @DisplayName("Gia sư chọn độ dài từng ca: server điền giờ kết thúc rồi mới đồng ý")
    void respond_accept_appliesTutorDurations() {
        ConnectionRequest cr = pendingStartOnlyRequest();
        service.respond("tutor@test.com", cr.getRequestId(), acceptWith(cr, 60, 120));
        assertEquals(ConnectionRequestStatus.ACCEPTED, cr.getStatus());
        assertEquals(LocalTime.of(20, 0), cr.getSlots().get(0).getEndTime());
        assertEquals(LocalTime.of(22, 0), cr.getSlots().get(1).getEndTime());
    }

    @Test
    @DisplayName("Độ dài gia sư chọn làm hai ca chồng nhau thì báo lỗi; độ dài ngoài 60/90/120 bị từ chối")
    void respond_accept_rejectsOverlapAndBadDuration() {
        ConnectionRequest cr = pendingStartOnlyRequest();
        BadRequestException overlap = assertThrows(BadRequestException.class,
                () -> service.respond("tutor@test.com", cr.getRequestId(), acceptWith(cr, 90, 60)));
        assertTrue(overlap.getMessage().contains("chồng giờ"));
        BadRequestException bad = assertThrows(BadRequestException.class,
                () -> service.respond("tutor@test.com", cr.getRequestId(), acceptWith(cr, 45, 60)));
        assertTrue(bad.getMessage().contains("60, 90 hoặc 120"));
        assertEquals(ConnectionRequestStatus.PENDING, cr.getStatus());
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
    @DisplayName("Giờ gia sư đã nhận cho học viên khác (ca đã khóa) thì không gửi được")
    void create_lockedSlot() {
        stubLearnerAndTutor();
        when(tutorScheduleService.isSlotAvailable(eq(tutorId), anyInt(), any(), any())).thenReturn(true);
        when(slotRepository.existsLockedSlotOverlap(eq(tutorId), any(), eq(2), any(), any())).thenReturn(true);

        ConflictException ex = assertThrows(ConflictException.class,
                () -> service.createDirectRequest("learner@test.com", request(slot(2, "19:00", 60))));
        assertTrue(ex.getMessage().contains("gia sư đã nhận học viên khác"));
        verify(connectionRequestRepository, never()).save(any());
    }

    @Test
    @DisplayName("Buổi học thử lẻ của học viên khác không chặn việc gửi yêu cầu kết nối (chỉ là giờ mong muốn)")
    void create_trialDoesNotBlockRequest() {
        stubLearnerAndTutor();
        when(tutorScheduleService.isSlotAvailable(eq(tutorId), anyInt(), any(), any())).thenReturn(true);

        service.createDirectRequest("learner@test.com", request(slot(6, "18:00", 60)));

        verify(connectionRequestRepository).save(any());
        verify(trialBookingRepository, never()).existsTrialClashFrom(any(), any(), any(), anyInt(), any(), any());
    }

    @Test
    @DisplayName("Yêu cầu kết nối chỉ lưu ca mong muốn: không tạo buổi học, không giữ chỗ")
    void create_savesOnlyDesiredSlots() {
        stubLearnerAndTutor();
        when(tutorScheduleService.isSlotAvailable(eq(tutorId), anyInt(), any(), any())).thenReturn(true);

        CreateConnectionRequestDTO req = request(slot(2, "19:00", 60), slot(5, "19:00", 60));
        ConnectionRequestResponseDTO res = service.createDirectRequest("learner@test.com", req);

        ArgumentCaptor<ConnectionRequest> captor = ArgumentCaptor.forClass(ConnectionRequest.class);
        verify(connectionRequestRepository).save(captor.capture());
        assertEquals(2, captor.getValue().getSlots().size());
        assertTrue(captor.getValue().getSessions().isEmpty());
        assertNull(res.getTotalSessions());
        verify(busyLoader, never()).load(any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("Giờ bắt đầu mong muốn: chỉ cần giờ bắt đầu nằm trong lịch rảnh, buổi được phép kéo dài quá giờ rảnh")
    void create_onlyStartMustBeWithinAvailability() {
        stubLearnerAndTutor();
        // Gia sư rảnh 18:30–20:30: 19:30 nằm trong giờ rảnh, buổi 120 phút kết thúc 21:30 (quá giờ rảnh)
        when(tutorScheduleService.isSlotAvailable(tutorId, 3, LocalTime.of(19, 30), LocalTime.of(20, 0))).thenReturn(true);

        service.createDirectRequest("learner@test.com", request(slot(3, "19:30", 120)));

        verify(connectionRequestRepository).save(any());
        verify(tutorScheduleService, never()).isSlotAvailable(tutorId, 3, LocalTime.of(19, 30), LocalTime.of(21, 30));
    }

    @Test
    @DisplayName("Giờ bắt đầu nằm ngoài lịch rảnh thì bị từ chối")
    void create_startOutsideAvailabilityRejected() {
        stubLearnerAndTutor();
        when(tutorScheduleService.isSlotAvailable(eq(tutorId), anyInt(), any(), any())).thenReturn(false);
        BadRequestException ex = assertThrows(BadRequestException.class,
                () -> service.createDirectRequest("learner@test.com", request(slot(3, "21:00", 60))));
        assertTrue(ex.getMessage().contains("nằm ngoài lịch rảnh"));
    }

    @Test
    @DisplayName("Độ dài ca học viên gửi kèm (client cũ) bị bỏ qua: hai giờ bắt đầu gần nhau vẫn gửi được")
    void create_learnerDurationIgnored() {
        stubLearnerAndTutor();
        when(tutorScheduleService.isSlotAvailable(eq(tutorId), anyInt(), any(), any())).thenReturn(true);
        service.createDirectRequest("learner@test.com", request(slot(3, "18:00", 120), slot(3, "19:00", 45)));
        verify(connectionRequestRepository).save(any());
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

    private void expiredMembership() {
        var membership = mock(com.kizuna.features.payment.service.TutorMembershipService.class);
        org.springframework.test.util.ReflectionTestUtils.setField(service, "membershipService", membership);
        lenient().when(membership.hasActiveMembership(eq(tutorId), any())).thenReturn(false);
    }

    @Test
    @DisplayName("Gia sư không có gói hội viên còn hạn thì không gửi được yêu cầu kết nối (gọi thẳng API)")
    void create_blockedWhenTutorHasNoMembership() {
        expiredMembership();
        when(userRepository.findByEmail("learner@test.com")).thenReturn(Optional.of(learnerUser));
        when(tutorRepository.findById(tutorId)).thenReturn(Optional.of(tutor));

        BadRequestException ex = assertThrows(BadRequestException.class,
                () -> service.createDirectRequest("learner@test.com", request(slot(2, "19:00", 60))));
        assertTrue(ex.getMessage().contains("chưa sẵn sàng nhận học viên mới"));
        verify(connectionRequestRepository, never()).save(any());
    }

    @Test
    @DisplayName("Gia sư hết gói hội viên không đồng ý kết nối được; vẫn từ chối được")
    void respond_acceptBlockedWithoutMembership_declineAllowed() {
        expiredMembership();
        ConnectionRequest cr = pendingRequest();
        when(userRepository.findByEmail("tutor@test.com")).thenReturn(Optional.of(tutorUser));
        when(tutorRepository.findByUser(tutorUser)).thenReturn(Optional.of(tutor));
        when(connectionRequestRepository.findLockedById(cr.getRequestId())).thenReturn(Optional.of(cr));

        BadRequestException ex = assertThrows(BadRequestException.class, () -> service.respond("tutor@test.com",
                cr.getRequestId(), RespondConnectionRequestDTO.builder().action("ACCEPT").build()));
        assertTrue(ex.getMessage().contains("gói hội viên"));
        assertEquals(ConnectionRequestStatus.PENDING, cr.getStatus());
        verify(connectionRequestRepository, never()).save(any());

        service.respond("tutor@test.com", cr.getRequestId(), RespondConnectionRequestDTO.builder().action("DECLINE").build());
        assertEquals(ConnectionRequestStatus.DECLINED, cr.getStatus());
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
        when(connectionRequestRepository.findLockedById(cr.getRequestId())).thenReturn(Optional.of(cr));
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
        when(connectionRequestRepository.findLockedById(cr.getRequestId())).thenReturn(Optional.of(cr));
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
        when(connectionRequestRepository.findLockedById(cr.getRequestId())).thenReturn(Optional.of(cr));
        when(busyLoader.load(eq(tutorId), eq(learner.getLearnerId()), eq(cr.getRequestId()), any(), any()))
                .thenReturn(new BusyIndex(List.of(), List.of(new DatedBusy(day, LocalTime.of(20, 0), LocalTime.of(20, 30), BusyKind.TRIAL))));

        ConflictException ex = assertThrows(ConflictException.class, () -> service.respond("tutor@test.com", cr.getRequestId(),
                RespondConnectionRequestDTO.builder().action("ACCEPT").build()));
        assertTrue(ex.getMessage().contains("bạn đã có lịch học thử"));
        assertEquals(ConnectionRequestStatus.PENDING, cr.getStatus());
    }

    @Test
    @DisplayName("Gia sư không đồng ý được khi đã có buổi trong yêu cầu bắt đầu (học viên sẽ thiếu buổi) - phương án (a)")
    void respond_accept_blockedWhenASessionAlreadyStarted() {
        ConnectionRequest cr = pendingRequest();
        LocalDate today = LocalDate.now(ZoneId.of("Asia/Ho_Chi_Minh"));
        cr.getSessions().add(ConnectionRequestSession.builder().connectionRequest(cr).sessionDate(today.minusDays(1))
                .startTime(LocalTime.of(19, 0)).endTime(LocalTime.of(20, 30)).build());
        cr.getSessions().add(ConnectionRequestSession.builder().connectionRequest(cr).sessionDate(today.plusDays(6))
                .startTime(LocalTime.of(19, 0)).endTime(LocalTime.of(20, 30)).build());
        when(userRepository.findByEmail("tutor@test.com")).thenReturn(Optional.of(tutorUser));
        when(tutorRepository.findByUser(tutorUser)).thenReturn(Optional.of(tutor));
        when(connectionRequestRepository.findLockedById(cr.getRequestId())).thenReturn(Optional.of(cr));

        BadRequestException ex = assertThrows(BadRequestException.class, () -> service.respond("tutor@test.com", cr.getRequestId(),
                RespondConnectionRequestDTO.builder().action("ACCEPT").build()));
        assertTrue(ex.getMessage().contains("đã qua"));
        assertEquals(ConnectionRequestStatus.PENDING, cr.getStatus());
        verify(connectionRequestRepository, never()).save(any());
    }

    // ------------------------------------------------------------------ L1: lịch của học viên với gia sư khác (cách 1)

    // ----- Liên hệ: ai thấy liên hệ của ai -----

    /** Yêu cầu {@code cr} hiển thị cho {@code viewer} (qua danh sách "yêu cầu của tôi"). */
    private ConnectionRequestResponseDTO viewAs(ConnectionRequest cr, User viewer) {
        var page = new org.springframework.data.domain.PageImpl<>(List.of(cr));
        lenient().when(userRepository.findByEmail(viewer.getEmail())).thenReturn(Optional.of(viewer));
        lenient().when(tutorRepository.findByUser(tutorUser)).thenReturn(Optional.of(tutor));
        lenient().when(learnerRepository.findByUser(learnerUser)).thenReturn(Optional.of(learner));
        lenient().when(connectionRequestRepository.findByTutorTutorIdAndRequestType(eq(tutorId), eq(ConnectionRequestType.DIRECT), any()))
                .thenReturn(page);
        lenient().when(connectionRequestRepository.findByLearnerLearnerIdAndRequestType(eq(learner.getLearnerId()), eq(ConnectionRequestType.DIRECT), any()))
                .thenReturn(page);
        return service.getMyDirectRequests(viewer.getEmail(), "ALL", 0, 20).getContent().get(0);
    }

    @Test
    @DisplayName("Liên hệ: đã kết nối thì học viên thấy SĐT + email gia sư; gia sư thấy liên hệ học viên đã gửi")
    void contact_visibleAfterAccepted() {
        tutor.setPhone("0911222333");
        ConnectionRequest cr = pendingRequest();
        cr.setStatus(ConnectionRequestStatus.ACCEPTED);
        cr.setContactEmail("hv-lienhe@test.com");
        cr.setContactPhone("0912345678");

        var learnerView = viewAs(cr, learnerUser);
        assertEquals("0911222333", learnerView.getPartnerPhone());
        assertEquals("tutor@test.com", learnerView.getPartnerEmail());

        var tutorView = viewAs(cr, tutorUser);
        assertEquals("0912345678", tutorView.getPartnerPhone());
        assertEquals("hv-lienhe@test.com", tutorView.getPartnerEmail());
    }

    @Test
    @DisplayName("Yêu cầu đang chờ trùng ca gia sư đã nhận cho học viên khác: gia sư thấy cảnh báo, học viên thì không")
    void pending_showsSlotWarningToTutorOnly() {
        ConnectionRequest cr = pendingRequest(); // Thứ hai 19:00–20:30
        when(slotRepository.existsLockedSlotOverlap(eq(tutorId), eq(cr.getRequestId()), eq(2), any(), any())).thenReturn(true);

        String warning = viewAs(cr, tutorUser).getSlotWarning();
        assertNotNull(warning);
        assertTrue(warning.contains("Thứ hai 19:00–20:30 bạn đã nhận cho học viên khác"));
        assertNull(viewAs(cr, learnerUser).getSlotWarning());
    }

    @Test
    @DisplayName("Yêu cầu đang chờ không trùng gì thì không có cảnh báo")
    void pending_noWarningWhenFree() {
        assertNull(viewAs(pendingRequest(), tutorUser).getSlotWarning());
    }

    @Test
    @DisplayName("Liên hệ: đang chờ thì học viên chưa thấy liên hệ gia sư; đã hủy thì gia sư không thấy liên hệ học viên")
    void contact_hiddenWhenNotAgreedOrClosed() {
        ConnectionRequest cr = pendingRequest();
        cr.setContactPhone("0912345678");
        assertNull(viewAs(cr, learnerUser).getPartnerPhone());
        assertEquals("0912345678", viewAs(cr, tutorUser).getPartnerPhone());
        cr.setStatus(ConnectionRequestStatus.CANCELLED);
        assertNull(viewAs(cr, tutorUser).getPartnerPhone());
    }

    private ConnectionRequest pendingWithSession(LocalDate day, String start, String end) {
        ConnectionRequest cr = pendingRequest();
        cr.getSessions().add(ConnectionRequestSession.builder().connectionRequest(cr).sessionDate(day)
                .startTime(LocalTime.parse(start)).endTime(LocalTime.parse(end)).build());
        return cr;
    }

    @Test
    @DisplayName("L1 - Gia sư không đồng ý được khi học viên đã có khóa học với gia sư khác cùng giờ")
    void respond_accept_blockedByLearnerCourseWithOtherTutor() {
        LocalDate day = LocalDate.now(ZoneId.of("Asia/Ho_Chi_Minh")).plusDays(8);
        ConnectionRequest cr = pendingWithSession(day, "19:00", "20:30");
        when(userRepository.findByEmail("tutor@test.com")).thenReturn(Optional.of(tutorUser));
        when(tutorRepository.findByUser(tutorUser)).thenReturn(Optional.of(tutor));
        when(connectionRequestRepository.findLockedById(cr.getRequestId())).thenReturn(Optional.of(cr));
        when(busyLoader.load(eq(tutorId), eq(learner.getLearnerId()), eq(cr.getRequestId()), any(), any()))
                .thenReturn(new BusyIndex(List.of(), List.of(new DatedBusy(day, LocalTime.of(19, 0), LocalTime.of(20, 30), BusyKind.LEARNER_COURSE))));

        ConflictException ex = assertThrows(ConflictException.class, () -> service.respond("tutor@test.com", cr.getRequestId(),
                RespondConnectionRequestDTO.builder().action("ACCEPT").build()));
        assertTrue(ex.getMessage().contains("học viên đã có buổi học với gia sư khác"));
        assertEquals(ConnectionRequestStatus.PENDING, cr.getStatus());
    }

    @Test
    @DisplayName("Học viên trùng lớp học hiện tại của chính mình thì không gửi được")
    void create_blockedByLearnerOwnClass() {
        stubLearnerAndTutor();
        when(tutorScheduleService.isSlotAvailable(eq(tutorId), anyInt(), any(), any())).thenReturn(true);
        when(trialBookingRepository.findClassScheduleClash(eq(tutorId), any(), any(), anyInt(), any(), any()))
                .thenReturn(Optional.of("LEARNER"));

        ConflictException ex = assertThrows(ConflictException.class,
                () -> service.createDirectRequest("learner@test.com", request(slot(2, "19:00", 90))));
        assertTrue(ex.getMessage().contains("trùng lớp học hiện tại của bạn"));
        verify(connectionRequestRepository, never()).save(any());
    }

    @Test
    @DisplayName("L1 - Gia sư đồng ý: yêu cầu đang chờ của học viên với gia sư khác bị trùng giờ tự hủy, không trùng thì giữ nguyên")
    void respond_accept_autoCancelsLearnerConflictingPending() {
        LocalDate day = LocalDate.now(ZoneId.of("Asia/Ho_Chi_Minh")).plusDays(8);
        ConnectionRequest cr = pendingWithSession(day, "19:00", "20:30");
        Tutor otherTutor = Tutor.builder().tutorId(UUID.randomUUID()).user(User.builder().userId(UUID.randomUUID()).build()).build();

        ConnectionRequest clashing = ConnectionRequest.builder().requestId(UUID.randomUUID()).learner(learner).tutor(otherTutor)
                .requestType(ConnectionRequestType.DIRECT).status(ConnectionRequestStatus.PENDING).build();
        clashing.getSessions().add(ConnectionRequestSession.builder().connectionRequest(clashing).sessionDate(day)
                .startTime(LocalTime.of(20, 0)).endTime(LocalTime.of(21, 0)).build());
        ConnectionRequest notClashing = ConnectionRequest.builder().requestId(UUID.randomUUID()).learner(learner).tutor(otherTutor)
                .requestType(ConnectionRequestType.DIRECT).status(ConnectionRequestStatus.PENDING).build();
        notClashing.getSessions().add(ConnectionRequestSession.builder().connectionRequest(notClashing).sessionDate(day)
                .startTime(LocalTime.of(21, 0)).endTime(LocalTime.of(22, 0)).build());

        OffsetDateTime trialStart = day.atTime(19, 30).atZone(ZoneId.of("Asia/Ho_Chi_Minh")).toOffsetDateTime();
        TrialBooking clashingTrial = TrialBooking.builder().bookingId(UUID.randomUUID()).learner(learner).tutor(otherTutor)
                .scheduledStartTime(trialStart).scheduledEndTime(trialStart.plusMinutes(30))
                .bookingStatus(TrialBookingStatus.PENDING).build();

        when(userRepository.findByEmail("tutor@test.com")).thenReturn(Optional.of(tutorUser));
        when(tutorRepository.findByUser(tutorUser)).thenReturn(Optional.of(tutor));
        when(connectionRequestRepository.findLockedById(cr.getRequestId())).thenReturn(Optional.of(cr));
        when(connectionRequestRepository.findByLearnerLearnerIdAndRequestTypeAndStatus(
                learner.getLearnerId(), ConnectionRequestType.DIRECT, ConnectionRequestStatus.PENDING))
                .thenReturn(List.of(clashing, notClashing));
        when(trialBookingRepository.findByLearnerLearnerIdAndBookingStatusIn(eq(learner.getLearnerId()), any()))
                .thenReturn(List.of(clashingTrial));

        service.respond("tutor@test.com", cr.getRequestId(), RespondConnectionRequestDTO.builder().action("ACCEPT").build());

        assertEquals(ConnectionRequestStatus.ACCEPTED, cr.getStatus());
        assertEquals(ConnectionRequestStatus.CANCELLED, clashing.getStatus());
        assertTrue(clashing.getResponseMessage().contains("gia sư khác"));
        assertEquals(ConnectionRequestStatus.PENDING, notClashing.getStatus());
        assertEquals(TrialBookingStatus.CANCELLED, clashingTrial.getBookingStatus());
    }

    @Test
    @DisplayName("Gia sư vẫn từ chối được yêu cầu có buổi đã qua")
    void respond_decline_allowedWhenASessionAlreadyStarted() {
        ConnectionRequest cr = pendingRequest();
        cr.getSessions().add(ConnectionRequestSession.builder().connectionRequest(cr)
                .sessionDate(LocalDate.now(ZoneId.of("Asia/Ho_Chi_Minh")).minusDays(1))
                .startTime(LocalTime.of(19, 0)).endTime(LocalTime.of(20, 30)).build());
        when(userRepository.findByEmail("tutor@test.com")).thenReturn(Optional.of(tutorUser));
        when(tutorRepository.findByUser(tutorUser)).thenReturn(Optional.of(tutor));
        when(connectionRequestRepository.findLockedById(cr.getRequestId())).thenReturn(Optional.of(cr));

        service.respond("tutor@test.com", cr.getRequestId(),
                RespondConnectionRequestDTO.builder().action("DECLINE").message("Tuần này đã qua, em gửi lại giúp thầy").build());
        assertEquals(ConnectionRequestStatus.DECLINED, cr.getStatus());
    }

    @Test
    @DisplayName("Gia sư từ chối - lưu lý do, trạng thái DECLINED")
    void respond_decline() {
        ConnectionRequest cr = pendingRequest();
        when(userRepository.findByEmail("tutor@test.com")).thenReturn(Optional.of(tutorUser));
        when(tutorRepository.findByUser(tutorUser)).thenReturn(Optional.of(tutor));
        when(connectionRequestRepository.findLockedById(cr.getRequestId())).thenReturn(Optional.of(cr));

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
        when(connectionRequestRepository.findLockedById(cr.getRequestId())).thenReturn(Optional.of(cr));

        assertThrows(AccessDeniedException.class, () -> service.respond("tutor@test.com", cr.getRequestId(),
                RespondConnectionRequestDTO.builder().action("ACCEPT").build()));
    }

    @Test
    @DisplayName("Học viên hủy yêu cầu đang chờ")
    void cancel_pending() {
        ConnectionRequest cr = pendingRequest();
        when(userRepository.findByEmail("learner@test.com")).thenReturn(Optional.of(learnerUser));
        when(connectionRequestRepository.findLockedById(cr.getRequestId())).thenReturn(Optional.of(cr));

        ConnectionRequestResponseDTO res = service.cancel("learner@test.com", cr.getRequestId());
        assertEquals("CANCELLED", res.getStatus());
    }
}
