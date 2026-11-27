package com.kizuna.features.payment.entity;

import com.kizuna.core.entity.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcType;
import org.hibernate.dialect.PostgreSQLEnumJdbcType;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Giao dịch thanh toán gói hội viên gia sư qua VNPay (FE-09, UC-104).
 * Bảng không có created_at/updated_at nên KHÔNG kế thừa BaseEntity.
 * Trạng thái chỉ đi một chiều: PENDING → SUCCESS | FAILED (GB-01).
 */
@Entity
@Table(name = "payment_transactions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "transaction_id", updatable = false, nullable = false)
    private UUID transactionId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "plan_id")
    private MembershipPlan plan;

    /** Thuộc FE-05 (đặt cọc học thử); FE-09 không dùng. */
    @Column(name = "booking_id")
    private UUID bookingId;

    @Column(name = "amount", precision = 12, scale = 2, nullable = false)
    private BigDecimal amount;

    @Column(name = "payment_gateway", length = 50, nullable = false)
    @Builder.Default
    private String paymentGateway = "VNPay";

    /** Mã giao dịch phía cổng (vnp_TransactionNo). */
    @Column(name = "gateway_transaction_id", length = 100)
    private String gatewayTransactionId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", columnDefinition = "payment_transaction_status_enum", nullable = false)
    @JdbcType(PostgreSQLEnumJdbcType.class)
    @Builder.Default
    private PaymentTransactionStatus status = PaymentTransactionStatus.PENDING;

    /** Thời điểm tạo giao dịch; cũng là vnp_CreateDate / vnp_TransactionDate gửi VNPay. */
    @Column(name = "transaction_time", nullable = false, updatable = false)
    private OffsetDateTime transactionTime;

    /** Mã đơn gửi VNPay (vnp_TxnRef): UUID bỏ dấu gạch nối, 32 ký tự. */
    public String getTxnRef() {
        return transactionId == null ? null : transactionId.toString().replace("-", "");
    }
}
