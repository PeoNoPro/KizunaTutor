package com.kizuna.features.classroom.service.impl;

import com.kizuna.core.entity.Learner;
import com.kizuna.core.entity.Tutor;
import com.kizuna.core.entity.User;
import com.kizuna.core.repository.LearnerRepository;
import com.kizuna.features.classroom.dto.ClassScheduleRequest;
import com.kizuna.features.classroom.dto.ClassScheduleResponse;
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
import com.kizuna.features.classroom.util.CurrentActor;
import com.kizuna.features.payment.entity.TutorMembershipStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ClassroomServiceImpl implements ClassroomService {

    static final int TITLE_MAX = 150;
    static final List<LearningSpaceStatus> OPEN_STATUSES = List.of(LearningSpaceStatus.ACTIVE, LearningSpaceStatus.PAUSED);

    private final LearningSpaceRepository spaceRepository;
    private final LearningMaterialRepository materialRepository;
    private final LearnerRepository learnerRepository;
    private final ClassScheduleRepository scheduleRepository;
    private final ClassScheduleRules scheduleRules;

    @Override
    @Transactional(readOnly = true)
    public List<ClassroomSummaryResponse> listMine(CurrentActor actor) {
        List<LearningSpace> spaces = actor.isTutor()
                ? spaceRepository.findByTutor_TutorIdOrderByCreatedAtDesc(actor.tutor().getTutorId())
                : spaceRepository.findByLearner_LearnerIdOrderByCreatedAtDesc(actor.learner().getLearnerId());
        return spaces.stream().map(space -> toSummary(space, actor)).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<EligibleLearnerResponse> listEligibleLearners(CurrentActor actor) {
        Tutor tutor = requireTutor(actor);
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

        if (tutor.getMembershipStatus() == TutorMembershipStatus.EXPIRED) {
            throw ClassroomException.conflict("Gói hội viên của bạn đã hết hạn. Vui lòng gia hạn để mở lớp học mới.");
        }
        Learner learner = learnerRepository.findById(request.learnerId())
                .orElseThrow(() -> ClassroomException.notFound("Không tìm thấy học viên đã chọn."));
        if (!spaceRepository.hasAcceptedConnection(tutor.getTutorId(), learner.getLearnerId())) {
            throw ClassroomException.forbidden("Học viên này chưa được kết nối với bạn nên chưa thể thêm vào lớp học.");
        }
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
                .build());
        scheduleRepository.saveAll(slots.stream().map(slot -> ClassSchedule.builder()
                .space(saved)
                .dayOfWeek(slot.dayOfWeek())
                .startTime(slot.startTime())
                .endTime(slot.endTime())
                .build()).toList());
        return toSummary(saved, actor);
    }

    @Override
    @Transactional(readOnly = true)
    public ClassroomSummaryResponse getDetail(CurrentActor actor, UUID spaceId) {
        return toSummary(loadAccessibleSpace(actor, spaceId), actor);
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
            throw ClassroomException.forbidden("Chỉ gia sư của lớp mới được quản lý tài liệu.");
        }
        if (space.getTutor().getMembershipStatus() == TutorMembershipStatus.EXPIRED) {
            throw ClassroomException.conflict("Gói hội viên của bạn đã hết hạn nên lớp học đang ở chế độ chỉ xem.");
        }
        if (space.getStatus() != LearningSpaceStatus.ACTIVE) {
            throw ClassroomException.conflict("Lớp học không còn hoạt động nên không thể thay đổi tài liệu.");
        }
        return space;
    }

    @Override
    public boolean isReadOnly(LearningSpace space) {
        return space.getStatus() != LearningSpaceStatus.ACTIVE
                || space.getTutor().getMembershipStatus() == TutorMembershipStatus.EXPIRED;
    }

    private ClassroomSummaryResponse toSummary(LearningSpace space, CurrentActor actor) {
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
