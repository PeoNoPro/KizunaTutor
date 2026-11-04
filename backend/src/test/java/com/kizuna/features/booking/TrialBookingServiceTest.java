package com.kizuna.features.booking;

import com.kizuna.core.entity.Learner;
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
import static org.mockito.ArgumentMatchers.any;
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

    @InjectMocks
    private TrialBookingServiceImpl trialBookingService;

    private User mockLearnerUser;
    private Learner mockLearner;
    private Tutor mockTutor;
    private UUID tutorId;

    @BeforeEach
    void setUp() {
        tutorId = UUID.randomUUID();

        mockLearnerUser = User.builder()
                .userId(UUID.randomUUID())
                .email("learner@test.com")
                .fullName("Nguyen Van Hoc Vien")
                .build();

        mockLearner = Learner.builder()
                .learnerId(UUID.randomUUID())
                .user(mockLearnerUser)
                .build();

        User mockTutorUser = User.builder()
                .userId(UUID.randomUUID())
                .email("tutor@test.com")
                .fullName("Yamada Sensei")
                .build();

        mockTutor = Tutor.builder()
                .tutorId(tutorId)
                .tutorCode(10001L)
                .user(mockTutorUser)
                .build();
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
}
