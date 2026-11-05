package com.kizuna.features.verification.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Từ chối hồ sơ (UC-28). Kiểm tra bắt buộc thực hiện ở service để trả đúng thông điệp UC-28 AF-1.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RejectVerificationRequest {
    private String reasonCode;
    private String feedback;
    private String internalNote;
}
