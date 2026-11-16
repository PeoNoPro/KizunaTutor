package com.kizuna.features.booking;

import com.kizuna.core.entity.Learner;
import com.kizuna.core.entity.Role;
import com.kizuna.core.entity.Tutor;
import com.kizuna.core.entity.User;
import com.kizuna.core.repository.LearnerRepository;
import com.kizuna.core.repository.TutorRepository;
import com.kizuna.core.repository.UserRepository;
import com.kizuna.features.booking.dto.request.ConnectionSlotRequestDTO;
import com.kizuna.features.booking.dto.request.CreateConnectionRequestDTO;
import com.kizuna.features.booking.dto.request.RespondConnectionRequestDTO;
import com.kizuna.features.booking.entity.*;
import com.kizuna.features.booking.repository.ConnectionRequestRepository;
import com.kizuna.features.booking.repository.ConnectionRequestSlotRepository;
import com.kizuna.features.booking.repository.TrialBookingHistoryRepository;
import com.kizuna.features.booking.repository.TrialBookingRepository;
import com.kizuna.features.booking.service.ConnectionBusyLoader;
import com.kizuna.features.booking.service.ConnectionSchedulePlanner.BusyIndex;
import com.kizuna.features.booking.service.impl.ConnectionRequestServiceImpl;
import com.kizuna.features.booking.service.impl.TrialBookingServiceImpl;
import com.kizuna.features.notification.service.NotificationEvents;
import com.kizuna.features.tutor_search.service.TutorScheduleService;
import com.kizuna.features.verification.entity.TutorVerificationStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** Phase 2: thông báo tự động của luồng kết nối trực tiếp (nhóm CONNECTION) và học thử (nhóm TRIAL). */
@ExtendWith(MockitoExtension.class)
class BookingNotificationTest {

    private static final ZoneId VN = ZoneId.of("Asia/Ho_Chi_Minh");

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
    @Mock private NotificationEvents notificationEvents;

    private ConnectionRequestServiceImpl connectionService;
    private TrialBookingServiceImpl trialService;

    private User learnerUser;
    private Learner learner;
    private User tutorUser;
    private Tutor tutor;

    @BeforeEach
    void setUp() {
        connectionService = new ConnectionRequestServiceImpl(userRepository, learnerRepository, tutorRepository,
                connectionRequestRepository, slotRepository, trialBookingRepository, trialBookingHistoryRepository,
                tutorScheduleService, jdbcTemplate, busyLoader);
        trialService = new TrialBookingServiceImpl(userRepository, learnerRepository, tutorRepository,
                connectionRequestRepository, trialBookingRepository, trialBookingHistoryRepository, jdbcTemplate, busyLoader);
        ReflectionTestUtils.setField(connectionService, "notificationEvents", notificationEvents);
        ReflectionTestUtils.setField(trialService, "notificationEvents", notificationEvents);

        learnerUser = User.builder().userId(UUID.randomUUID()).email("learner@test.com").fullName("Hoc Vien")
                .role(Role.builder().roleName("ROLE_LEARNER").build()).build();
        learner = Learner.builder().learnerId(UUID.randomUUID()).user(learnerUser).build();
        tutorUser = User.builder().userId(UUID.randomUUID()).email("tutor@test.com").fullName("Yamada Sensei")
                .role(Role.builder().roleName("ROLE_TUTOR").build()).build();
        tutor = Tutor.builder().tutorId(UUID.randomUUID()).tutorCode(10001L).user(tutorUser)
                .verificationStatus(TutorVerificationStatus.APPROVED).build();

        lenient().when(learnerRepository.save(any(Learner.class))).thenAnswer(inv -> inv.getArgument(0));
        lenient().when(connectionRequestRepository.save(any(ConnectionRequest.class))).thenAnswer(inv -> {
            ConnectionRequest cr = inv.getArgument(0);
            if (cr.getRequestId() == null) cr.setRequestId(UUID.randomUUID());
            return cr;
        });
        lenient().when(trialBookingRepository.save(any(TrialBooking.class))).thenAnswer(inv -> inv.getArgument(0));
        lenient().when(busyLoader.load(any(), any(), any(), any(), any())).thenReturn(BusyIndex.EMPTY);
    }

    private ConnectionRequest pendingDirect() {
        ConnectionRequest cr = ConnectionRequest.builder().requestId(UUID.randomUUID()).learner(learner).tutor(tutor)
                .requestType(ConnectionRequestType.DIRECT).status(ConnectionRequestStatus.PENDING).build();
        cr.getSlots().add(ConnectionRequestSlot.builder().connectionRequest(cr).dayOfWeek(2)
                .startTime(LocalTime.of(19, 0)).endTime(LocalTime.of(20, 30)).build());
        cr.getSessions().add(ConnectionRequestSession.builder().connectionRequest(cr)
                .sessionDate(LocalDate.now(VN).plusDays(8)).startTime(LocalTime.of(19, 0)).endTime(LocalTime.of(20, 30)).build());
        return cr;
    }

    private void stubTutorResponds(ConnectionRequest cr) {
        when(userRepository.findByEmail("tutor@test.com")).thenReturn(Optional.of(tutorUser));
        when(tutorRepository.findByUser(tutorUser)).thenReturn(Optional.of(tutor));
        when(connectionRequestRepository.findById(cr.getRequestId())).thenReturn(Optional.of(cr));
    }

    @Test
    @DisplayName("Học viên gửi yêu cầu kết nối → gia sư nhận thông báo nhóm CONNECTION, mở tab Kết nối trực tiếp")
    void created_notifiesTutor() {
        when(userRepository.findByEmail("learner@test.com")).thenReturn(Optional.of(learnerUser));
        when(tutorRepository.findById(tutor.getTutorId())).thenReturn(Optional.of(tutor));
        when(learnerRepository.findByUser(learnerUser)).thenReturn(Optional.of(learner));
        when(tutorScheduleService.isSlotAvailable(any(), anyInt(), any(), any())).thenReturn(true);

        var res = connectionService.createDirectRequest("learner@test.com", CreateConnectionRequestDTO.builder().contactPhone("0912345678").shareContact(true)
                .tutorId(tutor.getTutorId())
                .slots(List.of(ConnectionSlotRequestDTO.builder().dayOfWeek(2).startTime(LocalTime.of(19, 0)).durationMinutes(90).build()))
                .build());

        verify(notificationEvents).publish(eq("connection:" + res.getRequestId() + ":created"), eq("CONNECTION"),
                eq("Có yêu cầu kết nối mới"), argThat(c -> c.contains("Hoc Vien") && c.contains("Thứ hai 19:00") && !c.contains("buổi")),
                eq("/tutor/connections?tab=direct"), eq(List.of(tutorUser.getUserId())));
    }

    @Test
    @DisplayName("Gia sư đồng ý kết nối → học viên nhận thông báo, mở tab Kết nối trực tiếp của học viên")
    void accepted_notifiesLearner() {
        ConnectionRequest cr = pendingDirect();
        stubTutorResponds(cr);

        connectionService.respond("tutor@test.com", cr.getRequestId(),
                RespondConnectionRequestDTO.builder().action("ACCEPT").message("Hẹn em thứ hai").build());

        verify(notificationEvents).publish(eq("connection:" + cr.getRequestId() + ":accepted"), eq("CONNECTION"),
                eq("Gia sư đã đồng ý kết nối"), argThat(c -> c.contains("Yamada Sensei") && c.contains("1 buổi") && c.contains("Hẹn em thứ hai")),
                eq("/pages/learner-requests/?tab=direct"), eq(List.of(learnerUser.getUserId())));
    }

    @Test
    @DisplayName("Gia sư từ chối → học viên nhận thông báo kèm lý do")
    void declined_notifiesLearnerWithReason() {
        ConnectionRequest cr = pendingDirect();
        stubTutorResponds(cr);

        connectionService.respond("tutor@test.com", cr.getRequestId(),
                RespondConnectionRequestDTO.builder().action("DECLINE").message("Thầy đã kín lịch").build());

        verify(notificationEvents).publish(eq("connection:" + cr.getRequestId() + ":declined"), eq("CONNECTION"),
                eq("Gia sư đã từ chối yêu cầu kết nối"), argThat(c -> c.contains("Lý do: Thầy đã kín lịch")),
                eq("/pages/learner-requests/?tab=direct"), eq(List.of(learnerUser.getUserId())));
    }

    @Test
    @DisplayName("Học viên hủy yêu cầu đang chờ → gia sư nhận thông báo")
    void cancelled_notifiesTutor() {
        ConnectionRequest cr = pendingDirect();
        when(userRepository.findByEmail("learner@test.com")).thenReturn(Optional.of(learnerUser));
        when(connectionRequestRepository.findById(cr.getRequestId())).thenReturn(Optional.of(cr));

        connectionService.cancel("learner@test.com", cr.getRequestId());

        verify(notificationEvents).publish(eq("connection:" + cr.getRequestId() + ":cancelled"), eq("CONNECTION"),
                eq("Học viên đã hủy yêu cầu kết nối"), anyString(),
                eq("/tutor/connections?tab=direct"), eq(List.of(tutorUser.getUserId())));
    }

    @Test
    @DisplayName("Yêu cầu kết nối hết hạn → cả học viên và gia sư đều nhận thông báo (khóa sự kiện riêng)")
    void directExpired_notifiesBoth() {
        ConnectionRequest cr = pendingDirect();
        when(connectionRequestRepository.findByRequestTypeAndStatusAndCreatedAtBefore(any(), any(), any())).thenReturn(List.of(cr));
        when(connectionRequestRepository.findPendingDirectWithStartedSession()).thenReturn(List.of());

        connectionService.expireStaleDirectRequests();

        verify(notificationEvents).publish(eq("connection:" + cr.getRequestId() + ":expired:learner"), eq("CONNECTION"),
                anyString(), anyString(), eq("/pages/learner-requests/?tab=direct"), eq(List.of(learnerUser.getUserId())));
        verify(notificationEvents).publish(eq("connection:" + cr.getRequestId() + ":expired:tutor"), eq("CONNECTION"),
                anyString(), anyString(), eq("/tutor/connections?tab=direct"), eq(List.of(tutorUser.getUserId())));
    }

    @Test
    @DisplayName("Gia sư đồng ý → gia sư khác có yêu cầu đang chờ trùng giờ nhận thông báo yêu cầu đã tự hủy")
    void autoCancelled_notifiesOtherTutor() {
        ConnectionRequest cr = pendingDirect();
        User otherTutorUser = User.builder().userId(UUID.randomUUID()).fullName("Sato Sensei").build();
        Tutor otherTutor = Tutor.builder().tutorId(UUID.randomUUID()).user(otherTutorUser).build();
        ConnectionRequest other = ConnectionRequest.builder().requestId(UUID.randomUUID()).learner(learner).tutor(otherTutor)
                .requestType(ConnectionRequestType.DIRECT).status(ConnectionRequestStatus.PENDING).build();
        other.getSessions().add(ConnectionRequestSession.builder().connectionRequest(other)
                .sessionDate(cr.getSessions().get(0).getSessionDate()).startTime(LocalTime.of(20, 0)).endTime(LocalTime.of(21, 0)).build());
        stubTutorResponds(cr);
        when(connectionRequestRepository.findByLearnerLearnerIdAndRequestTypeAndStatus(any(), any(), any())).thenReturn(List.of(other));

        connectionService.respond("tutor@test.com", cr.getRequestId(), RespondConnectionRequestDTO.builder().action("ACCEPT").build());

        verify(notificationEvents).publish(eq("connection:" + other.getRequestId() + ":auto-cancelled"), eq("CONNECTION"),
                eq("Yêu cầu kết nối đã tự hủy"), anyString(), eq("/tutor/connections?tab=direct"), eq(List.of(otherTutorUser.getUserId())));
    }

    @Test
    @DisplayName("Yêu cầu học thử hết hạn → cả học viên và gia sư nhận thông báo nhóm TRIAL")
    void trialExpired_notifiesBoth() {
        OffsetDateTime start = OffsetDateTime.now().minusMinutes(5);
        TrialBooking tb = TrialBooking.builder().bookingId(UUID.randomUUID()).learner(learner).tutor(tutor)
                .scheduledStartTime(start).scheduledEndTime(start.plusMinutes(30)).bookingStatus(TrialBookingStatus.PENDING).build();
        when(trialBookingRepository.findStalePending(any(), any(), any())).thenReturn(List.of(tb));

        trialService.expireStalePendingTrials();

        verify(notificationEvents).publish(eq("trial:" + tb.getBookingId() + ":expired:learner"), eq("TRIAL"),
                eq("Yêu cầu học thử đã hết hạn"), anyString(), eq("/pages/learner-requests/?tab=trial"), eq(List.of(learnerUser.getUserId())));
        verify(notificationEvents).publish(eq("trial:" + tb.getBookingId() + ":expired:tutor"), eq("TRIAL"),
                eq("Yêu cầu học thử đã hết hạn"), anyString(), eq("/tutor/connections"), eq(List.of(tutorUser.getUserId())));
    }

    @Test
    @DisplayName("Gia sư đồng ý kết nối sau buổi học thử → học viên nhận thông báo nhóm CONNECTION")
    void agreedAfterTrial_notifiesLearner() {
        ConnectionRequest trialConnection = ConnectionRequest.builder().requestId(UUID.randomUUID()).learner(learner).tutor(tutor)
                .status(ConnectionRequestStatus.PENDING).build();
        TrialBooking completed = TrialBooking.builder().bookingId(UUID.randomUUID()).connectionRequest(trialConnection)
                .learner(learner).tutor(tutor).scheduledStartTime(OffsetDateTime.now().minusDays(1))
                .scheduledEndTime(OffsetDateTime.now().minusDays(1).plusMinutes(30)).bookingStatus(TrialBookingStatus.COMPLETED).build();
        when(userRepository.findByEmail("tutor@test.com")).thenReturn(Optional.of(tutorUser));
        when(tutorRepository.findByUser(tutorUser)).thenReturn(Optional.of(tutor));
        when(trialBookingRepository.findById(completed.getBookingId())).thenReturn(Optional.of(completed));

        trialService.agreeConnectionAfterTrial("tutor@test.com", completed.getBookingId());

        verify(notificationEvents).publish(eq("trial:" + completed.getBookingId() + ":agreed"), eq("CONNECTION"),
                eq("Gia sư đã đồng ý kết nối"), anyString(), eq("/pages/learner-requests/?tab=trial"), eq(List.of(learnerUser.getUserId())));
    }
}
