package com.kizuna.features.payment;

import com.kizuna.features.payment.config.VnpayProperties;
import com.kizuna.features.payment.service.VnpaySigner;
import org.junit.jupiter.api.Test;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Chữ ký HMAC-SHA512 theo VNPay 2.1.0 (API-EXT-01/02). Không dùng khóa thật.
 */
class VnpaySignerTest {

    static final String SECRET = "TESTSECRETKEY0123456789ABCDEFGHIJ";

    static VnpayProperties properties(String secret) {
        return new VnpayProperties("TESTCODE", secret, "https://pay.test/vpcpay.html", "https://api.test/querydr",
                "http://localhost:3000/tutor/membership/result", 15, 10);
    }

    private final VnpaySigner signer = new VnpaySigner(properties(SECRET));

    private static String hmac(String data) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA512");
        mac.init(new SecretKeySpec(SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA512"));
        return HexFormat.of().formatHex(mac.doFinal(data.getBytes(StandardCharsets.UTF_8)));
    }

    @Test
    void hashDataSortsKeysEncodesValuesAndSkipsEmpty() {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("vnp_TxnRef", "abc");
        params.put("vnp_Amount", "19900000");
        params.put("vnp_OrderInfo", "Thanh toan goi hoi vien abc");
        params.put("vnp_BankCode", "");
        params.put("vnp_SecureHash", "ignored");

        assertEquals("vnp_Amount=19900000&vnp_OrderInfo=Thanh+toan+goi+hoi+vien+abc&vnp_TxnRef=abc",
                signer.hashData(params));
    }

    @Test
    void signedQueryAppendsCorrectHmac() throws Exception {
        Map<String, String> params = Map.of("vnp_ReturnUrl", "http://localhost:3000/a?b=1", "vnp_Amount", "100");
        String expectedData = "vnp_Amount=100&vnp_ReturnUrl=http%3A%2F%2Flocalhost%3A3000%2Fa%3Fb%3D1";

        String query = signer.signedQuery(params);

        assertTrue(query.startsWith(expectedData + "&vnp_SecureHash="));
        assertTrue(query.endsWith(hmac(expectedData)));
    }

    @Test
    void verifyAcceptsValidSignatureCaseInsensitive() throws Exception {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("vnp_Amount", "19900000");
        params.put("vnp_ResponseCode", "00");
        params.put("vnp_TxnRef", "abc");
        params.put("vnp_SecureHashType", "HmacSHA512");
        params.put("vnp_SecureHash", hmac("vnp_Amount=19900000&vnp_ResponseCode=00&vnp_TxnRef=abc").toUpperCase());

        assertTrue(signer.verify(params));
    }

    @Test
    void verifyRejectsTamperedOrMissingSignature() throws Exception {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("vnp_Amount", "19900000");
        params.put("vnp_TxnRef", "abc");
        params.put("vnp_SecureHash", hmac("vnp_Amount=19900000&vnp_TxnRef=abc"));
        params.put("vnp_Amount", "100");

        assertFalse(signer.verify(params));
        assertFalse(signer.verify(Map.of("vnp_Amount", "1")));
    }

    @Test
    void verifyFailsWhenSecretNotConfigured() throws Exception {
        VnpaySigner unconfigured = new VnpaySigner(properties(""));
        assertFalse(unconfigured.verify(Map.of("vnp_Amount", "1", "vnp_SecureHash", hmac("vnp_Amount=1"))));
    }
}
