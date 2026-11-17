package com.kizuna.features.booking.repository;

import com.kizuna.features.booking.entity.ConnectionRequestSlot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalTime;
import java.util.UUID;

@Repository
public interface ConnectionRequestSlotRepository extends JpaRepository<ConnectionRequestSlot, UUID> {

    /**
     * Ca hằng tuần của gia sư đã bị "khóa" bởi một kết nối trực tiếp đã được đồng ý (trừ yêu cầu excludeRequestId).
     * Ca được nhả khi cặp gia sư - học viên chỉ còn lớp đã kết thúc (COMPLETED/CLOSED), không còn lớp ACTIVE/PAUSED.
     * Truy vấn CHỈ ĐỌC learning_spaces (FE-06). dayOfWeek: 1 = Chủ nhật ... 7 = Thứ Bảy.
     */
    @Query(value = """
            SELECT EXISTS (
                SELECT 1
                FROM connection_request_slots s
                JOIN connection_requests cr ON cr.request_id = s.request_id
                WHERE cr.tutor_id = :tutorId
                  AND cr.request_type = 'DIRECT'
                  AND cr.status = 'ACCEPTED'
                  AND cr.request_id <> :excludeRequestId
                  AND s.day_of_week = :dayOfWeek
                  AND s.start_time < :endTime
                  AND s.end_time > :startTime
                  AND (
                      EXISTS (SELECT 1 FROM learning_spaces ls
                              WHERE ls.tutor_id = cr.tutor_id AND ls.learner_id = cr.learner_id
                                AND ls.status IN ('ACTIVE', 'PAUSED'))
                      OR NOT EXISTS (SELECT 1 FROM learning_spaces ls
                                     WHERE ls.tutor_id = cr.tutor_id AND ls.learner_id = cr.learner_id)
                  ))
            """, nativeQuery = true)
    boolean existsLockedSlotOverlap(@Param("tutorId") UUID tutorId,
                                    @Param("excludeRequestId") UUID excludeRequestId,
                                    @Param("dayOfWeek") int dayOfWeek,
                                    @Param("startTime") LocalTime startTime,
                                    @Param("endTime") LocalTime endTime);
}
