package com.kizuna.features.verification.controller;

import com.kizuna.features.verification.service.DocumentFile;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.nio.charset.StandardCharsets;

/**
 * Phản hồi tải tệp giấy tờ: hiển thị trực tiếp, không cache, không cho trình duyệt đoán kiểu nội dung.
 */
final class DocumentFileResponses {

    private DocumentFileResponses() {
    }

    static ResponseEntity<Resource> inline(DocumentFile file) {
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(file.contentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.inline()
                        .filename(file.fileName(), StandardCharsets.UTF_8)
                        .build()
                        .toString())
                .header("X-Content-Type-Options", "nosniff")
                .cacheControl(CacheControl.noStore())
                .contentLength(file.content().length)
                .body(new ByteArrayResource(file.content()));
    }
}
