package com.kizuna.features.booking.repository;

import com.kizuna.features.booking.entity.TrialBooking;
import com.kizuna.features.booking.entity.TrialBookingStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface TrialBookingRepository extends JpaRepository<TrialBooking, UUID> {

    /**
     * Đọc buổi học thử và khóa dòng (SELECT ... FOR UPDATE) tới hết transaction: hai thao tác cùng lúc
     * (gia sư chấp nhận / học viên hủy / bấm đúp) phải chờ nhau, lệnh sau đọc được trạng thái mới.
     */
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT tb FROM TrialBooking tb WHERE tb.bookingId = :id")
    java.util.Optional<TrialBooking> findLockedById(@Param("id") UUID id);

    /** Yêu cầu học thử đang chờ đã quá hạn của một học viên (khóa dòng để tự hết hạn), cùng điều kiện với findStalePending. */
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT tb FROM TrialBooking tb " +
           "WHERE tb.learner.learnerId = :learnerId AND tb.bookingStatus = :status " +
           "AND (tb.updatedAt < :staleBefore OR tb.scheduledStartTime <= :now)")
    List<TrialBooking> findStalePendingOfLearnerLocked(@Param("learnerId") UUID learnerId,
                                                       @Param("status") TrialBookingStatus status,
                                                       @Param("staleBefore") OffsetDateTime staleBefore,
                                                       @Param("now") OffsetDateTime now);

    /**
     * Gia sư đã có buổi học thử ĐÃ XÁC NHẬN (CONFIRMED/RESCHEDULED) khác chồng lấn khung giờ này hay chưa.
     * Yêu cầu đang chờ (PENDING) được phép trùng nhau vì học viên tự chọn giờ; trùng lịch chỉ bị chặn
     * khi gia sư bấm đồng ý hoặc khi dời lịch.
     */
    @Query("SELECT COUNT(tb) > 0 FROM TrialBooking tb " +
           "WHERE tb.tutor.tutorId = :tutorId " +
           "AND tb.bookingId <> :excludeBookingId " +
           "AND tb.bookingStatus IN ('CONFIRMED', 'RESCHEDULED') " +
           "AND tb.scheduledStartTime < :newEndTime " +
           "AND tb.scheduledEndTime > :newStartTime")
    boolean existsConfirmedTutorOverlap(
            @Param("tutorId") UUID tutorId,
            @Param("excludeBookingId") UUID excludeBookingId,
            @Param("newStartTime") OffsetDateTime newStartTime,
            @Param("newEndTime") OffsetDateTime newEndTime);

    /** Học viên đã có buổi học thử ĐÃ XÁC NHẬN (CONFIRMED/RESCHEDULED) khác chồng lấn khung giờ này hay chưa. */
    @Query("SELECT COUNT(tb) > 0 FROM TrialBooking tb " +
           "WHERE tb.learner.learnerId = :learnerId " +
           "AND tb.bookingId <> :excludeBookingId " +
           "AND tb.bookingStatus IN ('CONFIRMED', 'RESCHEDULED') " +
           "AND tb.scheduledStartTime < :newEndTime " +
           "AND tb.scheduledEndTime > :newStartTime")
    boolean existsConfirmedLearnerOverlap(
            @Param("learnerId") UUID learnerId,
            @Param("excludeBookingId") UUID excludeBookingId,
            @Param("newStartTime") OffsetDateTime newStartTime,
            @Param("newEndTime") OffsetDateTime newEndTime);

    @EntityGraph(attributePaths = {"learner", "learner.user", "tutor", "tutor.user", "connectionRequest"})
    Page<TrialBooking> findByLearnerLearnerIdAndBookingStatus(UUID learnerId, TrialBookingStatus status, Pageable pageable);

    @EntityGraph(attributePaths = {"learner", "learner.user", "tutor", "tutor.user", "connectionRequest"})
    Page<TrialBooking> findByLearnerLearnerId(UUID learnerId, Pageable pageable);

    @EntityGraph(attributePaths = {"learner", "learner.user", "tutor", "tutor.user", "connectionRequest"})
    Page<TrialBooking> findByTutorTutorIdAndBookingStatus(UUID tutorId, TrialBookingStatus status, Pageable pageable);

    @EntityGraph(attributePaths = {"learner", "learner.user", "tutor", "tutor.user", "connectionRequest"})
    Page<TrialBooking> findByTutorTutorId(UUID tutorId, Pageable pageable);

    List<TrialBooking> findByTutorTutorIdAndBookingStatusIn(UUID tutorId, List<TrialBookingStatus> statuses);

    /** Buổi học thử của một học viên theo trạng thái (dùng để tự hủy yêu cầu đang chờ bị trùng khi học viên đã kết nối). */
    List<TrialBooking> findByLearnerLearnerIdAndBookingStatusIn(UUID learnerId, List<TrialBookingStatus> statuses);

    /** Các buổi học thử của một cặp gia sư – học viên theo trạng thái (dùng khi đã đồng ý kết nối). */
    List<TrialBooking> findByTutorTutorIdAndLearnerLearnerIdAndBookingStatusIn(UUID tutorId, UUID learnerId,
                                                                               List<TrialBookingStatus> statuses);

    /**
     * Truy vấn CHỈ ĐỌC lịch học chính thức (FE-06) của gia sư HOẶC học viên vào NGÀY {@code date} (giờ Việt Nam),
     * trùng khung giờ [startTime, endTime). Sinh các buổi CỤ THỂ của khóa theo đúng quy tắc của
     * ClassSessionCalendar (FE-06): từ ngày bắt đầu khóa, buổi hằng tuần còn hiệu lực (active_from / active_until),
     * bỏ buổi đã hủy, áp dụng buổi đã dời (có thể sang tuần khác), thêm buổi riêng, đánh số theo thời gian thực tế
     * và chỉ lấy tối đa total_sessions buổi.
     * dayOfWeek: 1 = Thứ Hai ... 7 = Chủ nhật (thứ của {@code date}).
     * @return 'TUTOR' nếu trùng lịch dạy của gia sư, 'LEARNER' nếu trùng lịch học của học viên; rỗng nếu không trùng
     */
    @Query(value = """
            WITH sp AS (
                SELECT ls.space_id, ls.tutor_id, ls.learner_id, ls.total_sessions,
                       COALESCE(ls.course_start_date, CAST(ls.created_at AT TIME ZONE 'Asia/Ho_Chi_Minh' AS date)) AS start_date,
                       GREATEST(CAST(:date AS date), COALESCE((
                           SELECT MAX(ch.original_date) FROM class_session_changes ch
                           JOIN class_schedules cs2 ON cs2.schedule_id = ch.schedule_id
                           WHERE cs2.space_id = ls.space_id), CAST(:date AS date))) AS horizon
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
                                         CAST(sp.horizon AS timestamp), interval '1 day') g
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

    /**
     * Gia sư có buổi học thử còn hiệu lực (của học viên KHÁC) rơi vào ca hằng tuần này, tính từ ngày {@code fromDate}
     * trở đi (theo giờ Việt Nam). pgDow theo EXTRACT(DOW): 0 = Chủ nhật ... 6 = Thứ Bảy.
     * Buổi học thử của chính học viên không tính vì sẽ tự hủy khi gia sư đồng ý kết nối.
     */
    @Query(value = """
            SELECT EXISTS (
                SELECT 1 FROM trial_bookings tb
                WHERE tb.tutor_id = :tutorId
                  AND tb.learner_id <> :learnerId
                  AND tb.booking_status IN ('CONFIRMED', 'RESCHEDULED')
                  AND tb.scheduled_end_time > CURRENT_TIMESTAMP
                  AND CAST(tb.scheduled_start_time AT TIME ZONE 'Asia/Ho_Chi_Minh' AS date) >= :fromDate
                  AND EXTRACT(DOW FROM tb.scheduled_start_time AT TIME ZONE 'Asia/Ho_Chi_Minh') = :pgDow
                  AND CAST(tb.scheduled_start_time AT TIME ZONE 'Asia/Ho_Chi_Minh' AS time) < :endTime
                  AND (CAST(tb.scheduled_end_time AT TIME ZONE 'Asia/Ho_Chi_Minh' AS time) > :startTime
                       OR CAST(tb.scheduled_end_time AT TIME ZONE 'Asia/Ho_Chi_Minh' AS date)
                          > CAST(tb.scheduled_start_time AT TIME ZONE 'Asia/Ho_Chi_Minh' AS date))
            )
            """, nativeQuery = true)
    boolean existsTrialClashFrom(@Param("tutorId") UUID tutorId,
                                 @Param("learnerId") UUID learnerId,
                                 @Param("fromDate") java.time.LocalDate fromDate,
                                 @Param("pgDow") int pgDow,
                                 @Param("startTime") java.time.LocalTime startTime,
                                 @Param("endTime") java.time.LocalTime endTime);

    /**
     * Gia sư đã có buổi học của yêu cầu kết nối trực tiếp vào ngày/giờ này (giờ Việt Nam):
     * buổi cụ thể của yêu cầu ACCEPTED (và PENDING còn hạn nếu includePending), hoặc ca hằng tuần kiểu cũ
     * (yêu cầu ACCEPTED chưa có buổi cụ thể). pgDow theo EXTRACT(DOW): 0 = Chủ nhật ... 6 = Thứ Bảy.
     */
    @Query(value = """
            SELECT EXISTS (
                SELECT 1 FROM connection_request_sessions s
                JOIN connection_requests cr ON cr.request_id = s.request_id
                WHERE cr.tutor_id = :tutorId AND cr.request_type = 'DIRECT'
                  AND (cr.status = 'ACCEPTED'
                       OR (:includePending AND cr.status = 'PENDING'
                           AND cr.created_at > CURRENT_TIMESTAMP - (:pendingHours * INTERVAL '1 hour')))
                  AND s.session_date = :date AND s.start_time < :endTime AND s.end_time > :startTime
            ) OR EXISTS (
                SELECT 1 FROM connection_request_slots sl
                JOIN connection_requests cr ON cr.request_id = sl.request_id
                WHERE cr.tutor_id = :tutorId AND cr.request_type = 'DIRECT' AND cr.status = 'ACCEPTED'
                  AND NOT EXISTS (SELECT 1 FROM connection_request_sessions x WHERE x.request_id = cr.request_id)
                  AND sl.day_of_week = :pgDow + 1 AND sl.start_time < :endTime AND sl.end_time > :startTime
                  AND (EXISTS (SELECT 1 FROM learning_spaces ls WHERE ls.tutor_id = cr.tutor_id
                               AND ls.learner_id = cr.learner_id AND ls.status IN ('ACTIVE', 'PAUSED'))
                       OR NOT EXISTS (SELECT 1 FROM learning_spaces ls WHERE ls.tutor_id = cr.tutor_id
                                      AND ls.learner_id = cr.learner_id))
            )
            """, nativeQuery = true)
    boolean existsConnectionSessionClash(@Param("tutorId") UUID tutorId,
                                         @Param("date") java.time.LocalDate date,
                                         @Param("pgDow") int pgDow,
                                         @Param("startTime") java.time.LocalTime startTime,
                                         @Param("endTime") java.time.LocalTime endTime,
                                         @Param("includePending") boolean includePending,
                                         @Param("pendingHours") int pendingHours);

    /** Số lần học viên đề xuất học thử NGOÀI lịch rảnh với gia sư này kể từ mốc thời gian cho trước. */
    @Query(value = """
            SELECT COUNT(*) FROM trial_bookings
            WHERE learner_id = :learnerId AND tutor_id = :tutorId AND is_off_schedule AND created_at > :since
            """, nativeQuery = true)
    long countOffScheduleSince(@Param("learnerId") UUID learnerId,
                               @Param("tutorId") UUID tutorId,
                               @Param("since") OffsetDateTime since);

    /**
     * Số lần học viên ĐỔI LỊCH buổi học thử sang giờ ngoài lịch rảnh với gia sư này kể từ mốc cho trước
     * (dòng lịch sử bắt đầu bằng TrialBookingServiceImpl.OFF_SCHEDULE_RESCHEDULE_MARK).
     */
    @Query(value = """
            SELECT COUNT(*) FROM trial_booking_histories h
            JOIN trial_bookings tb ON tb.booking_id = h.booking_id
            WHERE tb.learner_id = :learnerId AND tb.tutor_id = :tutorId
              AND h.change_reason LIKE '[đề xuất ngoài lịch rảnh]%' AND h.created_at > :since
            """, nativeQuery = true)
    long countOffScheduleReschedulesSince(@Param("learnerId") UUID learnerId,
                                          @Param("tutorId") UUID tutorId,
                                          @Param("since") OffsetDateTime since);

    /** Khung giờ [startTime, endTime] nằm trọn trong một ca rảnh đang bật của gia sư (dayOfWeek: 1 = Chủ nhật ... 7 = Thứ Bảy). */
    @Query(value = """
            SELECT EXISTS (
                SELECT 1 FROM tutor_availabilities a
                WHERE a.tutor_id = :tutorId AND a.is_active = TRUE AND a.day_of_week = :dayOfWeek
                  AND a.start_time <= :startTime
                  AND (a.end_time >= :endTime OR a.end_time = TIME '00:00')
            )
            """, nativeQuery = true)
    boolean isWithinAvailability(@Param("tutorId") UUID tutorId,
                                 @Param("dayOfWeek") int dayOfWeek,
                                 @Param("startTime") java.time.LocalTime startTime,
                                 @Param("endTime") java.time.LocalTime endTime);

    /** Cặp học viên - gia sư đang có buổi học thử ở một trong các trạng thái (dùng để chặn gửi trùng). */
    /** Số yêu cầu học thử của học viên đang ở trạng thái cho trước (với mọi gia sư). */
    long countByLearnerLearnerIdAndBookingStatus(UUID learnerId, TrialBookingStatus status);

    boolean existsByLearnerLearnerIdAndTutorTutorIdAndBookingStatusIn(UUID learnerId, UUID tutorId,
                                                                       java.util.Collection<TrialBookingStatus> statuses);

    /** Gia sư đã từ chối học viên này sau mốc thời gian cho trước hay chưa (thời gian chờ sau khi bị từ chối). */
    boolean existsByLearnerLearnerIdAndTutorTutorIdAndBookingStatusAndUpdatedAtAfter(UUID learnerId, UUID tutorId,
                                                                                    TrialBookingStatus status,
                                                                                    OffsetDateTime after);

    /**
     * Yêu cầu học thử đang chờ đã quá hạn: gia sư chưa phản hồi kể từ lần gửi/đổi lịch gần nhất (updatedAt)
     * trước mốc staleBefore, hoặc đã tới giờ học.
     * Khóa dòng (tác vụ tự hết hạn): yêu cầu vừa được gia sư/học viên xử lý xong thì không còn khớp và bị bỏ qua.
     */
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT tb FROM TrialBooking tb " +
           "WHERE tb.bookingStatus = :status " +
           "AND (tb.updatedAt < :staleBefore OR tb.scheduledStartTime <= :now)")
    List<TrialBooking> findStalePending(@Param("status") TrialBookingStatus status,
                                        @Param("staleBefore") OffsetDateTime staleBefore,
                                        @Param("now") OffsetDateTime now);

    /** Khóa dòng (tác vụ tự hoàn thành): buổi vừa bị hủy / dời lịch thì không còn khớp và bị bỏ qua. */
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    List<TrialBooking> findByBookingStatusInAndScheduledEndTimeBefore(List<TrialBookingStatus> statuses, OffsetDateTime time);
}

