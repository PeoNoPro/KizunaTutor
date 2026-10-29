package com.kizuna.repository;

import com.kizuna.entity.PaymentProcessingLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.UUID;

@Repository
public interface PaymentProcessingLogRepository extends JpaRepository<PaymentProcessingLog, UUID> {
}
