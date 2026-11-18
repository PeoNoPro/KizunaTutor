package com.kizuna.features.practice.storage;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.http.AbortableInputStream;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.io.ByteArrayInputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class R2PracticeFileStorageTest {
    @TempDir Path dir;

    @Test
    void saveUploadsToPracticeFolderAndLoadDownloadsWhenCacheMissing() throws Exception {
        S3Client s3 = mock(S3Client.class);
        var local = new LocalPracticeFileStorage(dir.toString());
        var storage = new R2PracticeFileStorage(s3, "bucket", local);

        assertEquals(3, storage.save("exam/1/a.mp3", new ByteArrayInputStream(new byte[]{1, 2, 3}), 100));
        var put = ArgumentCaptor.forClass(PutObjectRequest.class);
        verify(s3).putObject(put.capture(), any(RequestBody.class));
        assertEquals("audio/a.mp3", put.getValue().key());
        assertEquals("audio/mpeg", put.getValue().contentType());

        // Mất bộ nhớ đệm trên máy chủ → tải lại từ R2.
        Files.delete(dir.resolve("exam/1/a.mp3"));
        when(s3.getObject(any(GetObjectRequest.class))).thenReturn(new ResponseInputStream<>(
                GetObjectResponse.builder().build(), AbortableInputStream.create(new ByteArrayInputStream(new byte[]{9, 9}))));
        assertEquals(2, storage.load("exam/1/a.mp3").contentLength());

        assertEquals("images/x.png", R2PracticeFileStorage.r2Key("exam/1/x.png"));
        storage.delete("exam/1/a.mp3");
        verify(s3).deleteObject(DeleteObjectRequest.builder().bucket("bucket").key("audio/a.mp3").build());
        assertFalse(Files.exists(dir.resolve("exam/1/a.mp3")));
    }

    @Test
    void failedUploadRemovesCachedFile() {
        S3Client s3 = mock(S3Client.class);
        when(s3.putObject(any(PutObjectRequest.class), any(RequestBody.class))).thenThrow(S3Exception.builder().message("down").build());
        var storage = new R2PracticeFileStorage(s3, "bucket", new LocalPracticeFileStorage(dir.toString()));
        assertThrows(RuntimeException.class, () -> storage.save("exam/1/b.mp3", new ByteArrayInputStream(new byte[]{1}), 100));
        assertFalse(Files.exists(dir.resolve("exam/1/b.mp3")));
    }
}
