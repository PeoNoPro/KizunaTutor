package com.kizuna.features.profile.service.impl;

import com.kizuna.core.exception.BadRequestException;
import com.kizuna.core.exception.ResourceNotFoundException;
import com.kizuna.core.exception.ServiceUnavailableException;
import com.kizuna.features.profile.dto.response.AvatarUploadResponseDto;
import com.kizuna.features.profile.service.AvatarStorageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Lưu ảnh đại diện vào thư mục app.profile.avatar-storage-dir (ngoài frontend/ và static).
 * Tên tệp luôn do hệ thống sinh (UUID + đuôi), không dùng tên gốc của người dùng.
 */
@Slf4j
@Service
public class AvatarStorageServiceImpl implements AvatarStorageService {

    static final String PUBLIC_PATH = "/api/profile/avatar/";

    private static final String INVALID_FILE_MESSAGE =
            "Ảnh đại diện chỉ nhận tệp JPG, PNG hoặc WEBP, dung lượng tối đa 5MB.";
    private static final String NOT_FOUND_MESSAGE = "Không tìm thấy ảnh đại diện.";
    private static final Pattern FILE_NAME_PATTERN =
            Pattern.compile("^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}\\.(jpg|png|webp)$");

    private static final byte[] PNG_SIGNATURE = {(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A};
    private static final byte[] JPG_SIGNATURE = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF};
    private static final byte[] RIFF = "RIFF".getBytes(StandardCharsets.US_ASCII);
    private static final byte[] WEBP = "WEBP".getBytes(StandardCharsets.US_ASCII);

    private final Path storageDir;

    public AvatarStorageServiceImpl(@Value("${app.profile.avatar-storage-dir}") String storageDir) {
        this.storageDir = Paths.get(storageDir).toAbsolutePath().normalize();
    }

    @Override
    public AvatarUploadResponseDto store(MultipartFile file) {
        if (file == null || file.isEmpty() || file.getSize() > MAX_FILE_SIZE) {
            throw new BadRequestException(INVALID_FILE_MESSAGE);
        }
        String extension = normalizedExtension(file.getOriginalFilename());
        byte[] content = readBytes(file);
        if (content.length == 0 || content.length > MAX_FILE_SIZE || !matchesFormat(extension, content)) {
            throw new BadRequestException(INVALID_FILE_MESSAGE);
        }
        String fileName = UUID.randomUUID() + "." + extension;
        try {
            Files.createDirectories(storageDir);
            Files.write(storageDir.resolve(fileName), content);
        } catch (IOException e) {
            log.error("Không ghi được ảnh đại diện {}", fileName, e);
            throw new ServiceUnavailableException("Chưa lưu được ảnh đại diện. Vui lòng thử lại sau.");
        }
        return new AvatarUploadResponseDto(PUBLIC_PATH + fileName, fileName, content.length);
    }

    @Override
    public AvatarFile load(String fileName) {
        if (fileName == null || !FILE_NAME_PATTERN.matcher(fileName).matches()) {
            throw new ResourceNotFoundException(NOT_FOUND_MESSAGE);
        }
        Path path = storageDir.resolve(fileName).normalize();
        if (!path.startsWith(storageDir) || !Files.isRegularFile(path)) {
            throw new ResourceNotFoundException(NOT_FOUND_MESSAGE);
        }
        try {
            return new AvatarFile(Files.readAllBytes(path), contentType(fileName));
        } catch (IOException e) {
            log.warn("Không đọc được ảnh đại diện {}: {}", fileName, e.getMessage());
            throw new ResourceNotFoundException(NOT_FOUND_MESSAGE);
        }
    }

    private static byte[] readBytes(MultipartFile file) {
        try {
            return file.getBytes();
        } catch (IOException e) {
            throw new BadRequestException(INVALID_FILE_MESSAGE);
        }
    }

    private static String normalizedExtension(String originalFilename) {
        String name = originalFilename == null ? "" : originalFilename;
        int dot = name.lastIndexOf('.');
        String ext = dot < 0 ? "" : name.substring(dot + 1).toLowerCase(Locale.ROOT);
        return switch (ext) {
            case "jpg", "jpeg" -> "jpg";
            case "png" -> "png";
            case "webp" -> "webp";
            default -> throw new BadRequestException(INVALID_FILE_MESSAGE);
        };
    }

    private static boolean matchesFormat(String extension, byte[] content) {
        return switch (extension) {
            case "png" -> startsWith(content, PNG_SIGNATURE, 0);
            case "jpg" -> startsWith(content, JPG_SIGNATURE, 0);
            case "webp" -> startsWith(content, RIFF, 0) && startsWith(content, WEBP, 8);
            default -> false;
        };
    }

    private static boolean startsWith(byte[] content, byte[] prefix, int offset) {
        if (content.length < offset + prefix.length) {
            return false;
        }
        for (int i = 0; i < prefix.length; i++) {
            if (content[offset + i] != prefix[i]) {
                return false;
            }
        }
        return true;
    }

    private static String contentType(String fileName) {
        if (fileName.endsWith(".png")) {
            return "image/png";
        }
        if (fileName.endsWith(".webp")) {
            return "image/webp";
        }
        return "image/jpeg";
    }
}
