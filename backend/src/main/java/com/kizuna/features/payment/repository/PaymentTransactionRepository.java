package com.kizuna.features.payment.repository;

import com.kizuna.features.payment.entity.PaymentTransaction;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Giao dịch thanh toán gói hội viên.
 * Mọi thay đổi trạng thái PHẢI đi qua findLockedById (SELECT ... FOR UPDATE) để kết quả từ cổng chỉ được xử lý một lần.
 */
@Repository
public interface PaymentTransactionRepository extends JpaRepository<PaymentTransaction, UUID>,
        JpaSpecificationExecutor<PaymentTransaction> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT t FROM PaymentTransaction t JOIN FETCH t.user JOIN FETCH t.plan WHERE t.transactionId = :id")
    Optional<PaymentTransaction> findLockedById(@Param("id") UUID id);

    @Query("SELECT t FROM PaymentTransaction t JOIN FETCH t.user LEFT JOIN FETCH t.plan WHERE t.transactionId = :id")
    Optional<PaymentTransaction> findWithDetailsById(@Param("id") UUID id);

    @EntityGraph(attributePaths = {"plan"})
    Page<PaymentTransaction> findByUserUserIdAndPlanIsNotNull(UUID userId, Pageable pageable);

    @EntityGraph(attributePaths = {"plan"})
    List<PaymentTransaction> findByUserUserIdAndPlanIsNotNullOrderByTransactionTimeDesc(UUID userId);
}
