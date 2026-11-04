package com.kizuna.features.payment.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kizuna.features.payment.entity.PaymentProcessingLog;
import com.kizuna.features.payment.entity.PaymentTransaction;
import com.kizuna.features.payment.repository.PaymentProcessingLogRepository;
import com.kizuna.features.payment.repository.PaymentTransactionRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Ghi nhật ký xử lý thanh toán (FR-011, UC-98) trong transaction riêng, nên lỗi ghi log không ảnh hưởng nghiệp vụ.
 * CHỈ gọi khi không giữ khóa dòng payment_transactions (sau khi xử lý xong) để tránh tự chờ khóa khóa ngoại.
 * Không bao giờ ghi khóa bí mật; vnp_SecureHash chỉ giữ 8 ký tự đầu.
 */
@Slf4j
@Component
public class PaymentLogWriter {

    public static final String TYPE_CREATE = "CREATE_PAYMENT_URL";
    public static final String TYPE_IPN = "IPN";
    public static final String TYPE_RETURN = "RETURN";
    public static final String TYPE_QUERY = "QUERYDR";
    public static final String TYPE_NOTE = "ADMIN_NOTE";

    private final PaymentProcessingLogRepository logRepository;
    private final PaymentTransactionRepository transactionRepository;
    private final ObjectMapper objectMapper;

    public PaymentLogWriter(PaymentProcessingLogRepository logRepository,
                            PaymentTransactionRepository transactionRepository,
                            ObjectMapper objectMapper) {
        this.logRepository = logRepository;
        this.transactionRepository = transactionRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void write(UUID transactionId, String type, Map<String, ?> request, Object response, Integer httpStatus) {
        try {
            PaymentTransaction tx = transactionRepository.getReferenceById(transactionId);
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("type", type);
            if (request != null) {
                request.forEach((key, value) -> payload.put(key, mask(key, value)));
            }
            logRepository.save(PaymentProcessingLog.builder()
                    .transaction(tx)
                    .requestPayload(objectMapper.writeValueAsString(payload))
                    .responsePayload(response == null ? null : objectMapper.writeValueAsString(response))
                    .httpStatusCode(httpStatus)
                    .build());
        } catch (Exception e) {
            log.warn("Không ghi được nhật ký thanh toán {} cho giao dịch {}: {}", type, transactionId, e.getMessage());
        }
    }

    private static Object mask(String key, Object value) {
        if (value == null) {
            return null;
        }
        if (VnpaySigner.SECURE_HASH.equals(key)) {
            String text = String.valueOf(value);
            return text.length() <= 8 ? "***" : text.substring(0, 8) + "…";
        }
        return value;
    }
}
