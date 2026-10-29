package com.kizuna.features.payment.repository;


import com.kizuna.core.entity.*;
import com.kizuna.features.payment.entity.PaymentProcessingLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.UUID;

@Repository
public interface PaymentProcessingLogRepository extends JpaRepository<PaymentProcessingLog, UUID> {
}
