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

    Page<TrialBooking> findByLearnerLearnerIdAndBookingStatus(UUID learnerId, TrialBookingStatus status, Pageable pageable);

    Page<TrialBooking> findByLearnerLearnerId(UUID learnerId, Pageable pageable);

    Page<TrialBooking> findByTutorTutorIdAndBookingStatus(UUID tutorId, TrialBookingStatus status, Pageable pageable);

    Page<TrialBooking> findByTutorTutorId(UUID tutorId, Pageable pageable);

    List<TrialBooking> findByTutorTutorIdAndBookingStatusIn(UUID tutorId, List<TrialBookingStatus> statuses);
}

