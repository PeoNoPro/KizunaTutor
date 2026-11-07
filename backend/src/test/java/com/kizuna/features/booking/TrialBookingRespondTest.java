package com.kizuna.features.booking;

import com.kizuna.core.entity.Learner;
import com.kizuna.core.entity.Tutor;
import com.kizuna.core.entity.User;
import com.kizuna.core.exception.BadRequestException;
import com.kizuna.core.repository.TutorRepository;
import com.kizuna.core.repository.UserRepository;
import com.kizuna.features.booking.dto.request.RespondTrialBookingRequestDTO;
import com.kizuna.features.booking.dto.response.TrialBookingResponseDTO;
import com.kizuna.features.booking.entity.ConnectionRequest;
import com.kizuna.features.booking.entity.ConnectionRequestStatus;
import com.kizuna.features.booking.entity.TrialBooking;
import com.kizuna.features.booking.entity.TrialBookingHistory;
import com.kizuna.features.booking.entity.TrialBookingStatus;
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

        mockTutorUser = User.builder()
                .userId(UUID.randomUUID())
                .email("tutor@test.com")
                .fullName("Yamada Sensei")
                .build();

        mockTutor = Tutor.builder()
                .tutorId(tutorId)
                .tutorCode(10001L)
                .user(mockTutorUser)
                .build();

        User mockLearnerUser = User.builder()
                .userId(UUID.randomUUID())
                .email("learner@test.com")
                .fullName("Nguyen Van Hoc Vien")
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
        assertEquals(ConnectionRequestStatus.ACCEPTED, mockRequest.getStatus());
        verify(trialBookingHistoryRepository, times(1)).save(any(TrialBookingHistory.class));
    }

    @Test
    @DisplayName("Gia sư từ chối (DECLINE) - Chuyển sang CANCELLED và cập nhật lý do từ chối")
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
        assertEquals("CANCELLED", response.getBookingStatus());
        assertEquals("Đã hủy", response.getStatusLabel());
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
