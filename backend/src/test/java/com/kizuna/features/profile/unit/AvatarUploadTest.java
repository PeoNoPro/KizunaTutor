package com.kizuna.features.profile.unit;

import com.kizuna.core.exception.BadRequestException;
import com.kizuna.core.exception.ServiceUnavailableException;
import com.kizuna.features.profile.dto.response.AvatarUploadResponseDto;
import com.kizuna.features.profile.service.AvatarStorageService;
import com.kizuna.features.profile.service.impl.AvatarStorageServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit test FE-02 / UC-02.05 Upload Avatar - AvatarStorageServiceImpl.store(file, ownerId, currentAvatarUrl).
 * Lưu vào thư mục tạm (không cấu hình R2) để kiểm tra kết quả trên đĩa.
 * Quy tắc: JPG/PNG/WEBP, không rỗng, tối đa 5 MB, nội dung khớp định dạng; tên tệp = ownerId_UUID.
 */
class AvatarUploadTest {
    private static final String INVALID = "Ảnh đại diện chỉ nhận tệp JPG, PNG hoặc WEBP, dung lượng tối đa 5MB.";
    private static final UUID OWNER = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final byte[] JPG = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, 0x00};
    private static final byte[] PNG = {(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 0x00};
    private static final byte[] WEBP = {'R', 'I', 'F', 'F', 0, 0, 0, 0, 'W', 'E', 'B', 'P'};

    @TempDir Path dir;

    private AvatarStorageServiceImpl service() { return new AvatarStorageServiceImpl(dir.toString()); }

    private static MockMultipartFile file(String name, byte[] content) {
        return new MockMultipartFile("file", name, "application/octet-stream", content);
    }

    private static byte[] jpgOfSize(long size) {
        byte[] b = new byte[(int) size];
        System.arraycopy(JPG, 0, b, 0, JPG.length);
        return b;
    }

    @Test @DisplayName("UTCID01: Tải JPG hợp lệ -> lưu tệp, trả URL /api/profile/avatar/<tên tệp>")
    void utcid01_validJpg() {
        AvatarUploadResponseDto dto = service().store(file("avatar.jpg", JPG), OWNER, null);

        assertTrue(dto.avatarUrl().startsWith("/api/profile/avatar/"));
        assertTrue(Files.isRegularFile(dir.resolve(dto.fileName())));
        assertEquals(JPG.length, dto.size());
    }

    @Test @DisplayName("UTCID02: Tải PNG hợp lệ -> được nhận, lưu đuôi .png")
    void utcid02_png() {
        assertTrue(service().store(file("a.png", PNG), OWNER, null).fileName().endsWith(".png"));
    }

    @Test @DisplayName("UTCID03: Tải WEBP hợp lệ -> được nhận, lưu đuôi .webp")
    void utcid03_webp() {
        assertTrue(service().store(file("b.webp", WEBP), OWNER, null).fileName().endsWith(".webp"));
    }

    @Test @DisplayName("UTCID04: Tệp GIF -> 400")
    void utcid04_gif() {
        var ex = assertThrows(BadRequestException.class, () -> service().store(file("a.gif", JPG), OWNER, null));
        assertEquals(INVALID, ex.getMessage());
    }

    @Test @DisplayName("UTCID05: Tệp PDF -> 400")
    void utcid05_pdf() {
        var ex = assertThrows(BadRequestException.class, () -> service().store(file("a.pdf", JPG), OWNER, null));
        assertEquals(INVALID, ex.getMessage());
    }

    @Test @DisplayName("UTCID06: Tệp đúng 5 MB (biên) -> được nhận")
    void utcid06_exactly5Mb() {
        var dto = service().store(file("big.jpg", jpgOfSize(AvatarStorageService.MAX_FILE_SIZE)), OWNER, null);
        assertEquals(AvatarStorageService.MAX_FILE_SIZE, dto.size());
    }

    @Test @DisplayName("UTCID07: Tệp 5 MB + 1 byte (vượt biên) -> 400")
    void utcid07_over5Mb() {
        var ex = assertThrows(BadRequestException.class,
                () -> service().store(file("big.jpg", jpgOfSize(AvatarStorageService.MAX_FILE_SIZE + 1)), OWNER, null));
        assertEquals(INVALID, ex.getMessage());
    }

    @Test @DisplayName("UTCID08: Tệp rỗng 0 byte -> 400")
    void utcid08_emptyFile() {
        assertThrows(BadRequestException.class, () -> service().store(file("empty.jpg", new byte[0]), OWNER, null));
    }

    @Test @DisplayName("UTCID09: Tệp văn bản đổi đuôi thành .jpg -> 400 (nội dung không khớp)")
    void utcid09_fakeJpg() {
        var ex = assertThrows(BadRequestException.class,
                () -> service().store(file("fake.jpg", "hello world".getBytes()), OWNER, null));
        assertEquals(INVALID, ex.getMessage());
    }

    @Test @DisplayName("UTCID10: Tên tệp lưu = <userId>_<UUID>.jpg, không dùng tên gốc")
    void utcid10_generatedName() {
        var dto = service().store(file("my photo.jpg", JPG), OWNER, null);
        assertTrue(dto.fileName().matches(OWNER + "_[0-9a-f-]{36}\\.jpg"));
        assertFalse(dto.avatarUrl().contains("my photo"));
    }

    @Test @DisplayName("UTCID11: Ảnh đã tải nhưng chưa dùng bị xóa khi tải ảnh mới; ảnh đang dùng được giữ")
    void utcid11_cleansUnusedUploads() {
        AvatarStorageServiceImpl service = service();
        var current = service.store(file("current.jpg", JPG), OWNER, null);
        var unused = service.store(file("a.jpg", JPG), OWNER, current.avatarUrl());

        var latest = service.store(file("b.jpg", JPG), OWNER, current.avatarUrl());

        assertTrue(Files.exists(dir.resolve(current.fileName())), "ảnh đang làm avatar phải còn");
        assertFalse(Files.exists(dir.resolve(unused.fileName())), "ảnh tải lên chưa dùng phải bị xóa");
        assertTrue(Files.exists(dir.resolve(latest.fileName())));
    }

    @Test @DisplayName("UTCID12: Không ghi được vào kho lưu trữ -> 503 'Chưa lưu được ảnh đại diện. Vui lòng thử lại sau.'")
    void utcid12_storageUnavailable() throws Exception {
        Path notADirectory = Files.writeString(dir.resolve("blocked"), "x");
        var service = new AvatarStorageServiceImpl(notADirectory.toString());

        var ex = assertThrows(ServiceUnavailableException.class, () -> service.store(file("a.jpg", JPG), OWNER, null));
        assertEquals("Chưa lưu được ảnh đại diện. Vui lòng thử lại sau.", ex.getMessage());
    }
}
