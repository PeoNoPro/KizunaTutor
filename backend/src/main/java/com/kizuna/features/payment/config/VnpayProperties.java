package com.kizuna.features.payment.config;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Cấu hình cổng VNPay cho FE-09 (application.properties mục 13, đọc từ biến môi trường).
 * Không dùng khối "vnpay.*" cũ của dự án. Thiếu mã website hoặc khóa bí mật thì isConfigured() = false.
 */
@Getter
@Component
public class VnpayProperties {

    private final String tmnCode;
    private final String hashSecret;
    private final String payUrl;
    private final String apiUrl;
    private final String returnUrl;
    private final int expireMinutes;
    private final int queryTimeoutSeconds;

    public VnpayProperties(@Value("${app.payment.vnpay.tmn-code:}") String tmnCode,
                           @Value("${app.payment.vnpay.hash-secret:}") String hashSecret,
                           @Value("${app.payment.vnpay.pay-url:https://sandbox.vnpayment.vn/paymentv2/vpcpay.html}") String payUrl,
                           @Value("${app.payment.vnpay.api-url:https://sandbox.vnpayment.vn/merchant_webapi/api/transaction}") String apiUrl,
                           @Value("${app.payment.vnpay.return-url:http://localhost:8085/tutor/membership/result}") String returnUrl,
                           @Value("${app.payment.vnpay.expire-minutes:15}") int expireMinutes,
                           @Value("${app.payment.vnpay.query-timeout-seconds:10}") int queryTimeoutSeconds) {
        this.tmnCode = trim(tmnCode);
        this.hashSecret = trim(hashSecret);
        this.payUrl = trim(payUrl);
        this.apiUrl = trim(apiUrl);
        this.returnUrl = trim(returnUrl);
        this.expireMinutes = expireMinutes;
        this.queryTimeoutSeconds = queryTimeoutSeconds;
    }

    public boolean isConfigured() {
        return !tmnCode.isEmpty() && !hashSecret.isEmpty();
    }

    private static String trim(String value) {
        return value == null ? "" : value.trim();
    }
}
