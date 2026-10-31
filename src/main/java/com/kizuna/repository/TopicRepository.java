package com.kizuna.repository;

import com.kizuna.entity.Topic;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TopicRepository extends JpaRepository<Topic, UUID> {

    Optional<Topic> findByCode(String code);

    @Query("SELECT t FROM Topic t ORDER BY t.displayOrder ASC")
    List<Topic> findAllOrderByDisplayOrder();
}
