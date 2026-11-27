package com.kizuna.features.payment.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;

/**
 * Ghi nhật ký qua proxy của PaymentLogWriter (transaction riêng) và nuốt mọi lỗi:
 * nhật ký không bao giờ được làm hỏng luồng thanh toán.
 */
@Slf4j
@Component
public class PaymentAudit {

    private final PaymentLogWriter logWriter;

    public PaymentAudit(PaymentLogWriter logWriter) {
        this.logWriter = logWriter;
    }

    public void log(UUID transactionId, String type, Map<String, ?> request, Map<String, ?> response, Integer httpStatus) {
        if (transactionId == null) {
            return;
        }
        try {
            logWriter.write(transactionId, type, request, response, httpStatus);
        } catch (Exception e) {
            log.warn("Không ghi được nhật ký thanh toán {} cho {}: {}", type, transactionId, e.getMessage());
        }
    }
}
