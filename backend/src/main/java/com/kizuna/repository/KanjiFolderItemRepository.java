package com.kizuna.repository;

import com.kizuna.entity.KanjiFolderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface KanjiFolderItemRepository extends JpaRepository<KanjiFolderItem, UUID> {

    List<KanjiFolderItem> findByFolderFolderIdOrderByOrderIndexAsc(UUID folderId);

    boolean existsByFolderFolderIdAndKanjiKanjiId(UUID folderId, UUID kanjiId);

    Optional<KanjiFolderItem> findByFolderFolderIdAndKanjiKanjiId(UUID folderId, UUID kanjiId);

    @Modifying
    @Query("DELETE FROM KanjiFolderItem i WHERE i.folder.folderId = :folderId AND i.kanji.kanjiId = :kanjiId")
    void deleteByFolderIdAndKanjiId(@Param("folderId") UUID folderId, @Param("kanjiId") UUID kanjiId);

    @Modifying
    @Query("DELETE FROM KanjiFolderItem i WHERE i.folder.folderId = :folderId")
    void deleteAllByFolderId(@Param("folderId") UUID folderId);
}
