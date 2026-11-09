package com.kizuna.features.classroom.service.impl;

import com.kizuna.core.entity.User;
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
import com.kizuna.features.classroom.service.ClassSessionService;
import com.kizuna.features.classroom.service.ClassroomService;
import com.kizuna.features.classroom.util.ClassScheduleRules;
import com.kizuna.features.classroom.util.ClassSessionCalendar;
import com.kizuna.features.classroom.util.ClassSessionCalendar.Session;
import com.kizuna.features.classroom.util.CurrentActor;
import com.kizuna.features.classroom.util.MembershipRules;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Predicate;

import static com.kizuna.features.classroom.util.ClassScheduleRules.SESSION_FIELD;
import static com.kizuna.features.classroom.util.ClassScheduleRules.VN_ZONE;

/**
 * Buổi học cụ thể của khóa (FE-06):
 * - chỉ gia sư của lớp, lớp đang hoạt động, gói hội viên còn hiệu lực mới được dời / hủy / thêm buổi;
 * - mỗi thao tác chỉ ảnh hưởng đúng một buổi trong một tuần, các tuần khác giữ nguyên;
 * - buổi phải chưa bắt đầu, thời gian mới ở tương lai, cùng tuần, không trước ngày bắt đầu khóa,
 *   tuân thủ quy tắc giờ học (30 phút – 4 giờ) và không trùng giờ với buổi học thử / buổi học khác của gia sư hoặc học viên;
 * - lịch tự sinh cho đủ tổng số buổi: hủy buổi thì khóa bù thêm buổi ở cuối, thêm buổi thì khóa kết thúc sớm hơn.
 * Mọi thời điểm tính theo giờ Việt Nam.
 */
@Service
public class ClassSessionServiceImpl implements ClassSessionService {

    static final int MAX_RANGE_DAYS = 62;
    private static final DateTimeFormatter DD_MM = DateTimeFormatter.ofPattern("dd/MM");

    private final ClassroomService classroomService;
    private final ClassScheduleRepository scheduleRepository;
    private final ClassSessionChangeRepository changeRepository;
    private final ClassExtraSessionRepository extraRepository;
    private final LearningSpaceRepository spaceRepository;
    private final ClassScheduleRules rules;
    private final ClassSessionCalendar calendar;
    private final Clock clock;

    @Autowired
    public ClassSessionServiceImpl(ClassroomService classroomService, ClassScheduleRepository scheduleRepository,
                                   ClassSessionChangeRepository changeRepository, ClassExtraSessionRepository extraRepository,
                                   LearningSpaceRepository spaceRepository, ClassScheduleRules rules,
                                   ClassSessionCalendar calendar) {
        this(classroomService, scheduleRepository, changeRepository, extraRepository, spaceRepository, rules, calendar,
                Clock.system(VN_ZONE));
    }

    public ClassSessionServiceImpl(ClassroomService classroomService, ClassScheduleRepository scheduleRepository,
                                   ClassSessionChangeRepository changeRepository, ClassExtraSessionRepository extraRepository,
                                   LearningSpaceRepository spaceRepository, ClassScheduleRules rules,
                                   ClassSessionCalendar calendar, Clock clock) {
        this.classroomService = classroomService;
        this.scheduleRepository = scheduleRepository;
        this.changeRepository = changeRepository;
        this.extraRepository = extraRepository;
        this.spaceRepository = spaceRepository;
        this.rules = rules;
        this.calendar = calendar;
        this.clock = clock;
    }

    // ------------------------------------------------------------------ xem

    @Override
    @Transactional(readOnly = true)
    public List<ClassSessionResponse> sessions(CurrentActor actor, LocalDate from, LocalDate to) {
        UUID tutorId = actor.isTutor() ? actor.tutor().getTutorId() : null;
        UUID learnerId = actor.isLearner() ? actor.learner().getLearnerId() : null;
        if (tutorId == null && learnerId == null) {
            throw ClassroomException.forbidden("Chức năng này chỉ dành cho gia sư và học viên.");
        }
        if (from == null || to == null || to.isBefore(from) || ChronoUnit.DAYS.between(from, to) > MAX_RANGE_DAYS) {
            throw ClassroomException.badRequest("Khoảng ngày xem lịch không hợp lệ.");
        }
        return calendar.sessionsOfParticipants(tutorId, learnerId, from, to).stream()
                .map(s -> toResponse(s, actor))
                .toList();
    }

    // ------------------------------------------------------------------ buổi theo lịch hằng tuần

    @Override
    @Transactional
    public void move(CurrentActor actor, UUID spaceId, UUID scheduleId, LocalDate originalDate,
                     ClassSessionMoveRequest request) {
        LearningSpace space = loadEditableSpace(actor, spaceId);
        ClassSchedule schedule = findSchedule(scheduleId, space);
        LocalDateTime now = LocalDateTime.now(clock);
        Session current = requireOccurrence(space, scheduleId, originalDate);
        ensureNotStarted(current, now);

        LocalDate newDate = request == null ? null : request.newDate();
        LocalTime start = request == null ? null : request.startTime();
        LocalTime end = request == null ? null : request.endTime();
        validateNewTime(space, originalDate, newDate, start, end, now);
        rules.ensureNoSessionConflict(space.getTutor().getTutorId(), space.getLearner().getLearnerId(), space.getSpaceId(),
                s -> s.isOccurrence(scheduleId, originalDate), newDate, start, end);

        Optional<ClassSessionChange> existing = changeRepository.findBySchedule_ScheduleIdAndOriginalDate(scheduleId, originalDate);
        boolean backToOriginal = newDate.equals(originalDate) && start.equals(schedule.getStartTime())
                && end.equals(schedule.getEndTime());
        if (backToOriginal) {
            existing.ifPresent(changeRepository::delete);
            return;
        }
        ClassSessionChange change = existing.orElseGet(() -> ClassSessionChange.builder()
                .schedule(schedule).originalDate(originalDate).build());
        change.setCancelled(false);
        change.setNewDate(newDate);
        change.setStartTime(start);
        change.setEndTime(end);
        changeRepository.save(change);
        ensureStillInCourse(space, newDate, s -> s.isOccurrence(scheduleId, originalDate),
                "Buổi này là buổi cuối của khóa nên không thể dời ra sau các buổi khác trong tuần. Hãy tăng tổng số buổi trước.");
    }

    @Override
    @Transactional
    public void cancel(CurrentActor actor, UUID spaceId, UUID scheduleId, LocalDate originalDate) {
        LearningSpace space = loadEditableSpace(actor, spaceId);
        ClassSchedule schedule = findSchedule(scheduleId, space);
        Session current = requireOccurrence(space, scheduleId, originalDate);
        ensureNotStarted(current, LocalDateTime.now(clock));
        ClassSessionChange change = changeRepository.findBySchedule_ScheduleIdAndOriginalDate(scheduleId, originalDate)
                .orElseGet(() -> ClassSessionChange.builder().schedule(schedule).originalDate(originalDate).build());
        change.setCancelled(true);
        change.setNewDate(null);
        change.setStartTime(null);
        change.setEndTime(null);
        changeRepository.save(change);
    }

    @Override
    @Transactional
    public void restore(CurrentActor actor, UUID spaceId, UUID scheduleId, LocalDate originalDate) {
        LearningSpace space = loadEditableSpace(actor, spaceId);
        ClassSchedule schedule = findSchedule(scheduleId, space);
        ClassSessionChange change = changeRepository.findBySchedule_ScheduleIdAndOriginalDate(scheduleId, originalDate)
                .filter(c -> !c.isCancelled())
                .orElseThrow(() -> ClassroomException.notFound("Buổi học này chưa được dời nên đang theo lịch gốc."));
        LocalDateTime now = LocalDateTime.now(clock);
        if (!change.getNewDate().atTime(change.getStartTime()).isAfter(now)) {
            throw ClassroomException.conflict("Buổi học này đã bắt đầu hoặc đã diễn ra nên không thể thay đổi.");
        }
        if (!originalDate.atTime(schedule.getStartTime()).isAfter(now)) {
            throw ClassroomException.conflict("Giờ gốc của buổi học đã qua nên không thể đưa về lịch gốc. Bạn có thể dời sang thời gian khác.");
        }
        rules.ensureNoSessionConflict(space.getTutor().getTutorId(), space.getLearner().getLearnerId(), space.getSpaceId(),
                s -> s.isOccurrence(scheduleId, originalDate), originalDate, schedule.getStartTime(), schedule.getEndTime());
        changeRepository.delete(change);
    }

    // ------------------------------------------------------------------ buổi thêm riêng

    @Override
    @Transactional
    public void addExtra(CurrentActor actor, UUID spaceId, ClassSessionMoveRequest request) {
        LearningSpace space = loadEditableSpace(actor, spaceId);
        LocalDateTime now = LocalDateTime.now(clock);
        LocalDate date = request == null ? null : request.newDate();
        LocalTime start = request == null ? null : request.startTime();
        LocalTime end = request == null ? null : request.endTime();
        ClassScheduleRules.validateSession(date, start, end);
        validateDateInCourse(space, date, start, now);
        rules.ensureNoSessionConflict(space.getTutor().getTutorId(), space.getLearner().getLearnerId(), space.getSpaceId(),
                null, date, start, end);
        ClassExtraSession saved = extraRepository.save(ClassExtraSession.builder()
                .space(space).sessionDate(date).startTime(start).endTime(end).build());
        UUID extraId = saved.getExtraId();
        ensureStillInCourse(space, date, s -> Objects.equals(s.extraId(), extraId),
                "Khóa học đã đủ " + space.getTotalSessions() + " buổi trước ngày này. Hãy tăng tổng số buổi của khóa trước khi thêm buổi.");
    }

    @Override
    @Transactional
    public void moveExtra(CurrentActor actor, UUID spaceId, UUID extraId, ClassSessionMoveRequest request) {
        LearningSpace space = loadEditableSpace(actor, spaceId);
        ClassExtraSession extra = findExtra(extraId, space);
        LocalDateTime now = LocalDateTime.now(clock);
        if (!extra.getSessionDate().atTime(extra.getStartTime()).isAfter(now)) {
            throw ClassroomException.conflict("Buổi học này đã bắt đầu hoặc đã diễn ra nên không thể thay đổi.");
        }
        LocalDate newDate = request == null ? null : request.newDate();
        LocalTime start = request == null ? null : request.startTime();
        LocalTime end = request == null ? null : request.endTime();
        validateNewTime(space, extra.getSessionDate(), newDate, start, end, now);
        rules.ensureNoSessionConflict(space.getTutor().getTutorId(), space.getLearner().getLearnerId(), space.getSpaceId(),
                s -> Objects.equals(s.extraId(), extraId), newDate, start, end);
        extra.setSessionDate(newDate);
        extra.setStartTime(start);
        extra.setEndTime(end);
        extraRepository.save(extra);
        ensureStillInCourse(space, newDate, s -> Objects.equals(s.extraId(), extraId),
                "Buổi này là buổi cuối của khóa nên không thể dời ra sau các buổi khác trong tuần. Hãy tăng tổng số buổi trước.");
    }

    @Override
    @Transactional
    public void cancelExtra(CurrentActor actor, UUID spaceId, UUID extraId) {
        LearningSpace space = loadEditableSpace(actor, spaceId);
        ClassExtraSession extra = findExtra(extraId, space);
        if (!extra.getSessionDate().atTime(extra.getStartTime()).isAfter(LocalDateTime.now(clock))) {
            throw ClassroomException.conflict("Buổi học này đã bắt đầu hoặc đã diễn ra nên không thể hủy.");
        }
        extraRepository.delete(extra);
    }

    // ------------------------------------------------------------------ hỗ trợ

    /** Chỉ gia sư của lớp, lớp đang hoạt động và gói hội viên còn hiệu lực mới được thay đổi buổi học. */
    private LearningSpace loadEditableSpace(CurrentActor actor, UUID spaceId) {
        LearningSpace space = classroomService.loadAccessibleSpace(actor, spaceId);
        boolean isTutorOfSpace = actor.isTutor()
                && Objects.equals(actor.tutor().getTutorId(), space.getTutor().getTutorId());
        if (!isTutorOfSpace) {
            throw ClassroomException.forbidden("Chỉ gia sư của lớp mới được thay đổi buổi học.");
        }
        if (!MembershipRules.isActive(space.getTutor())) {
            throw ClassroomException.conflict(MembershipRules.readOnlyMessage(space.getTutor()));
        }
        if (space.getStatus() != LearningSpaceStatus.ACTIVE) {
            throw ClassroomException.conflict("Lớp học không còn hoạt động nên không thể thay đổi buổi học.");
        }
        // Khóa lớp để hai thao tác cùng lúc không cùng lọt qua bước kiểm tra trùng giờ / số buổi.
        spaceRepository.lockById(space.getSpaceId());
        return space;
    }

    private ClassSchedule findSchedule(UUID scheduleId, LearningSpace space) {
        return scheduleRepository.findByScheduleIdAndSpace_SpaceId(scheduleId, space.getSpaceId())
                .orElseThrow(() -> ClassroomException.notFound("Không tìm thấy buổi học trong lớp này."));
    }

    private ClassExtraSession findExtra(UUID extraId, LearningSpace space) {
        return extraRepository.findByExtraIdAndSpace_SpaceId(extraId, space.getSpaceId())
                .orElseThrow(() -> ClassroomException.notFound("Không tìm thấy buổi học thêm trong lớp này."));
    }

    /** Buổi (lịch hằng tuần + ngày gốc) phải có trong lịch của khóa (chưa bị hủy, chưa vượt quá tổng số buổi). */
    private Session requireOccurrence(LearningSpace space, UUID scheduleId, LocalDate originalDate) {
        if (originalDate == null) {
            throw ClassroomException.field(SESSION_FIELD, "Thiếu ngày của buổi học.");
        }
        return calendar.sessionsOfSpace(space, originalDate.plusDays(6)).stream()
                .filter(s -> s.isOccurrence(scheduleId, originalDate))
                .findFirst()
                .orElseThrow(() -> ClassroomException.notFound("Buổi học này không có trong lịch của khóa (đã hủy hoặc ngoài số buổi của khóa)."));
    }

    private static void ensureNotStarted(Session session, LocalDateTime now) {
        if (!session.start().isAfter(now)) {
            throw ClassroomException.conflict("Buổi học này đã bắt đầu hoặc đã diễn ra nên không thể thay đổi.");
        }
    }

    /** Ngày/giờ mới: đủ thông tin, đúng quy tắc giờ học, cùng tuần với ngày cũ, không trước ngày bắt đầu khóa, ở tương lai. */
    private static void validateNewTime(LearningSpace space, LocalDate currentDate, LocalDate newDate,
                                        LocalTime start, LocalTime end, LocalDateTime now) {
        ClassScheduleRules.validateSession(newDate, start, end);
        LocalDate monday = ClassSessionCalendar.mondayOf(currentDate);
        if (!ClassSessionCalendar.mondayOf(newDate).equals(monday)) {
            throw ClassroomException.field(SESSION_FIELD, "Chỉ được dời sang một ngày trong cùng tuần ("
                    + monday.format(DD_MM) + " – " + monday.plusDays(6).format(DD_MM) + ").");
        }
        validateDateInCourse(space, newDate, start, now);
    }

    private static void validateDateInCourse(LearningSpace space, LocalDate date, LocalTime start, LocalDateTime now) {
        LocalDate courseStart = ClassSessionCalendar.startDateOf(space);
        if (date.isBefore(courseStart)) {
            throw ClassroomException.field(SESSION_FIELD, "Khóa học bắt đầu từ ngày " + courseStart.format(DD_MM)
                    + " nên không thể xếp buổi học trước ngày này.");
        }
        if (!date.atTime(start).isAfter(now)) {
            throw ClassroomException.field(SESSION_FIELD, "Thời gian của buổi học phải sau thời điểm hiện tại.");
        }
    }

    /** Sau khi ghi, buổi vẫn phải nằm trong số buổi của khóa; nếu không thì hủy toàn bộ thao tác. */
    private void ensureStillInCourse(LearningSpace space, LocalDate date, Predicate<Session> target, String message) {
        boolean included = calendar.sessionsOfSpace(space, date).stream().anyMatch(target);
        if (!included) {
            throw ClassroomException.fieldConflict(SESSION_FIELD, message);
        }
    }

    private static ClassSessionResponse toResponse(Session s, CurrentActor actor) {
        LearningSpace sp = s.space();
        User partner = actor.isTutor() ? sp.getLearner().getUser() : sp.getTutor().getUser();
        return new ClassSessionResponse(sp.getSpaceId(), sp.getTitle(), partner == null ? null : partner.getFullName(),
                sp.getStatus(), s.scheduleId(), s.extraId(), s.originalDate(), s.date(), s.startTime(), s.endTime(),
                s.moved(), s.extra(), s.number(), sp.getTotalSessions());
    }
}
