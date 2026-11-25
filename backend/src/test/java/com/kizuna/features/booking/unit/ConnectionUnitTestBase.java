package com.kizuna.features.booking.unit;

import com.kizuna.core.entity.*;
import com.kizuna.core.repository.LearnerRepository;
import com.kizuna.core.repository.TutorRepository;
import com.kizuna.core.repository.UserRepository;
import com.kizuna.features.booking.entity.*;
import com.kizuna.features.booking.repository.ConnectionRequestRepository;
import com.kizuna.features.booking.repository.ConnectionRequestSlotRepository;
import com.kizuna.features.booking.repository.TrialBookingHistoryRepository;
import com.kizuna.features.booking.repository.TrialBookingRepository;
import com.kizuna.features.booking.service.ConnectionBusyLoader;
import com.kizuna.features.booking.service.impl.ConnectionRequestServiceImpl;
import com.kizuna.features.notification.service.NotificationEvents;
import com.kizuna.features.payment.service.TutorMembershipService;
import com.kizuna.features.tutor_post.entity.TutorPost;
import com.kizuna.features.tutor_post.entity.TutorPostStatus;
import com.kizuna.features.tutor_post.repository.TutorPostRepository;
import com.kizuna.features.tutor_search.service.TutorScheduleService;
import com.kizuna.features.verification.entity.TutorVerificationStatus;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Dữ liệu và mock dùng chung cho unit test FE-08 (yêu cầu kết nối trực tiếp) - ConnectionRequestServiceImpl.
 * Mặc định: gia sư đã duyệt, còn gói hội viên, dạy online, giờ học viên chọn nằm trong lịch rảnh, không bị chiếm.
 */
abstract class ConnectionUnitTestBase {
    static final String LEARNER_EMAIL = "learner@test.com";
    static final String TUTOR_EMAIL = "tutor@test.com";
    static final UUID REQUEST_ID = UUID.fromString("00000000-0000-0000-0000-0000000000c1");
    private static final Validator VALIDATOR = Validation.buildDefaultValidatorFactory().getValidator();

    final UserRepository users = mock(UserRepository.class);
    final LearnerRepository learners = mock(LearnerRepository.class);
    final TutorRepository tutors = mock(TutorRepository.class);
    final ConnectionRequestRepository requests = mock(ConnectionRequestRepository.class);
    final ConnectionRequestSlotRepository slots = mock(ConnectionRequestSlotRepository.class);
    final TrialBookingRepository trials = mock(TrialBookingRepository.class);
    final TrialBookingHistoryRepository trialHistory = mock(TrialBookingHistoryRepository.class);
    final TutorScheduleService schedule = mock(TutorScheduleService.class);
    final JdbcTemplate jdbc = mock(JdbcTemplate.class);
    final ConnectionBusyLoader busyLoader = mock(ConnectionBusyLoader.class);
    final NotificationEvents events = mock(NotificationEvents.class);
    final TutorPostRepository posts = mock(TutorPostRepository.class);
    final TutorMembershipService membership = mock(TutorMembershipService.class);
    final ConnectionRequestServiceImpl service = new ConnectionRequestServiceImpl(users, learners, tutors, requests, slots,
            trials, trialHistory, schedule, jdbc, busyLoader);

    final User learnerUser = User.builder().userId(UUID.randomUUID()).email(LEARNER_EMAIL).fullName("Hoc Vien A")
            .phone("0912345678").role(Role.builder().roleName("ROLE_LEARNER").build()).build();
    final Learner learner = Learner.builder().learnerId(UUID.randomUUID()).user(learnerUser).build();
    final User tutorUser = User.builder().userId(UUID.randomUUID()).email(TUTOR_EMAIL).fullName("Gia Su A")
            .role(Role.builder().roleName("ROLE_TUTOR").build()).build();
    final Tutor tutor = Tutor.builder().tutorId(UUID.randomUUID()).user(tutorUser)
            .verificationStatus(TutorVerificationStatus.APPROVED).build();

    {
        ReflectionTestUtils.setField(service, "notificationEvents", events);
        ReflectionTestUtils.setField(service, "tutorPostRepository", posts);
        ReflectionTestUtils.setField(service, "membershipService", membership);
        when(membership.hasActiveMembership(any(), any())).thenReturn(true);
        when(schedule.isSlotAvailable(any(), any(), any(), any())).thenReturn(true);
        when(requests.save(any(ConnectionRequest.class))).thenAnswer(inv -> {
            ConnectionRequest cr = inv.getArgument(0);
            if (cr.getRequestId() == null) cr.setRequestId(UUID.randomUUID());
            return cr;
        });
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

    /** Hình thức dạy trên bài đăng đang hiển thị của gia sư: ONLINE / OFFLINE / BOTH. */
    void tutorTeaches(String format) {
        when(posts.findFirstByTutorTutorIdAndStatusOrderByReviewedAtDesc(tutor.getTutorId(), TutorPostStatus.PUBLISHED))
                .thenReturn(Optional.of(TutorPost.builder().teachingFormat(format).build()));
    }

    /** Yêu cầu kết nối trực tiếp REQUEST_ID ở trạng thái status, tạo `hoursAgo` giờ trước, có các ca (thứ, giờ bắt đầu). */
    ConnectionRequest request(ConnectionRequestStatus status, int hoursAgo, Object... dayAndStart) {
        ConnectionRequest cr = ConnectionRequest.builder().requestId(REQUEST_ID).learner(learner).tutor(tutor)
                .requestType(ConnectionRequestType.DIRECT).status(status).build();
        cr.setCreatedAt(OffsetDateTime.now().minusHours(hoursAgo));
        for (int i = 0; i < dayAndStart.length; i += 2) {
            cr.getSlots().add(ConnectionRequestSlot.builder().slotId(UUID.randomUUID()).connectionRequest(cr)
                    .dayOfWeek((Integer) dayAndStart[i]).startTime(LocalTime.parse((String) dayAndStart[i + 1])).build());
        }
        when(requests.findLockedById(REQUEST_ID)).thenReturn(Optional.of(cr));
        return cr;
    }

    static <T> boolean hasError(T dto, String field, String message) {
        return VALIDATOR.validate(dto).stream()
                .anyMatch(v -> v.getPropertyPath().toString().equals(field) && v.getMessage().equals(message));
    }
}
