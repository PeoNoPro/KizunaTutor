package com.kizuna.features.verification.dto.request;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Yêu cầu gia sư bổ sung thông tin (UC-26). Nội dung 1–2.000 ký tự, kiểm tra ở service.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RequestInfoRequest {
    private String message;
}
