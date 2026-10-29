package com.kizuna.repository;

import com.kizuna.entity.ConnectionRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.UUID;

@Repository
public interface ConnectionRequestRepository extends JpaRepository<ConnectionRequest, UUID> {
}
