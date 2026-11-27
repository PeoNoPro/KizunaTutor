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

    /**
     * Đọc yêu cầu và khóa dòng (SELECT ... FOR UPDATE) tới hết transaction: hai thao tác cùng lúc trên một yêu cầu
     * (gia sư đồng ý / học viên hủy / bấm đúp) phải chờ nhau, lệnh sau đọc được trạng thái mới.
     */
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("SELECT cr FROM ConnectionRequest cr WHERE cr.requestId = :id")
    Optional<ConnectionRequest> findLockedById(@org.springframework.data.repository.query.Param("id") UUID id);

    /** Yêu cầu kết nối đang chờ đã quá hạn của một cặp học viên–gia sư (khóa dòng để tự hết hạn). */
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("SELECT cr FROM ConnectionRequest cr "
            + "WHERE cr.learner.learnerId = :learnerId AND cr.tutor.tutorId = :tutorId "
            + "AND cr.requestType = com.kizuna.features.booking.entity.ConnectionRequestType.DIRECT "
            // status truyền bằng tham số: hằng enum trong JPQL bị Hibernate ép kiểu '::ConnectionRequestStatus' (kiểu không tồn tại trong DB)
            + "AND cr.status = :status "
            + "AND cr.createdAt < :before")
    java.util.List<ConnectionRequest> findStaleDirectOfPairLocked(
            @org.springframework.data.repository.query.Param("learnerId") UUID learnerId,
            @org.springframework.data.repository.query.Param("tutorId") UUID tutorId,
            @org.springframework.data.repository.query.Param("status") ConnectionRequestStatus status,
            @org.springframework.data.repository.query.Param("before") java.time.OffsetDateTime before);

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

    /**
     * Yêu cầu kết nối trực tiếp đang chờ mà đã có buổi học bắt đầu (giờ Việt Nam).
     * Khóa dòng (tác vụ tự hết hạn): yêu cầu vừa được gia sư/học viên xử lý xong thì không còn khớp và bị bỏ qua.
     */
    @org.springframework.data.jpa.repository.Query(value = """
            SELECT cr.* FROM connection_requests cr
            WHERE cr.request_type = 'DIRECT' AND cr.status = 'PENDING'
              AND EXISTS (SELECT 1 FROM connection_request_sessions s
                          WHERE s.request_id = cr.request_id
                            AND (s.session_date + s.start_time) <= (CURRENT_TIMESTAMP AT TIME ZONE 'Asia/Ho_Chi_Minh'))
            FOR UPDATE OF cr
            """, nativeQuery = true)
    java.util.List<ConnectionRequest> findPendingDirectWithStartedSession();

    /** Khóa dòng (tác vụ tự hết hạn), như findPendingDirectWithStartedSession. */
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
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
