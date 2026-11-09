package com.kizuna.features.classroom.util;

import com.kizuna.features.classroom.dto.ClassScheduleRequest;
import com.kizuna.features.classroom.entity.ClassSchedule;
import com.kizuna.features.classroom.entity.LearningSpace;
import com.kizuna.features.classroom.entity.LearningSpaceStatus;
import com.kizuna.features.classroom.exception.ClassroomException;
import com.kizuna.features.classroom.repository.ClassScheduleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.Predicate;

/**
 * Quy tắc lịch học của lớp (FE-06):
 * - mỗi buổi có thứ (1 = Thứ Hai ... 7 = Chủ nhật), giờ bắt đầu &lt; giờ kết thúc, dài 30 phút – 4 giờ;
 * - lớp có 1 – {@value #MAX_SLOTS} buổi hằng tuần;
 * - không trùng giờ với buổi học thử đã xác nhận, buổi khác của chính lớp, của các lớp đang mở của gia sư hoặc của học viên;
 * - tính theo các buổi CỤ THỂ của khóa (đã áp dụng dời / hủy / thêm buổi, giới hạn tổng số buổi) — {@link ClassSessionCalendar}.
 */
@Component
@RequiredArgsConstructor
public class ClassScheduleRules {

    public static final String FIELD = "schedules";
    /** Trường lỗi khi dời / hủy / thêm một buổi học cụ thể. */
    public static final String SESSION_FIELD = "session";
    public static final int MAX_SLOTS = 14;
    static final Duration MIN_DURATION = Duration.ofMinutes(30);
    static final Duration MAX_DURATION = Duration.ofHours(4);
    static final List<LearningSpaceStatus> OPEN_STATUSES = List.of(LearningSpaceStatus.ACTIVE, LearningSpaceStatus.PAUSED);
    /** Khoảng thời gian tới để kiểm tra các buổi dời / thêm riêng khi đặt lịch hằng tuần. */
    static final int LOOKAHEAD_WEEKS = 26;
    private static final String[] DAY_LABELS = {"Thứ Hai", "Thứ Ba", "Thứ Tư", "Thứ Năm", "Thứ Sáu", "Thứ Bảy", "Chủ nhật"};
    private static final DateTimeFormatter HH_MM = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter DD_MM = DateTimeFormatter.ofPattern("dd/MM");
    private static final DateTimeFormatter DD_MM_YYYY = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    public static final ZoneId VN_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    private final ClassScheduleRepository scheduleRepository;
    private final ClassSessionCalendar calendar;

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

    /** Giống {@link #validateSlot} nhưng báo lỗi ở trường "session" — dùng cho dời / thêm một buổi cụ thể. */
    public static void validateSession(LocalDate date, LocalTime start, LocalTime end) {
        if (date == null || start == null || end == null) {
            throw ClassroomException.field(SESSION_FIELD, "Vui lòng chọn ngày, giờ bắt đầu và giờ kết thúc cho buổi học.");
        }
        try {
            validateSlot(new ClassScheduleRequest(date.getDayOfWeek().getValue(), start, end));
        } catch (ClassroomException e) {
            throw ClassroomException.field(SESSION_FIELD, e.getMessage());
        }
    }

    /**
     * Lịch HẰNG TUẦN mới / sửa không được trùng giờ với lịch các lớp đang mở (chưa kết thúc khóa) của gia sư hoặc học viên,
     * buổi học thử đã xác nhận, và các buổi đã dời / thêm riêng sắp tới.
     * @param currentSpaceId lớp đang thao tác (null khi tạo lớp mới)
     * @param excludeScheduleId buổi hằng tuần đang được sửa (null khi thêm mới)
     */
    public void ensureNoConflict(UUID tutorId, UUID learnerId, UUID currentSpaceId,
                                 UUID excludeScheduleId, List<ClassScheduleRequest> candidates) {
        ensureNoTrialConflict(tutorId, learnerId, candidates);
        LocalDateTime now = LocalDateTime.now(VN_ZONE);
        List<ClassSessionCalendar.SpaceData> all = calendar.loadOfParticipants(tutorId, learnerId);
        Set<UUID> ended = new HashSet<>();
        ClassSessionCalendar.progressOf(all, now).forEach((id, p) -> {
            if (p.totalSessions() != null && p.startedSessions() >= p.totalSessions()) ended.add(id);
        });
        List<ClassSchedule> existing = scheduleRepository.findOpenSchedulesOfParticipants(tutorId, learnerId, OPEN_STATUSES);
        for (ClassScheduleRequest candidate : candidates) {
            for (ClassSchedule other : existing) {
                if (Objects.equals(other.getScheduleId(), excludeScheduleId) || !overlaps(candidate, other)) continue;
                LearningSpace otherSpace = other.getSpace();
                if (ended.contains(otherSpace.getSpaceId())) continue;
                throw scheduleConflict(otherSpace, tutorId, currentSpaceId, describe(candidate), FIELD);
            }
        }
        // Các buổi đã dời / thêm riêng sắp tới cũng chiếm khung giờ của ngày đó
        LocalDate today = now.toLocalDate();
        List<ClassSessionCalendar.Session> upcoming = ClassSessionCalendar.sessionsIn(all, today, today.plusWeeks(LOOKAHEAD_WEEKS));
        for (ClassScheduleRequest candidate : candidates) {
            for (ClassSessionCalendar.Session s : upcoming) {
                if (!(s.moved() || s.extra()) || !s.start().isAfter(now)) continue;
                if (s.scheduleId() != null && s.scheduleId().equals(excludeScheduleId)) continue;
                if (!overlaps(candidate.dayOfWeek(), candidate.startTime(), candidate.endTime(),
                        s.date().getDayOfWeek().getValue(), s.startTime(), s.endTime())) continue;
                throw ClassroomException.fieldConflict(FIELD, "Khung giờ " + describe(candidate)
                        + " trùng với một buổi học " + (s.extra() ? "thêm" : "đã dời") + " vào ngày " + s.date().format(DD_MM_YYYY)
                        + " (" + s.startTime().format(HH_MM) + "–" + s.endTime().format(HH_MM) + ").");
            }
        }
    }

    /**
     * Một buổi CỤ THỂ (ngày + giờ) không được trùng với buổi học thử đã xác nhận hoặc buổi học khác
     * của gia sư / học viên trong ngày đó.
     * @param self buổi đang được dời (bỏ qua chính nó); null khi thêm buổi mới
     */
    public void ensureNoSessionConflict(UUID tutorId, UUID learnerId, UUID currentSpaceId,
                                        Predicate<ClassSessionCalendar.Session> self,
                                        LocalDate date, LocalTime start, LocalTime end) {
        String when = describe(date, start, end);
        int day = date.getDayOfWeek().getValue();
        String dateText = date.format(DD_MM_YYYY);

        List<Object[]> trials = scheduleRepository.findUpcomingTrialSlots(tutorId, learnerId);
        for (Object[] row : trials == null ? List.<Object[]>of() : trials) {
            LocalTime ts = parseTime(row[2]);
            LocalTime te = parseTime(row[3]);
            if (ts == null || te == null || !dateText.equals(String.valueOf(row[4]))) continue;
            if (!overlaps(day, start, end, day, ts, te)) continue;
            boolean ofTutor = tutorId != null && tutorId.toString().equalsIgnoreCase(String.valueOf(row[0]));
            throw ClassroomException.fieldConflict(SESSION_FIELD, "Khung giờ " + when + " trùng với buổi học thử"
                    + (ofTutor ? " của bạn" : " của học viên") + " (" + ts.format(HH_MM) + "–" + te.format(HH_MM) + ").");
        }

        for (ClassSessionCalendar.Session s : calendar.sessionsOfParticipants(tutorId, learnerId, date, date)) {
            if (self != null && self.test(s)) continue;
            if (!overlaps(day, start, end, day, s.startTime(), s.endTime())) continue;
            throw scheduleConflict(s.space(), tutorId, currentSpaceId, when, SESSION_FIELD);
        }
    }

    private static ClassroomException scheduleConflict(LearningSpace other, UUID tutorId, UUID currentSpaceId,
                                                       String when, String field) {
        if (currentSpaceId != null && currentSpaceId.equals(other.getSpaceId())) {
            return ClassroomException.fieldConflict(field, "Khung giờ " + when + " trùng với một buổi học khác của lớp này.");
        }
        if (Objects.equals(other.getTutor().getTutorId(), tutorId)) {
            return ClassroomException.fieldConflict(field, "Khung giờ " + when + " trùng với lịch lớp “"
                    + other.getTitle() + "” của bạn.");
        }
        // Lớp của học viên với gia sư khác: không lộ tên lớp/gia sư khác.
        return ClassroomException.fieldConflict(field, "Học viên đã có lớp học khác vào khung giờ " + when + ".");
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

    /** "Thứ Năm 19/11 06:00–07:00" */
    public static String describe(LocalDate date, LocalTime start, LocalTime end) {
        return dayLabel(date.getDayOfWeek().getValue()) + " " + date.format(DD_MM) + " "
                + start.format(HH_MM) + "–" + end.format(HH_MM);
    }
}
