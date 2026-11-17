package com.kizuna.features.storage;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class R2FileStoreTest {

    private static final byte[] DATA = {1, 2, 3};
    private final S3Client s3 = mock(S3Client.class);
    private final R2FileStore store = new R2FileStore(s3, "bucket");

    @Test
    void disabledKeepsContentForDatabase() {
        R2FileStore off = R2FileStore.disabled();
        assertThat(off.enabled()).isFalse();
        assertThat(off.put("materials/x", DATA, "application/pdf")).isEqualTo(DATA);
        assertThat(off.read(DATA, "materials/x")).isEqualTo(DATA);
        assertThat(off.read(null, "materials/x")).isNull();
    }

    @Test
    void enabledUploadsAndLeavesDatabaseColumnEmpty() {
        assertThat(store.put("materials/x", DATA, "application/pdf")).isNull();

        ArgumentCaptor<PutObjectRequest> put = ArgumentCaptor.forClass(PutObjectRequest.class);
        verify(s3).putObject(put.capture(), any(RequestBody.class));
        assertThat(put.getValue().bucket()).isEqualTo("bucket");
        assertThat(put.getValue().key()).isEqualTo("materials/x");
        assertThat(put.getValue().contentType()).isEqualTo("application/pdf");
    }

    @Test
    void readPrefersOldDatabaseContentThenR2() {
        assertThat(store.read(DATA, "materials/x")).isEqualTo(DATA);
        verifyNoInteractions(s3);

        when(s3.getObjectAsBytes(any(GetObjectRequest.class)))
                .thenReturn(ResponseBytes.fromByteArray(GetObjectResponse.builder().build(), DATA));
        assertThat(store.read(null, "materials/x")).isEqualTo(DATA);
    }

    @Test
    void missingR2ObjectReadsAsNull() {
        when(s3.getObjectAsBytes(any(GetObjectRequest.class))).thenThrow(NoSuchKeyException.builder().build());
        assertThat(store.read(new byte[0], "materials/x")).isNull();
    }

    @Test
    void deleteOutsideTransactionRunsImmediately() {
        store.deleteAfterCommit(List.of("materials/a", "materials/b"));
        verify(s3, times(2)).deleteObject(any(DeleteObjectRequest.class));
    }
}
