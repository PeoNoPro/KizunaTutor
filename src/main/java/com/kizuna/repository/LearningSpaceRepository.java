package com.kizuna.repository;

import com.kizuna.entity.LearningSpace;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface LearningSpaceRepository extends JpaRepository<LearningSpace, UUID> {

    /**
     * Kiểm tra Học Viên (theo userId) có thuộc bất kỳ Không gian học tập (Learning Space)
     * đang hoạt động (ACTIVE) cùng Gia sư hay không.
     */
    @Query(value = "SELECT COUNT(ls.space_id) > 0 FROM learning_spaces ls " +
                   "JOIN learners l ON l.learner_id = ls.learner_id " +
                   "WHERE l.user_id = :userId AND ls.status = 'ACTIVE'", nativeQuery = true)
    boolean isStudentAssignedToAnyTutor(@Param("userId") UUID userId);

    /**
     * Kiểm tra Học Viên (theo email) có thuộc bất kỳ Không gian học tập nào đang hoạt động hay không.
     */
    @Query(value = "SELECT COUNT(ls.space_id) > 0 FROM learning_spaces ls " +
                   "JOIN learners l ON l.learner_id = ls.learner_id " +
                   "JOIN users u ON u.user_id = l.user_id " +
                   "WHERE u.email = :email AND ls.status = 'ACTIVE'", nativeQuery = true)
    boolean isStudentAssignedByEmail(@Param("email") String email);

    @org.springframework.transaction.annotation.Transactional
    @org.springframework.data.jpa.repository.Modifying
    @Query(value = "DELETE FROM learning_spaces WHERE learner_id IN (SELECT l.learner_id FROM learners l JOIN users u ON u.user_id = l.user_id WHERE u.email = :email)", nativeQuery = true)
    void deleteByLearnerEmail(@Param("email") String email);

    List<LearningSpace> findByLearner_User_UserId(UUID userId);

    @Query("SELECT COUNT(ls) FROM LearningSpace ls WHERE ls.tutor.user.email = :email AND ls.status = 'ACTIVE'")
    long countActiveSpacesByTutorEmail(@Param("email") String email);

    @Query("SELECT ls FROM LearningSpace ls WHERE ls.tutor.user.email = :email AND ls.status = 'ACTIVE'")
    List<LearningSpace> findActiveSpacesByTutorEmail(@Param("email") String email);

    @Query("SELECT COUNT(ls) FROM LearningSpace ls WHERE ls.learner.user.email = :email AND ls.status = 'ACTIVE'")
    long countActiveSpacesByLearnerEmail(@Param("email") String email);

    @Query("SELECT ls FROM LearningSpace ls WHERE ls.learner.user.email = :email AND ls.status = 'ACTIVE'")
    List<LearningSpace> findActiveSpacesByLearnerEmail(@Param("email") String email);
}
