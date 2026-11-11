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

    boolean existsByLearnerLearnerIdAndTutorTutorIdAndStatus(UUID learnerId, UUID tutorId, ConnectionRequestStatus status);

    boolean existsByLearnerLearnerIdAndTutorTutorIdAndRequestTypeAndStatus(UUID learnerId, UUID tutorId,
                                                                          ConnectionRequestType requestType,
                                                                          ConnectionRequestStatus status);

    /** Gia sư đã từ chối yêu cầu loại này của học viên sau mốc thời gian cho trước hay chưa. */
    boolean existsByLearnerLearnerIdAndTutorTutorIdAndRequestTypeAndStatusAndRespondedAtAfter(
            UUID learnerId, UUID tutorId, ConnectionRequestType requestType,
            ConnectionRequestStatus status, java.time.OffsetDateTime after);

    /** Yêu cầu loại này đang ở trạng thái cho trước và được tạo trước mốc thời gian (dùng để tự hết hạn). */
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
