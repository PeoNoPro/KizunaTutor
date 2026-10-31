package com.kizuna.repository;

import com.kizuna.entity.TopicVocabulary;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface TopicVocabularyRepository extends JpaRepository<TopicVocabulary, UUID> {

    List<TopicVocabulary> findByTopicId(UUID topicId);

    @Query("SELECT v FROM TopicVocabulary v WHERE v.topic.id = :topicId " +
           "AND (:keyword IS NULL OR LOWER(v.word) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
           "OR LOWER(v.meaningVn) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
           "OR LOWER(v.romaji) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
           "OR LOWER(v.hanViet) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
           "OR LOWER(v.furigana) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    Page<TopicVocabulary> findByTopicIdWithSearch(
            @Param("topicId") UUID topicId,
            @Param("keyword") String keyword,
            Pageable pageable);

    @Query("SELECT v FROM TopicVocabulary v WHERE " +
           "(:keyword IS NULL OR LOWER(v.word) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
           "OR LOWER(v.meaningVn) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
           "OR LOWER(v.romaji) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
           "OR LOWER(v.hanViet) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
           "OR LOWER(v.furigana) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    Page<TopicVocabulary> findAllWithSearch(
            @Param("keyword") String keyword,
            Pageable pageable);

    long countByTopicId(UUID topicId);
}
