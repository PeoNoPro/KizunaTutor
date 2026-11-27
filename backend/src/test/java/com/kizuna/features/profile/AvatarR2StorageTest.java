package com.kizuna.features.profile;

import com.kizuna.core.exception.BadRequestException;
import com.kizuna.core.exception.ResourceNotFoundException;
import com.kizuna.features.profile.dto.response.AvatarUploadResponseDto;
import com.kizuna.features.profile.service.impl.AvatarStorageServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.web.MockMultipartFile;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.pagination.sync.SdkIterable;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Object;
import software.amazon.awssdk.services.s3.paginators.ListObjectsV2Iterable;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/** Ảnh đại diện khi bật R2: ghi / đọc / xóa trên bucket, không đụng ổ đĩa (S3Client là mock). */
class AvatarR2StorageTest {

    private static final byte[] PNG = {(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 0x00, 0x01};
    private static final UUID OWNER = UUID.fromString("11111111-2222-3333-4444-555555555555");

    @TempDir
    Path storageDir;

    private final S3Client s3 = mock(S3Client.class);

    private static SdkIterable<S3Object> objects(S3Object... items) {
        return () -> List.of(items).iterator();
    }

    private AvatarStorageServiceImpl service() {
        ListObjectsV2Iterable listing = mock(ListObjectsV2Iterable.class);
        when(listing.contents()).thenReturn(objects());
        when(s3.listObjectsV2Paginator(any(ListObjectsV2Request.class))).thenReturn(listing);
        return new AvatarStorageServiceImpl(storageDir.toString(), s3, "bucket");
    }

    @Test
    void uploadGoesToR2AvatarsFolderNotDisk() throws Exception {
        AvatarUploadResponseDto result = service().store(new MockMultipartFile("file", "a.png", "image/png", PNG), OWNER, null);

        ArgumentCaptor<PutObjectRequest> put = ArgumentCaptor.forClass(PutObjectRequest.class);
        verify(s3).putObject(put.capture(), any(RequestBody.class));
        assertThat(put.getValue().bucket()).isEqualTo("bucket");
        assertThat(put.getValue().key()).isEqualTo("avatars/" + result.fileName());
        assertThat(put.getValue().contentType()).isEqualTo("image/png");
        assertThat(result.avatarUrl()).isEqualTo("/api/profile/avatar/" + result.fileName());
        try (var files = Files.list(storageDir)) {
            assertThat(files).isEmpty();
        }
    }

    @Test
    void uploadDeletesOwnUnusedR2Uploads() {
        String unused = OWNER + "_00000000-0000-0000-0000-000000000001.png";
        String current = OWNER + "_00000000-0000-0000-0000-000000000002.png";
        AvatarStorageServiceImpl service = service();
        ListObjectsV2Iterable listing = mock(ListObjectsV2Iterable.class);
        when(listing.contents()).thenReturn(objects(
                S3Object.builder().key("avatars/" + unused).build(),
                S3Object.builder().key("avatars/" + current).build()));
        when(s3.listObjectsV2Paginator(any(ListObjectsV2Request.class))).thenReturn(listing);

        service.store(new MockMultipartFile("file", "a.png", "image/png", PNG), OWNER, "/api/profile/avatar/" + current);

        ArgumentCaptor<DeleteObjectRequest> delete = ArgumentCaptor.forClass(DeleteObjectRequest.class);
        verify(s3).deleteObject(delete.capture());
        assertThat(delete.getValue().key()).isEqualTo("avatars/" + unused);
    }

    @Test
    void loadReadsFromR2() {
        when(s3.getObjectAsBytes(any(GetObjectRequest.class)))
                .thenReturn(ResponseBytes.fromByteArray(GetObjectResponse.builder().build(), PNG));

        var file = service().load(OWNER + "_00000000-0000-0000-0000-000000000001.png");

        assertThat(file.content()).isEqualTo(PNG);
        assertThat(file.contentType()).isEqualTo("image/png");
    }

    @Test
    void missingR2ObjectIsNotFound() {
        when(s3.getObjectAsBytes(any(GetObjectRequest.class))).thenThrow(NoSuchKeyException.builder().build());
        assertThatThrownBy(() -> service().load(OWNER + "_00000000-0000-0000-0000-000000000001.png"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void requireOwnAvatarChecksR2() {
        String mine = "/api/profile/avatar/" + OWNER + "_00000000-0000-0000-0000-000000000001.png";
        AvatarStorageServiceImpl service = service();

        assertThatCode(() -> service.requireOwnAvatar(mine, null, OWNER)).doesNotThrowAnyException();
        verify(s3).headObject(any(HeadObjectRequest.class));

        when(s3.headObject(any(HeadObjectRequest.class))).thenThrow(NoSuchKeyException.builder().build());
        assertThatThrownBy(() -> service.requireOwnAvatar(mine, null, OWNER)).isInstanceOf(BadRequestException.class);
    }
}
