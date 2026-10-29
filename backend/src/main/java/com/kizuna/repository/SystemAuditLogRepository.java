package com.kizuna.repository;

import com.kizuna.entity.SystemAuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.UUID;

@Repository
public interface SystemAuditLogRepository extends JpaRepository<SystemAuditLog, UUID> {
}
