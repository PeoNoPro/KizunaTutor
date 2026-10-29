package com.kizuna.features.admin.repository;


import com.kizuna.core.entity.*;
import com.kizuna.features.admin.entity.SystemAuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.UUID;

@Repository
public interface SystemAuditLogRepository extends JpaRepository<SystemAuditLog, UUID> {
}
