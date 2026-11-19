package com.kizuna.features.profile.dto.response;

/**
 * Kết quả tải ảnh đại diện (contracts/profile-api.md mục 3.1).
 *
 * @param avatarUrl đường dẫn công khai để hiển thị ảnh, dạng /api/profile/avatar/{fileName}
 * @param fileName  tên tệp do hệ thống sinh ra
 * @param size      dung lượng tệp (byte)
 */
public record AvatarUploadResponseDto(String avatarUrl, String fileName, long size) {
}
