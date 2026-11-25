package com.kizuna.features.tutor_search.unit;

import com.kizuna.core.entity.Tutor;
import com.kizuna.core.entity.User;
import com.kizuna.core.repository.TutorRepository;
import com.kizuna.core.repository.UserRepository;
import com.kizuna.features.booking.service.ConnectionRequestService;
import com.kizuna.features.booking.service.TrialBookingService;
import com.kizuna.features.tutor_search.entity.TutorAvailability;
import com.kizuna.features.tutor_search.repository.TutorAvailabilityRepository;
import com.kizuna.features.tutor_search.service.impl.TutorScheduleServiceImpl;

import java.time.LocalTime;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Dữ liệu và mock dùng chung cho unit test FE-06 (lịch rảnh của gia sư) - TutorScheduleServiceImpl.
 * Repository và hai service kiểm tra học viên trong ca (học thử, kết nối) là mock nên không chạm DB thật.
 */
abstract class AvailabilityUnitTestBase {
    static final String EMAIL = "tutor@test.com";
    static final UUID TUTOR_ID = UUID.fromString("00000000-0000-0000-0000-0000000000a1");
    static final UUID SLOT_ID = UUID.fromString("00000000-0000-0000-0000-0000000000b1");

    final UserRepository users = mock(UserRepository.class);
    final TutorRepository tutors = mock(TutorRepository.class);
    final TutorAvailabilityRepository slots = mock(TutorAvailabilityRepository.class);
    final TrialBookingService trials = mock(TrialBookingService.class);
    final ConnectionRequestService connections = mock(ConnectionRequestService.class);
    final TutorScheduleServiceImpl service = new TutorScheduleServiceImpl(users, tutors, slots, trials, connections);

    final User user = User.builder().userId(UUID.randomUUID()).email(EMAIL).build();
    final Tutor tutor = Tutor.builder().tutorId(TUTOR_ID).user(user).build();

    {
        // repository.save trả lại đúng đối tượng được lưu, như JPA
        when(slots.save(any(TutorAvailability.class))).thenAnswer(inv -> {
            TutorAvailability a = inv.getArgument(0);
            if (a.getAvailabilityId() == null) a.setAvailabilityId(UUID.randomUUID());
            return a;
        });
    }

    /** Đăng nhập bằng tài khoản gia sư đã liên kết hồ sơ. */
    void loggedInTutor() {
        when(users.findByEmail(EMAIL)).thenReturn(Optional.of(user));
        when(tutors.findByUser(user)).thenReturn(Optional.of(tutor));
    }

    /** Ca rảnh SLOT_ID của gia sư đang đăng nhập. */
    TutorAvailability ownSlot(int day, String start, String end, boolean active) {
        TutorAvailability slot = TutorAvailability.builder().availabilityId(SLOT_ID).tutor(tutor).dayOfWeek(day)
                .startTime(LocalTime.parse(start)).endTime(LocalTime.parse(end)).isActive(active).build();
        when(slots.findByAvailabilityIdAndTutor_TutorId(SLOT_ID, TUTOR_ID)).thenReturn(Optional.of(slot));
        return slot;
    }

    /** Có học thử đang chờ / đã xác nhận trong ca. */
    void trialInSlot(boolean value) {
        when(trials.hasActiveBookingsInSlot(eq(TUTOR_ID), any(), any(), any())).thenReturn(value);
    }

    /** Có buổi học kết nối sắp tới (đã đồng ý hoặc đang chờ còn hạn) trong ca. */
    void connectionInSlot(boolean value) {
        when(connections.hasUpcomingSessionsInSlot(eq(TUTOR_ID), any(), any(), any())).thenReturn(value);
    }
}
