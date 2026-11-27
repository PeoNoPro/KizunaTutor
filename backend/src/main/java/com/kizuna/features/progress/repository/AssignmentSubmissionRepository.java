package com.kizuna.features.progress.repository;

import com.kizuna.features.progress.entity.AssignmentSubmission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AssignmentSubmissionRepository extends JpaRepository<AssignmentSubmission, UUID> {

    /** Các lần nộp của một học viên cho một bài tập, cũ → mới. */
    List<AssignmentSubmission> findByAssignment_AssignmentIdAndLearner_LearnerIdOrderByAttemptNumberAsc(
            UUID assignmentId, UUID learnerId);

    Optional<AssignmentSubmission> findBySubmissionIdAndAssignment_AssignmentId(UUID submissionId, UUID assignmentId);

    boolean existsByAssignment_AssignmentIdAndFinalizedTrue(UUID assignmentId);

    /*
     * Nội dung tệp (cột file_data BYTEA) không ánh xạ vào entity; chỉ đọc/ghi qua hai truy vấn dưới đây.
     */

    @Modifying(flushAutomatically = true)
    @Query(value = "UPDATE assignment_submissions SET file_data = :data WHERE submission_id = :submissionId",
            nativeQuery = true)
    int updateFileData(@Param("submissionId") UUID submissionId, @Param("data") byte[] data);

    @Query(value = "SELECT file_data FROM assignment_submissions WHERE submission_id = :submissionId", nativeQuery = true)
    byte[] findFileData(@Param("submissionId") UUID submissionId);
}
