package com.kizuna.features.verification.repository;

import com.kizuna.features.verification.entity.TutorCredential;
import com.kizuna.features.verification.entity.TutorCredentialStatus;
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
public interface TutorCredentialRepository extends JpaRepository<TutorCredential, UUID> {

    List<TutorCredential> findByTutor_TutorIdOrderByUploadedAtAsc(UUID tutorId);

    Optional<TutorCredential> findByCredentialIdAndTutor_TutorId(UUID credentialId, UUID tutorId);

    @Modifying
    @Query("UPDATE TutorCredential c SET c.verifiedStatus = :newStatus "
            + "WHERE c.tutor.tutorId = :tutorId AND c.verifiedStatus = :currentStatus")
    int updateStatusForTutor(@Param("tutorId") UUID tutorId,
                             @Param("currentStatus") TutorCredentialStatus currentStatus,
                             @Param("newStatus") TutorCredentialStatus newStatus);

    /*
     * Nội dung tệp (cột file_data BYTEA) KHÔNG ánh xạ vào entity để danh sách giấy tờ
     * không phải tải dữ liệu nhị phân; chỉ đọc/ghi riêng qua các truy vấn dưới đây (cùng cách FE-06).
     */

    @Modifying(flushAutomatically = true)
    @Query(value = "UPDATE tutor_credentials SET file_data = :data WHERE credential_id = :credentialId", nativeQuery = true)
    int updateFileData(@Param("credentialId") UUID credentialId, @Param("data") byte[] data);

    @Query(value = "SELECT file_data FROM tutor_credentials WHERE credential_id = :credentialId", nativeQuery = true)
    byte[] findFileData(@Param("credentialId") UUID credentialId);

    /** Mã các giấy tờ (trong danh sách) đã có nội dung tệp trong DB — không tải dữ liệu nhị phân. */
    @Query(value = "SELECT CAST(credential_id AS VARCHAR) FROM tutor_credentials "
            + "WHERE credential_id IN (:credentialIds) AND file_data IS NOT NULL", nativeQuery = true)
    List<String> findIdsWithFileData(@Param("credentialIds") Collection<UUID> credentialIds);
}
