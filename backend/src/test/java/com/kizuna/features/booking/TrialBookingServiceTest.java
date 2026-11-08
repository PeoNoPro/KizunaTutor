package com.kizuna.features.booking;

import com.kizuna.core.entity.Learner;
import com.kizuna.core.entity.Role;
import com.kizuna.core.entity.Tutor;
import com.kizuna.core.entity.User;
import com.kizuna.core.exception.ConflictException;
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
import com.kizuna.features.booking.service.impl.TrialBookingServiceImpl;
import com.kizuna.features.tutor_search.service.TutorScheduleService;
import com.kizuna.features.verification.entity.TutorVerificationStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalTime;
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
    private TutorScheduleService tutorScheduleService;

    @Mock
    private org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;

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
    }

    @Test
    @DisplayName("Đặt lịch học thử thành công - Khung giờ rảnh, trả về PENDING")
    void testCreateTrialBooking_Success() {
        OffsetDateTime startTime = OffsetDateTime.now().plusDays(1).withHour(14).withMinute(0);
        CreateTrialBookingRequestDTO request = CreateTrialBookingRequestDTO.builder()
                .tutorId(tutorId)
                .scheduledStartTime(startTime)
                .durationMinutes(50)
                .learningMode("ONLINE")
                .currentLevel("N5")
                .learningGoals("Muốn luyện hội thoại Kaiwa")
                .initialMessage("Em xin chào Sensei ạ")
                .build();

        when(userRepository.findByEmail("learner@test.com")).thenReturn(Optional.of(mockLearnerUser));
        when(learnerRepository.findByUser(mockLearnerUser)).thenReturn(Optional.of(mockLearner));
        when(tutorRepository.findById(tutorId)).thenReturn(Optional.of(mockTutor));
        when(trialBookingRepository.existsOverlappingBooking(eq(tutorId), any(OffsetDateTime.class), any(OffsetDateTime.class)))
                .thenReturn(false);
        when(trialBookingRepository.existsOverlappingLearnerBooking(eq(mockLearner.getLearnerId()), any(OffsetDateTime.class), any(OffsetDateTime.class)))
                .thenReturn(false);
        when(tutorScheduleService.hasActiveAvailabilities(tutorId)).thenReturn(false);

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

    @Test
    @DisplayName("Đặt lịch học thử thất bại - Khung giờ bị trùng lịch ném ConflictException (409)")
    void testCreateTrialBooking_OverlappingSlot_ThrowsConflictException() {
        OffsetDateTime startTime = OffsetDateTime.now().plusDays(1).withHour(14).withMinute(0);
        CreateTrialBookingRequestDTO request = CreateTrialBookingRequestDTO.builder()
                .tutorId(tutorId)
                .scheduledStartTime(startTime)
                .durationMinutes(50)
                .learningMode("ONLINE")
                .build();

        when(userRepository.findByEmail("learner@test.com")).thenReturn(Optional.of(mockLearnerUser));
        when(learnerRepository.findByUser(mockLearnerUser)).thenReturn(Optional.of(mockLearner));
        when(tutorRepository.findById(tutorId)).thenReturn(Optional.of(mockTutor));
        when(trialBookingRepository.existsOverlappingBooking(eq(tutorId), any(OffsetDateTime.class), any(OffsetDateTime.class)))
                .thenReturn(true);

        ConflictException ex = assertThrows(ConflictException.class, () ->
                trialBookingService.createTrialBooking("learner@test.com", request)
        );

        assertTrue(ex.getMessage().contains("Khung giờ này vừa có người đăng ký"));
        verify(trialBookingRepository, never()).save(any());
        verify(connectionRequestRepository, never()).save(any());
    }

    @Test
    @DisplayName("Đặt lịch học thử thất bại - Khung giờ ngoài ca rảnh ném BadRequestException (400)")
    void testCreateTrialBooking_OutsideAvailability_ThrowsBadRequestException() {
        OffsetDateTime startTime = OffsetDateTime.now().plusDays(1).withHour(14).withMinute(0);
        CreateTrialBookingRequestDTO request = CreateTrialBookingRequestDTO.builder()
                .tutorId(tutorId)
                .scheduledStartTime(startTime)
                .durationMinutes(50)
                .learningMode("ONLINE")
                .build();

        when(userRepository.findByEmail("learner@test.com")).thenReturn(Optional.of(mockLearnerUser));
        when(learnerRepository.findByUser(mockLearnerUser)).thenReturn(Optional.of(mockLearner));
        when(tutorRepository.findById(tutorId)).thenReturn(Optional.of(mockTutor));
        when(trialBookingRepository.existsOverlappingBooking(eq(tutorId), any(OffsetDateTime.class), any(OffsetDateTime.class)))
                .thenReturn(false);
        when(trialBookingRepository.existsOverlappingLearnerBooking(eq(mockLearner.getLearnerId()), any(OffsetDateTime.class), any(OffsetDateTime.class)))
                .thenReturn(false);

        when(tutorScheduleService.hasActiveAvailabilities(tutorId)).thenReturn(true);
        when(tutorScheduleService.isSlotAvailable(eq(tutorId), anyInt(), any(LocalTime.class), any(LocalTime.class)))
                .thenReturn(false);

        com.kizuna.core.exception.BadRequestException ex = assertThrows(com.kizuna.core.exception.BadRequestException.class, () ->
                trialBookingService.createTrialBooking("learner@test.com", request)
        );

        assertTrue(ex.getMessage().contains("Gia sư không có lịch rảnh vào khung giờ này"));
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
                .durationMinutes(50)
                .build();

        when(userRepository.findByEmail("learner@test.com")).thenReturn(Optional.of(mockLearnerUser));
        when(learnerRepository.findByUser(mockLearnerUser)).thenReturn(Optional.of(mockLearner));
        when(tutorRepository.findById(tutorId)).thenReturn(Optional.of(mockTutor));

        com.kizuna.core.exception.BadRequestException ex = assertThrows(com.kizuna.core.exception.BadRequestException.class, () ->
                trialBookingService.createTrialBooking("learner@test.com", request)
        );

        assertTrue(ex.getMessage().contains("Gia sư hiện chưa sẵn sàng nhận học viên mới"));
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

    @Test
    @DisplayName("Gia sư chốt lịch buổi học cho học viên thành công")
    void testScheduleLessonByTutor_Success() {
        UUID learnerId = mockLearner.getLearnerId();
        User mockTutorUser = mockTutor.getUser();

        com.kizuna.features.booking.dto.request.TutorScheduleLessonRequestDTO request =
                com.kizuna.features.booking.dto.request.TutorScheduleLessonRequestDTO.builder()
                        .learnerId(learnerId)
                        .scheduledStartTime(OffsetDateTime.now().plusDays(2))
                        .durationMinutes(60)
                        .lessonTitle("Lớp hội thoại Kaiwa N4")
                        .meetingLink("https://meet.google.com/abc-xyz")
                        .build();

        when(userRepository.findByEmail("tutor@test.com")).thenReturn(Optional.of(mockTutorUser));
        when(tutorRepository.findByUser(mockTutorUser)).thenReturn(Optional.of(mockTutor));
        when(learnerRepository.findById(learnerId)).thenReturn(Optional.of(mockLearner));
        when(trialBookingRepository.existsOverlappingBooking(eq(tutorId), any(OffsetDateTime.class), any(OffsetDateTime.class)))
                .thenReturn(false);
        when(trialBookingRepository.save(any(TrialBooking.class))).thenAnswer(invocation -> {
            TrialBooking tb = invocation.getArgument(0);
            tb.setBookingId(UUID.randomUUID());
            return tb;
        });

        TrialBookingResponseDTO response = trialBookingService.scheduleLessonByTutor("tutor@test.com", request);

        assertNotNull(response);
        assertEquals(TrialBookingStatus.CONFIRMED.name(), response.getBookingStatus());
        assertEquals("Đã xác nhận", response.getStatusLabel());
        assertEquals("https://meet.google.com/abc-xyz", response.getMeetingLink());
        verify(trialBookingRepository, times(1)).save(any(TrialBooking.class));
        verify(trialBookingHistoryRepository, times(1)).save(any(TrialBookingHistory.class));
    }

    @Test
    @DisplayName("Gia sư chốt lịch thành công khi truyền userId của học viên thay vì learnerId")
    void testScheduleLessonByTutor_WithUserId_Success() {
        UUID userId = mockLearnerUser.getUserId();
        User mockTutorUser = mockTutor.getUser();

        com.kizuna.features.booking.dto.request.TutorScheduleLessonRequestDTO request =
                com.kizuna.features.booking.dto.request.TutorScheduleLessonRequestDTO.builder()
                        .learnerId(userId)
                        .scheduledStartTime(OffsetDateTime.now().plusDays(2))
                        .durationMinutes(60)
                        .lessonTitle("Lớp hội thoại Kaiwa N4")
                        .build();

        when(userRepository.findByEmail("tutor@test.com")).thenReturn(Optional.of(mockTutorUser));
        when(tutorRepository.findByUser(mockTutorUser)).thenReturn(Optional.of(mockTutor));
        // learnerRepository.findById(userId) trả về rỗng vì đây là user_id
        when(learnerRepository.findById(userId)).thenReturn(Optional.empty());
        // findByUser_UserId(userId) tìm thấy học viên
        when(learnerRepository.findByUser_UserId(userId)).thenReturn(Optional.of(mockLearner));
        when(trialBookingRepository.existsOverlappingBooking(eq(tutorId), any(OffsetDateTime.class), any(OffsetDateTime.class)))
                .thenReturn(false);
        when(trialBookingRepository.save(any(TrialBooking.class))).thenAnswer(invocation -> {
            TrialBooking tb = invocation.getArgument(0);
            tb.setBookingId(UUID.randomUUID());
            return tb;
        });

        TrialBookingResponseDTO response = trialBookingService.scheduleLessonByTutor("tutor@test.com", request);

        assertNotNull(response);
        assertEquals(TrialBookingStatus.CONFIRMED.name(), response.getBookingStatus());
        assertEquals(mockLearner.getLearnerId(), response.getLearnerId());
        verify(learnerRepository, times(1)).findByUser_UserId(userId);
    }
}
