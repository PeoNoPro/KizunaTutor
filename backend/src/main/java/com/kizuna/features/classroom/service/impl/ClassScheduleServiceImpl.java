package com.kizuna.features.classroom.service.impl;

import com.kizuna.features.classroom.dto.ClassScheduleRequest;
import com.kizuna.core.entity.User;
import com.kizuna.features.classroom.dto.ClassScheduleResponse;
import com.kizuna.features.classroom.dto.TimetableEntryResponse;
import com.kizuna.features.classroom.entity.ClassSchedule;
import com.kizuna.features.classroom.entity.LearningSpace;
import com.kizuna.features.classroom.entity.LearningSpaceStatus;
import com.kizuna.features.classroom.exception.ClassroomException;
import com.kizuna.features.classroom.repository.ClassScheduleRepository;
import com.kizuna.features.classroom.repository.ClassSessionChangeRepository;
import com.kizuna.features.classroom.repository.LearningSpaceRepository;
import com.kizuna.features.classroom.service.ClassScheduleService;
import com.kizuna.features.classroom.service.ClassroomService;
import com.kizuna.features.classroom.util.ClassScheduleRules;
import com.kizuna.features.classroom.util.ClassSessionCalendar;
import com.kizuna.features.classroom.util.CurrentActor;
import com.kizuna.features.classroom.util.MembershipRules;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ClassScheduleServiceImpl implements ClassScheduleService {

    private final ClassroomService classroomService;
    private final ClassScheduleRepository scheduleRepository;
    private final ClassScheduleRules rules;
    private final LearningSpaceRepository spaceRepository;
    private final ClassSessionChangeRepository changeRepository;

    @Override
    @Transactional(readOnly = true)
    public List<TimetableEntryResponse> timetable(CurrentActor actor) {
        UUID tutorId = actor.isTutor() ? actor.tutor().getTutorId() : null;
        UUID learnerId = actor.isLearner() ? actor.learner().getLearnerId() : null;
        if (tutorId == null && learnerId == null) {
            throw ClassroomException.forbidden("Chức năng này chỉ dành cho gia sư và học viên.");
        }
        return scheduleRepository.findTimetable(tutorId, learnerId,
                        List.of(LearningSpaceStatus.ACTIVE, LearningSpaceStatus.PAUSED))
                .stream()
                .map(s -> {
                    LearningSpace sp = s.getSpace();
                    User partner = actor.isTutor() ? sp.getLearner().getUser() : sp.getTutor().getUser();
                    return new TimetableEntryResponse(s.getScheduleId(), sp.getSpaceId(), sp.getTitle(),
                            partner == null ? null : partner.getFullName(), sp.getStatus(),
                            s.getDayOfWeek(), s.getStartTime(), s.getEndTime(), sp.getCreatedAt());
                })
                .toList();
    }

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
        long weekly = scheduleRepository.countBySpace_SpaceId(space.getSpaceId());
        if (weekly >= ClassScheduleRules.MAX_SLOTS) {
            throw ClassroomException.fieldConflict(ClassScheduleRules.FIELD, "Mỗi lớp có tối đa " + ClassScheduleRules.MAX_SLOTS + " buổi học mỗi tuần.");
        }
        // Tổng số buổi của khóa không được ít hơn số buổi trong một tuần
        Integer total = space.getTotalSessions();
        if (total != null && weekly + 1 > total) {
            throw ClassroomException.fieldConflict(ClassScheduleRules.FIELD, "Khóa học có tổng " + total
                    + " buổi nên không thể có nhiều hơn " + total + " buổi mỗi tuần. Hãy tăng tổng số buổi của khóa trước.");
        }
        rules.ensureNoConflict(space.getTutor().getTutorId(), space.getLearner().getLearnerId(),
                space.getSpaceId(), null, List.of(request));
        // Buổi mới chỉ sinh ra các buổi từ bây giờ (và từ ngày bắt đầu khóa) trở đi
        ClassSchedule saved = scheduleRepository.save(ClassSchedule.builder()
                .space(space)
                .dayOfWeek(request.dayOfWeek())
                .startTime(request.startTime())
                .endTime(request.endTime())
                .activeFrom(ClassSessionCalendar.activeFromFor(space, now()))
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
        boolean slotChanged = !request.dayOfWeek().equals(schedule.getDayOfWeek())
                || !request.startTime().equals(schedule.getStartTime())
                || !request.endTime().equals(schedule.getEndTime());
        if (!slotChanged) return ClassScheduleResponse.of(schedule);

        // Đổi lịch cố định chỉ áp dụng từ bây giờ: các lần dời / hủy riêng của những buổi chưa tới không còn khớp nên bỏ đi.
        changeRepository.deleteBySchedule_ScheduleIdAndOriginalDateGreaterThanEqual(schedule.getScheduleId(),
                LocalDate.now(ClassScheduleRules.VN_ZONE));
        OffsetDateTime now = now();
        if (schedule.getActiveFrom() != null && schedule.getActiveFrom().isAfter(now)) {
            // Chưa có buổi nào diễn ra theo lịch này → sửa trực tiếp
            schedule.setDayOfWeek(request.dayOfWeek());
            schedule.setStartTime(request.startTime());
            schedule.setEndTime(request.endTime());
            return ClassScheduleResponse.of(scheduleRepository.save(schedule));
        }
        // Đã có buổi diễn ra → giữ lịch cũ cho các buổi đã qua, lịch mới có hiệu lực từ bây giờ
        schedule.setActiveUntil(now);
        scheduleRepository.save(schedule);
        ClassSchedule replacement = scheduleRepository.save(ClassSchedule.builder()
                .space(space)
                .dayOfWeek(request.dayOfWeek())
                .startTime(request.startTime())
                .endTime(request.endTime())
                .activeFrom(ClassSessionCalendar.activeFromFor(space, now))
                .build());
        return ClassScheduleResponse.of(replacement);
    }

    @Override
    @Transactional
    public void delete(CurrentActor actor, UUID spaceId, UUID scheduleId) {
        LearningSpace space = loadEditableSpace(actor, spaceId);
        ClassSchedule schedule = scheduleRepository.findByScheduleIdAndSpace_SpaceId(scheduleId, space.getSpaceId())
                .orElseThrow(() -> ClassroomException.notFound("Không tìm thấy buổi học trong lớp này."));
        if (scheduleRepository.countBySpace_SpaceId(space.getSpaceId()) <= 1) {
            throw ClassroomException.fieldConflict(ClassScheduleRules.FIELD,
                    "Lớp học cần ít nhất một buổi học mỗi tuần nên không thể xóa buổi cuối cùng. Bạn có thể sửa giờ của buổi này.");
        }
        // Xóa buổi hằng tuần ở mọi tuần từ bây giờ trở đi; các buổi đã diễn ra vẫn được giữ và tính vào khóa.
        // Tổng số buổi của khóa giữ nguyên nên lịch tự kéo dài thêm tuần để bù cho đủ.
        changeRepository.deleteBySchedule_ScheduleIdAndOriginalDateGreaterThanEqual(schedule.getScheduleId(),
                LocalDate.now(ClassScheduleRules.VN_ZONE));
        OffsetDateTime now = now();
        if (schedule.getActiveFrom() != null && schedule.getActiveFrom().isAfter(now)) {
            scheduleRepository.delete(schedule);
        } else {
            schedule.setActiveUntil(now);
            scheduleRepository.save(schedule);
        }
    }

    private static OffsetDateTime now() {
        return OffsetDateTime.now(ClassScheduleRules.VN_ZONE);
    }

    /** Chỉ gia sư của lớp, lớp đang hoạt động và gói hội viên còn hiệu lực mới được thay đổi lịch. */
    private LearningSpace loadEditableSpace(CurrentActor actor, UUID spaceId) {
        LearningSpace space = classroomService.loadAccessibleSpace(actor, spaceId);
        boolean isTutorOfSpace = actor.isTutor()
                && Objects.equals(actor.tutor().getTutorId(), space.getTutor().getTutorId());
        if (!isTutorOfSpace) {
            throw ClassroomException.forbidden("Chỉ gia sư của lớp mới được thay đổi lịch học.");
        }
        if (!MembershipRules.isActive(space.getTutor())) {
            throw ClassroomException.conflict(MembershipRules.readOnlyMessage(space.getTutor()));
        }
        if (space.getStatus() != LearningSpaceStatus.ACTIVE) {
            throw ClassroomException.conflict("Lớp học không còn hoạt động nên không thể thay đổi lịch học.");
        }
        // Khóa lớp trước khi kiểm tra số buổi / trùng giờ để hai yêu cầu đồng thời không cùng lọt qua.
        spaceRepository.lockById(space.getSpaceId());
        return space;
    }
}
