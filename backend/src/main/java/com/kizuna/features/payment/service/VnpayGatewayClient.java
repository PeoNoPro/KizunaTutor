package com.kizuna.features.payment.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kizuna.core.exception.ServiceUnavailableException;
import com.kizuna.features.payment.config.VnpayProperties;
import com.kizuna.features.payment.config.VnpayTrust;
import com.kizuna.features.payment.entity.PaymentTransaction;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Tích hợp VNPay sandbox: tạo đường dẫn thanh toán (API-EXT-01) và tra cứu giao dịch querydr (phương án C).
 * Thứ tự trường chữ ký querydr theo tài liệu VNPay 2.1.0 — cần đối chiếu với sandbox khi nghiệm thu (tasks T014).
 */
@Slf4j
@Component
public class VnpayGatewayClient {

    public static final ZoneId VIETNAM = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final DateTimeFormatter VNP_TIME = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private static final String VERSION = "2.1.0";

    private final VnpayProperties properties;
    private final VnpaySigner signer;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    public VnpayGatewayClient(VnpayProperties properties, VnpaySigner signer, ObjectMapper objectMapper) {
        this.properties = properties;
        this.signer = signer;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .sslContext(VnpayTrust.sslContext())
                .connectTimeout(Duration.ofSeconds(Math.max(1, properties.getQueryTimeoutSeconds())))
                .build();
    }

    /** Kết quả một lần tra cứu querydr. */
    public record QueryOutcome(boolean verified, int httpStatus, String queryResponseCode, String message,
                               GatewayResult result, Map<String, Object> requestBody, String rawResponse) {
    }

    public static String vnpTime(OffsetDateTime time) {
        return time.atZoneSameInstant(VIETNAM).format(VNP_TIME);
    }

    public static long amountTimes100(BigDecimal amount) {
        return amount.setScale(0, RoundingMode.UNNECESSARY).multiply(BigDecimal.valueOf(100)).longValueExact();
    }

    /** Tham số đường dẫn thanh toán (chưa ký) — tách riêng để ghi nhật ký. */
    public Map<String, String> paymentParams(PaymentTransaction tx, String clientIp) {
        OffsetDateTime created = tx.getTransactionTime();
        Map<String, String> params = new LinkedHashMap<>();
        params.put("vnp_Version", VERSION);
        params.put("vnp_Command", "pay");
        params.put("vnp_TmnCode", properties.getTmnCode());
        params.put("vnp_Amount", String.valueOf(amountTimes100(tx.getAmount())));
        params.put("vnp_CurrCode", "VND");
        params.put("vnp_TxnRef", tx.getTxnRef());
        params.put("vnp_OrderInfo", orderInfo(tx));
        params.put("vnp_OrderType", "other");
        params.put("vnp_Locale", "vn");
        params.put("vnp_ReturnUrl", properties.getReturnUrl());
        params.put("vnp_IpAddr", safeIp(clientIp));
        params.put("vnp_CreateDate", vnpTime(created));
        params.put("vnp_ExpireDate", vnpTime(created.plusMinutes(properties.getExpireMinutes())));
        return params;
    }

    public String buildPaymentUrl(Map<String, String> params) {
        requireConfigured();
        return properties.getPayUrl() + "?" + signer.signedQuery(params);
    }

    /**
     * Tra cứu trạng thái giao dịch với cổng. Lỗi mạng/HTTP → ServiceUnavailableException (MSG37).
     * Chữ ký phản hồi sai → verified = false, không được dùng để đổi trạng thái.
     */
    public QueryOutcome queryTransaction(PaymentTransaction tx, String clientIp) {
        requireConfigured();
        String requestId = UUID.randomUUID().toString().replace("-", "");
        String createDate = vnpTime(OffsetDateTime.now(VIETNAM));
        String transactionDate = vnpTime(tx.getTransactionTime());
        String ip = safeIp(clientIp);
        String info = "Tra cuu giao dich " + tx.getTxnRef();

        String hashData = String.join("|", requestId, VERSION, "querydr", properties.getTmnCode(), tx.getTxnRef(),
                transactionDate, createDate, ip, info);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("vnp_RequestId", requestId);
        body.put("vnp_Version", VERSION);
        body.put("vnp_Command", "querydr");
        body.put("vnp_TmnCode", properties.getTmnCode());
        body.put("vnp_TxnRef", tx.getTxnRef());
        body.put("vnp_OrderInfo", info);
        body.put("vnp_TransactionDate", transactionDate);
        body.put("vnp_CreateDate", createDate);
        body.put("vnp_IpAddr", ip);
        body.put("vnp_SecureHash", signer.hmacSha512(hashData));

        HttpResponse<String> response;
        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(properties.getApiUrl()))
                    .timeout(Duration.ofSeconds(Math.max(1, properties.getQueryTimeoutSeconds())))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body), StandardCharsets.UTF_8))
                    .build();
            response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ServiceUnavailableException(PaymentMessages.MSG37_GATEWAY_UNAVAILABLE, e);
        } catch (Exception e) {
            log.warn("Tra cứu giao dịch {} với VNPay thất bại: {}", tx.getTxnRef(), e.getMessage());
            throw new ServiceUnavailableException(PaymentMessages.MSG37_GATEWAY_UNAVAILABLE, e);
        }

        Map<String, String> data;
        try {
            data = objectMapper.readValue(response.body(), new TypeReference<Map<String, Object>>() { })
                    .entrySet().stream()
                    .collect(LinkedHashMap::new, (map, e) -> map.put(e.getKey(), e.getValue() == null ? "" : String.valueOf(e.getValue())),
                            LinkedHashMap::putAll);
        } catch (Exception e) {
            log.warn("Phản hồi tra cứu VNPay cho {} không đọc được (HTTP {})", tx.getTxnRef(), response.statusCode());
            return new QueryOutcome(false, response.statusCode(), null, null, null, body, response.body());
        }

        String responseHashData = String.join("|",
                data.getOrDefault("vnp_ResponseId", ""), data.getOrDefault("vnp_Command", ""),
                data.getOrDefault("vnp_ResponseCode", ""), data.getOrDefault("vnp_Message", ""),
                data.getOrDefault("vnp_TmnCode", ""), data.getOrDefault("vnp_TxnRef", ""),
                data.getOrDefault("vnp_Amount", ""), data.getOrDefault("vnp_BankCode", ""),
                data.getOrDefault("vnp_PayDate", ""), data.getOrDefault("vnp_TransactionNo", ""),
                data.getOrDefault("vnp_TransactionType", ""), data.getOrDefault("vnp_TransactionStatus", ""),
                data.getOrDefault("vnp_OrderInfo", ""), data.getOrDefault("vnp_PromotionCode", ""),
                data.getOrDefault("vnp_PromotionAmount", ""));
        boolean verified = VnpaySigner.constantTimeEquals(signer.hmacSha512(responseHashData),
                data.getOrDefault("vnp_SecureHash", "").toLowerCase());
        if (!verified) {
            log.warn("Chữ ký phản hồi tra cứu VNPay cho {} không khớp", tx.getTxnRef());
        }

        GatewayResult result = new GatewayResult(
                data.getOrDefault("vnp_TxnRef", tx.getTxnRef()),
                data.get("vnp_ResponseCode"),
                data.get("vnp_TransactionStatus"),
                data.get("vnp_TransactionNo"),
                GatewayResult.parseLong(data.get("vnp_Amount")),
                data.get("vnp_BankCode"));
        return new QueryOutcome(verified, response.statusCode(), data.get("vnp_ResponseCode"), data.get("vnp_Message"),
                result, body, response.body());
    }

    private void requireConfigured() {
        if (!properties.isConfigured()) {
            throw new ServiceUnavailableException(PaymentMessages.MSG37_GATEWAY_UNAVAILABLE);
        }
    }

    private static String orderInfo(PaymentTransaction tx) {
        return "Thanh toan goi hoi vien " + tx.getTxnRef();
    }

    private static String safeIp(String ip) {
        if (ip == null || ip.isBlank()) {
            return "127.0.0.1";
        }
        String first = ip.split(",")[0].trim();
        return "0:0:0:0:0:0:0:1".equals(first) || "::1".equals(first) ? "127.0.0.1" : first;
    }
}
