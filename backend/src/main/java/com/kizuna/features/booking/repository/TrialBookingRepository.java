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

    List<TrialBooking> findByBookingStatusInAndScheduledEndTimeBefore(List<TrialBookingStatus> statuses, OffsetDateTime time);
}

