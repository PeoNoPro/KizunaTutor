package com.kizuna.features.verification;

import com.kizuna.features.verification.entity.TutorCredential;
import com.kizuna.features.verification.repository.TutorCredentialRepository;
import com.kizuna.features.verification.service.VerificationDocumentStorage;
import com.kizuna.features.verification.service.VerificationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class VerificationDocumentStorageTest {

    @TempDir
    Path tempDir;

    private TutorCredentialRepository repository;
    private VerificationDocumentStorage storage;

    @BeforeEach
    void setUp() {
        repository = mock(TutorCredentialRepository.class);
        storage = new VerificationDocumentStorage(tempDir.toString(), repository);
    }

    private static byte[] validPdf() {
        return "%PDF-1.4\n1 0 obj\n<<>>\nendobj\ntrailer\n<<>>\n%%EOF\n".getBytes(StandardCharsets.US_ASCII);
    }

    private static byte[] image(String format) throws IOException {
        BufferedImage img = new BufferedImage(4, 4, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(img, format, out);
        return out.toByteArray();
    }

    private static TutorCredential credential(String key) {
        return TutorCredential.builder().credentialId(UUID.randomUUID()).title("Giấy tờ").storageKey(key).build();
    }

    private void assertInvalid(MockMultipartFile file) {
        VerificationException ex = assertThrows(VerificationException.class, () -> storage.validate(file));
        assertEquals("INVALID_FILE", ex.getCode());
    }

    @Test
    void validatesPdfPngAndJpegWithRandomKeysAndWritesNothingToDisk() throws IOException {
        var pdf = storage.validate(new MockMultipartFile("file", "bang.pdf", "application/pdf", validPdf()));
        var png = storage.validate(new MockMultipartFile("file", "anh.PNG", "image/png", image("png")));
        var jpg = storage.validate(new MockMultipartFile("file", "cccd.jpeg", "image/jpeg", image("jpg")));

        assertTrue(pdf.key().endsWith(".pdf"));
        assertTrue(png.key().endsWith(".png"));
        assertTrue(jpg.key().endsWith(".jpg"));
        assertFalse(pdf.key().contains("bang"));
        assertTrue(VerificationDocumentStorage.isManagedKey(pdf.key()));
        assertArrayEquals(validPdf(), pdf.content());
        assertEquals("application/pdf", storage.contentType(pdf.key()));
        assertEquals("image/jpeg", storage.contentType(jpg.key()));
        try (var files = Files.list(tempDir)) {
            assertEquals(0, files.count());
        }
    }

    @Test
    void rejectsFileLargerThanTenMegabytes() {
        byte[] big = new byte[(int) VerificationDocumentStorage.MAX_FILE_SIZE + 1];
        System.arraycopy(validPdf(), 0, big, 0, validPdf().length);
        assertInvalid(new MockMultipartFile("file", "lon.pdf", "application/pdf", big));
    }

    @Test
    void rejectsExecutableRenamedToPdf() {
        byte[] exe = new byte[]{'M', 'Z', (byte) 0x90, 0, 3, 0, 0, 0};
        assertInvalid(new MockMultipartFile("file", "virus.pdf", "application/pdf", exe));
    }

    @Test
    void rejectsPdfWithoutEofMarker() {
        byte[] truncated = "%PDF-1.4\n1 0 obj\n<<>>\n".getBytes(StandardCharsets.US_ASCII);
        assertInvalid(new MockMultipartFile("file", "hong.pdf", "application/pdf", truncated));
    }

    @Test
    void rejectsCorruptPngWithValidSignature() {
        byte[] corrupt = new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 1, 2, 3};
        assertInvalid(new MockMultipartFile("file", "hong.png", "image/png", corrupt));
    }

    @Test
    void rejectsUnsupportedExtensionAndEmptyFile() {
        assertInvalid(new MockMultipartFile("file", "tai-lieu.docx", "application/octet-stream", validPdf()));
        assertInvalid(new MockMultipartFile("file", "rong.pdf", "application/pdf", new byte[0]));
        assertInvalid(new MockMultipartFile("file", "khong-duoi", "application/pdf", validPdf()));
    }

    @Test
    void saveContentWritesToDatabaseColumn() {
        UUID id = UUID.randomUUID();
        when(repository.updateFileData(id, validPdf())).thenReturn(1);
        storage.saveContent(id, validPdf());
        verify(repository).updateFileData(id, validPdf());
    }

    @Test
    void saveContentFailsWhenRecordWasNotUpdated() {
        when(repository.updateFileData(any(), any())).thenReturn(0);
        assertThrows(IllegalStateException.class, () -> storage.saveContent(UUID.randomUUID(), validPdf()));
    }

    @Test
    void loadPrefersDatabaseContentSoAnyMachineCanServeTheFile() {
        TutorCredential c = credential(UUID.randomUUID() + ".pdf");
        when(repository.findFileData(c.getCredentialId())).thenReturn(validPdf());

        assertArrayEquals(validPdf(), storage.load(c).orElseThrow());
    }

    @Test
    void loadFallsBackToLegacyDiskFileWhenDatabaseHasNoContent() throws IOException {
        String key = UUID.randomUUID() + ".pdf";
        Files.write(tempDir.resolve(key), validPdf());
        TutorCredential c = credential(key);
        when(repository.findFileData(c.getCredentialId())).thenReturn(null);

        assertArrayEquals(validPdf(), storage.load(c).orElseThrow());
    }

    @Test
    void loadIsEmptyWhenNeitherDatabaseNorDiskHasTheFile() {
        TutorCredential c = credential(UUID.randomUUID() + ".pdf");
        assertTrue(storage.load(c).isEmpty());
    }

    @Test
    void availableIdsCombinesDatabaseContentAndLegacyDiskFiles() throws IOException {
        TutorCredential inDb = credential(UUID.randomUUID() + ".pdf");
        TutorCredential onDisk = credential(UUID.randomUUID() + ".png");
        TutorCredential missing = credential(UUID.randomUUID() + ".jpg");
        Files.write(tempDir.resolve(onDisk.getStorageKey()), new byte[]{1});
        when(repository.findIdsWithFileData(any())).thenReturn(List.of(inDb.getCredentialId().toString()));

        Set<UUID> available = storage.availableIds(List.of(inDb, onDisk, missing));

        assertEquals(Set.of(inDb.getCredentialId(), onDisk.getCredentialId()), available);
    }

    @Test
    void availableIdsOfEmptyListSkipsQuery() {
        assertTrue(storage.availableIds(List.of()).isEmpty());
        verify(repository, never()).findIdsWithFileData(any());
    }

    @Test
    void rejectsKeysThatEscapeStorageDirectory() throws IOException {
        Files.writeString(tempDir.getParent().resolve("secret.pdf"), "x");
        assertTrue(storage.resolveExisting("../secret.pdf").isEmpty());
        assertTrue(storage.resolveExisting("..\\secret.pdf").isEmpty());
        assertTrue(storage.resolveExisting("/etc/passwd").isEmpty());
        assertTrue(storage.resolveExisting(null).isEmpty());
    }

    @Test
    void deleteLegacyWithoutTransactionRemovesDiskFileImmediately() throws IOException {
        String key = UUID.randomUUID() + ".pdf";
        Files.write(tempDir.resolve(key), validPdf());
        storage.deleteLegacyAfterCommit(key);
        assertTrue(storage.resolveExisting(key).isEmpty());
    }
}
