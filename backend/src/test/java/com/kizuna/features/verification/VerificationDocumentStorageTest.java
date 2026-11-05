package com.kizuna.features.verification;

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

import static org.junit.jupiter.api.Assertions.*;

class VerificationDocumentStorageTest {

    @TempDir
    Path tempDir;

    private VerificationDocumentStorage storage;

    @BeforeEach
    void setUp() {
        storage = new VerificationDocumentStorage(tempDir.toString());
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

    private void assertInvalid(MockMultipartFile file) {
        VerificationException ex = assertThrows(VerificationException.class, () -> storage.store(file));
        assertEquals("INVALID_FILE", ex.getCode());
    }

    @Test
    void storesValidPdfPngAndJpegWithRandomKeys() throws IOException {
        String pdfKey = storage.store(new MockMultipartFile("file", "bang.pdf", "application/pdf", validPdf()));
        String pngKey = storage.store(new MockMultipartFile("file", "anh.PNG", "image/png", image("png")));
        String jpgKey = storage.store(new MockMultipartFile("file", "cccd.jpeg", "image/jpeg", image("jpg")));

        assertTrue(pdfKey.endsWith(".pdf"));
        assertTrue(pngKey.endsWith(".png"));
        assertTrue(jpgKey.endsWith(".jpg"));
        assertFalse(pdfKey.contains("bang"));
        assertTrue(storage.exists(pdfKey));
        assertEquals("application/pdf", storage.contentType(pdfKey));
        assertEquals("image/jpeg", storage.contentType(jpgKey));
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
    void rejectsKeysThatEscapeStorageDirectory() throws IOException {
        Files.writeString(tempDir.getParent().resolve("secret.pdf"), "x");
        assertTrue(storage.resolveExisting("../secret.pdf").isEmpty());
        assertTrue(storage.resolveExisting("..\\secret.pdf").isEmpty());
        assertTrue(storage.resolveExisting("/etc/passwd").isEmpty());
        assertTrue(storage.resolveExisting(null).isEmpty());
    }

    @Test
    void deleteWithoutTransactionRemovesFileImmediately() {
        String key = storage.store(new MockMultipartFile("file", "a.pdf", "application/pdf", validPdf()));
        storage.deleteAfterCommit(key);
        assertFalse(storage.exists(key));
    }
}
