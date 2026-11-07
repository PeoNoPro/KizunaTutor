package com.kizuna.features.classroom.service.impl;

import com.kizuna.features.classroom.dto.ClassScheduleRequest;
import com.kizuna.features.classroom.dto.ClassScheduleResponse;
import com.kizuna.features.classroom.entity.ClassSchedule;
import com.kizuna.features.classroom.entity.LearningSpace;
import com.kizuna.features.classroom.entity.LearningSpaceStatus;
import com.kizuna.features.classroom.exception.ClassroomException;
import com.kizuna.features.classroom.repository.ClassScheduleRepository;
import com.kizuna.features.classroom.service.ClassScheduleService;
import com.kizuna.features.classroom.service.ClassroomService;
import com.kizuna.features.classroom.util.ClassScheduleRules;
import com.kizuna.features.classroom.util.CurrentActor;
import com.kizuna.features.payment.entity.TutorMembershipStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ClassScheduleServiceImpl implements ClassScheduleService {

    private final ClassroomService classroomService;
    private final ClassScheduleRepository scheduleRepository;
    private final ClassScheduleRules rules;

    @Override
    @Transactional(readOnly = true)
    public List<ClassScheduleResponse> list(CurrentActor actor, UUID spaceId) {
        LearningSpace space = classroomService.loadAccessibleSpace(actor, spaceId);
        return scheduleRepository.findBySpace_SpaceIdOrderByDayOfWeekAscStartTimeAsc(space.getSpaceId())
                .stream().map(ClassScheduleResponse::of).toList();
    }

    @Override
    @Transactional
    public ClassScheduleResponse add(CurrentActor actor, UUID spaceId, ClassScheduleRequest request) {
        LearningSpace space = loadEditableSpace(actor, spaceId);
        ClassScheduleRules.validateSlot(request);
        if (scheduleRepository.countBySpace_SpaceId(space.getSpaceId()) >= ClassScheduleRules.MAX_SLOTS) {
            throw ClassroomException.conflict("Mỗi lớp có tối đa " + ClassScheduleRules.MAX_SLOTS + " buổi học mỗi tuần.");
        }
        rules.ensureNoConflict(space.getTutor().getTutorId(), space.getLearner().getLearnerId(),
                space.getSpaceId(), null, List.of(request));
        ClassSchedule saved = scheduleRepository.save(ClassSchedule.builder()
                .space(space)
                .dayOfWeek(request.dayOfWeek())
                .startTime(request.startTime())
                .endTime(request.endTime())
                .build());
        return ClassScheduleResponse.of(saved);
    }

    @Override
    @Transactional
    public ClassScheduleResponse update(CurrentActor actor, UUID spaceId, UUID scheduleId, ClassScheduleRequest request) {
        LearningSpace space = loadEditableSpace(actor, spaceId);
        ClassSchedule schedule = scheduleRepository.findByScheduleIdAndSpace_SpaceId(scheduleId, space.getSpaceId())
                .orElseThrow(() -> ClassroomException.notFound("Không tìm thấy buổi học trong lớp này."));
        ClassScheduleRules.validateSlot(request);
        rules.ensureNoConflict(space.getTutor().getTutorId(), space.getLearner().getLearnerId(),
                space.getSpaceId(), schedule.getScheduleId(), List.of(request));
        schedule.setDayOfWeek(request.dayOfWeek());
        schedule.setStartTime(request.startTime());
        schedule.setEndTime(request.endTime());
        return ClassScheduleResponse.of(scheduleRepository.save(schedule));
    }

    /** Chỉ gia sư của lớp, lớp đang hoạt động và gói hội viên còn hiệu lực mới được thay đổi lịch. */
    private LearningSpace loadEditableSpace(CurrentActor actor, UUID spaceId) {
        LearningSpace space = classroomService.loadAccessibleSpace(actor, spaceId);
        boolean isTutorOfSpace = actor.isTutor()
                && Objects.equals(actor.tutor().getTutorId(), space.getTutor().getTutorId());
        if (!isTutorOfSpace) {
            throw ClassroomException.forbidden("Chỉ gia sư của lớp mới được thay đổi lịch học.");
        }
        if (space.getTutor().getMembershipStatus() == TutorMembershipStatus.EXPIRED) {
            throw ClassroomException.conflict("Gói hội viên của bạn đã hết hạn nên lớp học đang ở chế độ chỉ xem.");
        }
        if (space.getStatus() != LearningSpaceStatus.ACTIVE) {
            throw ClassroomException.conflict("Lớp học không còn hoạt động nên không thể thay đổi lịch học.");
        }
        return space;
    }
}
