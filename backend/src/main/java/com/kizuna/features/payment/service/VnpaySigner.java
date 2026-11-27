package com.kizuna.features.payment.service;

import com.kizuna.features.payment.config.VnpayProperties;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Map;
import java.util.TreeMap;

/**
 * Ký và kiểm chữ ký HMAC-SHA512 theo VNPay 2.1.0 (API-EXT-01, API-EXT-02).
 * Chuỗi ký: tham số khác rỗng, sắp xếp theo tên, giá trị mã hóa URL US-ASCII, nối "k=v&k=v";
 * bỏ vnp_SecureHash và vnp_SecureHashType khi kiểm chữ ký.
 */
@Component
public class VnpaySigner {

    public static final String SECURE_HASH = "vnp_SecureHash";
    public static final String SECURE_HASH_TYPE = "vnp_SecureHashType";

    private final VnpayProperties properties;

    public VnpaySigner(VnpayProperties properties) {
        this.properties = properties;
    }

    /** Chuỗi dữ liệu dùng để ký (đã sắp xếp, đã mã hóa). */
    public String hashData(Map<String, String> params) {
        StringBuilder builder = new StringBuilder();
        for (Map.Entry<String, String> entry : sortedSignable(params).entrySet()) {
            if (!builder.isEmpty()) {
                builder.append('&');
            }
            builder.append(entry.getKey()).append('=').append(encode(entry.getValue()));
        }
        return builder.toString();
    }

    /** Query string gửi cổng, đã kèm vnp_SecureHash. */
    public String signedQuery(Map<String, String> params) {
        StringBuilder builder = new StringBuilder();
        for (Map.Entry<String, String> entry : sortedSignable(params).entrySet()) {
            if (!builder.isEmpty()) {
                builder.append('&');
            }
            builder.append(encode(entry.getKey())).append('=').append(encode(entry.getValue()));
        }
        builder.append('&').append(SECURE_HASH).append('=').append(hmacSha512(hashData(params)));
        return builder.toString();
    }

    /** true khi vnp_SecureHash khớp chữ ký tính lại; so sánh thời gian hằng, không phân biệt hoa thường. */
    public boolean verify(Map<String, String> params) {
        if (params == null || !properties.isConfigured()) {
            return false;
        }
        String received = params.get(SECURE_HASH);
        if (received == null || received.isBlank()) {
            return false;
        }
        String expected = hmacSha512(hashData(params));
        return constantTimeEquals(expected, received.trim().toLowerCase());
    }

    public String hmacSha512(String data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA512");
            mac.init(new SecretKeySpec(properties.getHashSecret().getBytes(StandardCharsets.UTF_8), "HmacSHA512"));
            return HexFormat.of().formatHex(mac.doFinal(data.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException("Không tạo được chữ ký thanh toán", e);
        }
    }

    public static boolean constantTimeEquals(String a, String b) {
        if (a == null || b == null) {
            return false;
        }
        return MessageDigest.isEqual(a.getBytes(StandardCharsets.US_ASCII), b.getBytes(StandardCharsets.US_ASCII));
    }

    private static Map<String, String> sortedSignable(Map<String, String> params) {
        Map<String, String> sorted = new TreeMap<>();
        params.forEach((key, value) -> {
            if (key != null && value != null && !value.isEmpty()
                    && !SECURE_HASH.equals(key) && !SECURE_HASH_TYPE.equals(key)) {
                sorted.put(key, value);
            }
        });
        return sorted;
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.US_ASCII);
    }
}
