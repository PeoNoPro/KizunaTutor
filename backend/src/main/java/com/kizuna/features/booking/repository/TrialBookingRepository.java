package com.kizuna.features.booking.repository;

import com.kizuna.features.booking.entity.TrialBooking;
import com.kizuna.features.booking.entity.TrialBookingStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface TrialBookingRepository extends JpaRepository<TrialBooking, UUID> {

    @Query("SELECT COUNT(tb) > 0 FROM TrialBooking tb " +
           "WHERE tb.tutor.tutorId = :tutorId " +
           "AND tb.bookingStatus IN ('PENDING', 'CONFIRMED') " +
           "AND tb.scheduledStartTime < :newEndTime " +
           "AND tb.scheduledEndTime > :newStartTime")
    boolean existsOverlappingBooking(
            @Param("tutorId") UUID tutorId,
            @Param("newStartTime") OffsetDateTime newStartTime,
            @Param("newEndTime") OffsetDateTime newEndTime);

    @Query("SELECT COUNT(tb) > 0 FROM TrialBooking tb " +
           "WHERE tb.learner.learnerId = :learnerId " +
           "AND tb.bookingStatus IN ('PENDING', 'CONFIRMED') " +
           "AND tb.scheduledStartTime < :newEndTime " +
           "AND tb.scheduledEndTime > :newStartTime")
    boolean existsOverlappingLearnerBooking(
            @Param("learnerId") UUID learnerId,
            @Param("newStartTime") OffsetDateTime newStartTime,
            @Param("newEndTime") OffsetDateTime newEndTime);

    @Query("SELECT COUNT(tb) > 0 FROM TrialBooking tb " +
           "WHERE tb.tutor.tutorId = :tutorId " +
           "AND tb.bookingId <> :excludeBookingId " +
           "AND tb.bookingStatus IN ('PENDING', 'CONFIRMED') " +
           "AND tb.scheduledStartTime < :newEndTime " +
           "AND tb.scheduledEndTime > :newStartTime")
    boolean existsOverlappingBookingExcludingId(
            @Param("tutorId") UUID tutorId,
            @Param("excludeBookingId") UUID excludeBookingId,
            @Param("newStartTime") OffsetDateTime newStartTime,
            @Param("newEndTime") OffsetDateTime newEndTime);

    @Query("SELECT COUNT(tb) > 0 FROM TrialBooking tb " +
           "WHERE tb.learner.learnerId = :learnerId " +
           "AND tb.bookingId <> :excludeBookingId " +
           "AND tb.bookingStatus IN ('PENDING', 'CONFIRMED') " +
           "AND tb.scheduledStartTime < :newEndTime " +
           "AND tb.scheduledEndTime > :newStartTime")
    boolean existsOverlappingLearnerBookingExcludingId(
            @Param("learnerId") UUID learnerId,
            @Param("excludeBookingId") UUID excludeBookingId,
            @Param("newStartTime") OffsetDateTime newStartTime,
            @Param("newEndTime") OffsetDateTime newEndTime);

    Page<TrialBooking> findByLearnerLearnerIdAndBookingStatus(UUID learnerId, TrialBookingStatus status, Pageable pageable);

    Page<TrialBooking> findByLearnerLearnerId(UUID learnerId, Pageable pageable);

    Page<TrialBooking> findByTutorTutorIdAndBookingStatus(UUID tutorId, TrialBookingStatus status, Pageable pageable);

    Page<TrialBooking> findByTutorTutorId(UUID tutorId, Pageable pageable);

    List<TrialBooking> findByTutorTutorIdAndBookingStatusIn(UUID tutorId, List<TrialBookingStatus> statuses);

    /** Các buổi học thử của một cặp gia sư – học viên theo trạng thái (dùng khi đã đồng ý kết nối). */
    List<TrialBooking> findByTutorTutorIdAndLearnerLearnerIdAndBookingStatusIn(UUID tutorId, UUID learnerId,
                                                                               List<TrialBookingStatus> statuses);

    /**
     * Truy vấn CHỈ ĐỌC lịch học chính thức (FE-06) của gia sư HOẶC học viên vào NGÀY {@code date} (giờ Việt Nam),
     * trùng khung giờ [startTime, endTime). Sinh các buổi CỤ THỂ của khóa theo đúng quy tắc của
     * ClassSessionCalendar (FE-06): từ ngày bắt đầu khóa, buổi hằng tuần còn hiệu lực (active_from / active_until),
     * bỏ buổi đã hủy, áp dụng buổi đã dời, thêm buổi riêng, đánh số theo thời gian và chỉ lấy tối đa total_sessions buổi.
     * dayOfWeek: 1 = Thứ Hai ... 7 = Chủ nhật (thứ của {@code date}).
     * @return 'TUTOR' nếu trùng lịch dạy của gia sư, 'LEARNER' nếu trùng lịch học của học viên; rỗng nếu không trùng
     */
    @Query(value = """
            WITH sp AS (
                SELECT ls.space_id, ls.tutor_id, ls.learner_id, ls.total_sessions,
                       COALESCE(ls.course_start_date, CAST(ls.created_at AT TIME ZONE 'Asia/Ho_Chi_Minh' AS date)) AS start_date
                FROM learning_spaces ls
                WHERE ls.status IN ('ACTIVE', 'PAUSED')
                  AND (ls.tutor_id = :tutorId OR ls.learner_id = :learnerId)
                  AND :dayOfWeek BETWEEN 1 AND 7
            ),
            occ AS (
                SELECT cs.space_id,
                       COALESCE(ch.new_date, d.day) AS session_date,
                       COALESCE(ch.start_time, cs.start_time) AS st,
                       COALESCE(ch.end_time, cs.end_time) AS et
                FROM sp
                JOIN class_schedules cs ON cs.space_id = sp.space_id
                CROSS JOIN LATERAL (
                    SELECT CAST(g AS date) AS day
                    FROM generate_series(CAST(sp.start_date AS timestamp),
                                         CAST(CAST(:date AS date) + 6 AS timestamp), interval '1 day') g
                ) d
                LEFT JOIN class_session_changes ch ON ch.schedule_id = cs.schedule_id AND ch.original_date = d.day
                WHERE EXTRACT(ISODOW FROM d.day) = cs.day_of_week
                  AND ((d.day + cs.start_time) AT TIME ZONE 'Asia/Ho_Chi_Minh') >= cs.active_from
                  AND (cs.active_until IS NULL OR ((d.day + cs.start_time) AT TIME ZONE 'Asia/Ho_Chi_Minh') < cs.active_until)
                  AND COALESCE(ch.cancelled, FALSE) = FALSE
                UNION ALL
                SELECT e.space_id, e.session_date, e.start_time, e.end_time
                FROM class_extra_sessions e
                JOIN sp ON sp.space_id = e.space_id
                WHERE e.session_date >= sp.start_date
                  AND e.session_date <= CAST(:date AS date) + 6
            ),
            numbered AS (
                SELECT occ.space_id, occ.session_date, occ.st, occ.et,
                       ROW_NUMBER() OVER (PARTITION BY occ.space_id ORDER BY occ.session_date, occ.st) AS rn
                FROM occ
            )
            SELECT CASE WHEN sp.tutor_id = :tutorId THEN 'TUTOR' ELSE 'LEARNER' END
            FROM numbered n
            JOIN sp ON sp.space_id = n.space_id
            WHERE (sp.total_sessions IS NULL OR n.rn <= sp.total_sessions)
              AND n.session_date = CAST(:date AS date)
              AND n.st < :endTime
              AND n.et > :startTime
            ORDER BY CASE WHEN sp.tutor_id = :tutorId THEN 0 ELSE 1 END
            LIMIT 1
            """, nativeQuery = true)
    java.util.Optional<String> findClassScheduleClash(@Param("tutorId") UUID tutorId,
                                                      @Param("learnerId") UUID learnerId,
                                                      @Param("date") java.time.LocalDate date,
                                                      @Param("dayOfWeek") int dayOfWeek,
                                                      @Param("startTime") java.time.LocalTime startTime,
                                                      @Param("endTime") java.time.LocalTime endTime);

    List<TrialBooking> findByBookingStatusInAndScheduledEndTimeBefore(List<TrialBookingStatus> statuses, OffsetDateTime time);
}

