package com.kizuna.features.booking;

import com.kizuna.core.exception.BadRequestException;
import com.kizuna.core.exception.ConflictException;
import com.kizuna.core.entity.Learner;
import com.kizuna.core.entity.Role;
import com.kizuna.core.entity.Tutor;
import com.kizuna.core.entity.User;
import com.kizuna.core.repository.TutorRepository;
import com.kizuna.core.repository.UserRepository;
import com.kizuna.features.booking.dto.request.RespondTrialBookingRequestDTO;
import com.kizuna.features.booking.dto.response.TrialBookingResponseDTO;
import com.kizuna.features.booking.entity.*;
import com.kizuna.features.booking.repository.TrialBookingHistoryRepository;
import com.kizuna.features.booking.repository.TrialBookingRepository;
import com.kizuna.features.booking.service.impl.TrialBookingServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TrialBookingRespondTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private TutorRepository tutorRepository;

    @Mock
    private TrialBookingRepository trialBookingRepository;

    @Mock
    private TrialBookingHistoryRepository trialBookingHistoryRepository;

    @Mock
    private org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;

    @InjectMocks
    private TrialBookingServiceImpl trialBookingService;

    private User mockTutorUser;
    private Tutor mockTutor;
    private TrialBooking mockBooking;
    private ConnectionRequest mockRequest;
    private UUID bookingId;

    @BeforeEach
    void setUp() {
        UUID tutorId = UUID.randomUUID();
        bookingId = UUID.randomUUID();

        Role tutorRole = Role.builder().roleName("ROLE_TUTOR").build();
        mockTutorUser = User.builder()
                .userId(UUID.randomUUID())
                .email("tutor@test.com")
                .fullName("Yamada Sensei")
                .role(tutorRole)
                .build();

        mockTutor = Tutor.builder()
                .tutorId(tutorId)
                .tutorCode(10001L)
                .user(mockTutorUser)
                .build();

        Role learnerRole = Role.builder().roleName("ROLE_LEARNER").build();
        User mockLearnerUser = User.builder()
                .userId(UUID.randomUUID())
                .email("learner@test.com")
                .fullName("Nguyen Van Hoc Vien")
                .role(learnerRole)
                .build();

        Learner mockLearner = Learner.builder()
                .learnerId(UUID.randomUUID())
                .user(mockLearnerUser)
                .build();

        mockRequest = ConnectionRequest.builder()
                .requestId(UUID.randomUUID())
                .learner(mockLearner)
                .tutor(mockTutor)
                .status(ConnectionRequestStatus.PENDING)
                .build();

        mockBooking = TrialBooking.builder()
                .bookingId(bookingId)
                .connectionRequest(mockRequest)
                .learner(mockLearner)
                .tutor(mockTutor)
                .learningMode("ONLINE")
                .scheduledStartTime(OffsetDateTime.now().plusDays(2))
                .scheduledEndTime(OffsetDateTime.now().plusDays(2).plusMinutes(50))
                .bookingStatus(TrialBookingStatus.PENDING)
                .build();
    }

    @Test
    @DisplayName("Gia sư duyệt Chấp nhận (ACCEPT) - Chuyển sang CONFIRMED và lưu link Google Meet")
    void testRespondTrialBooking_Accept_Success() {
        RespondTrialBookingRequestDTO dto = RespondTrialBookingRequestDTO.builder()
                .action("ACCEPT")
                .meetingLink("https://meet.google.com/abc-xyz-123")
                .note("Hẹn gặp em đúng giờ nhé!")
                .build();

        when(userRepository.findByEmail("tutor@test.com")).thenReturn(Optional.of(mockTutorUser));
        when(tutorRepository.findByUser(mockTutorUser)).thenReturn(Optional.of(mockTutor));
        when(trialBookingRepository.findById(bookingId)).thenReturn(Optional.of(mockBooking));
        when(trialBookingRepository.save(any(TrialBooking.class))).thenAnswer(inv -> inv.getArgument(0));

        TrialBookingResponseDTO response = trialBookingService.respondTrialBooking("tutor@test.com", bookingId, dto);

        assertNotNull(response);
        assertEquals("CONFIRMED", response.getBookingStatus());
        assertEquals("Đã xác nhận", response.getStatusLabel());
        assertEquals("https://meet.google.com/abc-xyz-123", response.getMeetingLink());
        // Chấp nhận lịch học thử chưa phải là kết nối: chỉ ACCEPTED sau khi học thử xong và gia sư đồng ý.
        assertEquals(ConnectionRequestStatus.PENDING, mockRequest.getStatus());
        verify(trialBookingHistoryRepository, times(1)).save(any(TrialBookingHistory.class));
    }

    @Test
    @DisplayName("Đồng ý kết nối sau khi học thử hoàn thành - kết nối ACCEPTED, đánh dấu đã thỏa thuận học chính thức")
    void testAgreeConnectionAfterTrial_Success() {
        mockBooking.setBookingStatus(TrialBookingStatus.COMPLETED);
        when(userRepository.findByEmail("tutor@test.com")).thenReturn(Optional.of(mockTutorUser));
        when(tutorRepository.findByUser(mockTutorUser)).thenReturn(Optional.of(mockTutor));
        when(trialBookingRepository.findById(bookingId)).thenReturn(Optional.of(mockBooking));
        when(trialBookingRepository.save(any(TrialBooking.class))).thenAnswer(inv -> inv.getArgument(0));

        TrialBookingResponseDTO response = trialBookingService.agreeConnectionAfterTrial("tutor@test.com", bookingId);

        assertEquals(ConnectionRequestStatus.ACCEPTED, mockRequest.getStatus());
        assertNotNull(mockRequest.getRespondedAt());
        assertEquals(Boolean.TRUE, mockBooking.getOfficialArrangementAgreed());
        assertEquals(Boolean.TRUE, response.isOfficialArrangementAgreed());
    }

    @Test
    @DisplayName("Đồng ý kết nối thì các buổi học thử khác chưa diễn ra giữa hai người tự chuyển sang Đã hủy")
    void testAgreeConnectionAfterTrial_CancelsOtherOpenTrials() {
        mockBooking.setBookingStatus(TrialBookingStatus.COMPLETED);
        ConnectionRequest otherRequest = ConnectionRequest.builder()
                .requestId(UUID.randomUUID()).learner(mockBooking.getLearner()).tutor(mockTutor)
                .status(ConnectionRequestStatus.PENDING).build();
        TrialBooking otherTrial = TrialBooking.builder()
                .bookingId(UUID.randomUUID()).connectionRequest(otherRequest)
                .learner(mockBooking.getLearner()).tutor(mockTutor)
                .scheduledStartTime(OffsetDateTime.now().plusDays(1))
                .scheduledEndTime(OffsetDateTime.now().plusDays(1).plusMinutes(25))
                .bookingStatus(TrialBookingStatus.CONFIRMED).build();

        when(userRepository.findByEmail("tutor@test.com")).thenReturn(Optional.of(mockTutorUser));
        when(tutorRepository.findByUser(mockTutorUser)).thenReturn(Optional.of(mockTutor));
        when(trialBookingRepository.findById(bookingId)).thenReturn(Optional.of(mockBooking));
        when(trialBookingRepository.save(any(TrialBooking.class))).thenAnswer(inv -> inv.getArgument(0));
        when(trialBookingRepository.findByTutorTutorIdAndLearnerLearnerIdAndBookingStatusIn(any(), any(), any()))
                .thenReturn(java.util.List.of(otherTrial));

        trialBookingService.agreeConnectionAfterTrial("tutor@test.com", bookingId);

        assertEquals(TrialBookingStatus.CANCELLED, otherTrial.getBookingStatus());
        assertEquals(ConnectionRequestStatus.CANCELLED, otherRequest.getStatus());
        assertEquals(ConnectionRequestStatus.ACCEPTED, mockRequest.getStatus());
        verify(trialBookingHistoryRepository, times(1)).save(any(TrialBookingHistory.class));
    }

    @Test
    @DisplayName("Không cho chấp nhận học thử trùng lịch dạy lớp chính thức của gia sư")
    void testRespondTrialBooking_Accept_ClashWithOfficialClass() {
        RespondTrialBookingRequestDTO dto = RespondTrialBookingRequestDTO.builder()
                .action("ACCEPT").meetingLink("https://meet.google.com/abc-xyz-123").build();
        when(userRepository.findByEmail("tutor@test.com")).thenReturn(Optional.of(mockTutorUser));
        when(tutorRepository.findByUser(mockTutorUser)).thenReturn(Optional.of(mockTutor));
        when(trialBookingRepository.findById(bookingId)).thenReturn(Optional.of(mockBooking));
        when(trialBookingRepository.findClassScheduleClash(any(), any(), anyInt(), any(), any()))
                .thenReturn(Optional.of("TUTOR"));

        ConflictException ex = assertThrows(ConflictException.class,
                () -> trialBookingService.respondTrialBooking("tutor@test.com", bookingId, dto));
        assertTrue(ex.getMessage().contains("lịch dạy lớp chính thức"));
        assertEquals(TrialBookingStatus.PENDING, mockBooking.getBookingStatus());
        verify(trialBookingRepository, never()).save(any(TrialBooking.class));
    }

    @Test
    @DisplayName("Không cho chấp nhận học thử trùng giờ một buổi học thử khác gia sư đã xác nhận")
    void testRespondTrialBooking_Accept_ClashWithConfirmedTrial() {
        RespondTrialBookingRequestDTO dto = RespondTrialBookingRequestDTO.builder()
                .action("ACCEPT").meetingLink("https://meet.google.com/abc-xyz-123").build();
        when(userRepository.findByEmail("tutor@test.com")).thenReturn(Optional.of(mockTutorUser));
        when(tutorRepository.findByUser(mockTutorUser)).thenReturn(Optional.of(mockTutor));
        when(trialBookingRepository.findById(bookingId)).thenReturn(Optional.of(mockBooking));
        when(trialBookingRepository.existsConfirmedTutorOverlap(any(), any(), any(), any())).thenReturn(true);

        ConflictException ex = assertThrows(ConflictException.class,
                () -> trialBookingService.respondTrialBooking("tutor@test.com", bookingId, dto));
        assertTrue(ex.getMessage().contains("buổi học thử khác trùng khung giờ"));
        assertEquals(TrialBookingStatus.PENDING, mockBooking.getBookingStatus());
        verify(trialBookingRepository, never()).save(any(TrialBooking.class));
    }

    @Test
    @DisplayName("Không cho đồng ý kết nối khi buổi học thử chưa hoàn thành")
    void testAgreeConnectionAfterTrial_NotCompleted() {
        mockBooking.setBookingStatus(TrialBookingStatus.CONFIRMED);
        when(userRepository.findByEmail("tutor@test.com")).thenReturn(Optional.of(mockTutorUser));
        when(tutorRepository.findByUser(mockTutorUser)).thenReturn(Optional.of(mockTutor));
        when(trialBookingRepository.findById(bookingId)).thenReturn(Optional.of(mockBooking));

        assertThrows(BadRequestException.class,
                () -> trialBookingService.agreeConnectionAfterTrial("tutor@test.com", bookingId));
        assertEquals(ConnectionRequestStatus.PENDING, mockRequest.getStatus());
        verify(trialBookingRepository, never()).save(any(TrialBooking.class));
    }

    @Test
    @DisplayName("Gia sư từ chối (DECLINE) - Chuyển sang DECLINED và cập nhật lý do từ chối")
    void testRespondTrialBooking_Decline_Success() {
        RespondTrialBookingRequestDTO dto = RespondTrialBookingRequestDTO.builder()
                .action("DECLINE")
                .declineReason("Bận lịch công tác đột xuất")
                .build();

        when(userRepository.findByEmail("tutor@test.com")).thenReturn(Optional.of(mockTutorUser));
        when(tutorRepository.findByUser(mockTutorUser)).thenReturn(Optional.of(mockTutor));
        when(trialBookingRepository.findById(bookingId)).thenReturn(Optional.of(mockBooking));
        when(trialBookingRepository.save(any(TrialBooking.class))).thenAnswer(inv -> inv.getArgument(0));

        TrialBookingResponseDTO response = trialBookingService.respondTrialBooking("tutor@test.com", bookingId, dto);

        assertNotNull(response);
        assertEquals("DECLINED", response.getBookingStatus());
        assertEquals("Đã từ chối", response.getStatusLabel());
        assertEquals(ConnectionRequestStatus.DECLINED, mockRequest.getStatus());
        verify(trialBookingHistoryRepository, times(1)).save(any(TrialBookingHistory.class));
    }

    @Test
    @DisplayName("Gia sư khác can thiệp vào lịch không thuộc về mình - Bị chặn AccessDeniedException")
    void testRespondTrialBooking_WrongTutor_ThrowsAccessDenied() {
        Tutor anotherTutor = Tutor.builder()
                .tutorId(UUID.randomUUID())
                .user(mockTutorUser)
                .build();

        RespondTrialBookingRequestDTO dto = RespondTrialBookingRequestDTO.builder()
                .action("ACCEPT")
                .build();

        when(userRepository.findByEmail("tutor@test.com")).thenReturn(Optional.of(mockTutorUser));
        when(tutorRepository.findByUser(mockTutorUser)).thenReturn(Optional.of(anotherTutor));
        when(trialBookingRepository.findById(bookingId)).thenReturn(Optional.of(mockBooking));

        assertThrows(AccessDeniedException.class, () ->
                trialBookingService.respondTrialBooking("tutor@test.com", bookingId, dto)
        );
    }
}
