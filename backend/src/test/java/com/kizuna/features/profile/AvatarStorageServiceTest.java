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
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AvatarStorageServiceTest {

    private static final byte[] PNG = {(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 0x00, 0x01};
    private static final byte[] JPG = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 0x00, 0x10};
    private static final byte[] WEBP = "RIFF\0\0\0\0WEBPVP8 ".getBytes(StandardCharsets.ISO_8859_1);
    private static final UUID OWNER = UUID.fromString("11111111-2222-3333-4444-555555555555");
    private static final UUID OTHER = UUID.fromString("99999999-8888-7777-6666-555555555555");

    @TempDir
    Path storageDir;

    private AvatarStorageService service;

    @BeforeEach
    void setUp() {
        service = new AvatarStorageServiceImpl(storageDir.toString());
    }

    private static MockMultipartFile file(String name, String type, byte[] content) {
        return new MockMultipartFile("file", name, type, content);
    }

    private AvatarUploadResponseDto upload(UUID owner, String currentAvatarUrl) {
        return service.store(file("a.png", "image/png", PNG), owner, currentAvatarUrl);
    }

    @Test
    @DisplayName("Lưu ảnh PNG hợp lệ: tên tệp gắn với người tải lên, trả đường dẫn công khai và ghi tệp vào thư mục")
    void storesValidPng() throws Exception {
        AvatarUploadResponseDto result = service.store(file("Ảnh của tôi.PNG", "image/png", PNG), OWNER, null);

        assertThat(result.fileName()).matches("^" + OWNER + "_[0-9a-f-]{36}\\.png$");
        assertThat(result.avatarUrl()).isEqualTo("/api/profile/avatar/" + result.fileName());
        assertThat(result.size()).isEqualTo(PNG.length);
        assertThat(Files.readAllBytes(storageDir.resolve(result.fileName()))).isEqualTo(PNG);
    }

    @Test
    @DisplayName("Đuôi .jpeg được chuẩn hóa thành .jpg; WEBP hợp lệ được chấp nhận")
    void acceptsJpegAndWebp() {
        assertThat(service.store(file("a.jpeg", "image/jpeg", JPG), OWNER, null).fileName()).endsWith(".jpg");
        assertThat(service.store(file("a.webp", "image/webp", WEBP), OWNER, null).fileName()).endsWith(".webp");
    }

    @Test
    @DisplayName("Từ chối đuôi không hỗ trợ, tệp rỗng, tệp null và nội dung không khớp đuôi")
    void rejectsInvalidFiles() {
        assertThatThrownBy(() -> service.store(file("a.gif", "image/gif", PNG), OWNER, null))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Ảnh đại diện chỉ nhận tệp JPG, PNG hoặc WEBP, dung lượng tối đa 5MB.");
        assertThatThrownBy(() -> service.store(file("a.png", "image/png", new byte[0]), OWNER, null))
                .isInstanceOf(BadRequestException.class);
        assertThatThrownBy(() -> service.store(file("a.png", "image/png", JPG), OWNER, null))
                .isInstanceOf(BadRequestException.class);
        assertThatThrownBy(() -> service.store(null, OWNER, null)).isInstanceOf(BadRequestException.class);
    }

    @Test
    @DisplayName("Từ chối ảnh vượt quá 5MB")
    void rejectsOversizedFile() {
        byte[] big = Arrays.copyOf(PNG, (int) AvatarStorageService.MAX_FILE_SIZE + 1);
        assertThatThrownBy(() -> service.store(file("big.png", "image/png", big), OWNER, null))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    @DisplayName("Đọc lại ảnh đã lưu kèm kiểu MIME đúng; ảnh tải lên trước đây (tên chỉ có UUID) vẫn đọc được")
    void loadsStoredFile() throws Exception {
        String fileName = service.store(file("a.jpg", "image/jpeg", JPG), OWNER, null).fileName();

        AvatarStorageService.AvatarFile avatar = service.load(fileName);
        assertThat(avatar.content()).isEqualTo(JPG);
        assertThat(avatar.contentType()).isEqualTo("image/jpeg");

        String legacy = "3f2b1c4d-5e6f-4a7b-8c9d-0e1f2a3b4c5d.png";
        Files.write(storageDir.resolve(legacy), PNG);
        assertThat(service.load(legacy).content()).isEqualTo(PNG);
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

    @Test
    @DisplayName("Tải ảnh mới: ảnh tải trước đó chưa dùng bị xóa, ảnh đang dùng và ảnh của người khác được giữ")
    void uploadDeletesOwnUnusedUploadsOnly() {
        AvatarUploadResponseDto current = upload(OWNER, null);
        AvatarUploadResponseDto unused = upload(OWNER, current.avatarUrl());
        AvatarUploadResponseDto others = upload(OTHER, null);

        AvatarUploadResponseDto latest = upload(OWNER, current.avatarUrl());

        assertThat(storageDir.resolve(current.fileName())).exists();
        assertThat(storageDir.resolve(latest.fileName())).exists();
        assertThat(storageDir.resolve(unused.fileName())).doesNotExist();
        assertThat(storageDir.resolve(others.fileName())).exists();
    }

    @Test
    @DisplayName("Ảnh đại diện mới chỉ nhận ảnh chính mình tải lên; giữ nguyên ảnh cũ (kể cả link ngoài) hoặc xóa ảnh thì được")
    void requireOwnAvatar() {
        String legacyExternal = "https://images.unsplash.com/photo-1.jpg";
        String mine = upload(OWNER, null).avatarUrl();
        String theirs = upload(OTHER, null).avatarUrl();

        assertThatCode(() -> service.requireOwnAvatar(mine, legacyExternal, OWNER)).doesNotThrowAnyException();
        assertThatCode(() -> service.requireOwnAvatar(legacyExternal, legacyExternal, OWNER)).doesNotThrowAnyException();
        assertThatCode(() -> service.requireOwnAvatar("", legacyExternal, OWNER)).doesNotThrowAnyException();
        assertThatCode(() -> service.requireOwnAvatar(null, mine, OWNER)).doesNotThrowAnyException();

        assertThatThrownBy(() -> service.requireOwnAvatar("https://evil.example/a.png", null, OWNER))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("tải lên từ máy của bạn");
        assertThatThrownBy(() -> service.requireOwnAvatar(theirs, null, OWNER)).isInstanceOf(BadRequestException.class);
        assertThatThrownBy(() -> service.requireOwnAvatar(
                "/api/profile/avatar/" + OWNER + "_00000000-0000-0000-0000-000000000000.png", null, OWNER))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    @DisplayName("Đổi / xóa ảnh: tệp ảnh cũ do hệ thống lưu bị xóa; link ngoài và ảnh không đổi thì không đụng tới")
    void deleteReplaced() {
        AvatarUploadResponseDto old = upload(OWNER, null);
        AvatarUploadResponseDto now = upload(OWNER, old.avatarUrl());

        service.deleteReplaced(now.avatarUrl(), now.avatarUrl());
        assertThat(storageDir.resolve(now.fileName())).exists();

        service.deleteReplaced(old.avatarUrl(), now.avatarUrl());
        assertThat(storageDir.resolve(old.fileName())).doesNotExist();
        assertThat(storageDir.resolve(now.fileName())).exists();

        assertThatCode(() -> service.deleteReplaced("https://images.unsplash.com/photo-1.jpg", null))
                .doesNotThrowAnyException();
        service.deleteReplaced(now.avatarUrl(), null);
        assertThat(storageDir.resolve(now.fileName())).doesNotExist();
    }
}
