package com.kizuna.features.classroom.service.impl;

import com.kizuna.core.entity.Learner;
import com.kizuna.core.entity.Tutor;
import com.kizuna.core.entity.User;
import com.kizuna.core.repository.LearnerRepository;
import com.kizuna.features.classroom.dto.ClassScheduleRequest;
import com.kizuna.features.classroom.dto.ClassScheduleResponse;
import com.kizuna.features.classroom.dto.ClassroomCourseRequest;
import com.kizuna.features.classroom.dto.ClassroomCreateRequest;
import com.kizuna.features.classroom.dto.ClassroomSummaryResponse;
import com.kizuna.features.classroom.dto.EligibleLearnerResponse;
import com.kizuna.features.classroom.entity.ClassSchedule;
import com.kizuna.features.classroom.entity.LearningSpace;
import com.kizuna.features.classroom.entity.LearningSpaceStatus;
import com.kizuna.features.classroom.exception.ClassroomException;
import com.kizuna.features.classroom.repository.ClassScheduleRepository;
import com.kizuna.features.classroom.repository.LearningMaterialRepository;
import com.kizuna.features.classroom.repository.LearningSpaceRepository;
import com.kizuna.features.classroom.service.ClassroomService;
import com.kizuna.features.classroom.util.ClassScheduleRules;
import com.kizuna.features.classroom.util.ClassSessionCalendar;
import com.kizuna.features.classroom.util.CurrentActor;
import com.kizuna.features.classroom.util.MembershipRules;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
public class ClassroomServiceImpl implements ClassroomService {

    static final int TITLE_MAX = 150;
    static final int MAX_TOTAL_SESSIONS = 500;
    static final List<LearningSpaceStatus> OPEN_STATUSES = List.of(LearningSpaceStatus.ACTIVE, LearningSpaceStatus.PAUSED);
    private static final DateTimeFormatter DD_MM_YYYY = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final LearningSpaceRepository spaceRepository;
    private final LearningMaterialRepository materialRepository;
    private final LearnerRepository learnerRepository;
    private final ClassScheduleRepository scheduleRepository;
    private final ClassScheduleRules scheduleRules;
    private final ClassSessionCalendar calendar;
    private final Clock clock;

    @Autowired
    public ClassroomServiceImpl(LearningSpaceRepository spaceRepository, LearningMaterialRepository materialRepository,
                                LearnerRepository learnerRepository, ClassScheduleRepository scheduleRepository,
                                ClassScheduleRules scheduleRules, ClassSessionCalendar calendar) {
        this(spaceRepository, materialRepository, learnerRepository, scheduleRepository, scheduleRules, calendar,
                Clock.system(ClassScheduleRules.VN_ZONE));
    }

    public ClassroomServiceImpl(LearningSpaceRepository spaceRepository, LearningMaterialRepository materialRepository,
                                LearnerRepository learnerRepository, ClassScheduleRepository scheduleRepository,
                                ClassScheduleRules scheduleRules, ClassSessionCalendar calendar, Clock clock) {
        this.spaceRepository = spaceRepository;
        this.materialRepository = materialRepository;
        this.learnerRepository = learnerRepository;
        this.scheduleRepository = scheduleRepository;
        this.scheduleRules = scheduleRules;
        this.calendar = calendar;
        this.clock = clock;
    }

    @Override
    @Transactional
    public List<ClassroomSummaryResponse> listMine(CurrentActor actor) {
        List<LearningSpace> spaces = actor.isTutor()
                ? spaceRepository.findByTutor_TutorIdOrderByCreatedAtDesc(actor.tutor().getTutorId())
                : spaceRepository.findByLearner_LearnerIdOrderByCreatedAtDesc(actor.learner().getLearnerId());
        java.util.Map<UUID, ClassSessionCalendar.Progress> progress =
                ClassSessionCalendar.progressOf(calendar.load(spaces), LocalDateTime.now(clock));
        // Lớp đã học xong buổi cuối → "Đã hoàn thành" (tính khi truy cập)
        calendar.completeFinished(spaces, progress);
        // Lớp đang hoạt động / tạm dừng lên trước, lớp đã kết thúc xuống cuối (giữ thứ tự mới nhất trong từng nhóm)
        return spaces.stream()
                .sorted(Comparator.comparing((LearningSpace sp) -> !OPEN_STATUSES.contains(sp.getStatus())))
                .map(space -> toSummary(space, actor, progress.get(space.getSpaceId()))).toList();
    }

    @Override
    @Transactional
    public List<EligibleLearnerResponse> listEligibleLearners(CurrentActor actor) {
        Tutor tutor = requireTutor(actor);
        if (!MembershipRules.isActive(tutor)) {
            throw ClassroomException.conflict(MembershipRules.createBlockedMessage(tutor));
        }
        // Lớp đã học xong không còn chặn việc mở lớp mới với học viên đó
        completeFinishedOfTutor(tutor.getTutorId());
        List<UUID> ids = spaceRepository.findEligibleLearnerIds(tutor.getTutorId());
        if (ids.isEmpty()) return List.of();
        return learnerRepository.findAllById(ids).stream()
                .map(learner -> {
                    User u = learner.getUser();
                    return new EligibleLearnerResponse(learner.getLearnerId(), u.getFullName(), u.getEmail(), u.getAvatarUrl());
                })
                .sorted(Comparator.comparing(EligibleLearnerResponse::fullName, Comparator.nullsLast(String::compareToIgnoreCase)))
                .toList();
    }

    @Override
    @Transactional
    public ClassroomSummaryResponse create(CurrentActor actor, ClassroomCreateRequest request) {
        Tutor tutor = requireTutor(actor);
        if (request == null || request.learnerId() == null) {
            throw ClassroomException.field("learnerId", "Vui lòng chọn học viên.");
        }
        String title = normalizeTitle(request.title());
        List<ClassScheduleRequest> slots = request.schedules();
        ClassScheduleRules.validateNewList(slots);
        LocalDate today = LocalDate.now(clock);
        LocalDate startDate = validateStartDate(request.startDate(), today);
        int totalSessions = validateTotalSessions(request.totalSessions(), 0, slots.size());

        if (!MembershipRules.isActive(tutor)) {
            throw ClassroomException.conflict(MembershipRules.createBlockedMessage(tutor));
        }
        Learner learner = learnerRepository.findById(request.learnerId())
                .orElseThrow(() -> ClassroomException.notFound("Không tìm thấy học viên đã chọn."));
        if (!spaceRepository.hasAcceptedConnection(tutor.getTutorId(), learner.getLearnerId())) {
            throw ClassroomException.forbidden("Học viên này chưa được kết nối với bạn nên chưa thể thêm vào lớp học.");
        }
        completeFinishedOfTutor(tutor.getTutorId());
        if (spaceRepository.existsByTutor_TutorIdAndLearner_LearnerIdAndStatusIn(
                tutor.getTutorId(), learner.getLearnerId(), OPEN_STATUSES)) {
            throw ClassroomException.conflict("Học viên này đã có lớp học đang hoạt động với bạn.");
        }
        scheduleRules.ensureNoConflict(tutor.getTutorId(), learner.getLearnerId(), null, null, slots);

        LearningSpace saved = spaceRepository.save(LearningSpace.builder()
                .tutor(tutor)
                .learner(learner)
                .title(title)
                .status(LearningSpaceStatus.ACTIVE)
                .courseStartDate(startDate)
                .totalSessions(totalSessions)
                .build());
        OffsetDateTime activeFrom = ClassSessionCalendar.activeFromFor(saved, OffsetDateTime.now(clock));
        scheduleRepository.saveAll(slots.stream().map(slot -> ClassSchedule.builder()
                .space(saved)
                .dayOfWeek(slot.dayOfWeek())
                .startTime(slot.startTime())
                .endTime(slot.endTime())
                .activeFrom(activeFrom)
                .build()).toList());
        return toSummary(saved, actor);
    }

    @Override
    @Transactional
    public ClassroomSummaryResponse getDetail(CurrentActor actor, UUID spaceId) {
        LearningSpace space = loadAccessibleSpace(actor, spaceId);
        ClassSessionCalendar.Progress progress = calendar.progress(space, LocalDateTime.now(clock));
        calendar.completeFinished(List.of(space), java.util.Map.of(space.getSpaceId(), progress));
        return toSummary(space, actor, progress);
    }

    /** Các lớp đang hoạt động của gia sư đã học xong buổi cuối → "Đã hoàn thành". */
    private void completeFinishedOfTutor(UUID tutorId) {
        calendar.completeFinished(spaceRepository.findByTutor_TutorIdOrderByCreatedAtDesc(tutorId), LocalDateTime.now(clock));
    }

    @Override
    @Transactional
    public ClassroomSummaryResponse rename(CurrentActor actor, UUID spaceId, String rawTitle) {
        LearningSpace space = loadManageableSpace(actor, spaceId);
        space.setTitle(normalizeTitle(rawTitle));
        return toSummary(spaceRepository.save(space), actor);
    }

    @Override
    @Transactional
    public ClassroomSummaryResponse updateCourse(CurrentActor actor, UUID spaceId, ClassroomCourseRequest request) {
        LearningSpace space = loadManageableSpace(actor, spaceId);
        spaceRepository.lockById(space.getSpaceId());
        LocalDateTime now = LocalDateTime.now(clock);
        int started = calendar.progress(space, now).startedSessions();
        int weekly = (int) scheduleRepository.countBySpace_SpaceId(space.getSpaceId());
        int total = validateTotalSessions(request == null ? null : request.totalSessions(), started, weekly);

        LocalDate newStart = request == null ? null : request.startDate();
        LocalDate currentStart = ClassSessionCalendar.startDateOf(space);
        if (newStart != null && !newStart.equals(currentStart)) {
            if (started > 0) {
                throw ClassroomException.field("startDate", "Khóa học đã có buổi diễn ra nên không thể đổi ngày bắt đầu.");
            }
            validateStartDate(newStart, now.toLocalDate());
            space.setCourseStartDate(newStart);
            OffsetDateTime activeFrom = ClassSessionCalendar.activeFromFor(space, OffsetDateTime.now(clock));
            scheduleRepository.findBySpace_SpaceIdOrderByDayOfWeekAscStartTimeAsc(space.getSpaceId())
                    .forEach(slot -> slot.setActiveFrom(activeFrom));
        }
        space.setTotalSessions(total);
        return toSummary(spaceRepository.save(space), actor);
    }

    @Override
    @Transactional(readOnly = true)
    public LearningSpace loadAccessibleSpace(CurrentActor actor, UUID spaceId) {
        LearningSpace space = spaceRepository.findWithParticipantsBySpaceId(spaceId)
                .orElseThrow(() -> ClassroomException.notFound("Không tìm thấy lớp học."));
        if (!isTutorOf(actor, space) && !isLearnerOf(actor, space)) {
            throw ClassroomException.forbidden("Bạn không có quyền truy cập lớp học này.");
        }
        return space;
    }

    @Override
    @Transactional(readOnly = true)
    public LearningSpace loadManageableSpace(CurrentActor actor, UUID spaceId) {
        LearningSpace space = loadAccessibleSpace(actor, spaceId);
        if (!isTutorOf(actor, space)) {
            throw ClassroomException.forbidden("Chỉ gia sư của lớp mới được thay đổi nội dung lớp học.");
        }
        if (!MembershipRules.isActive(space.getTutor())) {
            throw ClassroomException.conflict(MembershipRules.readOnlyMessage(space.getTutor()));
        }
        if (space.getStatus() != LearningSpaceStatus.ACTIVE) {
            throw ClassroomException.conflict("Lớp học không còn hoạt động nên không thể thay đổi nội dung.");
        }
        return space;
    }

    @Override
    @Transactional(readOnly = true)
    public LearningSpace loadGradableSpace(CurrentActor actor, UUID spaceId) {
        LearningSpace space = loadAccessibleSpace(actor, spaceId);
        if (!isTutorOf(actor, space)) {
            throw ClassroomException.forbidden("Chỉ gia sư của lớp mới được chấm điểm.");
        }
        if (!MembershipRules.isActive(space.getTutor())) {
            throw ClassroomException.conflict(MembershipRules.readOnlyMessage(space.getTutor()));
        }
        if (isGradeLocked(space)) {
            throw ClassroomException.conflict("Lớp học không còn hoạt động nên không thể chấm điểm.");
        }
        return space;
    }

    @Override
    public boolean isGradeLocked(LearningSpace space) {
        boolean statusOk = space.getStatus() == LearningSpaceStatus.ACTIVE || space.getStatus() == LearningSpaceStatus.COMPLETED;
        return !statusOk || !MembershipRules.isActive(space.getTutor());
    }

    @Override
    public boolean isReadOnly(LearningSpace space) {
        return space.getStatus() != LearningSpaceStatus.ACTIVE
                || !MembershipRules.isActive(space.getTutor());
    }

    private ClassroomSummaryResponse toSummary(LearningSpace space, CurrentActor actor) {
        return toSummary(space, actor, space.getSpaceId() == null ? null
                : calendar.progress(space, LocalDateTime.now(clock)));
    }

    private ClassroomSummaryResponse toSummary(LearningSpace space, CurrentActor actor, ClassSessionCalendar.Progress progress) {
        boolean readOnly = isReadOnly(space);
        User tutorUser = space.getTutor().getUser();
        User learnerUser = space.getLearner().getUser();
        return ClassroomSummaryResponse.builder()
                .spaceId(space.getSpaceId())
                .title(space.getTitle())
                .status(space.getStatus())
                .tutorId(space.getTutor().getTutorId())
                .tutorName(tutorUser != null ? tutorUser.getFullName() : null)
                .learnerId(space.getLearner().getLearnerId())
                .learnerName(learnerUser != null ? learnerUser.getFullName() : null)
                .createdAt(space.getCreatedAt())
                .readOnly(readOnly)
                .canManage(!readOnly && isTutorOf(actor, space))
                .materialCount(space.getSpaceId() == null ? 0 : materialRepository.countBySpace_SpaceId(space.getSpaceId()))
                .schedules(space.getSpaceId() == null ? List.of()
                        : scheduleRepository.findBySpace_SpaceIdOrderByDayOfWeekAscStartTimeAsc(space.getSpaceId())
                                .stream().map(ClassScheduleResponse::of).toList())
                .courseStartDate(ClassSessionCalendar.startDateOf(space))
                .totalSessions(space.getTotalSessions())
                .startedSessions(progress == null ? 0 : progress.startedSessions())
                .courseEndDate(progress == null ? null : progress.endDate())
                .build();
    }

    private static Tutor requireTutor(CurrentActor actor) {
        if (!actor.isTutor()) {
            throw ClassroomException.forbidden("Chức năng này chỉ dành cho gia sư.");
        }
        return actor.tutor();
    }

    private static boolean isTutorOf(CurrentActor actor, LearningSpace space) {
        return actor.isTutor() && Objects.equals(actor.tutor().getTutorId(), space.getTutor().getTutorId());
    }

    private static boolean isLearnerOf(CurrentActor actor, LearningSpace space) {
        return actor.isLearner() && Objects.equals(actor.learner().getLearnerId(), space.getLearner().getLearnerId());
    }

    /** Ngày bắt đầu khóa: bắt buộc, từ hôm nay trở đi và không quá 1 năm tới. */
    static LocalDate validateStartDate(LocalDate startDate, LocalDate today) {
        if (startDate == null) {
            throw ClassroomException.field("startDate", "Vui lòng chọn ngày bắt đầu khóa học.");
        }
        if (startDate.isBefore(today)) {
            throw ClassroomException.field("startDate", "Ngày bắt đầu khóa học phải từ hôm nay (" + today.format(DD_MM_YYYY) + ") trở đi.");
        }
        if (startDate.isAfter(today.plusYears(1))) {
            throw ClassroomException.field("startDate", "Ngày bắt đầu khóa học không được quá 1 năm kể từ hôm nay.");
        }
        return startDate;
    }

    /**
     * Tổng số buổi: bắt buộc, tối đa 500 và không nhỏ hơn số buổi tối thiểu =
     * max(số buổi đã học, số buổi trong một tuần của lịch hằng tuần).
     */
    static int validateTotalSessions(Integer total, int started, int weeklySlots) {
        if (total == null) {
            throw ClassroomException.field("totalSessions", "Vui lòng nhập tổng số buổi của khóa học.");
        }
        if (total > MAX_TOTAL_SESSIONS) {
            throw ClassroomException.field("totalSessions", "Mỗi khóa học có tối đa " + MAX_TOTAL_SESSIONS + " buổi.");
        }
        int min = Math.max(1, Math.max(started, weeklySlots));
        if (total < min) {
            String reason = started >= weeklySlots && started > 0
                    ? "Khóa học đã học " + started + " buổi"
                    : "Lịch học có " + weeklySlots + " buổi mỗi tuần";
            throw ClassroomException.field("totalSessions", min > 1
                    ? reason + " nên tổng số buổi cần từ " + min + " buổi trở lên."
                    : "Khóa học cần có ít nhất 1 buổi.");
        }
        return total;
    }

    static String normalizeTitle(String raw) {
        String title = raw == null ? "" : raw.strip();
        if (title.isEmpty()) {
            throw ClassroomException.field("title", "Vui lòng nhập tên lớp học.");
        }
        if (title.length() > TITLE_MAX) {
            throw ClassroomException.field("title", "Tên lớp học tối đa " + TITLE_MAX + " ký tự.");
        }
        return title;
    }
}
