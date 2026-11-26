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
 * Unit test FE-02 / UC-02.02, UC-02.04 - AvatarStorageServiceImpl.requireOwnAvatar(avatarUrl, currentAvatarUrl, ownerId): chỉ nhận ảnh do chính người dùng tải lên.
 */
class AvatarOwnershipTest {
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

    @Test @DisplayName("UTCID01: Ảnh do chính người dùng tải lên -> được nhận")
    void utcid01_ownAvatarAccepted() {
        AvatarStorageServiceImpl service = service();
        var mine = service.store(file("me.jpg", JPG), OWNER, null);
        assertDoesNotThrow(() -> service.requireOwnAvatar(mine.avatarUrl(), null, OWNER));
    }

    @Test @DisplayName("UTCID02: Ảnh do người khác tải lên -> 400, tệp của người kia không bị xóa")
    void utcid02_otherAvatarRejected() {
        AvatarStorageServiceImpl service = service();
        UUID other = UUID.fromString("00000000-0000-0000-0000-000000000002");
        var theirs = service.store(file("them.jpg", JPG), other, null);
        assertThrows(BadRequestException.class, () -> service.requireOwnAvatar(theirs.avatarUrl(), null, OWNER));
        assertTrue(Arrays.asList(dir.toFile().list()).contains(theirs.fileName()));
    }
}
