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

    List<ClassSchedule> findBySpace_SpaceIdOrderByDayOfWeekAscStartTimeAsc(UUID spaceId);

    Optional<ClassSchedule> findByScheduleIdAndSpace_SpaceId(UUID scheduleId, UUID spaceId);

    long countBySpace_SpaceId(UUID spaceId);

    /** Lịch của các lớp đang mở mà gia sư HOẶC học viên này tham gia — dùng để kiểm tra trùng giờ. */
    @Query("""
            SELECT s FROM ClassSchedule s JOIN FETCH s.space sp
            WHERE sp.status IN :statuses
              AND (sp.tutor.tutorId = :tutorId OR sp.learner.learnerId = :learnerId)
            """)
    List<ClassSchedule> findOpenSchedulesOfParticipants(@Param("tutorId") UUID tutorId,
                                                        @Param("learnerId") UUID learnerId,
                                                        @Param("statuses") Collection<LearningSpaceStatus> statuses);
}
