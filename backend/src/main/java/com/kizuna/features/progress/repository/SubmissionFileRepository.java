package com.kizuna.features.progress.repository;

import com.kizuna.features.progress.entity.SubmissionFile;
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
public interface SubmissionFileRepository extends JpaRepository<SubmissionFile, UUID> {

    List<SubmissionFile> findBySubmissionIdInOrderByCreatedAtAscFileNameAsc(Collection<UUID> submissionIds);

    List<SubmissionFile> findBySubmissionIdOrderByCreatedAtAscFileNameAsc(UUID submissionId);

    Optional<SubmissionFile> findByFileIdAndSubmissionId(UUID fileId, UUID submissionId);

    /** Ghi tệp kèm nội dung nhị phân (cột file_data không ánh xạ vào entity). */
    @Modifying(flushAutomatically = true)
    @Query(value = """
            INSERT INTO submission_files (file_id, submission_id, file_name, file_size, file_data)
            VALUES (:fileId, :submissionId, :fileName, :fileSize, :data)
            """, nativeQuery = true)
    int insertFile(@Param("fileId") UUID fileId, @Param("submissionId") UUID submissionId,
                   @Param("fileName") String fileName, @Param("fileSize") long fileSize, @Param("data") byte[] data);

    @Query(value = "SELECT file_data FROM submission_files WHERE file_id = :fileId", nativeQuery = true)
    byte[] findFileData(@Param("fileId") UUID fileId);
}
