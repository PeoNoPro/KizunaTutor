package com.kizuna.features.profile;

import com.kizuna.core.exception.BadRequestException;
import com.kizuna.core.exception.ResourceNotFoundException;
import com.kizuna.features.profile.dto.response.AvatarUploadResponseDto;
import com.kizuna.features.profile.service.AvatarStorageService;
import com.kizuna.features.profile.service.impl.AvatarStorageServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AvatarStorageServiceTest {

    private static final byte[] PNG = {(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 0x00, 0x01};
    private static final byte[] JPG = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 0x00, 0x10};
    private static final byte[] WEBP = "RIFF\0\0\0\0WEBPVP8 ".getBytes(StandardCharsets.ISO_8859_1);

    @TempDir
    Path storageDir;

    private AvatarStorageService service;

    @BeforeEach
    void setUp() {
        service = new AvatarStorageServiceImpl(storageDir.toString());
    }

    @Test
    @DisplayName("Lưu ảnh PNG hợp lệ: sinh tên UUID, trả đường dẫn công khai và ghi tệp vào thư mục")
    void storesValidPng() throws Exception {
        AvatarUploadResponseDto result = service.store(new MockMultipartFile("file", "Ảnh của tôi.PNG", "image/png", PNG));

        assertThat(result.fileName()).matches("^[0-9a-f-]{36}\\.png$");
        assertThat(result.avatarUrl()).isEqualTo("/api/profile/avatar/" + result.fileName());
        assertThat(result.size()).isEqualTo(PNG.length);
        assertThat(Files.readAllBytes(storageDir.resolve(result.fileName()))).isEqualTo(PNG);
    }

    @Test
    @DisplayName("Đuôi .jpeg được chuẩn hóa thành .jpg; WEBP hợp lệ được chấp nhận")
    void acceptsJpegAndWebp() {
        assertThat(service.store(new MockMultipartFile("file", "a.jpeg", "image/jpeg", JPG)).fileName()).endsWith(".jpg");
        assertThat(service.store(new MockMultipartFile("file", "a.webp", "image/webp", WEBP)).fileName()).endsWith(".webp");
    }

    @Test
    @DisplayName("Từ chối đuôi không hỗ trợ, tệp rỗng, tệp null và nội dung không khớp đuôi")
    void rejectsInvalidFiles() {
        assertThatThrownBy(() -> service.store(new MockMultipartFile("file", "a.gif", "image/gif", PNG)))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Ảnh đại diện chỉ nhận tệp JPG, PNG hoặc WEBP, dung lượng tối đa 5MB.");
        assertThatThrownBy(() -> service.store(new MockMultipartFile("file", "a.png", "image/png", new byte[0])))
                .isInstanceOf(BadRequestException.class);
        assertThatThrownBy(() -> service.store(new MockMultipartFile("file", "a.png", "image/png", JPG)))
                .isInstanceOf(BadRequestException.class);
        assertThatThrownBy(() -> service.store(null)).isInstanceOf(BadRequestException.class);
    }

    @Test
    @DisplayName("Từ chối ảnh vượt quá 5MB")
    void rejectsOversizedFile() {
        byte[] big = Arrays.copyOf(PNG, (int) AvatarStorageService.MAX_FILE_SIZE + 1);
        assertThatThrownBy(() -> service.store(new MockMultipartFile("file", "big.png", "image/png", big)))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    @DisplayName("Đọc lại ảnh đã lưu kèm kiểu MIME đúng")
    void loadsStoredFile() {
        String fileName = service.store(new MockMultipartFile("file", "a.jpg", "image/jpeg", JPG)).fileName();

        AvatarStorageService.AvatarFile avatar = service.load(fileName);

        assertThat(avatar.content()).isEqualTo(JPG);
        assertThat(avatar.contentType()).isEqualTo("image/jpeg");
    }

    @Test
    @DisplayName("Tên tệp lạ, chứa đường dẫn hoặc không tồn tại đều trả không tìm thấy")
    void rejectsUnknownOrTraversalNames() {
        assertThatThrownBy(() -> service.load("../application.properties")).isInstanceOf(ResourceNotFoundException.class);
        assertThatThrownBy(() -> service.load("avatar.png")).isInstanceOf(ResourceNotFoundException.class);
        assertThatThrownBy(() -> service.load("00000000-0000-0000-0000-000000000000.png"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Không tìm thấy ảnh đại diện.");
    }
}
