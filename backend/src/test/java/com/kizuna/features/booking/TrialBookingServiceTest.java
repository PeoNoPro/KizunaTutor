package com.kizuna.features.booking;

import com.kizuna.core.entity.Learner;
import com.kizuna.core.entity.Role;
import com.kizuna.core.entity.Tutor;
import com.kizuna.core.entity.User;
import com.kizuna.core.repository.LearnerRepository;
import com.kizuna.core.repository.TutorRepository;
import com.kizuna.core.repository.UserRepository;
import com.kizuna.features.booking.dto.request.CreateTrialBookingRequestDTO;
import com.kizuna.features.booking.dto.response.TrialBookingResponseDTO;
import com.kizuna.features.booking.entity.ConnectionRequest;
import com.kizuna.features.booking.entity.TrialBooking;
import com.kizuna.features.booking.entity.TrialBookingHistory;
import com.kizuna.features.booking.entity.TrialBookingStatus;
import com.kizuna.features.booking.repository.ConnectionRequestRepository;
import com.kizuna.features.booking.repository.TrialBookingHistoryRepository;
import com.kizuna.features.booking.repository.TrialBookingRepository;
import com.kizuna.features.booking.service.ConnectionSchedulePlanner;
import com.kizuna.features.booking.service.impl.TrialBookingServiceImpl;
import java.util.List;
import com.kizuna.features.verification.entity.TutorVerificationStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TrialBookingServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private LearnerRepository learnerRepository;

    @Mock
    private TutorRepository tutorRepository;

    @Mock
    private ConnectionRequestRepository connectionRequestRepository;

    @Mock
    private TrialBookingRepository trialBookingRepository;

    @Mock
    private TrialBookingHistoryRepository trialBookingHistoryRepository;

    @Mock
    private org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;

    @Mock
    private com.kizuna.features.booking.service.ConnectionBusyLoader busyLoader;

    @InjectMocks
    private TrialBookingServiceImpl trialBookingService;

    private User mockLearnerUser;
    private Learner mockLearner;
    private Tutor mockTutor;
    private UUID tutorId;

    @BeforeEach
    void setUp() {
        tutorId = UUID.randomUUID();

        Role learnerRole = Role.builder().roleName("ROLE_LEARNER").build();
        mockLearnerUser = User.builder()
                .userId(UUID.randomUUID())
                .email("learner@test.com")
                .fullName("Nguyen Van Hoc Vien")
                .role(learnerRole)
                .build();

        mockLearner = Learner.builder()
                .learnerId(UUID.randomUUID())
                .user(mockLearnerUser)
                .build();

        Role tutorRole = Role.builder().roleName("ROLE_TUTOR").build();
        User mockTutorUser = User.builder()
                .userId(UUID.randomUUID())
                .email("tutor@test.com")
                .fullName("Yamada Sensei")
                .role(tutorRole)
                .build();

        mockTutor = Tutor.builder()
                .tutorId(tutorId)
                .tutorCode(10001L)
                .user(mockTutorUser)
                .verificationStatus(TutorVerificationStatus.APPROVED)
                .build();

        lenient().when(learnerRepository.save(any(Learner.class))).thenAnswer(inv -> inv.getArgument(0));
        lenient().when(trialBookingRepository.isWithinAvailability(eq(tutorId), anyInt(), any(), any())).thenReturn(true);
        lenient().when(busyLoader.load(any(), any(), any(), any(), any(), any()))
                .thenReturn(ConnectionSchedulePlanner.BusyIndex.EMPTY);
    }

    @Test
    @DisplayName("Đặt lịch học thử thành công - Học viên tự chọn giờ, trả về PENDING")
    void testCreateTrialBooking_Success() {
        OffsetDateTime startTime = OffsetDateTime.now().plusDays(1).withHour(14).withMinute(0);
        CreateTrialBookingRequestDTO request = CreateTrialBookingRequestDTO.builder()
                .tutorId(tutorId)
                .scheduledStartTime(startTime)
                .durationMinutes(30)
                .learningMode("ONLINE")
                .currentLevel("N5")
                .learningGoals("Muốn luyện hội thoại Kaiwa")
                .initialMessage("Em xin chào Sensei ạ")
                .build();

        when(userRepository.findByEmail("learner@test.com")).thenReturn(Optional.of(mockLearnerUser));
        when(learnerRepository.findByUser(mockLearnerUser)).thenReturn(Optional.of(mockLearner));
        when(tutorRepository.findById(tutorId)).thenReturn(Optional.of(mockTutor));

        when(connectionRequestRepository.save(any(ConnectionRequest.class)))
                .thenAnswer(inv -> {
                    ConnectionRequest cr = inv.getArgument(0);
                    cr.setRequestId(UUID.randomUUID());
                    return cr;
                });

        when(trialBookingRepository.save(any(TrialBooking.class)))
                .thenAnswer(inv -> {
                    TrialBooking tb = inv.getArgument(0);
                    tb.setBookingId(UUID.randomUUID());
                    return tb;
                });

        TrialBookingResponseDTO response = trialBookingService.createTrialBooking("learner@test.com", request);

        assertNotNull(response);
        assertEquals("PENDING", response.getBookingStatus());
        assertEquals("Chờ gia sư xác nhận", response.getStatusLabel());
        assertEquals("Yamada Sensei", response.getPartnerName());
        verify(trialBookingHistoryRepository, times(1)).save(any(TrialBookingHistory.class));
    }

    private CreateTrialBookingRequestDTO offScheduleRequest(Boolean proposal) {
        return CreateTrialBookingRequestDTO.builder()
                .tutorId(tutorId)
                .scheduledStartTime(OffsetDateTime.now().plusDays(2).withHour(21).withMinute(30))
                .durationMinutes(30)
                .offSchedule(proposal)
                .build();
    }

    private void stubLearnerTutorAndSave() {
        when(userRepository.findByEmail("learner@test.com")).thenReturn(Optional.of(mockLearnerUser));
        when(learnerRepository.findByUser(mockLearnerUser)).thenReturn(Optional.of(mockLearner));
        when(tutorRepository.findById(tutorId)).thenReturn(Optional.of(mockTutor));
        lenient().when(connectionRequestRepository.save(any(ConnectionRequest.class))).thenAnswer(inv -> inv.getArgument(0));
        lenient().when(trialBookingRepository.save(any(TrialBooking.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    @DisplayName("Giờ ngoài lịch rảnh mà không dùng 'Đề xuất giờ khác' thì bị từ chối")
    void testCreateTrialBooking_OutsideAvailabilityWithoutProposal_Rejected() {
        stubLearnerTutorAndSave();
        when(trialBookingRepository.isWithinAvailability(eq(tutorId), anyInt(), any(), any())).thenReturn(false);

        com.kizuna.core.exception.BadRequestException ex = assertThrows(com.kizuna.core.exception.BadRequestException.class,
                () -> trialBookingService.createTrialBooking("learner@test.com", offScheduleRequest(null)));
        assertTrue(ex.getMessage().contains("ngoài lịch rảnh"));
        verify(trialBookingRepository, never()).save(any());
    }

    @Test
    @DisplayName("Đề xuất giờ khác (ngoài lịch rảnh) được gửi và gắn nhãn offSchedule")
    void testCreateTrialBooking_Proposal_SavedAsOffSchedule() {
        stubLearnerTutorAndSave();
        when(trialBookingRepository.isWithinAvailability(eq(tutorId), anyInt(), any(), any())).thenReturn(false);

        TrialBookingResponseDTO response = trialBookingService.createTrialBooking("learner@test.com", offScheduleRequest(true));

        assertEquals("PENDING", response.getBookingStatus());
        assertTrue(response.isOffSchedule());
    }

    @Test
    @DisplayName("Đề xuất giờ khác quá 2 lần/tuần với cùng gia sư thì bị chặn")
    void testCreateTrialBooking_ProposalLimit() {
        stubLearnerTutorAndSave();
        when(trialBookingRepository.isWithinAvailability(eq(tutorId), anyInt(), any(), any())).thenReturn(false);
        when(trialBookingRepository.countOffScheduleSince(eq(mockLearner.getLearnerId()), eq(tutorId), any())).thenReturn(2L);

        com.kizuna.core.exception.ConflictException ex = assertThrows(com.kizuna.core.exception.ConflictException.class,
                () -> trialBookingService.createTrialBooking("learner@test.com", offScheduleRequest(true)));
        assertTrue(ex.getMessage().contains("tối đa 2 lần"));
    }

    @Test
    @DisplayName("Giờ gia sư đang bận thật thì không đặt được, kể cả khi là đề xuất")
    void testCreateTrialBooking_TutorBusy_Rejected() {
        stubLearnerTutorAndSave();
        when(trialBookingRepository.isWithinAvailability(eq(tutorId), anyInt(), any(), any())).thenReturn(false);
        when(busyLoader.load(any(), any(), any(), any(), any(), any())).thenReturn(new ConnectionSchedulePlanner.BusyIndex(
                List.of(new ConnectionSchedulePlanner.WeeklyBusy(1, java.time.LocalTime.MIN, java.time.LocalTime.MAX,
                                ConnectionSchedulePlanner.BusyKind.TRIAL),
                        new ConnectionSchedulePlanner.WeeklyBusy(2, java.time.LocalTime.MIN, java.time.LocalTime.MAX,
                                ConnectionSchedulePlanner.BusyKind.TRIAL),
                        new ConnectionSchedulePlanner.WeeklyBusy(3, java.time.LocalTime.MIN, java.time.LocalTime.MAX,
                                ConnectionSchedulePlanner.BusyKind.TRIAL),
                        new ConnectionSchedulePlanner.WeeklyBusy(4, java.time.LocalTime.MIN, java.time.LocalTime.MAX,
                                ConnectionSchedulePlanner.BusyKind.TRIAL),
                        new ConnectionSchedulePlanner.WeeklyBusy(5, java.time.LocalTime.MIN, java.time.LocalTime.MAX,
                                ConnectionSchedulePlanner.BusyKind.TRIAL),
                        new ConnectionSchedulePlanner.WeeklyBusy(6, java.time.LocalTime.MIN, java.time.LocalTime.MAX,
                                ConnectionSchedulePlanner.BusyKind.TRIAL),
                        new ConnectionSchedulePlanner.WeeklyBusy(7, java.time.LocalTime.MIN, java.time.LocalTime.MAX,
                                ConnectionSchedulePlanner.BusyKind.TRIAL)),
                List.of()));

        com.kizuna.core.exception.ConflictException ex = assertThrows(com.kizuna.core.exception.ConflictException.class,
                () -> trialBookingService.createTrialBooking("learner@test.com", offScheduleRequest(true)));
        assertTrue(ex.getMessage().contains("đã kín"));
        verify(trialBookingRepository, never()).save(any());
    }

    @Test
    @DisplayName("Đặt lịch học thử thất bại - Thời gian trong quá khứ ném BadRequestException")
    void testCreateTrialBooking_PastTime_ThrowsBadRequest() {
        CreateTrialBookingRequestDTO request = CreateTrialBookingRequestDTO.builder()
                .tutorId(tutorId)
                .scheduledStartTime(OffsetDateTime.now().minusHours(1))
                .durationMinutes(30)
                .build();

        when(userRepository.findByEmail("learner@test.com")).thenReturn(Optional.of(mockLearnerUser));
        when(learnerRepository.findByUser(mockLearnerUser)).thenReturn(Optional.of(mockLearner));
        when(tutorRepository.findById(tutorId)).thenReturn(Optional.of(mockTutor));

        com.kizuna.core.exception.BadRequestException ex = assertThrows(com.kizuna.core.exception.BadRequestException.class, () ->
                trialBookingService.createTrialBooking("learner@test.com", request)
        );

        assertTrue(ex.getMessage().contains("phải ở tương lai"));
        verify(trialBookingRepository, never()).save(any());
    }

    @Test
    @DisplayName("Đặt lịch học thử thất bại - Gia sư chưa được phê duyệt ném BadRequestException")
    void testCreateTrialBooking_UnapprovedTutor_ThrowsBadRequest() {
        mockTutor.setVerificationStatus(TutorVerificationStatus.PENDING);
        OffsetDateTime startTime = OffsetDateTime.now().plusDays(1).withHour(14).withMinute(0);
        CreateTrialBookingRequestDTO request = CreateTrialBookingRequestDTO.builder()
                .tutorId(tutorId)
                .scheduledStartTime(startTime)
                .durationMinutes(30)
                .build();

        when(userRepository.findByEmail("learner@test.com")).thenReturn(Optional.of(mockLearnerUser));
        when(learnerRepository.findByUser(mockLearnerUser)).thenReturn(Optional.of(mockLearner));
        when(tutorRepository.findById(tutorId)).thenReturn(Optional.of(mockTutor));

        com.kizuna.core.exception.BadRequestException ex = assertThrows(com.kizuna.core.exception.BadRequestException.class, () ->
                trialBookingService.createTrialBooking("learner@test.com", request)
        );

        assertTrue(ex.getMessage().contains("chưa sẵn sàng nhận học viên mới"));
        verify(trialBookingRepository, never()).save(any());
    }

    @Test
    @DisplayName("Đặt lịch học thử thất bại - Thời lượng khác 30 phút ném BadRequestException")
    void testCreateTrialBooking_InvalidDuration_ThrowsBadRequest() {
        OffsetDateTime startTime = OffsetDateTime.now().plusDays(1).withHour(14).withMinute(0);
        CreateTrialBookingRequestDTO request = CreateTrialBookingRequestDTO.builder()
                .tutorId(tutorId)
                .scheduledStartTime(startTime)
                .durationMinutes(50)
                .build();

        when(userRepository.findByEmail("learner@test.com")).thenReturn(Optional.of(mockLearnerUser));
        when(learnerRepository.findByUser(mockLearnerUser)).thenReturn(Optional.of(mockLearner));
        when(tutorRepository.findById(tutorId)).thenReturn(Optional.of(mockTutor));

        com.kizuna.core.exception.BadRequestException ex = assertThrows(com.kizuna.core.exception.BadRequestException.class, () ->
                trialBookingService.createTrialBooking("learner@test.com", request)
        );

        assertTrue(ex.getMessage().contains("Thời lượng buổi học thử là 30 phút"));
        verify(trialBookingRepository, never()).save(any());
    }

    @Test
    @DisplayName("Hủy lịch học thử thất bại - Hủy lịch đã CONFIRMED dưới 24h ném BadRequestException")
    void testCancelTrialBooking_ConfirmedUnder24h_ThrowsBadRequest() {
        UUID bookingId = UUID.randomUUID();
        TrialBooking confirmedBooking = TrialBooking.builder()
                .bookingId(bookingId)
                .learner(mockLearner)
                .tutor(mockTutor)
                .scheduledStartTime(OffsetDateTime.now().plusHours(12)) // Chỉ còn 12 tiếng nữa là học
                .bookingStatus(TrialBookingStatus.CONFIRMED)
                .build();

        when(userRepository.findByEmail("learner@test.com")).thenReturn(Optional.of(mockLearnerUser));
        when(trialBookingRepository.findById(bookingId)).thenReturn(Optional.of(confirmedBooking));

        com.kizuna.features.booking.dto.request.CancelTrialBookingRequestDTO cancelDto =
                com.kizuna.features.booking.dto.request.CancelTrialBookingRequestDTO.builder()
                        .cancelReason("Bận việc đột xuất")
                        .build();

        com.kizuna.core.exception.BadRequestException ex = assertThrows(com.kizuna.core.exception.BadRequestException.class, () ->
                trialBookingService.cancelTrialBooking("learner@test.com", bookingId, cancelDto)
        );

        assertTrue(ex.getMessage().contains("ít nhất 24 giờ"));
        verify(trialBookingRepository, never()).save(any());
    }

    @Test
    @DisplayName("Hoàn thành buổi học thử thành công - Chuyển sang COMPLETED và ghi vết lịch sử")
    void testCompleteTrialBooking_Success() {
        UUID bookingId = UUID.randomUUID();
        TrialBooking confirmedBooking = TrialBooking.builder()
                .bookingId(bookingId)
                .learner(mockLearner)
                .tutor(mockTutor)
                .scheduledStartTime(OffsetDateTime.now().minusHours(2))
                .scheduledEndTime(OffsetDateTime.now().minusHours(1))
                .bookingStatus(TrialBookingStatus.CONFIRMED)
                .build();

        when(userRepository.findByEmail("learner@test.com")).thenReturn(Optional.of(mockLearnerUser));
        when(trialBookingRepository.findById(bookingId)).thenReturn(Optional.of(confirmedBooking));
        when(trialBookingRepository.save(any(TrialBooking.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TrialBookingResponseDTO response = trialBookingService.completeTrialBooking("learner@test.com", bookingId);

        assertNotNull(response);
        assertEquals(TrialBookingStatus.COMPLETED.name(), response.getBookingStatus());
        assertEquals("Đã hoàn thành", response.getStatusLabel());
        verify(trialBookingRepository, times(1)).save(any(TrialBooking.class));
        verify(trialBookingHistoryRepository, times(1)).save(any(TrialBookingHistory.class));
    }

    @Test
    @DisplayName("Hoàn thành buổi học thử thất bại - Trạng thái PENDING ném BadRequestException")
    void testCompleteTrialBooking_PendingStatus_ThrowsBadRequest() {
        UUID bookingId = UUID.randomUUID();
        TrialBooking pendingBooking = TrialBooking.builder()
                .bookingId(bookingId)
                .learner(mockLearner)
                .tutor(mockTutor)
                .bookingStatus(TrialBookingStatus.PENDING)
                .build();

        when(userRepository.findByEmail("learner@test.com")).thenReturn(Optional.of(mockLearnerUser));
        when(trialBookingRepository.findById(bookingId)).thenReturn(Optional.of(pendingBooking));

        com.kizuna.core.exception.BadRequestException ex = assertThrows(
                com.kizuna.core.exception.BadRequestException.class,
                () -> trialBookingService.completeTrialBooking("learner@test.com", bookingId)
        );

        assertTrue(ex.getMessage().contains("Chỉ có thể hoàn thành buổi học thử đã được xác nhận"));
        verify(trialBookingRepository, never()).save(any());
    }

    @Test
    @DisplayName("Hoàn thành buổi học thử thất bại - Chưa đến giờ kết thúc ném BadRequestException")
    void testCompleteTrialBooking_BeforeEndTime_ThrowsBadRequest() {
        UUID bookingId = UUID.randomUUID();
        TrialBooking futureBooking = TrialBooking.builder()
                .bookingId(bookingId)
                .learner(mockLearner)
                .tutor(mockTutor)
                .scheduledStartTime(OffsetDateTime.now().plusHours(1))
                .scheduledEndTime(OffsetDateTime.now().plusHours(2))
                .bookingStatus(TrialBookingStatus.CONFIRMED)
                .build();

        when(userRepository.findByEmail("learner@test.com")).thenReturn(Optional.of(mockLearnerUser));
        when(trialBookingRepository.findById(bookingId)).thenReturn(Optional.of(futureBooking));

        com.kizuna.core.exception.BadRequestException ex = assertThrows(
                com.kizuna.core.exception.BadRequestException.class,
                () -> trialBookingService.completeTrialBooking("learner@test.com", bookingId)
        );

        assertTrue(ex.getMessage().contains("Buổi học chưa kết thúc"));
        verify(trialBookingRepository, never()).save(any());
    }
}
