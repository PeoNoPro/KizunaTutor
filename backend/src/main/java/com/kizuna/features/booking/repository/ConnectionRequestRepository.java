package com.kizuna.features.booking.repository;

import com.kizuna.features.booking.entity.ConnectionRequest;
import com.kizuna.features.booking.entity.ConnectionRequestStatus;
import com.kizuna.features.booking.entity.ConnectionRequestType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ConnectionRequestRepository extends JpaRepository<ConnectionRequest, UUID> {

    Page<ConnectionRequest> findByLearnerLearnerIdAndStatus(UUID learnerId, ConnectionRequestStatus status, Pageable pageable);

    Page<ConnectionRequest> findByLearnerLearnerId(UUID learnerId, Pageable pageable);

    Page<ConnectionRequest> findByTutorTutorIdAndStatus(UUID tutorId, ConnectionRequestStatus status, Pageable pageable);

    Page<ConnectionRequest> findByTutorTutorId(UUID tutorId, Pageable pageable);

    /** Một cặp có thể có nhiều kết nối ACCEPTED (học thử + trực tiếp), nên chỉ lấy bản ghi đầu tiên. */
    Optional<ConnectionRequest> findFirstByLearnerLearnerIdAndTutorTutorIdAndStatus(UUID learnerId, UUID tutorId, ConnectionRequestStatus status);

    /**
     * Học viên và gia sư ĐANG kết nối: có yêu cầu kết nối trực tiếp (DIRECT) đã được nhận và còn buổi học chưa kết thúc.
     * Yêu cầu học thử (TRIAL) đã nhận / đã học xong KHÔNG tính là đã kết nối.
     */
    @org.springframework.data.jpa.repository.Query(value = """
            SELECT EXISTS (
                SELECT 1 FROM connection_requests cr
                JOIN connection_request_sessions s ON s.request_id = cr.request_id
                WHERE cr.learner_id = :learnerId AND cr.tutor_id = :tutorId
                  AND cr.request_type = 'DIRECT' AND cr.status = 'ACCEPTED'
                  AND (s.session_date + s.end_time) > (CURRENT_TIMESTAMP AT TIME ZONE 'Asia/Ho_Chi_Minh'))
            """, nativeQuery = true)
    boolean existsActiveDirectConnection(@org.springframework.data.repository.query.Param("learnerId") UUID learnerId,
                                         @org.springframework.data.repository.query.Param("tutorId") UUID tutorId);

    boolean existsByLearnerLearnerIdAndTutorTutorIdAndRequestTypeAndStatus(UUID learnerId, UUID tutorId,
                                                                          ConnectionRequestType requestType,
                                                                          ConnectionRequestStatus status);

    /** Gia sư đã từ chối yêu cầu loại này của học viên sau mốc thời gian cho trước hay chưa. */
    boolean existsByLearnerLearnerIdAndTutorTutorIdAndRequestTypeAndStatusAndRespondedAtAfter(
            UUID learnerId, UUID tutorId, ConnectionRequestType requestType,
            ConnectionRequestStatus status, java.time.OffsetDateTime after);

    /** Yêu cầu loại này đang ở trạng thái cho trước và được tạo trước mốc thời gian (dùng để tự hết hạn). */
    /** Yêu cầu loại này của một học viên theo trạng thái (dùng để tự hủy yêu cầu đang chờ bị trùng khi học viên đã kết nối). */
    java.util.List<ConnectionRequest> findByLearnerLearnerIdAndRequestTypeAndStatus(
            UUID learnerId, ConnectionRequestType requestType, ConnectionRequestStatus status);

    /**
     * Gia sư có buổi học kết nối SẮP TỚI trong ca hằng tuần này không (yêu cầu đã đồng ý).
     * dayOfWeek: 1 = Chủ nhật ... 7 = Thứ Bảy (giờ Việt Nam). Dùng để chặn xóa / tắt ca rảnh đang có học viên.
     */
    @org.springframework.data.jpa.repository.Query(value = """
            SELECT EXISTS (
                SELECT 1 FROM connection_request_sessions s
                JOIN connection_requests cr ON cr.request_id = s.request_id
                WHERE cr.tutor_id = :tutorId AND cr.request_type = 'DIRECT'
                  AND cr.status = 'ACCEPTED'
                  AND (s.session_date + s.start_time) > (CURRENT_TIMESTAMP AT TIME ZONE 'Asia/Ho_Chi_Minh')
                  AND (EXTRACT(DOW FROM s.session_date) + 1) = :dayOfWeek
                  AND s.start_time < :endTime AND s.end_time > :startTime)
            """, nativeQuery = true)
    boolean existsUpcomingSessionInWeeklySlot(@org.springframework.data.repository.query.Param("tutorId") UUID tutorId,
                                              @org.springframework.data.repository.query.Param("dayOfWeek") int dayOfWeek,
                                              @org.springframework.data.repository.query.Param("startTime") java.time.LocalTime startTime,
                                              @org.springframework.data.repository.query.Param("endTime") java.time.LocalTime endTime);

    /** Yêu cầu kết nối trực tiếp đang chờ mà đã có buổi học bắt đầu (giờ Việt Nam). */
    @org.springframework.data.jpa.repository.Query(value = """
            SELECT cr.* FROM connection_requests cr
            WHERE cr.request_type = 'DIRECT' AND cr.status = 'PENDING'
              AND EXISTS (SELECT 1 FROM connection_request_sessions s
                          WHERE s.request_id = cr.request_id
                            AND (s.session_date + s.start_time) <= (CURRENT_TIMESTAMP AT TIME ZONE 'Asia/Ho_Chi_Minh'))
            """, nativeQuery = true)
    java.util.List<ConnectionRequest> findPendingDirectWithStartedSession();

    java.util.List<ConnectionRequest> findByRequestTypeAndStatusAndCreatedAtBefore(
            ConnectionRequestType requestType, ConnectionRequestStatus status, java.time.OffsetDateTime before);

    @EntityGraph(attributePaths = {"learner", "learner.user", "tutor", "tutor.user"})
    Page<ConnectionRequest> findByLearnerLearnerIdAndRequestType(UUID learnerId, ConnectionRequestType requestType, Pageable pageable);

    @EntityGraph(attributePaths = {"learner", "learner.user", "tutor", "tutor.user"})
    Page<ConnectionRequest> findByLearnerLearnerIdAndRequestTypeAndStatus(UUID learnerId, ConnectionRequestType requestType,
                                                                         ConnectionRequestStatus status, Pageable pageable);

    @EntityGraph(attributePaths = {"learner", "learner.user", "tutor", "tutor.user"})
    Page<ConnectionRequest> findByTutorTutorIdAndRequestType(UUID tutorId, ConnectionRequestType requestType, Pageable pageable);

    @EntityGraph(attributePaths = {"learner", "learner.user", "tutor", "tutor.user"})
    Page<ConnectionRequest> findByTutorTutorIdAndRequestTypeAndStatus(UUID tutorId, ConnectionRequestType requestType,
                                                                     ConnectionRequestStatus status, Pageable pageable);
}
