package com.kizuna.features.practice.storage;

import com.kizuna.core.exception.BusinessException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

import java.io.ByteArrayInputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/** Lưu file luyện đề trên kho đám mây: không giữ bản đệm trên máy chủ, phát bằng đường dẫn ký sẵn của kho. */
class R2PracticeFileStorageTest {
    @TempDir Path dir;

    private static S3Presigner presigner() {
        return S3Presigner.builder().endpointOverride(URI.create("https://acc.r2.cloudflarestorage.com")).region(Region.of("auto"))
                .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create("key", "secret")))
                .serviceConfiguration(S3Configuration.builder().pathStyleAccessEnabled(true).build()).build();
    }

    @Test
    void saveUploadsToAudioFolderWithoutKeepingALocalCopy() {
        S3Client s3 = mock(S3Client.class);
        var storage = new R2PracticeFileStorage(s3, null, "bucket", new LocalPracticeFileStorage(dir.toString()));

        assertEquals(3, storage.save("exam/1/a.mp3", new ByteArrayInputStream(new byte[]{1, 2, 3}), 100));
        var put = ArgumentCaptor.forClass(PutObjectRequest.class);
        verify(s3).putObject(put.capture(), any(RequestBody.class));
        assertEquals("audio/a.mp3", put.getValue().key());
        assertEquals("audio/mpeg", put.getValue().contentType());
        assertFalse(Files.exists(dir.resolve("exam/1/a.mp3")), "không còn bộ nhớ đệm trên máy chủ");
        assertEquals("images/x.png", R2PracticeFileStorage.r2Key("exam/1/x.png"));

        storage.delete("exam/1/a.mp3");
        verify(s3).deleteObject(DeleteObjectRequest.builder().bucket("bucket").key("audio/a.mp3").build());
    }

    @Test
    void tooLargeFileIsRejectedBeforeUploading() {
        S3Client s3 = mock(S3Client.class);
        var storage = new R2PracticeFileStorage(s3, null, "bucket", new LocalPracticeFileStorage(dir.toString()));
        var error = assertThrows(BusinessException.class, () -> storage.save("exam/1/b.mp3", new ByteArrayInputStream(new byte[10]), 5));
        assertEquals(413, error.status());
        verify(s3, never()).putObject(any(PutObjectRequest.class), any(RequestBody.class));
    }

    @Test
    void failedUploadReportsServiceUnavailable() {
        S3Client s3 = mock(S3Client.class);
        when(s3.putObject(any(PutObjectRequest.class), any(RequestBody.class))).thenThrow(S3Exception.builder().message("down").build());
        var storage = new R2PracticeFileStorage(s3, null, "bucket", new LocalPracticeFileStorage(dir.toString()));
        assertEquals(503, assertThrows(BusinessException.class, () -> storage.save("exam/1/b.mp3", new ByteArrayInputStream(new byte[]{1}), 100)).status());
    }

    @Test
    void directUrlIsASignedLinkToTheBucketObject() {
        try (var presigner = presigner()) {
            var storage = new R2PracticeFileStorage(mock(S3Client.class), presigner, "bucket", new LocalPracticeFileStorage(dir.toString()));
            String url = storage.directUrl("exam/1/a.mp3").orElseThrow();
            assertTrue(url.startsWith("https://acc.r2.cloudflarestorage.com/bucket/audio/a.mp3?"), url);
            assertTrue(url.contains("X-Amz-Signature="));
            assertTrue(url.contains("X-Amz-Expires=21600"), "hạn 6 giờ");
        }
    }

    @Test
    void withoutCloudStorageFilesStayOnTheServer() throws Exception {
        var storage = new R2PracticeFileStorage((S3Client) null, null, "", new LocalPracticeFileStorage(dir.toString()));
        storage.save("exam/1/c.mp3", new ByteArrayInputStream(new byte[]{1, 2}), 100);
        assertTrue(Files.exists(dir.resolve("exam/1/c.mp3")));
        assertTrue(storage.directUrl("exam/1/c.mp3").isEmpty(), "máy chủ tự phát file");
        assertEquals(2, storage.load("exam/1/c.mp3").contentLength());
    }
}
