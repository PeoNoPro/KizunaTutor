package com.kizuna.features.progress.repository;

import com.kizuna.features.progress.entity.Assignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AssignmentRepository extends JpaRepository<Assignment, UUID> {

    List<Assignment> findBySpace_SpaceIdOrderByCreatedAtDesc(UUID spaceId);

    Optional<Assignment> findByAssignmentIdAndSpace_SpaceId(UUID assignmentId, UUID spaceId);

    /*
     * Nội dung tệp đề bài (cột file_data BYTEA) không ánh xạ vào entity; chỉ đọc/ghi qua hai truy vấn dưới đây.
     */

    @Modifying(flushAutomatically = true)
    @Query(value = "UPDATE assignments SET file_data = :data WHERE assignment_id = :assignmentId", nativeQuery = true)
    int updateFileData(@Param("assignmentId") UUID assignmentId, @Param("data") byte[] data);

    @Query(value = "SELECT file_data FROM assignments WHERE assignment_id = :assignmentId", nativeQuery = true)
    byte[] findFileData(@Param("assignmentId") UUID assignmentId);
}
