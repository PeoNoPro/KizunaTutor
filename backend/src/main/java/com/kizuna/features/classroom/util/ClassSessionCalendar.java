package com.kizuna.features.classroom.util;

import com.kizuna.features.classroom.entity.ClassExtraSession;
import com.kizuna.features.classroom.entity.ClassSchedule;
import com.kizuna.features.classroom.entity.ClassSessionChange;
import com.kizuna.features.classroom.entity.LearningSpace;
import com.kizuna.features.classroom.entity.LearningSpaceStatus;
import com.kizuna.features.classroom.repository.ClassExtraSessionRepository;
import com.kizuna.features.classroom.repository.ClassScheduleRepository;
import com.kizuna.features.classroom.repository.ClassSessionChangeRepository;
import com.kizuna.features.classroom.repository.LearningSpaceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Sinh danh sách buổi học CỤ THỂ của khóa học (FE-06) từ:
 * - ngày bắt đầu khóa (course_start_date) và tổng số buổi (total_sessions; null = lặp hằng tuần không giới hạn — lớp cũ);
 * - các buổi hằng tuần với thời gian hiệu lực (active_from / active_until);
 * - các thay đổi riêng trong tuần: dời (class_session_changes), hủy (cancelled = true), thêm buổi (class_extra_sessions).
 * Buổi được đánh số theo thứ tự thời gian thực tế; khi đủ total_sessions buổi thì dừng (buổi bị hủy không tính,
 * nên khóa tự kéo dài để bù; buổi thêm được tính nên khóa kết thúc sớm hơn).
 * Mọi ngày giờ tính theo giờ Việt Nam.
 *
 * Lưu ý: câu SQL kiểm tra trùng giờ khi đặt học thử (TrialBookingRepository.findClassScheduleClash, FE-05)
 * sinh buổi theo đúng quy tắc này — sửa quy tắc ở đây thì sửa cả câu SQL đó.
 */
@Component
@RequiredArgsConstructor
public class ClassSessionCalendar {

    public static final List<LearningSpaceStatus> OPEN_STATUSES = List.of(LearningSpaceStatus.ACTIVE, LearningSpaceStatus.PAUSED);
    /** Giới hạn an toàn khi sinh lịch: tối đa ~11 năm. */
    static final int MAX_WEEKS = 600;

    /** Một buổi học cụ thể. scheduleId != null: buổi theo lịch hằng tuần; extraId != null: buổi thêm riêng. */
    public record Session(LearningSpace space, UUID scheduleId, UUID extraId, LocalDate originalDate,
                          LocalDate date, LocalTime startTime, LocalTime endTime,
                          boolean moved, boolean extra, int number) {
        public LocalDateTime start() {
            return date.atTime(startTime);
        }

        public LocalDateTime end() {
            return date.atTime(endTime);
        }

        public boolean isOccurrence(UUID scheduleId, LocalDate originalDate) {
            return this.scheduleId != null && this.scheduleId.equals(scheduleId) && this.originalDate.equals(originalDate);
        }
    }

    /** Tiến độ khóa học. endDate = ngày buổi cuối (null nếu khóa không giới hạn số buổi). */
    public record Progress(Integer totalSessions, int startedSessions, LocalDate startDate, LocalDate endDate) {
    }

    /** Dữ liệu thô của một lớp để sinh lịch. */
    public record SpaceData(LearningSpace space, List<ClassSchedule> slots, List<ClassSessionChange> changes,
                            List<ClassExtraSession> extras) {
    }

    private final LearningSpaceRepository spaceRepository;
    private final ClassScheduleRepository scheduleRepository;
    private final ClassSessionChangeRepository changeRepository;
    private final ClassExtraSessionRepository extraRepository;

    // ------------------------------------------------------------------ nạp dữ liệu

    public List<SpaceData> load(Collection<LearningSpace> spaces) {
        if (spaces.isEmpty()) return List.of();
        List<UUID> ids = spaces.stream().map(LearningSpace::getSpaceId).toList();
        Map<UUID, List<ClassSchedule>> slots = scheduleRepository.findBySpace_SpaceIdIn(ids).stream()
                .collect(Collectors.groupingBy(s -> s.getSpace().getSpaceId()));
        Map<UUID, List<ClassSessionChange>> changes = changeRepository.findBySchedule_Space_SpaceIdIn(ids).stream()
                .collect(Collectors.groupingBy(c -> c.getSchedule().getSpace().getSpaceId()));
        Map<UUID, List<ClassExtraSession>> extras = extraRepository.findBySpace_SpaceIdIn(ids).stream()
                .collect(Collectors.groupingBy(e -> e.getSpace().getSpaceId()));
        return spaces.stream().map(sp -> new SpaceData(sp,
                slots.getOrDefault(sp.getSpaceId(), List.of()),
                changes.getOrDefault(sp.getSpaceId(), List.of()),
                extras.getOrDefault(sp.getSpaceId(), List.of()))).toList();
    }

    public SpaceData load(LearningSpace space) {
        return load(List.of(space)).get(0);
    }

    /** Các buổi trong khoảng [from, to] của mọi lớp đang mở mà gia sư HOẶC học viên tham gia. */
    public List<Session> sessionsOfParticipants(UUID tutorId, UUID learnerId, LocalDate from, LocalDate to) {
        List<LearningSpace> spaces = spaceRepository.findOpenOfParticipants(tutorId, learnerId, OPEN_STATUSES);
        List<Session> result = new ArrayList<>();
        for (SpaceData data : load(spaces)) {
            generate(data, to).stream()
                    .filter(s -> !s.date().isBefore(from) && !s.date().isAfter(to))
                    .forEach(result::add);
        }
        result.sort(Comparator.comparing(Session::date).thenComparing(Session::startTime));
        return result;
    }

    /** Các buổi của một lớp từ đầu khóa tới hết tuần chứa ngày {@code until}. */
    public List<Session> sessionsOfSpace(LearningSpace space, LocalDate until) {
        return generate(load(space), until);
    }

    public Progress progress(LearningSpace space, LocalDateTime now) {
        return progress(load(space), now);
    }

    public static Progress progress(SpaceData data, LocalDateTime now) {
        LearningSpace space = data.space();
        Integer total = space.getTotalSessions();
        LocalDate until = total != null ? startDateOf(space).plusWeeks(MAX_WEEKS) : now.toLocalDate();
        List<Session> sessions = generate(data, until);
        int started = (int) sessions.stream().filter(s -> !s.start().isAfter(now)).count();
        LocalDate end = total != null && !sessions.isEmpty() ? sessions.get(sessions.size() - 1).date() : null;
        return new Progress(total, started, startDateOf(space), end);
    }

    // ------------------------------------------------------------------ sinh lịch (hàm thuần)

    public static LocalDate startDateOf(LearningSpace space) {
        if (space.getCourseStartDate() != null) return space.getCourseStartDate();
        OffsetDateTime created = space.getCreatedAt();
        return created != null ? created.atZoneSameInstant(ClassScheduleRules.VN_ZONE).toLocalDate()
                : LocalDate.now(ClassScheduleRules.VN_ZONE);
    }

    public static LocalDate mondayOf(LocalDate date) {
        return date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
    }

    /**
     * Sinh các buổi theo thứ tự thời gian, đánh số 1..n, dừng khi đủ tổng số buổi
     * hoặc khi đã qua hết tuần chứa ngày {@code until}.
     */
    public static List<Session> generate(SpaceData data, LocalDate until) {
        LearningSpace space = data.space();
        Integer total = space.getTotalSessions();
        LocalDate start = startDateOf(space);
        Map<String, ClassSessionChange> changes = new HashMap<>();
        for (ClassSessionChange c : data.changes()) {
            changes.put(c.getSchedule().getScheduleId() + "|" + c.getOriginalDate(), c);
        }
        LocalDate lastExtra = data.extras().stream().map(ClassExtraSession::getSessionDate)
                .max(Comparator.naturalOrder()).orElse(null);

        List<Session> result = new ArrayList<>();
        int count = 0;
        LocalDate monday = mondayOf(start);
        for (int week = 0; week < MAX_WEEKS; week++, monday = monday.plusWeeks(1)) {
            if (monday.isAfter(until)) break;
            if (total != null && count >= total) break;
            if (!canProduceFrom(data.slots(), monday, lastExtra)) break;

            List<Session> weekSessions = new ArrayList<>();
            for (ClassSchedule slot : data.slots()) {
                LocalDate original = monday.plusDays(slot.getDayOfWeek() - 1L);
                if (original.isBefore(start) || !isActiveAt(slot, original)) continue;
                ClassSessionChange change = changes.get(slot.getScheduleId() + "|" + original);
                if (change != null && change.isCancelled()) continue;
                if (change != null) {
                    weekSessions.add(new Session(space, slot.getScheduleId(), null, original, change.getNewDate(),
                            change.getStartTime(), change.getEndTime(), true, false, 0));
                } else {
                    weekSessions.add(new Session(space, slot.getScheduleId(), null, original, original,
                            slot.getStartTime(), slot.getEndTime(), false, false, 0));
                }
            }
            LocalDate sunday = monday.plusDays(6);
            for (ClassExtraSession e : data.extras()) {
                LocalDate d = e.getSessionDate();
                if (d.isBefore(monday) || d.isAfter(sunday) || d.isBefore(start)) continue;
                weekSessions.add(new Session(space, null, e.getExtraId(), d, d, e.getStartTime(), e.getEndTime(),
                        false, true, 0));
            }
            weekSessions.sort(Comparator.comparing(Session::date).thenComparing(Session::startTime));
            for (Session s : weekSessions) {
                if (total != null && count >= total) break;
                count++;
                result.add(new Session(s.space(), s.scheduleId(), s.extraId(), s.originalDate(), s.date(),
                        s.startTime(), s.endTime(), s.moved(), s.extra(), count));
            }
        }
        return result;
    }

    /** Buổi hằng tuần còn hiệu lực ở buổi rơi vào ngày {@code date} (so theo giờ bắt đầu, giờ Việt Nam). */
    static boolean isActiveAt(ClassSchedule slot, LocalDate date) {
        OffsetDateTime at = date.atTime(slot.getStartTime()).atZone(ClassScheduleRules.VN_ZONE).toOffsetDateTime();
        if (slot.getActiveFrom() != null && at.isBefore(slot.getActiveFrom())) return false;
        return slot.getActiveUntil() == null || at.isBefore(slot.getActiveUntil());
    }

    /** Còn buổi hằng tuần hiệu lực hoặc buổi thêm từ tuần này trở đi không (tránh vòng lặp vô ích). */
    private static boolean canProduceFrom(List<ClassSchedule> slots, LocalDate monday, LocalDate lastExtra) {
        if (lastExtra != null && !lastExtra.isBefore(monday)) return true;
        OffsetDateTime weekStart = monday.atStartOfDay(ClassScheduleRules.VN_ZONE).toOffsetDateTime();
        return slots.stream().anyMatch(s -> s.getActiveUntil() == null || s.getActiveUntil().isAfter(weekStart));
    }

    /** Thời điểm bắt đầu hiệu lực cho buổi hằng tuần mới: không sớm hơn ngày bắt đầu khóa và không sớm hơn hiện tại. */
    public static OffsetDateTime activeFromFor(LearningSpace space, OffsetDateTime now) {
        OffsetDateTime courseStart = startDateOf(space).atStartOfDay(ClassScheduleRules.VN_ZONE).toOffsetDateTime();
        return courseStart.isAfter(now) ? courseStart : now;
    }
}
