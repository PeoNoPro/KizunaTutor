package com.kizuna.features.classroom.repository;


import com.kizuna.core.entity.*;
import com.kizuna.features.classroom.entity.KanjiFolder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface KanjiFolderRepository extends JpaRepository<KanjiFolder, UUID> {

    List<KanjiFolder> findByUserUserIdOrderByCreatedAtDesc(UUID userId);

    List<KanjiFolder> findAllByOrderByCreatedAtDesc();

    @Query("SELECT COUNT(i) FROM KanjiFolderItem i WHERE i.folder.folderId = :folderId")
    long countItemsByFolderId(@Param("folderId") UUID folderId);
}
