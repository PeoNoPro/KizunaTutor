package com.kizuna.features.profile.service;

import com.kizuna.features.profile.dto.response.AvatarUploadResponseDto;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

/**
 * Lưu và phục vụ tệp ảnh đại diện của hồ sơ (FE-02, research.md quyết định 2).
 * Mỗi tệp gắn với người tải lên (tiền tố userId trong tên tệp) để dọn ảnh không còn dùng.
 */
public interface AvatarStorageService {

    /** Dung lượng tối đa của một ảnh đại diện: 5MB. */
    long MAX_FILE_SIZE = 5L * 1024 * 1024;

    /** Nội dung ảnh đã lưu kèm kiểu MIME để trả về trình duyệt. */
    record AvatarFile(byte[] content, String contentType) {
    }

    /**
     * Kiểm tra (đuôi jpg/jpeg/png/webp, nội dung thật, tối đa 5MB) rồi lưu ảnh với tên "{ownerId}_{ngẫu nhiên}".
     * Ảnh tải lên trước đó của người này mà chưa dùng làm ảnh đại diện (khác currentAvatarUrl) bị xóa,
     * nên mỗi người chỉ còn tối đa ảnh đang dùng và ảnh vừa tải. Tệp không hợp lệ thì ném BadRequestException.
     */
    AvatarUploadResponseDto store(MultipartFile file, UUID ownerId, String currentAvatarUrl);

    /**
     * Đọc ảnh đã lưu theo tên tệp do hệ thống sinh; tên lạ hoặc không còn tệp thì ném ResourceNotFoundException.
     */
    AvatarFile load(String fileName);

    /**
     * Ảnh đại diện mới phải là ảnh chính người này đã tải lên hệ thống (/api/profile/avatar/...).
     * Giữ nguyên ảnh đang có (kể cả link ảnh ngoài của dữ liệu cũ) hoặc xóa ảnh thì cho qua.
     * Không hợp lệ thì ném BadRequestException.
     */
    void requireOwnAvatar(String newAvatarUrl, String currentAvatarUrl, UUID ownerId);

    /** Hồ sơ đã đổi / xóa ảnh: xóa tệp ảnh cũ do hệ thống lưu (sau khi transaction lưu hồ sơ commit). */
    void deleteReplaced(String oldAvatarUrl, String newAvatarUrl);
}
