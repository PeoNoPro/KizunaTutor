package com.kizuna.features.verification.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Giấy tờ xác minh trả ra API. Không bao giờ chứa khóa lưu trữ hay đường dẫn tệp trên máy chủ.
 */
@Getter
@Builder
@AllArgsConstructor
public class VerificationDocumentResponse {
    private UUID id;
    private String type;
    private String typeLabel;
    private String title;
    private String fileName;
    private String contentType;
    private String status;
    private String statusLabel;
    private OffsetDateTime uploadedAt;
    private boolean fileAvailable;
}
