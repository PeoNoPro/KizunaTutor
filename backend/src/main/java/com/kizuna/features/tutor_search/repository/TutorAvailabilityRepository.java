package com.kizuna.features.tutor_search.repository;

import com.kizuna.features.tutor_search.entity.TutorAvailability;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TutorAvailabilityRepository extends JpaRepository<TutorAvailability, UUID> {

    List<TutorAvailability> findByTutor_TutorIdOrderByDayOfWeekAscStartTimeAsc(UUID tutorId);

    List<TutorAvailability> findByTutor_TutorIdAndIsActiveTrueOrderByDayOfWeekAscStartTimeAsc(UUID tutorId);

    Optional<TutorAvailability> findByAvailabilityIdAndTutor_TutorId(UUID availabilityId, UUID tutorId);

    @Query("SELECT CASE WHEN COUNT(a) > 0 THEN TRUE ELSE FALSE END FROM TutorAvailability a " +
           "WHERE a.tutor.tutorId = :tutorId AND a.dayOfWeek = :dayOfWeek " +
           "AND a.isActive = true AND a.startTime < :endTime AND a.endTime > :startTime")
    boolean hasOverlappingSlot(@Param("tutorId") UUID tutorId,
                               @Param("dayOfWeek") Integer dayOfWeek,
                               @Param("startTime") LocalTime startTime,
                               @Param("endTime") LocalTime endTime);

    long countByTutor_TutorIdAndIsActiveTrue(UUID tutorId);

    @Query("SELECT CASE WHEN COUNT(a) > 0 THEN TRUE ELSE FALSE END FROM TutorAvailability a " +
           "WHERE a.tutor.tutorId = :tutorId AND a.dayOfWeek = :dayOfWeek AND a.isActive = true " +
           "AND a.startTime <= :startTime AND a.endTime >= :endTime")
    boolean isWithinActiveAvailability(@Param("tutorId") UUID tutorId,
                                       @Param("dayOfWeek") Integer dayOfWeek,
                                       @Param("startTime") LocalTime startTime,
                                       @Param("endTime") LocalTime endTime);

    void deleteByAvailabilityIdAndTutor_TutorId(UUID availabilityId, UUID tutorId);
}

