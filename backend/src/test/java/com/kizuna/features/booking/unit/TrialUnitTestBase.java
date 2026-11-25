package com.kizuna.features.booking.unit;

import com.kizuna.core.entity.*;
import com.kizuna.features.verification.entity.TutorVerificationStatus;
import com.kizuna.core.repository.LearnerRepository;
import com.kizuna.core.repository.TutorRepository;
import com.kizuna.core.repository.UserRepository;
import com.kizuna.features.booking.entity.ConnectionRequest;
import com.kizuna.features.booking.entity.ConnectionRequestStatus;
import com.kizuna.features.booking.entity.TrialBooking;
import com.kizuna.features.booking.entity.TrialBookingStatus;
import com.kizuna.features.booking.repository.ConnectionRequestRepository;
import com.kizuna.features.booking.repository.TrialBookingHistoryRepository;
import com.kizuna.features.booking.repository.TrialBookingRepository;
import com.kizuna.features.booking.service.ConnectionBusyLoader;
import com.kizuna.features.booking.service.ConnectionSchedulePlanner.BusyIndex;
import com.kizuna.features.booking.service.ConnectionSchedulePlanner.BusyKind;
import com.kizuna.features.booking.service.ConnectionSchedulePlanner.DatedBusy;
import com.kizuna.features.booking.service.impl.TrialBookingServiceImpl;
import com.kizuna.features.notification.service.NotificationEvents;
import com.kizuna.features.payment.service.TutorMembershipService;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;

/**
 * Dữ liệu và mock dùng chung cho unit test FE-07 (đặt lịch học thử) và UC-08.01 - TrialBookingServiceImpl.
 * Repository, JdbcTemplate (khóa theo gia sư), lịch bận, gói hội viên và thông báo là mock nên không chạm DB thật.
 * Mặc định: gia sư đã duyệt, còn gói hội viên, giờ nằm trong lịch rảnh, không ai bận.
 */
abstract class TrialUnitTestBase {
    static final ZoneId VN = ZoneId.of("Asia/Ho_Chi_Minh");
    static final String LEARNER_EMAIL = "learner@test.com";
    static final String TUTOR_EMAIL = "tutor@test.com";
    static final UUID BOOKING_ID = UUID.fromString("00000000-0000-0000-0000-0000000000b1");
    private static final Validator VALIDATOR = Validation.buildDefaultValidatorFactory().getValidator();

    final UserRepository users = mock(UserRepository.class);
    final LearnerRepository learners = mock(LearnerRepository.class);
    final TutorRepository tutors = mock(TutorRepository.class);
    final ConnectionRequestRepository connections = mock(ConnectionRequestRepository.class);
    final TrialBookingRepository bookings = mock(TrialBookingRepository.class);
    final TrialBookingHistoryRepository history = mock(TrialBookingHistoryRepository.class);
    final JdbcTemplate jdbc = mock(JdbcTemplate.class);
    final ConnectionBusyLoader busyLoader = mock(ConnectionBusyLoader.class);
    final NotificationEvents events = mock(NotificationEvents.class);
    final TutorMembershipService membership = mock(TutorMembershipService.class);
    final TrialBookingServiceImpl service =
            new TrialBookingServiceImpl(users, learners, tutors, connections, bookings, history, jdbc, busyLoader);

    final User learnerUser = User.builder().userId(UUID.randomUUID()).email(LEARNER_EMAIL).fullName("Hoc Vien A")
            .phone("0912345678").role(Role.builder().roleName("ROLE_LEARNER").build()).build();
    final Learner learner = Learner.builder().learnerId(UUID.randomUUID()).user(learnerUser).currentLevel("N5").build();
    final User tutorUser = User.builder().userId(UUID.randomUUID()).email(TUTOR_EMAIL).fullName("Gia Su A")
            .role(Role.builder().roleName("ROLE_TUTOR").build()).build();
    final Tutor tutor = Tutor.builder().tutorId(UUID.randomUUID()).tutorCode(10001L).user(tutorUser)
            .verificationStatus(TutorVerificationStatus.APPROVED).build();

    {
        ReflectionTestUtils.setField(service, "notificationEvents", events);
        ReflectionTestUtils.setField(service, "membershipService", membership);
        when(membership.hasActiveMembership(any(), any())).thenReturn(true);
        when(busyLoader.load(any(), any(), any(), any(), any())).thenReturn(BusyIndex.EMPTY);
        when(bookings.isWithinAvailability(any(), anyInt(), any(), any())).thenReturn(true);
        when(bookings.save(any(TrialBooking.class))).thenAnswer(inv -> {
            TrialBooking b = inv.getArgument(0);
            if (b.getBookingId() == null) b.setBookingId(UUID.randomUUID());
            return b;
        });
        when(connections.save(any(ConnectionRequest.class))).thenAnswer(inv -> inv.getArgument(0));
        when(learners.save(any(Learner.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    void asLearner() {
        when(users.findByEmail(LEARNER_EMAIL)).thenReturn(Optional.of(learnerUser));
        when(learners.findByUser(learnerUser)).thenReturn(Optional.of(learner));
    }

    void asTutor() {
        when(users.findByEmail(TUTOR_EMAIL)).thenReturn(Optional.of(tutorUser));
        when(tutors.findByUser(tutorUser)).thenReturn(Optional.of(tutor));
    }

    /** Một người dùng khác, không liên quan tới buổi học thử. */
    void asStranger(String email) {
        User stranger = User.builder().userId(UUID.randomUUID()).email(email).fullName("Nguoi La")
                .role(Role.builder().roleName("ROLE_LEARNER").build()).build();
        when(users.findByEmail(email)).thenReturn(Optional.of(stranger));
    }

    /** Buổi học thử BOOKING_ID giữa learner và tutor, bắt đầu sau `hoursFromNow` giờ, dài 30 phút. */
    TrialBooking booking(TrialBookingStatus status, double hoursFromNow) {
        OffsetDateTime start = OffsetDateTime.now().plusMinutes((long) (hoursFromNow * 60));
        ConnectionRequest cr = ConnectionRequest.builder().requestId(UUID.randomUUID()).learner(learner).tutor(tutor)
                .status(ConnectionRequestStatus.PENDING).build();
        TrialBooking b = TrialBooking.builder().bookingId(BOOKING_ID).connectionRequest(cr).learner(learner).tutor(tutor)
                .learningMode("ONLINE").scheduledStartTime(start).scheduledEndTime(start.plusMinutes(30))
                .bookingStatus(status).build();
        b.setUpdatedAt(OffsetDateTime.now());
        when(bookings.findLockedById(BOOKING_ID)).thenReturn(Optional.of(b));
        when(bookings.findById(BOOKING_ID)).thenReturn(Optional.of(b));
        return b;
    }

    /** Có lịch bận loại `kind` phủ cả ngày bắt đầu của `start` (giờ Việt Nam). */
    void busyAt(OffsetDateTime start, BusyKind kind) {
        var date = start.atZoneSameInstant(VN).toLocalDate();
        when(busyLoader.load(any(), any(), any(), eq(date), eq(date)))
                .thenReturn(new BusyIndex(List.of(), List.of(new DatedBusy(date, LocalTime.MIN, LocalTime.MAX, kind))));
    }

    static <T> boolean hasError(T dto, String field, String message) {
        return VALIDATOR.validate(dto).stream()
                .anyMatch(v -> v.getPropertyPath().toString().equals(field) && v.getMessage().equals(message));
    }
}
