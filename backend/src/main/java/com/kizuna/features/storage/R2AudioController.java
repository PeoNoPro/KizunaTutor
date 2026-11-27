package com.kizuna.features.storage;

import com.kizuna.core.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/storage/audio")
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "app.r2", name = "enabled", havingValue = "true")
public class R2AudioController {
    private final R2StorageService storage;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('ROLE_TUTOR')")
    public ResponseEntity<ApiResponse<R2StorageService.UploadedAudio>> upload(@RequestParam("file") MultipartFile file) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Tải âm thanh lên thành công.", storage.uploadAudio(file)));
    }

    @GetMapping("/url")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<DownloadUrl> downloadUrl(@RequestParam String objectKey) {
        return ApiResponse.ok(new DownloadUrl(storage.presignedGetUrl(objectKey)));
    }

    public record DownloadUrl(String url) {}
}
