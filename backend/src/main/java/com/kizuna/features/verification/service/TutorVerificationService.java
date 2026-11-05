package com.kizuna.features.verification.service;

import com.kizuna.features.verification.dto.request.SubmitVerificationRequest;
import com.kizuna.features.verification.dto.request.UpdateVerificationProfileRequest;
import com.kizuna.features.verification.dto.response.TutorVerificationResponse;
import com.kizuna.features.verification.dto.response.VerificationDocumentResponse;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

/**
 * Nghiệp vụ xác minh phía gia sư (UC-22, UC-23, UC-24, UC-82, UC-83).
 * Mọi thao tác chỉ áp dụng cho hồ sơ của người dùng đang đăng nhập.
 */
public interface TutorVerificationService {

    TutorVerificationResponse getMyVerification();

    TutorVerificationResponse updateProfile(UpdateVerificationProfileRequest request);

    VerificationDocumentResponse addDocument(String type, String title, MultipartFile file);

    VerificationDocumentResponse updateDocument(UUID documentId, String type, String title, MultipartFile file);

    void removeDocument(UUID documentId);

    DocumentFile loadMyDocumentFile(UUID documentId);

    TutorVerificationResponse submit(SubmitVerificationRequest request);
}
