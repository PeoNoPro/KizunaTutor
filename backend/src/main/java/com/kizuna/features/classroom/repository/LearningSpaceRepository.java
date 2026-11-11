package com.kizuna.features.classroom.repository;

import com.kizuna.features.classroom.entity.LearningSpace;
import com.kizuna.features.classroom.entity.LearningSpaceStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface LearningSpaceRepository extends JpaRepository<LearningSpace, UUID> {

    @EntityGraph(attributePaths = {"tutor", "tutor.user", "learner", "learner.user"})
    List<LearningSpace> findByTutor_TutorIdOrderByCreatedAtDesc(UUID tutorId);

    @EntityGraph(attributePaths = {"tutor", "tutor.user", "learner", "learner.user"})
    List<LearningSpace> findByLearner_LearnerIdOrderByCreatedAtDesc(UUID learnerId);

    @EntityGraph(attributePaths = {"tutor", "tutor.user", "learner", "learner.user"})
    Optional<LearningSpace> findWithParticipantsBySpaceId(UUID spaceId);

    /**
     * Khóa dòng lớp học (SELECT ... FOR UPDATE) tới hết transaction, để các thao tác thêm/sửa lịch
     * của cùng một lớp chạy lần lượt (chặn trường hợp bấm "Thêm buổi" 2 lần liên tiếp lọt buổi trùng giờ).
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s.spaceId FROM LearningSpace s WHERE s.spaceId = :spaceId")
    Optional<UUID> lockById(@Param("spaceId") UUID spaceId);

    boolean existsByTutor_TutorIdAndLearner_LearnerIdAndStatusIn(UUID tutorId, UUID learnerId,
                                                                 Collection<LearningSpaceStatus> statuses);

    /** Số lớp của học viên (theo user_id) đang ở các trạng thái cho trước; dùng để mở khóa công cụ học. */
    long countByLearner_User_UserIdAndStatusIn(UUID userId, Collection<LearningSpaceStatus> statuses);

    /**
     * Truy vấn CHỈ ĐỌC bảng connection_requests (thuộc tutor_search, không import entity của feature đó):
     * học viên đã được gia sư chấp nhận kết nối và chưa có lớp đang hoạt động/tạm dừng với gia sư này.
     */
    @Query(value = """
            SELECT DISTINCT cr.learner_id
            FROM connection_requests cr
            WHERE cr.tutor_id = :tutorId
              AND cr.status = 'ACCEPTED'
              AND NOT EXISTS (
                  SELECT 1 FROM learning_spaces ls
                  WHERE ls.tutor_id = cr.tutor_id
                    AND ls.learner_id = cr.learner_id
                    AND ls.status IN ('ACTIVE', 'PAUSED'))
            """, nativeQuery = true)
    List<UUID> findEligibleLearnerIds(@Param("tutorId") UUID tutorId);

    /** Truy vấn CHỈ ĐỌC: cặp gia sư - học viên có kết nối đã được chấp nhận hay không. */
    @Query(value = """
            SELECT EXISTS (
                SELECT 1 FROM connection_requests cr
                WHERE cr.tutor_id = :tutorId
                  AND cr.learner_id = :learnerId
                  AND cr.status = 'ACCEPTED')
            """, nativeQuery = true)
    boolean hasAcceptedConnection(@Param("tutorId") UUID tutorId, @Param("learnerId") UUID learnerId);
}
