package com.kizuna.features.tutor_search;

import com.kizuna.core.entity.Tutor;
import com.kizuna.core.entity.User;
import com.kizuna.core.exception.BadRequestException;
import com.kizuna.core.exception.ConflictException;
import com.kizuna.core.repository.TutorRepository;
import com.kizuna.core.repository.UserRepository;
import com.kizuna.features.tutor_search.dto.CreateAvailabilityRequestDTO;
import com.kizuna.features.tutor_search.dto.TutorAvailabilityDTO;
import com.kizuna.features.tutor_search.entity.TutorAvailability;
import com.kizuna.features.tutor_search.repository.TutorAvailabilityRepository;
import com.kizuna.features.tutor_search.service.impl.TutorScheduleServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TutorScheduleServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private TutorRepository tutorRepository;

    @Mock
    private TutorAvailabilityRepository availabilityRepository;

    @InjectMocks
    private TutorScheduleServiceImpl scheduleService;

    private User mockUser;
    private Tutor mockTutor;
    private final String tutorEmail = "tutor@test.com";
    private final UUID tutorId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        mockUser = User.builder().userId(UUID.randomUUID()).email(tutorEmail).build();
        mockTutor = Tutor.builder().tutorId(tutorId).user(mockUser).build();
    }

    @Test
    @DisplayName("Thêm ca rảnh hợp lệ thành công")
    void addAvailability_success() {
        when(userRepository.findByEmail(tutorEmail)).thenReturn(Optional.of(mockUser));
        when(tutorRepository.findByUser(mockUser)).thenReturn(Optional.of(mockTutor));
        when(availabilityRepository.hasOverlappingSlot(eq(tutorId), eq(2), any(LocalTime.class), any(LocalTime.class)))
                .thenReturn(false);

        TutorAvailability savedEntity = TutorAvailability.builder()
                .availabilityId(UUID.randomUUID())
                .tutor(mockTutor)
                .dayOfWeek(2)
                .startTime(LocalTime.of(18, 30))
                .endTime(LocalTime.of(20, 30))
                .isActive(true)
                .build();
        when(availabilityRepository.save(any(TutorAvailability.class))).thenReturn(savedEntity);

        CreateAvailabilityRequestDTO req = CreateAvailabilityRequestDTO.builder()
                .dayOfWeek(2)
                .startTime("18:30")
                .endTime("20:30")
                .isActive(true)
                .build();

        TutorAvailabilityDTO result = scheduleService.addAvailability(tutorEmail, req);

        assertNotNull(result);
        assertEquals(2, result.getDayOfWeek());
        assertEquals("Thứ hai", result.getDayName());
        assertEquals("18:30", result.getStartTime());
        assertEquals("20:30", result.getEndTime());
        assertTrue(result.getIsActive());
    }

    @Test
    @DisplayName("Giờ bắt đầu muộn hơn giờ kết thúc phải ném lỗi BadRequestException")
    void addAvailability_invalidTime_throwsBadRequest() {
        when(userRepository.findByEmail(tutorEmail)).thenReturn(Optional.of(mockUser));
        when(tutorRepository.findByUser(mockUser)).thenReturn(Optional.of(mockTutor));

        CreateAvailabilityRequestDTO req = CreateAvailabilityRequestDTO.builder()
                .dayOfWeek(2)
                .startTime("20:30")
                .endTime("18:30")
                .build();

        assertThrows(BadRequestException.class, () -> scheduleService.addAvailability(tutorEmail, req));
    }

    @Test
    @DisplayName("Thời lượng ca rảnh dưới 30 phút phải ném lỗi BadRequestException")
    void addAvailability_tooShort_throwsBadRequest() {
        when(userRepository.findByEmail(tutorEmail)).thenReturn(Optional.of(mockUser));
        when(tutorRepository.findByUser(mockUser)).thenReturn(Optional.of(mockTutor));

        CreateAvailabilityRequestDTO req = CreateAvailabilityRequestDTO.builder()
                .dayOfWeek(2)
                .startTime("18:00")
                .endTime("18:20")
                .build();

        assertThrows(BadRequestException.class, () -> scheduleService.addAvailability(tutorEmail, req));
    }

    @Test
    @DisplayName("Ca rảnh bị trùng lặp với ca đã có phải ném lỗi ConflictException")
    void addAvailability_overlapping_throwsConflict() {
        when(userRepository.findByEmail(tutorEmail)).thenReturn(Optional.of(mockUser));
        when(tutorRepository.findByUser(mockUser)).thenReturn(Optional.of(mockTutor));
        when(availabilityRepository.hasOverlappingSlot(eq(tutorId), eq(4), any(LocalTime.class), any(LocalTime.class)))
                .thenReturn(true);

        CreateAvailabilityRequestDTO req = CreateAvailabilityRequestDTO.builder()
                .dayOfWeek(4)
                .startTime("19:00")
                .endTime("21:00")
                .build();

        assertThrows(ConflictException.class, () -> scheduleService.addAvailability(tutorEmail, req));
    }

    @Test
    @DisplayName("Xóa ca rảnh thành công")
    void removeAvailability_success() {
        UUID slotId = UUID.randomUUID();
        when(userRepository.findByEmail(tutorEmail)).thenReturn(Optional.of(mockUser));
        when(tutorRepository.findByUser(mockUser)).thenReturn(Optional.of(mockTutor));

        TutorAvailability slot = TutorAvailability.builder()
                .availabilityId(slotId)
                .tutor(mockTutor)
                .build();
        when(availabilityRepository.findByAvailabilityIdAndTutor_TutorId(slotId, tutorId)).thenReturn(Optional.of(slot));

        scheduleService.removeAvailability(tutorEmail, slotId);

        verify(availabilityRepository, times(1)).delete(slot);
    }
}

