package com.kizuna.features.admin.service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@DisplayName("SystemSettingService DB-only AI Config Tests")
class SystemSettingServiceAiDbOnlyTest {

    private EntityManager em;
    private SystemSettingService service;

    @BeforeEach
    void setUp() {
        em = mock(EntityManager.class);
        OperationsActorResolver actors = mock(OperationsActorResolver.class);
        OperationsCommandExecutor locks = mock(OperationsCommandExecutor.class);
        Clock clock = Clock.fixed(Instant.parse("2026-11-26T00:00:00Z"), ZoneOffset.UTC);

        service = new SystemSettingService(em, actors, locks, clock);
        ReflectionTestUtils.setField(service, "settingsSecret", "test-secret-key-for-aes-gcm-256bit");
    }

    private void mockConfigValues(String provider, String model, String baseUrl, String encryptedKey) {
        when(em.createNativeQuery(anyString())).thenAnswer(inv -> {
            Query q = mock(Query.class);
            when(q.setParameter(eq("key"), anyString())).thenAnswer(pInv -> {
                String key = pInv.getArgument(1);
                List<Object> result = switch (key) {
                    case "AI_PROVIDER" -> provider == null ? Collections.emptyList() : List.of(provider);
                    case "AI_MODEL" -> model == null ? Collections.emptyList() : List.of(model);
                    case "AI_BASE_URL" -> baseUrl == null ? Collections.emptyList() : List.of(baseUrl);
                    case "AI_API_KEY_CUSTOM", "AI_API_KEY_GEMINI", "AI_API_KEY_OPENAI" ->
                            encryptedKey == null ? Collections.emptyList() : List.of(encryptedKey);
                    default -> Collections.emptyList();
                };
                when(q.getResultList()).thenReturn(result);
                return q;
            });
            return q;
        });
    }

    @Test
    @DisplayName("DB có đủ cấu hình AI -> aiChoice() trả về đúng provider, model, baseUrl và key đã giải mã")
    void aiChoice_whenConfiguredInDb_returnsDbValues() {
        String encrypted = service.encrypt("gsk_real_key_from_db");
        mockConfigValues("CUSTOM", "qwen/qwen3.8-27b", "https://api.groq.com/openai/v1", encrypted);

        SystemSettingService.AiChoice choice = service.aiChoice();

        assertThat(choice.provider()).isEqualTo("CUSTOM");
        assertThat(choice.model()).isEqualTo("qwen/qwen3.8-27b");
        assertThat(choice.baseUrl()).isEqualTo("https://api.groq.com/openai/v1");
        assertThat(choice.apiKey()).isEqualTo("gsk_real_key_from_db");
    }

    @Test
    @DisplayName("DB trống -> aiChoice() trả về chuỗi rỗng, không fallback về application.properties")
    void aiChoice_whenDbEmpty_returnsEmptyWithoutProjectFallback() {
        mockConfigValues(null, null, null, null);

        SystemSettingService.AiChoice choice = service.aiChoice();

        assertThat(choice.provider()).isEmpty();
        assertThat(choice.model()).isEmpty();
        assertThat(choice.apiKey()).isEmpty();
        assertThat(choice.baseUrl()).isEmpty();
    }

    @Test
    @DisplayName("Lỗi đọc DB -> aiChoice() trả về rỗng hoàn toàn, không lấy key/model từ project")
    void aiChoice_whenDbThrows_returnsEmptyWithoutProjectFallback() {
        when(em.createNativeQuery(anyString())).thenThrow(new RuntimeException("DB unavailable"));

        SystemSettingService.AiChoice choice = service.aiChoice();

        assertThat(choice.provider()).isEmpty();
        assertThat(choice.model()).isEmpty();
        assertThat(choice.apiKey()).isEmpty();
        assertThat(choice.baseUrl()).isEmpty();
    }
}
