package com.kizuna.features.booking.repository;

import com.kizuna.features.booking.entity.ConnectionRequest;
import com.kizuna.features.booking.entity.ConnectionRequestStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ConnectionRequestRepository extends JpaRepository<ConnectionRequest, UUID> {

    Page<ConnectionRequest> findByLearnerLearnerIdAndStatus(UUID learnerId, ConnectionRequestStatus status, Pageable pageable);

    Page<ConnectionRequest> findByLearnerLearnerId(UUID learnerId, Pageable pageable);

    Page<ConnectionRequest> findByTutorTutorIdAndStatus(UUID tutorId, ConnectionRequestStatus status, Pageable pageable);

    Page<ConnectionRequest> findByTutorTutorId(UUID tutorId, Pageable pageable);

    Optional<ConnectionRequest> findByLearnerLearnerIdAndTutorTutorIdAndStatus(UUID learnerId, UUID tutorId, ConnectionRequestStatus status);
}

