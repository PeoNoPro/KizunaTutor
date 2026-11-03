package com.kizuna.features.verification.controller;

import com.kizuna.features.verification.dto.request.SubmitVerificationRequest;
import com.kizuna.features.verification.dto.request.UpdateVerificationProfileRequest;
import com.kizuna.features.verification.dto.response.TutorVerificationResponse;
import com.kizuna.features.verification.dto.response.VerificationDocumentResponse;
import com.kizuna.features.verification.service.TutorVerificationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

/**
 * API xác minh hồ sơ phía gia sư (FE-03: UC-22, UC-23, UC-24, UC-82, UC-83).
 * Chỉ ROLE_TUTOR truy cập được (SecurityConfig); dữ liệu luôn là của người dùng đang đăng nhập.
 */
@RestController
@CrossOrigin(origins = {"http://localhost:3000", "http://127.0.0.1:3000"})
@RequestMapping("/api/tutor/verification")
@RequiredArgsConstructor
public class TutorVerificationController {

    private final TutorVerificationService verificationService;

    @GetMapping
    public ResponseEntity<TutorVerificationResponse> getMyVerification() {
        return ResponseEntity.ok(verificationService.getMyVerification());
    }

    @PutMapping
    public ResponseEntity<TutorVerificationResponse> updateProfile(
            @Valid @RequestBody UpdateVerificationProfileRequest request) {
        return ResponseEntity.ok(verificationService.updateProfile(request));
    }

    @PostMapping(value = "/documents", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<VerificationDocumentResponse> addDocument(
            @RequestParam(value = "type", required = false) String type,
            @RequestParam(value = "title", required = false) String title,
            @RequestPart(value = "file", required = false) MultipartFile file) {
        return ResponseEntity.status(HttpStatus.CREATED).body(verificationService.addDocument(type, title, file));
    }

    @PutMapping(value = "/documents/{documentId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<VerificationDocumentResponse> updateDocument(
            @PathVariable UUID documentId,
            @RequestParam(value = "type", required = false) String type,
            @RequestParam(value = "title", required = false) String title,
            @RequestPart(value = "file", required = false) MultipartFile file) {
        return ResponseEntity.ok(verificationService.updateDocument(documentId, type, title, file));
    }

    @DeleteMapping("/documents/{documentId}")
    public ResponseEntity<Void> removeDocument(@PathVariable UUID documentId) {
        verificationService.removeDocument(documentId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/documents/{documentId}/file")
    public ResponseEntity<Resource> downloadDocument(@PathVariable UUID documentId) {
        return DocumentFileResponses.inline(verificationService.loadMyDocumentFile(documentId));
    }

    @PostMapping("/submit")
    public ResponseEntity<TutorVerificationResponse> submit(@RequestBody(required = false) SubmitVerificationRequest request) {
        return ResponseEntity.ok(verificationService.submit(request));
    }
}
