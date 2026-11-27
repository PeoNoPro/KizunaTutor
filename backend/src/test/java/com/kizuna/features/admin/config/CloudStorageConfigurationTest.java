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
import software.amazon.awssdk.services.s3.model.*;

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

    /** Vừa đổi kho, tệp chưa chép sang: đọc ở kho mới bị 404 thì đọc ở kho cũ (không báo 404 cho người dùng). */
    @Test
    void missingFileInNewStoreIsReadFromPreviousStore() {
        var settings = mock(CloudStorageSettingService.class);
        var current = new CloudStorageSettingService.Target("B2", "https://new.invalid", "us-east-005", "new-bucket", "a", "b", "admin");
        var old = new CloudStorageSettingService.Target("R2", "https://old.invalid", "auto", "old-bucket", "c", "d", "previous");
        when(settings.target()).thenReturn(Optional.of(current));
        when(settings.fallback()).thenReturn(Optional.of(old));
        var newStore = mock(S3Client.class);
        var oldStore = mock(S3Client.class);
        when(newStore.headObject(any(HeadObjectRequest.class))).thenThrow(NoSuchKeyException.builder().statusCode(404).message("missing").build());
        when(oldStore.headObject(any(HeadObjectRequest.class))).thenReturn(HeadObjectResponse.builder().contentLength(7L).build());
        try (var mocked = mockStatic(CloudStorageSettingService.class)) {
            mocked.when(() -> CloudStorageSettingService.build(current)).thenReturn(newStore);
            mocked.when(() -> CloudStorageSettingService.build(old)).thenReturn(oldStore);
            var client = new CloudStorageConfiguration.SwitchingS3Client(settings);
            assertEquals(7L, client.headObject(HeadObjectRequest.builder().bucket("x").key("avatars/a.png").build()).contentLength());
        }
        var head = ArgumentCaptor.forClass(HeadObjectRequest.class);
        verify(oldStore).headObject(head.capture());
        assertEquals("old-bucket", head.getValue().bucket());
    }

    /** Lỗi khác 404 (ví dụ sai quyền) không được che bằng kho cũ. */
    @Test
    void otherErrorsAreNotHiddenByFallback() {
        var settings = mock(CloudStorageSettingService.class);
        var current = new CloudStorageSettingService.Target("B2", "https://new.invalid", "us-east-005", "new-bucket", "a", "b", "admin");
        when(settings.target()).thenReturn(Optional.of(current));
        when(settings.fallback()).thenReturn(Optional.of(new CloudStorageSettingService.Target("R2", "https://old.invalid", "auto", "old", "c", "d", "previous")));
        var newStore = mock(S3Client.class);
        when(newStore.headObject(any(HeadObjectRequest.class))).thenThrow(S3Exception.builder().statusCode(403).message("denied").build());
        try (var mocked = mockStatic(CloudStorageSettingService.class)) {
            mocked.when(() -> CloudStorageSettingService.build(current)).thenReturn(newStore);
            var client = new CloudStorageConfiguration.SwitchingS3Client(settings);
            assertEquals(403, assertThrows(S3Exception.class, () -> client.headObject(HeadObjectRequest.builder().key("k").build())).statusCode());
        }
    }
}
