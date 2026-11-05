package com.kizuna.features.tutor_search.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * DTO chứa thông tin chứng chỉ, bằng cấp đã được xác minh của gia sư (UC-31).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TutorCredentialDTO {
    private UUID credentialId;
    private String credentialType;
    private String title;
    private String fileUrl;
    private String verifiedStatus;
}

