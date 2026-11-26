package com.kizuna.features.auth.security;

import com.kizuna.features.auth.controller.GoogleOAuthController;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.List;
import java.util.Optional;

/**
 * Đọc cấu hình Google OAuth2 từ bảng system_configurations trong PostgreSQL
 * (ưu tiên DB, fallback về biến môi trường / application.properties).
 * Client Secret được giải mã AES-GCM tương thích với SystemSettingService.
 */
@Slf4j
@Service
public class GoogleOAuthConfigService {

    public static final String KEY_CLIENT_ID = "GOOGLE_OAUTH_CLIENT_ID";
    public static final String KEY_CLIENT_SECRET = "GOOGLE_OAUTH_CLIENT_SECRET";
    public static final String KEY_REDIRECT_URI = "GOOGLE_OAUTH_REDIRECT_URI";

    private final JdbcTemplate jdbcTemplate;

    @Value("${spring.security.oauth2.client.registration.google.client-id:}")
    private String fallbackClientId;

    @Value("${spring.security.oauth2.client.registration.google.client-secret:}")
    private String fallbackClientSecret;

    @Value("${app.auth.google-redirect-uri:http://localhost:3000/login/oauth2/code/google}")
    private String fallbackRedirectUri;

    @Value("${app.settings.secret-key:${app.jwt.secret:}}")
    private String settingsSecret;

    public GoogleOAuthConfigService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public record GoogleOAuthConfig(String clientId, String clientSecret, String redirectUri) {
        public boolean isConfigured() {
            return clientId != null && !clientId.isBlank()
                    && !GoogleOAuthController.PLACEHOLDER_CLIENT_ID.equals(clientId.trim())
                    && clientSecret != null && !clientSecret.isBlank();
        }
    }

    public GoogleOAuthConfig currentConfig() {
        String dbClientId = queryConfigValue(KEY_CLIENT_ID).orElse("");
        String dbSecretRaw = queryConfigValue(KEY_CLIENT_SECRET).orElse("");
        String dbSecret = dbSecretRaw.startsWith("enc:") ? decrypt(dbSecretRaw) : dbSecretRaw;
        String dbRedirectUri = queryConfigValue(KEY_REDIRECT_URI).orElse("");

        String clientId = !dbClientId.isBlank() ? dbClientId : safeTrim(fallbackClientId);
        String clientSecret = !dbSecret.isBlank() ? dbSecret : safeTrim(fallbackClientSecret);
        String redirectUri = !dbRedirectUri.isBlank() ? dbRedirectUri : safeTrim(fallbackRedirectUri);
        if (redirectUri.isBlank()) {
            redirectUri = "http://localhost:3000/login/oauth2/code/google";
        }
        return new GoogleOAuthConfig(clientId, clientSecret, redirectUri);
    }

    private Optional<String> queryConfigValue(String key) {
        if (jdbcTemplate == null) {
            return Optional.empty();
        }
        try {
            List<String> rows = jdbcTemplate.query(
                    "SELECT config_value FROM system_configurations WHERE config_key = ?",
                    (rs, rowNum) -> rs.getString("config_value"),
                    key
            );
            if (rows.isEmpty() || rows.get(0) == null) {
                return Optional.empty();
            }
            String trimmed = rows.get(0).trim();
            return trimmed.isEmpty() ? Optional.empty() : Optional.of(trimmed);
        } catch (Exception ex) {
            log.debug("[OAUTH2] Không đọc được {} từ DB, dùng fallback cấu hình: {}", key, ex.getMessage());
            return Optional.empty();
        }
    }

    public String encrypt(String plain) {
        try {
            byte[] iv = new byte[12];
            new SecureRandom().nextBytes(iv);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, cipherKey(), new GCMParameterSpec(128, iv));
            byte[] out = cipher.doFinal(plain.getBytes(StandardCharsets.UTF_8));
            byte[] all = new byte[iv.length + out.length];
            System.arraycopy(iv, 0, all, 0, iv.length);
            System.arraycopy(out, 0, all, iv.length, out.length);
            return "enc:" + Base64.getEncoder().encodeToString(all);
        } catch (Exception ex) {
            throw new IllegalStateException("Không mã hóa được Google Client Secret.", ex);
        }
    }

    String decrypt(String stored) {
        try {
            if (stored == null || !stored.startsWith("enc:")) {
                return "";
            }
            byte[] all = Base64.getDecoder().decode(stored.substring(4));
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, cipherKey(), new GCMParameterSpec(128, all, 0, 12));
            return new String(cipher.doFinal(all, 12, all.length - 12), StandardCharsets.UTF_8);
        } catch (Exception ex) {
            log.warn("[OAUTH2] Giải mã Google Client Secret thất bại: {}", ex.getMessage());
            return "";
        }
    }

    private SecretKeySpec cipherKey() throws Exception {
        byte[] digest = MessageDigest.getInstance("SHA-256")
                .digest(("kizuna-settings:" + safeTrim(settingsSecret)).getBytes(StandardCharsets.UTF_8));
        return new SecretKeySpec(digest, "AES");
    }

    private static String safeTrim(String value) {
        return value == null ? "" : value.trim();
    }
}
