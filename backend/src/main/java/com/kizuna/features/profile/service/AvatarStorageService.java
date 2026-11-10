package com.kizuna.features.profile.service;

import com.kizuna.features.profile.dto.response.AvatarUploadResponseDto;
import org.springframework.web.multipart.MultipartFile;

/**
 * Lưu và phục vụ tệp ảnh đại diện của hồ sơ (FE-02, research.md quyết định 2).
 */
public interface AvatarStorageService {

    /** Dung lượng tối đa của một ảnh đại diện: 5MB. */
    long MAX_FILE_SIZE = 5L * 1024 * 1024;

    /** Nội dung ảnh đã lưu kèm kiểu MIME để trả về trình duyệt. */
    record AvatarFile(byte[] content, String contentType) {
    }

    /**
     * Kiểm tra (đuôi jpg/jpeg/png/webp, nội dung thật, tối đa 5MB) rồi lưu ảnh với tên ngẫu nhiên.
     * Tệp không hợp lệ thì ném BadRequestException với thông điệp tiếng Việt.
     */
    AvatarUploadResponseDto store(MultipartFile file);

    /**
     * Đọc ảnh đã lưu theo tên tệp do hệ thống sinh; tên lạ hoặc không còn tệp thì ném ResourceNotFoundException.
     */
    AvatarFile load(String fileName);
}
