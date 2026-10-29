package com.kizuna.features.tutor_search.repository;


import com.kizuna.core.entity.*;
import com.kizuna.features.tutor_search.entity.ConnectionRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.UUID;

@Repository
public interface ConnectionRequestRepository extends JpaRepository<ConnectionRequest, UUID> {
}
