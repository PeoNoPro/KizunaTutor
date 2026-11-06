package com.kizuna.features.payment.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Nhật ký trao đổi với cổng thanh toán và ghi chú điều tra của quản trị viên (FE-09, UC-98).
 * request_payload luôn có trường "type": CREATE_PAYMENT_URL, IPN, RETURN, QUERYDR, ADMIN_NOTE.
 * Không bao giờ chứa khóa bí mật; chữ ký chỉ giữ phần đầu.
 */
@Entity
@Table(name = "payment_processing_logs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentProcessingLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "log_id", updatable = false, nullable = false)
    private UUID logId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "transaction_id", nullable = false)
    private PaymentTransaction transaction;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "request_payload", columnDefinition = "jsonb")
    private String requestPayload;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "response_payload", columnDefinition = "jsonb")
    private String responsePayload;

    @Column(name = "http_status_code")
    private Integer httpStatusCode;

    @CreationTimestamp
    @Column(name = "logged_at", nullable = false, updatable = false)
    private OffsetDateTime loggedAt;
}
