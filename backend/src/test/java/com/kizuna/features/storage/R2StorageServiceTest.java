package com.kizuna.features.storage;

import com.kizuna.core.exception.BadRequestException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.web.MockMultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

import java.net.URL;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/** Kiểm tra R2StorageService không cần kết nối R2 thật (S3Client / S3Presigner là mock). */
class R2StorageServiceTest {

    private final S3Client s3 = mock(S3Client.class);
    private final S3Presigner presigner = mock(S3Presigner.class);
    private R2StorageService service;

    @BeforeEach
    void setUp() throws Exception {
        PresignedGetObjectRequest presigned = mock(PresignedGetObjectRequest.class);
        when(presigned.url()).thenReturn(new URL("https://r2.example/audio/test.mp3?sig=1"));
        when(presigner.presignGetObject(any(GetObjectPresignRequest.class))).thenReturn(presigned);
        service = new R2StorageService(s3, presigner, "kizunatutor-storage", 900);
    }

    @Test
    void uploadsMp3UnderAudioFolderWithRandomNameAndReturnsTemporaryLink() {
        var result = service.uploadAudio(new MockMultipartFile("file", "Như Một Người Dưng.mp3", "audio/mpeg", new byte[]{1, 2, 3}));

        ArgumentCaptor<PutObjectRequest> put = ArgumentCaptor.forClass(PutObjectRequest.class);
        verify(s3).putObject(put.capture(), any(RequestBody.class));
        assertThat(put.getValue().bucket()).isEqualTo("kizunatutor-storage");
        assertThat(put.getValue().key()).matches("^audio/[0-9a-f-]{36}\\.mp3$");
        assertThat(put.getValue().contentType()).isEqualTo("audio/mpeg");
        assertThat(result.objectKey()).isEqualTo(put.getValue().key());
        assertThat(result.downloadUrl()).startsWith("https://r2.example/");
    }

    @Test
    void guessesAudioContentTypeWhenBrowserSendsGenericType() {
        var result = service.uploadAudio(new MockMultipartFile("file", "a.m4a", "application/octet-stream", new byte[]{1}));
        assertThat(result.contentType()).isEqualTo("audio/mp4");
    }

    @Test
    void userMistakesAreBadRequestsNotServerErrors() {
        assertThatThrownBy(() -> service.uploadAudio(new MockMultipartFile("file", "a.mp3", "audio/mpeg", new byte[0])))
                .isInstanceOf(BadRequestException.class);
        assertThatThrownBy(() -> service.uploadAudio(new MockMultipartFile("file", "a.exe", "audio/mpeg", new byte[]{1})))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("MP3");
        byte[] big = new byte[25 * 1024 * 1024 + 1];
        Arrays.fill(big, (byte) 1);
        assertThatThrownBy(() -> service.uploadAudio(new MockMultipartFile("file", "big.mp3", "audio/mpeg", big)))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("25 MB");
        verifyNoInteractions(s3);
    }

    @Test
    void temporaryLinkOnlyForAudioObjects() {
        assertThat(service.presignedGetUrl("audio/abc.mp3")).startsWith("https://");
        assertThatThrownBy(() -> service.presignedGetUrl("avatars/x.png")).isInstanceOf(BadRequestException.class);
        assertThatThrownBy(() -> service.presignedGetUrl("audio/../secret.txt")).isInstanceOf(BadRequestException.class);
        assertThatThrownBy(() -> service.presignedGetUrl(null)).isInstanceOf(BadRequestException.class);
    }
}
