package com.kizuna.features.payment.repository;

import com.kizuna.features.payment.entity.PaymentProcessingLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Nhật ký xử lý thanh toán (UC-98).
 */
@Repository
public interface PaymentProcessingLogRepository extends JpaRepository<PaymentProcessingLog, UUID> {

    List<PaymentProcessingLog> findByTransactionTransactionIdOrderByLoggedAtAsc(UUID transactionId);
}
