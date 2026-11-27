package com.kizuna.features.progress.repository;

import com.kizuna.features.progress.entity.AssignmentFile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AssignmentFileRepository extends JpaRepository<AssignmentFile, UUID> {

    List<AssignmentFile> findByAssignmentIdOrderByCreatedAtAscFileNameAsc(UUID assignmentId);

    List<AssignmentFile> findByAssignmentIdInOrderByCreatedAtAscFileNameAsc(Collection<UUID> assignmentIds);

    Optional<AssignmentFile> findByFileIdAndAssignmentId(UUID fileId, UUID assignmentId);

    long countByAssignmentId(UUID assignmentId);

    /** Ghi tệp kèm nội dung nhị phân (cột file_data không ánh xạ vào entity). */
    @Modifying(flushAutomatically = true)
    @Query(value = """
            INSERT INTO assignment_files (file_id, assignment_id, file_name, file_size, file_data)
            VALUES (:fileId, :assignmentId, :fileName, :fileSize, :data)
            """, nativeQuery = true)
    int insertFile(@Param("fileId") UUID fileId, @Param("assignmentId") UUID assignmentId,
                   @Param("fileName") String fileName, @Param("fileSize") long fileSize, @Param("data") byte[] data);

    @Query(value = "SELECT file_data FROM assignment_files WHERE file_id = :fileId", nativeQuery = true)
    byte[] findFileData(@Param("fileId") UUID fileId);
}
