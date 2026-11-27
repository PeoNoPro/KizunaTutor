package com.kizuna.features.classroom.service;

import com.kizuna.features.classroom.entity.LearningSpace;
import com.kizuna.features.classroom.util.ClassScheduleRules;
import com.kizuna.features.notification.service.NotificationEvents;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

/**
 * FE-12: thông báo tự động cho học viên khi gia sư thay đổi lớp học (buổi học, lịch hằng tuần, khóa học, tài liệu).
 * Gọi sau khi đã lưu xong thay đổi, trong cùng transaction của nghiệp vụ; lỗi tạo thông báo không làm hỏng nghiệp vụ.
 *
 * <p>Mã sự kiện không dùng số ngẫu nhiên/giờ hệ thống: gắn với đối tượng bị đổi và trạng thái trước → sau của lần đổi,
 * nên xử lý lại cùng một lần đổi không tạo thêm thông báo. (Hệ quả: lặp đúng chuỗi đổi A→B rồi B→A rồi A→B thì lần A→B
 * thứ hai không báo lại.)
 */
@Service
public class ClassroomNotifier {
    private static final String LEARNER_PATH = "/learner/connections";
    private static final String MATERIAL_PATH = "/learner/my-courses";
    private static final DateTimeFormatter HH_MM = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter DD_MM_YYYY = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    /** Không bắt buộc, nên test dựng service bằng tay vẫn chạy. */
    @Autowired(required = false)
    private NotificationEvents events;

    ClassroomNotifier() { }

    public ClassroomNotifier(NotificationEvents events) { this.events = events; }

    private static String session(LocalDate date, LocalTime start, LocalTime end) {
        return ClassScheduleRules.describe(date, start, end);
    }

    private static String slot(int dayOfWeek, LocalTime start, LocalTime end) {
        return ClassScheduleRules.dayLabel(dayOfWeek) + " " + start.format(HH_MM) + "–" + end.format(HH_MM);
    }

    private static String stamp(LocalDate date, LocalTime start, LocalTime end) { return date + "T" + start + "-" + end; }

    private void send(LearningSpace space, String key, String title, String content, String path) {
        if (events == null || space == null || space.getLearner() == null || space.getLearner().getUser() == null) return;
        events.publish("classroom:" + space.getSpaceId() + ":" + key, "CONNECTION", title, content, path,
                List.of(space.getLearner().getUser().getUserId()));
    }

    private static String lop(LearningSpace space) { return "Lớp \"" + space.getTitle() + "\""; }

    private static String capitalize(String text) { return Character.toUpperCase(text.charAt(0)) + text.substring(1); }

    /** "gia sư <tên>" của lớp; thiếu tên thì chỉ ghi "gia sư". */
    private static String gs(LearningSpace space) {
        var user = space.getTutor() == null ? null : space.getTutor().getUser();
        return user == null || user.getFullName() == null || user.getFullName().isBlank() ? "gia sư" : "gia sư " + user.getFullName();
    }

    // ---- buổi theo lịch hằng tuần

    public void sessionCancelled(LearningSpace space, UUID scheduleId, LocalDate originalDate,
                                 LocalDate date, LocalTime start, LocalTime end) {
        send(space, "session-cancelled:" + scheduleId + ":" + originalDate,
                "Buổi học đã bị hủy", lop(space) + ": buổi " + session(date, start, end)
                        + " đã được " + gs(space) + " hủy. Khóa học sẽ được bù thêm một buổi ở cuối.", LEARNER_PATH);
    }

    public void sessionMoved(LearningSpace space, UUID scheduleId, LocalDate originalDate,
                             LocalDate oldDate, LocalTime oldStart, LocalTime oldEnd,
                             LocalDate newDate, LocalTime newStart, LocalTime newEnd) {
        send(space, "session-moved:" + scheduleId + ":" + originalDate + ":" + stamp(oldDate, oldStart, oldEnd) + ">" + stamp(newDate, newStart, newEnd),
                "Buổi học được dời lịch", lop(space) + ": buổi " + session(oldDate, oldStart, oldEnd)
                        + " được dời sang " + session(newDate, newStart, newEnd) + ".", LEARNER_PATH);
    }

    /** Dời buổi về đúng lịch gốc (bỏ dời). */
    public void sessionRestored(LearningSpace space, UUID scheduleId, LocalDate originalDate,
                                LocalDate oldDate, LocalTime oldStart, LocalTime oldEnd,
                                LocalDate newDate, LocalTime newStart, LocalTime newEnd) {
        send(space, "session-restored:" + scheduleId + ":" + originalDate + ":" + stamp(oldDate, oldStart, oldEnd),
                "Buổi học trở về lịch gốc", lop(space) + ": buổi " + session(oldDate, oldStart, oldEnd)
                        + " được đưa về lịch gốc " + session(newDate, newStart, newEnd) + ".", LEARNER_PATH);
    }

    // ---- buổi thêm riêng

    public void extraAdded(LearningSpace space, UUID extraId, LocalDate date, LocalTime start, LocalTime end) {
        send(space, "extra-added:" + extraId, "Có buổi học thêm",
                lop(space) + ": " + gs(space) + " vừa thêm buổi học " + session(date, start, end) + ".", LEARNER_PATH);
    }

    public void extraMoved(LearningSpace space, UUID extraId, LocalDate oldDate, LocalTime oldStart, LocalTime oldEnd,
                           LocalDate newDate, LocalTime newStart, LocalTime newEnd) {
        send(space, "extra-moved:" + extraId + ":" + stamp(oldDate, oldStart, oldEnd) + ">" + stamp(newDate, newStart, newEnd),
                "Buổi học thêm được dời lịch", lop(space) + ": buổi học thêm " + session(oldDate, oldStart, oldEnd)
                        + " được dời sang " + session(newDate, newStart, newEnd) + ".", LEARNER_PATH);
    }

    public void extraCancelled(LearningSpace space, UUID extraId, LocalDate date, LocalTime start, LocalTime end) {
        send(space, "extra-cancelled:" + extraId, "Buổi học thêm đã bị hủy",
                lop(space) + ": buổi học thêm " + session(date, start, end) + " đã được " + gs(space) + " hủy.", LEARNER_PATH);
    }

    // ---- lịch cố định hằng tuần

    public void scheduleAdded(LearningSpace space, UUID scheduleId, int dayOfWeek, LocalTime start, LocalTime end) {
        send(space, "schedule-added:" + scheduleId, "Lớp học có thêm buổi hằng tuần",
                lop(space) + ": " + gs(space) + " thêm buổi học cố định " + slot(dayOfWeek, start, end) + " mỗi tuần.", LEARNER_PATH);
    }

    public void scheduleChanged(LearningSpace space, UUID scheduleId, int oldDay, LocalTime oldStart, LocalTime oldEnd,
                                int newDay, LocalTime newStart, LocalTime newEnd) {
        send(space, "schedule-changed:" + scheduleId + ":" + slot(oldDay, oldStart, oldEnd) + ">" + slot(newDay, newStart, newEnd),
                "Lịch học hằng tuần thay đổi", lop(space) + ": buổi cố định " + slot(oldDay, oldStart, oldEnd)
                        + " được đổi sang " + slot(newDay, newStart, newEnd) + " (áp dụng cho các buổi sắp tới).", LEARNER_PATH);
    }

    public void scheduleRemoved(LearningSpace space, UUID scheduleId, int dayOfWeek, LocalTime start, LocalTime end) {
        send(space, "schedule-removed:" + scheduleId, "Lớp học bỏ bớt buổi hằng tuần",
                lop(space) + ": buổi học cố định " + slot(dayOfWeek, start, end)
                        + " đã được gỡ khỏi lịch (áp dụng cho các buổi sắp tới).", LEARNER_PATH);
    }

    // ---- khóa học

    public void courseChanged(LearningSpace space, LocalDate oldStart, Integer oldTotal, LocalDate newStart, Integer newTotal) {
        boolean startChanged = oldStart != null && newStart != null && !oldStart.equals(newStart);
        boolean totalChanged = !java.util.Objects.equals(oldTotal, newTotal);
        if (!startChanged && !totalChanged) return;
        var parts = new java.util.ArrayList<String>();
        if (startChanged) parts.add("ngày bắt đầu đổi từ " + oldStart.format(DD_MM_YYYY) + " sang " + newStart.format(DD_MM_YYYY));
        if (totalChanged) parts.add("tổng số buổi đổi từ " + (oldTotal == null ? "không giới hạn" : oldTotal) + " sang " + (newTotal == null ? "không giới hạn" : newTotal));
        send(space, "course:" + oldStart + "/" + oldTotal + ">" + newStart + "/" + newTotal,
                "Khóa học được cập nhật", lop(space) + ": " + String.join("; ", parts) + ".", LEARNER_PATH);
    }

    // ---- tài liệu

    public void materialAdded(LearningSpace space, UUID materialId, String title) {
        send(space, "material:" + materialId + ":added", "Có tài liệu học tập mới",
                capitalize(gs(space)) + " vừa thêm tài liệu \"" + title + "\" vào lớp \"" + space.getTitle() + "\".", MATERIAL_PATH);
    }
}
