package com.kizuna.features.practice.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;

/**
 * Đường dẫn ký tạm thời cho file nghe/ảnh: thẻ &lt;audio&gt;/&lt;img&gt; không gửi được token đăng nhập, nên API
 * chỉ trả đường dẫn đã ký (hết hạn sau vài giờ) cho người được xem bài. Không cấu hình khóa thì tạo khóa ngẫu nhiên
 * mỗi lần khởi động (đường dẫn cũ hết hiệu lực, trang tải lại sẽ nhận đường dẫn mới).
 */
@Component
public class PracticeMediaTokens {
    private static final long TTL_SECONDS = 6 * 3600;
    private final byte[] secret;

    public PracticeMediaTokens(@Value("${app.practice.media-secret:}") String configured) {
        if (configured != null && configured.length() >= 32) secret = configured.getBytes(StandardCharsets.UTF_8);
        else { secret = new byte[32]; new SecureRandom().nextBytes(secret); }
    }

    /** Đường dẫn tương đối (sau tiền tố /api) để trình duyệt tải file. */
    public String path(UUID fileId) {
        if (fileId == null) return null;
        long exp = Instant.now().getEpochSecond() + TTL_SECONDS;
        return "/practice/media/" + fileId + "?exp=" + exp + "&sig=" + sign(fileId, exp);
    }

    public boolean valid(UUID fileId, long exp, String sig) {
        if (fileId == null || sig == null || exp < Instant.now().getEpochSecond()) return false;
        return MessageDigest.isEqual(sign(fileId, exp).getBytes(StandardCharsets.US_ASCII), sig.getBytes(StandardCharsets.US_ASCII));
    }

    private String sign(UUID fileId, long exp) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret, "HmacSHA256"));
            byte[] digest = mac.doFinal((fileId + ":" + exp).getBytes(StandardCharsets.US_ASCII));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(digest);
        } catch (Exception exception) {
            throw new IllegalStateException("Không ký được đường dẫn tệp.", exception);
        }
    }
}
