package com.kizuna.features.classroom.util;

import com.kizuna.features.classroom.dto.ClassScheduleRequest;
import com.kizuna.features.classroom.entity.ClassSchedule;
import com.kizuna.features.classroom.entity.LearningSpaceStatus;
import com.kizuna.features.classroom.exception.ClassroomException;
import com.kizuna.features.classroom.repository.ClassScheduleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Quy tắc lịch học hằng tuần của lớp (FE-06):
 * - mỗi buổi có thứ (1 = Thứ Hai ... 7 = Chủ nhật), giờ bắt đầu &lt; giờ kết thúc, dài 30 phút – 4 giờ;
 * - lớp có 1 – {@value #MAX_SLOTS} buổi mỗi tuần;
 * - không trùng giờ với buổi khác của chính lớp, của các lớp đang mở của gia sư, hoặc của học viên.
 */
@Component
@RequiredArgsConstructor
public class ClassScheduleRules {

    public static final String FIELD = "schedules";
    public static final int MAX_SLOTS = 14;
    static final Duration MIN_DURATION = Duration.ofMinutes(30);
    static final Duration MAX_DURATION = Duration.ofHours(4);
    static final List<LearningSpaceStatus> OPEN_STATUSES = List.of(LearningSpaceStatus.ACTIVE, LearningSpaceStatus.PAUSED);
    private static final String[] DAY_LABELS = {"Thứ Hai", "Thứ Ba", "Thứ Tư", "Thứ Năm", "Thứ Sáu", "Thứ Bảy", "Chủ nhật"};
    private static final DateTimeFormatter HH_MM = DateTimeFormatter.ofPattern("HH:mm");

    private final ClassScheduleRepository scheduleRepository;

    /** Kiểm tra danh sách buổi học khi tạo lớp: số lượng, từng buổi và trùng giờ lẫn nhau. */
    public static void validateNewList(List<ClassScheduleRequest> slots) {
        if (slots == null || slots.isEmpty()) {
            throw ClassroomException.field(FIELD, "Vui lòng thêm ít nhất một buổi học trong tuần.");
        }
        if (slots.size() > MAX_SLOTS) {
            throw ClassroomException.field(FIELD, "Mỗi lớp có tối đa " + MAX_SLOTS + " buổi học mỗi tuần.");
        }
        slots.forEach(ClassScheduleRules::validateSlot);
        for (int i = 0; i < slots.size(); i++) {
            for (int j = i + 1; j < slots.size(); j++) {
                if (overlaps(slots.get(i), slots.get(j))) {
                    throw ClassroomException.field(FIELD, "Các buổi học bị trùng giờ nhau (" + describe(slots.get(j)) + ").");
                }
            }
        }
    }

    public static void validateSlot(ClassScheduleRequest slot) {
        if (slot == null || slot.dayOfWeek() == null || slot.dayOfWeek() < 1 || slot.dayOfWeek() > 7) {
            throw ClassroomException.field(FIELD, "Vui lòng chọn thứ trong tuần cho buổi học.");
        }
        if (slot.startTime() == null || slot.endTime() == null) {
            throw ClassroomException.field(FIELD, "Vui lòng nhập giờ bắt đầu và giờ kết thúc của buổi học.");
        }
        if (!slot.startTime().isBefore(slot.endTime())) {
            throw ClassroomException.field(FIELD, "Giờ kết thúc phải sau giờ bắt đầu (" + describe(slot) + ").");
        }
        Duration length = Duration.between(slot.startTime(), slot.endTime());
        if (length.compareTo(MIN_DURATION) < 0 || length.compareTo(MAX_DURATION) > 0) {
            throw ClassroomException.field(FIELD, "Mỗi buổi học dài từ 30 phút đến 4 giờ (" + describe(slot) + ").");
        }
    }

    /**
     * Không cho trùng giờ với lịch các lớp đang mở của gia sư hoặc của học viên.
     * @param currentSpaceId lớp đang thao tác (null khi tạo lớp mới)
     * @param excludeScheduleId buổi đang được sửa (null khi thêm mới)
     */
    public void ensureNoConflict(UUID tutorId, UUID learnerId, UUID currentSpaceId,
                                 UUID excludeScheduleId, List<ClassScheduleRequest> candidates) {
        ensureNoTrialConflict(tutorId, learnerId, candidates);
        List<ClassSchedule> existing = scheduleRepository.findOpenSchedulesOfParticipants(tutorId, learnerId, OPEN_STATUSES);
        for (ClassScheduleRequest candidate : candidates) {
            for (ClassSchedule other : existing) {
                if (Objects.equals(other.getScheduleId(), excludeScheduleId) || !overlaps(candidate, other)) continue;
                UUID otherSpaceId = other.getSpace().getSpaceId();
                if (currentSpaceId != null && currentSpaceId.equals(otherSpaceId)) {
                    throw ClassroomException.fieldConflict(FIELD, "Khung giờ " + describe(candidate) + " trùng với một buổi học khác của lớp này.");
                }
                if (Objects.equals(other.getSpace().getTutor().getTutorId(), tutorId)) {
                    throw ClassroomException.fieldConflict(FIELD, "Khung giờ " + describe(candidate) + " trùng với lịch lớp “"
                            + other.getSpace().getTitle() + "” của bạn.");
                }
                // Lớp của học viên với gia sư khác: không lộ tên lớp/gia sư khác.
                throw ClassroomException.fieldConflict(FIELD, "Học viên đã có lớp học khác vào khung giờ " + describe(candidate) + ".");
            }
        }
    }

    /** Lịch lớp hằng tuần không được đè lên buổi học thử đã xác nhận (chưa diễn ra) của gia sư hoặc học viên. */
    private void ensureNoTrialConflict(UUID tutorId, UUID learnerId, List<ClassScheduleRequest> candidates) {
        List<Object[]> trials = scheduleRepository.findUpcomingTrialSlots(tutorId, learnerId);
        if (trials == null || trials.isEmpty()) return;
        for (Object[] row : trials) {
            Integer day = row[1] == null ? null : ((Number) row[1]).intValue();
            LocalTime start = parseTime(row[2]);
            LocalTime end = parseTime(row[3]);
            if (day == null || start == null || end == null || !start.isBefore(end)) continue;
            for (ClassScheduleRequest candidate : candidates) {
                if (!overlaps(candidate.dayOfWeek(), candidate.startTime(), candidate.endTime(), day, start, end)) continue;
                boolean ofTutor = tutorId != null && tutorId.toString().equalsIgnoreCase(String.valueOf(row[0]));
                String when = " ngày " + row[4] + " (" + start.format(HH_MM) + "–" + end.format(HH_MM) + ")";
                throw ClassroomException.fieldConflict(FIELD, "Khung giờ " + describe(candidate)
                        + " trùng với buổi học thử" + (ofTutor ? " của bạn" : " của học viên") + when + ".");
            }
        }
    }

    private static LocalTime parseTime(Object value) {
        try {
            return value == null ? null : LocalTime.parse(value.toString());
        } catch (java.time.format.DateTimeParseException e) {
            return null;
        }
    }

    static boolean overlaps(ClassScheduleRequest a, ClassScheduleRequest b) {
        return overlaps(a.dayOfWeek(), a.startTime(), a.endTime(), b.dayOfWeek(), b.startTime(), b.endTime());
    }

    static boolean overlaps(ClassScheduleRequest a, ClassSchedule b) {
        return overlaps(a.dayOfWeek(), a.startTime(), a.endTime(), b.getDayOfWeek(), b.getStartTime(), b.getEndTime());
    }

    private static boolean overlaps(Integer dayA, LocalTime startA, LocalTime endA,
                                    Integer dayB, LocalTime startB, LocalTime endB) {
        return Objects.equals(dayA, dayB) && startA.isBefore(endB) && startB.isBefore(endA);
    }

    public static String dayLabel(int dayOfWeek) {
        return dayOfWeek >= 1 && dayOfWeek <= 7 ? DAY_LABELS[dayOfWeek - 1] : "";
    }

    static String describe(ClassScheduleRequest s) {
        return dayLabel(s.dayOfWeek()) + " " + s.startTime().format(HH_MM) + "–" + s.endTime().format(HH_MM);
    }
}
