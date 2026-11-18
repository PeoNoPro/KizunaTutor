package com.kizuna.features.admin.config;

import com.kizuna.features.admin.service.CloudStorageSettingService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.core.sync.ResponseTransformer;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.*;

import java.net.URI;
import java.util.concurrent.ConcurrentHashMap;

/**
 * S3Client / S3Presigner dùng chung cho mọi chức năng lưu tệp (ảnh đại diện, tài liệu, bài nộp, hỗ trợ, xác minh,
 * luyện đề). Mỗi lần gọi đều chuyển tới kho Admin đang chọn ở trang Cấu hình hệ thống (chưa chọn thì kho trong cấu hình
 * server), và tự thay tên bucket trong yêu cầu bằng bucket của kho đó. Nhờ vậy đổi kho có hiệu lực ngay mà các service
 * khác vẫn tiêm S3Client như cũ, không phải sửa code của họ (giống cách AdminMailConfiguration làm với bộ gửi thư).
 * Lúc khởi động chưa có kho nào thì không tạo bean (các chức năng lưu như khi tắt R2); bật lần đầu cần khởi động lại.
 */
@Slf4j
@Configuration
public class CloudStorageConfiguration {

    @Bean(destroyMethod = "close")
    @Primary
    public S3Client cloudStorageS3Client(CloudStorageSettingService settings) {
        if (settings.target().isEmpty()) return null;
        settings.markRuntimeActive();
        return new SwitchingS3Client(settings);
    }

    @Bean(destroyMethod = "close")
    @Primary
    public S3Presigner cloudStoragePresigner(CloudStorageSettingService settings) {
        return settings.target().isEmpty() ? null : new SwitchingPresigner(settings);
    }

    /** Một client cho mỗi bộ cấu hình; đổi kho thì dùng client mới, client cũ đóng khi tắt server. */
    static final class Clients<T extends AutoCloseable> {
        private final ConcurrentHashMap<String, T> byTarget = new ConcurrentHashMap<>();
        T get(CloudStorageSettingService.Target target, java.util.function.Function<CloudStorageSettingService.Target, T> factory) {
            return byTarget.computeIfAbsent(target.fingerprint(), key -> factory.apply(target));
        }
        void close() {
            byTarget.values().forEach(client -> { try { client.close(); } catch (Exception ignored) { /* đang tắt server */ } });
            byTarget.clear();
        }
    }

    static CloudStorageSettingService.Target require(CloudStorageSettingService settings) {
        return settings.target().orElseThrow(() -> SdkClientException.create("Chưa cấu hình lưu trữ đám mây."));
    }

    public static final class SwitchingS3Client implements S3Client {
        private final CloudStorageSettingService settings;
        private final Clients<S3Client> clients = new Clients<>();

        public SwitchingS3Client(CloudStorageSettingService settings) { this.settings = settings; }

        private CloudStorageSettingService.Target target() { return require(settings); }
        private S3Client client(CloudStorageSettingService.Target target) { return clients.get(target, CloudStorageSettingService::build); }

        @Override public String serviceName() { return SERVICE_NAME; }
        @Override public void close() { clients.close(); }

        @Override public PutObjectResponse putObject(PutObjectRequest request, RequestBody body) {
            var t = target(); return client(t).putObject(request.toBuilder().bucket(t.bucket()).build(), body);
        }
        @Override public <R> R getObject(GetObjectRequest request, ResponseTransformer<GetObjectResponse, R> transformer) {
            var t = target(); return client(t).getObject(request.toBuilder().bucket(t.bucket()).build(), transformer);
        }
        @Override public ResponseInputStream<GetObjectResponse> getObject(GetObjectRequest request) {
            var t = target(); return client(t).getObject(request.toBuilder().bucket(t.bucket()).build());
        }
        @Override public HeadObjectResponse headObject(HeadObjectRequest request) {
            var t = target(); return client(t).headObject(request.toBuilder().bucket(t.bucket()).build());
        }
        @Override public DeleteObjectResponse deleteObject(DeleteObjectRequest request) {
            var t = target(); return client(t).deleteObject(request.toBuilder().bucket(t.bucket()).build());
        }
        @Override public ListObjectsV2Response listObjectsV2(ListObjectsV2Request request) {
            var t = target(); return client(t).listObjectsV2(request.toBuilder().bucket(t.bucket()).build());
        }
        @Override public CopyObjectResponse copyObject(CopyObjectRequest request) {
            var t = target(); return client(t).copyObject(request.toBuilder().sourceBucket(t.bucket()).destinationBucket(t.bucket()).build());
        }
        @Override public HeadBucketResponse headBucket(HeadBucketRequest request) {
            var t = target(); return client(t).headBucket(request.toBuilder().bucket(t.bucket()).build());
        }
    }

    public static final class SwitchingPresigner implements S3Presigner {
        private final CloudStorageSettingService settings;
        private final Clients<S3Presigner> presigners = new Clients<>();

        SwitchingPresigner(CloudStorageSettingService settings) { this.settings = settings; }

        private static S3Presigner build(CloudStorageSettingService.Target t) {
            return S3Presigner.builder()
                    .endpointOverride(URI.create(t.endpoint()))
                    .region(Region.of(t.region() == null || t.region().isBlank() ? "auto" : t.region()))
                    .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create(t.accessKey(), t.secretKey())))
                    .serviceConfiguration(S3Configuration.builder().pathStyleAccessEnabled(!"S3".equals(t.provider())).build())
                    .build();
        }

        @Override public PresignedGetObjectRequest presignGetObject(GetObjectPresignRequest request) {
            var t = require(settings);
            var get = request.getObjectRequest().toBuilder().bucket(t.bucket()).build();
            return presigners.get(t, SwitchingPresigner::build).presignGetObject(request.toBuilder().getObjectRequest(get).build());
        }
        @Override public PresignedPutObjectRequest presignPutObject(PutObjectPresignRequest request) {
            var t = require(settings);
            var put = request.putObjectRequest().toBuilder().bucket(t.bucket()).build();
            return presigners.get(t, SwitchingPresigner::build).presignPutObject(request.toBuilder().putObjectRequest(put).build());
        }
        @Override public PresignedDeleteObjectRequest presignDeleteObject(DeleteObjectPresignRequest request) {
            var t = require(settings);
            var delete = request.deleteObjectRequest().toBuilder().bucket(t.bucket()).build();
            return presigners.get(t, SwitchingPresigner::build).presignDeleteObject(request.toBuilder().deleteObjectRequest(delete).build());
        }
        // Hệ thống không tải tệp nhiều phần qua đường dẫn ký sẵn.
        @Override public PresignedCreateMultipartUploadRequest presignCreateMultipartUpload(CreateMultipartUploadPresignRequest request) { throw new UnsupportedOperationException(); }
        @Override public PresignedUploadPartRequest presignUploadPart(UploadPartPresignRequest request) { throw new UnsupportedOperationException(); }
        @Override public PresignedCompleteMultipartUploadRequest presignCompleteMultipartUpload(CompleteMultipartUploadPresignRequest request) { throw new UnsupportedOperationException(); }
        @Override public PresignedAbortMultipartUploadRequest presignAbortMultipartUpload(AbortMultipartUploadPresignRequest request) { throw new UnsupportedOperationException(); }
        @Override public void close() { presigners.close(); }
    }
}
