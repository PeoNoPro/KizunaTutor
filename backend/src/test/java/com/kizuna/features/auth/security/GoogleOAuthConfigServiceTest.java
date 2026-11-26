package com.kizuna.features.auth.security;

import com.kizuna.features.auth.controller.GoogleOAuthController;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DisplayName("GoogleOAuthConfigService & Database ClientRegistration Tests")
class GoogleOAuthConfigServiceTest {

    private JdbcTemplate jdbcTemplate;
    private GoogleOAuthConfigService service;

    @BeforeEach
    void setUp() {
        jdbcTemplate = mock(JdbcTemplate.class);
        service = new GoogleOAuthConfigService(jdbcTemplate);
        ReflectionTestUtils.setField(service, "fallbackClientId", GoogleOAuthController.PLACEHOLDER_CLIENT_ID);
        ReflectionTestUtils.setField(service, "fallbackClientSecret", "GOCSPX-kizuna-tutor-secret");
        ReflectionTestUtils.setField(service, "fallbackRedirectUri", "http://localhost:3000/login/oauth2/code/google");
        ReflectionTestUtils.setField(service, "settingsSecret", "test-secret-key-for-aes-gcm-256bit");
    }

    @Test
    @DisplayName("DB có clientId & secret mã hóa AES-GCM -> Ưu tiên đọc từ DB và giải mã đúng")
    @SuppressWarnings("unchecked")
    void currentConfig_whenStoredInDb_readsAndDecryptsFromDb() {
        String encryptedSecret = service.encrypt("GOCSPX-real-secret-from-db");
        when(jdbcTemplate.query(anyString(), any(RowMapper.class), eq(GoogleOAuthConfigService.KEY_CLIENT_ID)))
                .thenReturn(List.of("123456-real.apps.googleusercontent.com"));
        when(jdbcTemplate.query(anyString(), any(RowMapper.class), eq(GoogleOAuthConfigService.KEY_CLIENT_SECRET)))
                .thenReturn(List.of(encryptedSecret));
        when(jdbcTemplate.query(anyString(), any(RowMapper.class), eq(GoogleOAuthConfigService.KEY_REDIRECT_URI)))
                .thenReturn(Collections.emptyList());

        DatabaseGoogleClientRegistrationRepository repo = new DatabaseGoogleClientRegistrationRepository(service);
        var reg = repo.findByRegistrationId("google");

        assertThat(reg).isNotNull();
        assertThat(reg.getClientId()).isEqualTo("123456-real.apps.googleusercontent.com");
        assertThat(reg.getClientSecret()).isEqualTo("GOCSPX-real-secret-from-db");
        assertThat(reg.getRedirectUri()).isEqualTo("http://localhost:3000/login/oauth2/code/google");
    }

    @Test
    @DisplayName("DB chưa có cấu hình -> Fallback về application.properties")
    @SuppressWarnings("unchecked")
    void currentConfig_whenDbEmpty_fallsBackToProperties() {
        when(jdbcTemplate.query(anyString(), any(RowMapper.class), anyString()))
                .thenReturn(Collections.emptyList());

        var cfg = service.currentConfig();

        assertThat(cfg.clientId()).isEqualTo(GoogleOAuthController.PLACEHOLDER_CLIENT_ID);
        assertThat(cfg.isConfigured()).isFalse();
    }
}
