package com.kizuna.features.classroom;

import com.kizuna.core.entity.Learner;
import com.kizuna.core.entity.Tutor;
import com.kizuna.features.classroom.dto.ClassSessionMoveRequest;
import com.kizuna.features.classroom.dto.ClassSessionResponse;
import com.kizuna.features.classroom.entity.ClassExtraSession;
import com.kizuna.features.classroom.entity.ClassSchedule;
import com.kizuna.features.classroom.entity.ClassSessionChange;
import com.kizuna.features.classroom.entity.LearningSpace;
import com.kizuna.features.classroom.entity.LearningSpaceStatus;
import com.kizuna.features.classroom.exception.ClassroomException;
import com.kizuna.features.classroom.repository.ClassExtraSessionRepository;
import com.kizuna.features.classroom.repository.ClassScheduleRepository;
import com.kizuna.features.classroom.repository.ClassSessionChangeRepository;
import com.kizuna.features.classroom.repository.LearningSpaceRepository;
import com.kizuna.features.classroom.service.ClassroomService;
import com.kizuna.features.classroom.service.impl.ClassSessionServiceImpl;
import com.kizuna.features.classroom.util.ClassScheduleRules;
import com.kizuna.features.classroom.util.ClassSessionCalendar;
import com.kizuna.features.payment.entity.TutorMembershipStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.time.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static com.kizuna.features.classroom.ClassroomTestData.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Dời / hủy / thêm riêng một buổi trong tuần. "Bây giờ" cố định: Thứ Tư 18/11/2026 10:00 (giờ Việt Nam);
 * tuần đang xét: Thứ Hai 16/11 – Chủ nhật 22/11. Khóa bắt đầu Thứ Hai 02/11, mỗi tuần một buổi Thứ Năm 06:00–07:00.
 * Repository được giả lập bằng danh sách trong bộ nhớ để lịch buổi học sinh ra đúng như khi chạy thật.
 */
class ClassSessionServiceImplTest {

    private static final ZoneId VN = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 11, 18, 10, 0);
    private static final LocalDate COURSE_START = LocalDate.of(2026, 11, 2);
    private static final LocalDate MON = LocalDate.of(2026, 11, 16);
    private static final LocalDate THU = LocalDate.of(2026, 11, 19);
    private static final LocalDate SAT = LocalDate.of(2026, 11, 21);

    private ClassScheduleRepository schedules;
    private ClassSessionChangeRepository changes;
    private ClassExtraSessionRepository extras;
    private LearningSpaceRepository spaces;
    private ClassSessionCalendar calendar;
    private ClassSessionServiceImpl service;

    private Tutor tutor;
    private Learner learner;
    private LearningSpace space;
    private ClassSchedule thursday;
    private final List<LearningSpace> openSpaces = new ArrayList<>();
    private final List<ClassSchedule> slotStore = new ArrayList<>();
    private final List<ClassSessionChange> changeStore = new ArrayList<>();
    private final List<ClassExtraSession> extraStore = new ArrayList<>();

    @BeforeEach
    void setUp() {
        ClassroomService classroomService = mock(ClassroomService.class);
        schedules = mock(ClassScheduleRepository.class);
        changes = mock(ClassSessionChangeRepository.class);
        extras = mock(ClassExtraSessionRepository.class);
        spaces = mock(LearningSpaceRepository.class);
        calendar = new ClassSessionCalendar(spaces, schedules, changes, extras);
        Clock clock = Clock.fixed(NOW.atZone(VN).toInstant(), VN);
        service = new ClassSessionServiceImpl(classroomService, schedules, changes, extras, spaces,
                new ClassScheduleRules(schedules, calendar), calendar, clock);

        tutor = tutor(TutorMembershipStatus.PREMIUM);
        learner = learner();
        space = newSpace(tutor, learner, "Lớp N4", 20);
        thursday = slot(space, 4, 6, 7);

        when(classroomService.loadAccessibleSpace(any(), eq(space.getSpaceId()))).thenReturn(space);
        when(schedules.findByScheduleIdAndSpace_SpaceId(any(), any())).thenAnswer(inv -> slotStore.stream()
                .filter(s -> s.getScheduleId().equals(inv.getArgument(0)) && s.getSpace().getSpaceId().equals(inv.getArgument(1)))
                .findFirst());
        when(schedules.findBySpace_SpaceIdIn(any())).thenAnswer(inv -> List.copyOf(slotStore));
        when(schedules.findOpenSchedulesOfParticipants(any(), any(), any())).thenAnswer(inv -> List.copyOf(slotStore));
        when(schedules.findUpcomingTrialSlots(any(), any())).thenReturn(List.of());
        when(spaces.findOpenOfParticipants(any(), any(), any())).thenAnswer(inv -> List.copyOf(openSpaces));

        when(changes.findBySchedule_Space_SpaceIdIn(any())).thenAnswer(inv -> List.copyOf(changeStore));
        when(changes.findBySchedule_ScheduleIdAndOriginalDate(any(), any())).thenAnswer(inv -> changeStore.stream()
                .filter(c -> c.getSchedule().getScheduleId().equals(inv.getArgument(0)) && c.getOriginalDate().equals(inv.getArgument(1)))
                .findFirst());
        when(changes.save(any(ClassSessionChange.class))).thenAnswer(inv -> {
            ClassSessionChange c = inv.getArgument(0);
            if (c.getChangeId() == null) {
                c.setChangeId(UUID.randomUUID());
                changeStore.add(c);
            }
            return c;
        });
        doAnswer(inv -> changeStore.remove(inv.<ClassSessionChange>getArgument(0))).when(changes).delete(any(ClassSessionChange.class));

        when(extras.findBySpace_SpaceIdIn(any())).thenAnswer(inv -> List.copyOf(extraStore));
        when(extras.findByExtraIdAndSpace_SpaceId(any(), any())).thenAnswer(inv -> extraStore.stream()
                .filter(e -> e.getExtraId().equals(inv.getArgument(0))).findFirst());
        when(extras.save(any(ClassExtraSession.class))).thenAnswer(inv -> {
            ClassExtraSession e = inv.getArgument(0);
            if (e.getExtraId() == null) {
                e.setExtraId(UUID.randomUUID());
                extraStore.add(e);
            }
            return e;
        });
        doAnswer(inv -> extraStore.remove(inv.<ClassExtraSession>getArgument(0))).when(extras).delete(any(ClassExtraSession.class));
    }

    private LearningSpace newSpace(Tutor t, Learner l, String title, Integer total) {
        LearningSpace sp = space(t, l, LearningSpaceStatus.ACTIVE);
        sp.setTitle(title);
        sp.setCourseStartDate(COURSE_START);
        sp.setTotalSessions(total);
        openSpaces.add(sp);
        return sp;
    }

    private ClassSchedule slot(LearningSpace sp, int day, int sh, int eh) {
        ClassSchedule s = ClassSchedule.builder().scheduleId(UUID.randomUUID()).space(sp).dayOfWeek(day)
                .startTime(LocalTime.of(sh, 0)).endTime(LocalTime.of(eh, 0))
                .activeFrom(COURSE_START.atStartOfDay(VN).toOffsetDateTime()).build();
        slotStore.add(s);
        return s;
    }

    private static ClassSessionMoveRequest to(LocalDate date, int sh, int eh) {
        return new ClassSessionMoveRequest(date, LocalTime.of(sh, 0), LocalTime.of(eh, 0));
    }

    private static void assertStatus(Runnable call, HttpStatus status) {
        assertThatThrownBy(call::run).isInstanceOf(ClassroomException.class)
                .extracting(e -> ((ClassroomException) e).getStatus()).isEqualTo(status);
    }

    private LocalDate endDate() {
        return calendar.progress(space, NOW).endDate();
    }

    private List<ClassSessionCalendar.Session> week(LocalDate monday) {
        return calendar.sessionsOfSpace(space, monday.plusDays(6)).stream()
                .filter(s -> !s.date().isBefore(monday) && !s.date().isAfter(monday.plusDays(6))).toList();
    }

    // ---------------------------------------------------------------- dời

    @Test
    void tutorMovesOnlyThatWeeksSession() {
        service.move(asTutor(tutor), space.getSpaceId(), thursday.getScheduleId(), THU, to(SAT, 10, 11));

        assertThat(week(MON)).singleElement().satisfies(s -> {
            assertThat(s.date()).isEqualTo(SAT);
            assertThat(s.moved()).isTrue();
        });
        // Tuần sau vẫn theo lịch gốc
        assertThat(week(MON.plusWeeks(1))).singleElement().satisfies(s -> {
            assertThat(s.date()).isEqualTo(THU.plusWeeks(1));
            assertThat(s.moved()).isFalse();
        });
        verify(spaces).lockById(space.getSpaceId());
    }

    @Test
    void moveFollowsTimeRulesSameWeekAndFuture() {
        UUID id = thursday.getScheduleId();
        // Sang tuần khác
        assertStatus(() -> service.move(asTutor(tutor), space.getSpaceId(), id, THU, to(LocalDate.of(2026, 11, 23), 10, 11)), HttpStatus.BAD_REQUEST);
        // Giờ mới đã qua
        assertStatus(() -> service.move(asTutor(tutor), space.getSpaceId(), id, THU, to(LocalDate.of(2026, 11, 18), 8, 9)), HttpStatus.BAD_REQUEST);
        // Dài hơn 4 giờ / ngắn hơn 30 phút
        assertStatus(() -> service.move(asTutor(tutor), space.getSpaceId(), id, THU, to(SAT, 8, 13)), HttpStatus.BAD_REQUEST);
        assertStatus(() -> service.move(asTutor(tutor), space.getSpaceId(), id, THU,
                new ClassSessionMoveRequest(SAT, LocalTime.of(8, 0), LocalTime.of(8, 20))), HttpStatus.BAD_REQUEST);
        // Ngày gốc không phải buổi của khóa
        assertStatus(() -> service.move(asTutor(tutor), space.getSpaceId(), id, LocalDate.of(2026, 11, 20), to(SAT, 10, 11)), HttpStatus.NOT_FOUND);
        assertThat(changeStore).isEmpty();
    }

    @Test
    void sessionThatAlreadyHappenedCannotBeChanged() {
        ClassSchedule monday = slot(space, 1, 7, 8);
        assertStatus(() -> service.move(asTutor(tutor), space.getSpaceId(), monday.getScheduleId(), MON, to(SAT, 10, 11)), HttpStatus.CONFLICT);
        assertStatus(() -> service.cancel(asTutor(tutor), space.getSpaceId(), monday.getScheduleId(), MON), HttpStatus.CONFLICT);
    }

    @Test
    void onlyTutorOfActiveClassWithMembershipCanChangeSessions() {
        UUID id = thursday.getScheduleId();
        assertStatus(() -> service.move(asLearner(learner), space.getSpaceId(), id, THU, to(SAT, 10, 11)), HttpStatus.FORBIDDEN);
        assertStatus(() -> service.cancel(asLearner(learner), space.getSpaceId(), id, THU), HttpStatus.FORBIDDEN);
        assertStatus(() -> service.addExtra(asLearner(learner), space.getSpaceId(), to(SAT, 10, 11)), HttpStatus.FORBIDDEN);
        tutor.setMembershipStatus(TutorMembershipStatus.EXPIRED);
        assertStatus(() -> service.move(asTutor(tutor), space.getSpaceId(), id, THU, to(SAT, 10, 11)), HttpStatus.CONFLICT);
        assertThat(changeStore).isEmpty();
    }

    @Test
    void clashWithOtherClassOnNewDayButNotWithASessionCancelledThatWeek() {
        LearningSpace other = newSpace(tutor, learner(), "Lớp N3", 20);
        ClassSchedule otherSaturday = slot(other, 6, 10, 12);

        assertThatThrownBy(() -> service.move(asTutor(tutor), space.getSpaceId(), thursday.getScheduleId(), THU, to(SAT, 10, 11)))
                .isInstanceOfSatisfying(ClassroomException.class, e -> {
                    assertThat(e.getStatus()).isEqualTo(HttpStatus.CONFLICT);
                    assertThat(e.getFieldErrors()).containsKey("session");
                    assertThat(e.getMessage()).contains("Lớp N3");
                });

        changeStore.add(ClassSessionChange.builder().changeId(UUID.randomUUID()).schedule(otherSaturday)
                .originalDate(SAT).cancelled(true).build());
        service.move(asTutor(tutor), space.getSpaceId(), thursday.getScheduleId(), THU, to(SAT, 10, 11));
        assertThat(week(MON)).singleElement().extracting(ClassSessionCalendar.Session::date).isEqualTo(SAT);
    }

    @Test
    void clashWithExtraSessionOfOtherClassAndWithTrial() {
        LearningSpace other = newSpace(tutor, learner(), "Lớp N3", 20);
        extraStore.add(ClassExtraSession.builder().extraId(UUID.randomUUID()).space(other)
                .sessionDate(SAT).startTime(LocalTime.of(10, 30)).endTime(LocalTime.of(11, 30)).build());
        assertStatus(() -> service.move(asTutor(tutor), space.getSpaceId(), thursday.getScheduleId(), THU, to(SAT, 10, 11)), HttpStatus.CONFLICT);

        extraStore.clear();
        when(schedules.findUpcomingTrialSlots(any(), any())).thenReturn(List.<Object[]>of(
                new Object[]{learner.getLearnerId().toString(), 6, "09:30", "10:20", "21/11/2026"}));
        assertThatThrownBy(() -> service.move(asTutor(tutor), space.getSpaceId(), thursday.getScheduleId(), THU, to(SAT, 10, 11)))
                .hasMessageContaining("học thử của học viên");
    }

    @Test
    void restoreBringsMovedSessionBack() {
        service.move(asTutor(tutor), space.getSpaceId(), thursday.getScheduleId(), THU, to(SAT, 10, 11));
        service.restore(asTutor(tutor), space.getSpaceId(), thursday.getScheduleId(), THU);
        assertThat(changeStore).isEmpty();
        assertThat(week(MON)).singleElement().extracting(ClassSessionCalendar.Session::date).isEqualTo(THU);
        assertStatus(() -> service.restore(asTutor(tutor), space.getSpaceId(), thursday.getScheduleId(), THU), HttpStatus.NOT_FOUND);
    }

    // ---------------------------------------------------------------- hủy

    @Test
    void cancellingEvenTheOnlySessionOfTheWeekExtendsTheCourse() {
        LocalDate before = endDate();
        service.cancel(asTutor(tutor), space.getSpaceId(), thursday.getScheduleId(), THU);

        assertThat(week(MON)).isEmpty();
        assertThat(week(MON.plusWeeks(1))).hasSize(1);
        assertThat(endDate()).isEqualTo(before.plusWeeks(1));
        // Buổi đã hủy không còn trong lịch nên không dời / hủy lại được
        assertStatus(() -> service.move(asTutor(tutor), space.getSpaceId(), thursday.getScheduleId(), THU, to(SAT, 10, 11)), HttpStatus.NOT_FOUND);
    }

    // ---------------------------------------------------------------- thêm buổi

    @Test
    void extraSessionOnlyInThatWeekAndCourseEndsEarlier() {
        LocalDate before = endDate();
        service.addExtra(asTutor(tutor), space.getSpaceId(), to(SAT, 10, 11));

        assertThat(week(MON)).extracting(ClassSessionCalendar.Session::date).containsExactly(THU, SAT);
        assertThat(week(MON.plusWeeks(1))).hasSize(1);
        assertThat(endDate()).isEqualTo(before.minusWeeks(1));
    }

    @Test
    void extraSessionFollowsRulesAndCanBeMovedOrCancelled() {
        assertStatus(() -> service.addExtra(asTutor(tutor), space.getSpaceId(), to(SAT, 8, 13)), HttpStatus.BAD_REQUEST);
        assertStatus(() -> service.addExtra(asTutor(tutor), space.getSpaceId(), to(LocalDate.of(2026, 11, 17), 10, 11)), HttpStatus.BAD_REQUEST);
        assertStatus(() -> service.addExtra(asTutor(tutor), space.getSpaceId(), to(THU, 6, 7)), HttpStatus.CONFLICT);

        service.addExtra(asTutor(tutor), space.getSpaceId(), to(SAT, 10, 11));
        UUID extraId = extraStore.get(0).getExtraId();
        service.moveExtra(asTutor(tutor), space.getSpaceId(), extraId, to(LocalDate.of(2026, 11, 22), 14, 15));
        assertThat(extraStore.get(0).getSessionDate()).isEqualTo(LocalDate.of(2026, 11, 22));
        assertStatus(() -> service.moveExtra(asTutor(tutor), space.getSpaceId(), extraId, to(LocalDate.of(2026, 11, 24), 14, 15)), HttpStatus.BAD_REQUEST);

        service.cancelExtra(asTutor(tutor), space.getSpaceId(), extraId);
        assertThat(extraStore).isEmpty();
    }

    @Test
    void cannotAddExtraSessionAfterCourseIsComplete() {
        space.setTotalSessions(2); // 05/11 và 12/11 đã học xong
        assertThatThrownBy(() -> service.addExtra(asTutor(tutor), space.getSpaceId(), to(SAT, 10, 11)))
                .isInstanceOf(ClassroomException.class)
                .hasMessageContaining("đủ 2 buổi");
    }

    // ---------------------------------------------------------------- xem

    @Test
    void learnerSeesSessionsOfTheWeekWithNumbers() {
        service.addExtra(asTutor(tutor), space.getSpaceId(), to(SAT, 10, 11));
        List<ClassSessionResponse> list = service.sessions(asLearner(learner), MON, MON.plusDays(6));
        assertThat(list).extracting(ClassSessionResponse::date).containsExactly(THU, SAT);
        assertThat(list).extracting(ClassSessionResponse::number).containsExactly(3, 4);
        assertThat(list.get(0).partnerName()).isEqualTo("Gia sư A");
        assertThat(list.get(1).extra()).isTrue();
        assertThat(list.get(0).totalSessions()).isEqualTo(20);
        assertStatus(() -> service.sessions(asLearner(learner), MON, MON.plusDays(100)), HttpStatus.BAD_REQUEST);
    }
}
