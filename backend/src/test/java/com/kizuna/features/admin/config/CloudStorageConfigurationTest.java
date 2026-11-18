package com.kizuna.features.admin.config;

import com.kizuna.features.admin.service.CloudStorageSettingService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class CloudStorageConfigurationTest {

    @Configuration
    static class NoStorage {
        @Bean @Primary S3Client s3() { return null; }
        @Bean Holder holder(ObjectProvider<S3Client> s3) { return new Holder(s3.getIfAvailable()); }
    }
    record Holder(S3Client s3) { }

    /** Chưa có kho nào lúc khởi động: các service dùng ObjectProvider nhận null và lưu như khi tắt R2. */
    @Test
    void missingStorageGivesNullToObjectProviderUsers() {
        try (var context = new AnnotationConfigApplicationContext(NoStorage.class)) {
            assertNull(context.getBean(Holder.class).s3());
        }
    }

    @Test
    void switchingClientWritesToBucketOfCurrentTarget() {
        var settings = mock(CloudStorageSettingService.class);
        var target = new CloudStorageSettingService.Target("R2", "https://example.invalid", "auto", "new-bucket", "a", "b", "admin");
        when(settings.target()).thenReturn(Optional.of(target));
        var inner = mock(S3Client.class);
        try (var mocked = mockStatic(CloudStorageSettingService.class)) {
            mocked.when(() -> CloudStorageSettingService.build(target)).thenReturn(inner);
            var client = new CloudStorageConfiguration.SwitchingS3Client(settings);
            client.putObject(PutObjectRequest.builder().bucket("old-bucket").key("audio/x.mp3").build(), RequestBody.fromString("x"));
        }
        var put = ArgumentCaptor.forClass(PutObjectRequest.class);
        verify(inner).putObject(put.capture(), any(RequestBody.class));
        assertEquals("new-bucket", put.getValue().bucket());
        assertEquals("audio/x.mp3", put.getValue().key());
    }
}
