package com.kizuna.features.classroom.service;

/** Kho lưu trữ tệp tài liệu học tập (FE-06). Đường dẫn là đường dẫn đối tượng trong bucket riêng tư. */
public interface MaterialStorageService {

    boolean isConfigured();

    void upload(String objectPath, byte[] content, String contentType);

    /** @return URL ký tạm thời để mở/tải tệp. */
    String createSignedUrl(String objectPath);

    int signedUrlTtlSeconds();

    void delete(String objectPath);
}
