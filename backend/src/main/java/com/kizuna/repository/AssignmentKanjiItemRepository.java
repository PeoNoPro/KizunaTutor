package com.kizuna.repository;

import com.kizuna.entity.AssignmentKanjiItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.UUID;

@Repository
public interface AssignmentKanjiItemRepository extends JpaRepository<AssignmentKanjiItem, UUID> {
}
