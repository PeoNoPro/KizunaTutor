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

    /** Các buổi học thử của một cặp gia sư – học viên theo trạng thái (dùng khi đã đồng ý kết nối). */
    List<TrialBooking> findByTutorTutorIdAndLearnerLearnerIdAndBookingStatusIn(UUID tutorId, UUID learnerId,
                                                                               List<TrialBookingStatus> statuses);

    /**
     * Truy vấn CHỈ ĐỌC lịch học chính thức hằng tuần (FE-06: class_schedules + learning_spaces, lớp ACTIVE/PAUSED)
     * của gia sư HOẶC học viên trùng khung giờ (theo giờ Việt Nam). dayOfWeek: 1 = Thứ Hai ... 7 = Chủ nhật.
     * @return 'TUTOR' nếu trùng lịch dạy của gia sư, 'LEARNER' nếu trùng lịch học của học viên; rỗng nếu không trùng
     */
    @Query(value = """
            SELECT CASE WHEN ls.tutor_id = :tutorId THEN 'TUTOR' ELSE 'LEARNER' END
            FROM class_schedules cs
            JOIN learning_spaces ls ON ls.space_id = cs.space_id
            WHERE ls.status IN ('ACTIVE', 'PAUSED')
              AND (ls.tutor_id = :tutorId OR ls.learner_id = :learnerId)
              AND cs.day_of_week = :dayOfWeek
              AND cs.start_time < :endTime
              AND cs.end_time > :startTime
            ORDER BY CASE WHEN ls.tutor_id = :tutorId THEN 0 ELSE 1 END
            LIMIT 1
            """, nativeQuery = true)
    java.util.Optional<String> findClassScheduleClash(@Param("tutorId") UUID tutorId,
                                                      @Param("learnerId") UUID learnerId,
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
                  AND tb.booking_status IN ('PENDING', 'CONFIRMED', 'RESCHEDULED')
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
    boolean existsByLearnerLearnerIdAndTutorTutorIdAndBookingStatusIn(UUID learnerId, UUID tutorId,
                                                                       java.util.Collection<TrialBookingStatus> statuses);

    /** Gia sư đã từ chối học viên này sau mốc thời gian cho trước hay chưa (thời gian chờ sau khi bị từ chối). */
    boolean existsByLearnerLearnerIdAndTutorTutorIdAndBookingStatusAndUpdatedAtAfter(UUID learnerId, UUID tutorId,
                                                                                    TrialBookingStatus status,
                                                                                    OffsetDateTime after);

    /**
     * Yêu cầu học thử đang chờ đã quá hạn: gia sư chưa phản hồi kể từ lần gửi/đổi lịch gần nhất (updatedAt)
     * trước mốc staleBefore, hoặc đã tới giờ học.
     */
    @Query("SELECT tb FROM TrialBooking tb " +
           "WHERE tb.bookingStatus = :status " +
           "AND (tb.updatedAt < :staleBefore OR tb.scheduledStartTime <= :now)")
    List<TrialBooking> findStalePending(@Param("status") TrialBookingStatus status,
                                        @Param("staleBefore") OffsetDateTime staleBefore,
                                        @Param("now") OffsetDateTime now);

    List<TrialBooking> findByBookingStatusInAndScheduledEndTimeBefore(List<TrialBookingStatus> statuses, OffsetDateTime time);
}

