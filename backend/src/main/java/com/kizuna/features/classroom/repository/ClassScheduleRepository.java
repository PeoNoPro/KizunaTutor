package com.kizuna.features.classroom.repository;

import com.kizuna.features.classroom.entity.ClassSchedule;
import com.kizuna.features.classroom.entity.LearningSpaceStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ClassScheduleRepository extends JpaRepository<ClassSchedule, UUID> {

    /** Các buổi hằng tuần ĐANG hiệu lực của lớp. */
    @Query("""
            SELECT s FROM ClassSchedule s
            WHERE s.space.spaceId = :spaceId AND s.activeUntil IS NULL
            ORDER BY s.dayOfWeek, s.startTime
            """)
    List<ClassSchedule> findBySpace_SpaceIdOrderByDayOfWeekAscStartTimeAsc(@Param("spaceId") UUID spaceId);

    /** Mọi buổi hằng tuần của các lớp (kể cả đã hết hiệu lực) — dùng để sinh lịch buổi học của khóa. */
    List<ClassSchedule> findBySpace_SpaceIdIn(Collection<UUID> spaceIds);

    /** Buổi hằng tuần ĐANG hiệu lực của lớp. */
    @Query("SELECT s FROM ClassSchedule s WHERE s.scheduleId = :scheduleId AND s.space.spaceId = :spaceId AND s.activeUntil IS NULL")
    Optional<ClassSchedule> findByScheduleIdAndSpace_SpaceId(@Param("scheduleId") UUID scheduleId, @Param("spaceId") UUID spaceId);

    /** Số buổi hằng tuần ĐANG hiệu lực của lớp. */
    @Query("SELECT COUNT(s) FROM ClassSchedule s WHERE s.space.spaceId = :spaceId AND s.activeUntil IS NULL")
    long countBySpace_SpaceId(@Param("spaceId") UUID spaceId);

    /** Thời khóa biểu: lịch các lớp đang mở của gia sư (tutorId) hoặc học viên (learnerId); tham số còn lại để null. */
    @Query("""
            SELECT s FROM ClassSchedule s
            JOIN FETCH s.space sp
            JOIN FETCH sp.tutor t JOIN FETCH t.user
            JOIN FETCH sp.learner l JOIN FETCH l.user
            WHERE sp.status IN :statuses
              AND s.activeUntil IS NULL
              AND (t.tutorId = :tutorId OR l.learnerId = :learnerId)
            ORDER BY s.dayOfWeek, s.startTime
            """)
    List<ClassSchedule> findTimetable(@Param("tutorId") UUID tutorId,
                                      @Param("learnerId") UUID learnerId,
                                      @Param("statuses") Collection<LearningSpaceStatus> statuses);

    /**
     * Truy vấn CHỈ ĐỌC bảng trial_bookings (thuộc FE-05, không import entity của feature đó):
     * các buổi học thử đã xác nhận / đã dời, chưa kết thúc, của gia sư HOẶC học viên.
     * Mỗi dòng: [0] tutor_id, [1] thứ ISO (1 = Thứ Hai), [2] giờ bắt đầu "HH:mm", [3] giờ kết thúc "HH:mm",
     * [4] ngày "dd/MM/yyyy" — đã quy đổi sang giờ Việt Nam.
     */
    @Query(value = """
            SELECT CAST(tb.tutor_id AS VARCHAR),
                   CAST(EXTRACT(ISODOW FROM tb.scheduled_start_time AT TIME ZONE 'Asia/Ho_Chi_Minh') AS INTEGER),
                   to_char(tb.scheduled_start_time AT TIME ZONE 'Asia/Ho_Chi_Minh', 'HH24:MI'),
                   to_char(tb.scheduled_end_time AT TIME ZONE 'Asia/Ho_Chi_Minh', 'HH24:MI'),
                   to_char(tb.scheduled_start_time AT TIME ZONE 'Asia/Ho_Chi_Minh', 'DD/MM/YYYY')
            FROM trial_bookings tb
            WHERE tb.booking_status IN ('CONFIRMED', 'RESCHEDULED')
              AND tb.scheduled_end_time > CURRENT_TIMESTAMP
              AND (tb.tutor_id = :tutorId OR tb.learner_id = :learnerId)
            """, nativeQuery = true)
    List<Object[]> findUpcomingTrialSlots(@Param("tutorId") UUID tutorId, @Param("learnerId") UUID learnerId);

    /** Lịch của các lớp đang mở mà gia sư HOẶC học viên này tham gia — dùng để kiểm tra trùng giờ. */
    @Query("""
            SELECT s FROM ClassSchedule s JOIN FETCH s.space sp
            WHERE sp.status IN :statuses
              AND s.activeUntil IS NULL
              AND (sp.tutor.tutorId = :tutorId OR sp.learner.learnerId = :learnerId)
            """)
    List<ClassSchedule> findOpenSchedulesOfParticipants(@Param("tutorId") UUID tutorId,
                                                        @Param("learnerId") UUID learnerId,
                                                        @Param("statuses") Collection<LearningSpaceStatus> statuses);
}
