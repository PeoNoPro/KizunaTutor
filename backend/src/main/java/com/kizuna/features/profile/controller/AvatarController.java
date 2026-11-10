package com.kizuna.features.profile.controller;

import com.kizuna.core.dto.ApiResponse;
import com.kizuna.features.profile.dto.response.AvatarUploadResponseDto;
import com.kizuna.features.profile.service.AvatarStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.concurrent.TimeUnit;

/**
 * Tải lên và phục vụ ảnh đại diện (contracts/profile-api.md mục 3).
 */
@RestController
@RequestMapping("/api/profile/avatar")
@RequiredArgsConstructor
public class AvatarController {

    private final AvatarStorageService avatarStorageService;

    /** Người dùng đã đăng nhập tải ảnh đại diện lên (multipart, trường "file"). */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<AvatarUploadResponseDto>> upload(@RequestParam("file") MultipartFile file) {
        AvatarUploadResponseDto result = avatarStorageService.store(file);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Tải ảnh đại diện lên thành công.", result));
    }

    /** Ảnh đại diện công khai; tên tệp do hệ thống sinh nên nội dung không đổi, cho phép trình duyệt lưu đệm. */
    @GetMapping("/{fileName}")
    public ResponseEntity<byte[]> view(@PathVariable String fileName) {
        AvatarStorageService.AvatarFile avatar = avatarStorageService.load(fileName);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(avatar.contentType()))
                .cacheControl(CacheControl.maxAge(7, TimeUnit.DAYS).cachePublic())
                .header("X-Content-Type-Options", "nosniff")
                .body(avatar.content());
    }
}
