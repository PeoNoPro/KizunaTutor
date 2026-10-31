package com.kizuna.service;

import com.kizuna.dto.topic.AddVocabToDeckRequest;
import com.kizuna.dto.topic.TopicDto;
import com.kizuna.dto.topic.TopicVocabularyDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface ITopicService {

    List<TopicDto> getAllTopics();

    Page<TopicVocabularyDto> getVocabulariesByTopic(String topicIdentifier, String keyword, Pageable pageable);

    Map<String, Object> addVocabularyToDeck(UUID userId, UUID vocabId, AddVocabToDeckRequest request);

    TopicVocabularyDto getVocabularyById(UUID vocabId);

    void seedTopicsAndVocabulariesIfEmpty();
}
