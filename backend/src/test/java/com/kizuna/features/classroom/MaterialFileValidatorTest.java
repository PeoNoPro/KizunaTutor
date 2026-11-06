package com.kizuna.features.classroom;

import com.kizuna.features.classroom.exception.ClassroomException;
import com.kizuna.features.classroom.util.MaterialFileValidator;
import com.kizuna.features.classroom.util.MaterialFileValidator.MaterialFileType;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MaterialFileValidatorTest {

    private final MaterialFileValidator validator = new MaterialFileValidator();

    static final byte[] PDF = bytes(0x25, 0x50, 0x44, 0x46, 0x2D, 0x31);
    static final byte[] PNG = bytes(0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 0, 0);
    static final byte[] JPG = bytes(0xFF, 0xD8, 0xFF, 0xE0, 0, 0);
    static final byte[] MP3_ID3 = bytes(0x49, 0x44, 0x33, 0x03, 0);
    static final byte[] MP3_FRAME = bytes(0xFF, 0xFB, 0x90, 0x64);
    static final byte[] EXE = bytes(0x4D, 0x5A, 0x90, 0x00);

    static byte[] bytes(int... v) {
        byte[] b = new byte[v.length];
        for (int i = 0; i < v.length; i++) b[i] = (byte) v[i];
        return b;
    }

    private static MockMultipartFile file(String name, byte[] content) {
        return new MockMultipartFile("file", name, "application/octet-stream", content);
    }

    private void assertRejected(MockMultipartFile f, HttpStatus status) {
        assertThatThrownBy(() -> validator.validate(f)).isInstanceOf(ClassroomException.class)
                .extracting(e -> ((ClassroomException) e).getStatus()).isEqualTo(status);
    }

    @Test
    void acceptsAllowedTypes() {
        assertThat(validator.validate(file("bai1.pdf", PDF))).isEqualTo(MaterialFileType.PDF);
        assertThat(validator.validate(file("hinh.PNG", PNG))).isEqualTo(MaterialFileType.PNG);
        assertThat(validator.validate(file("anh.jpg", JPG))).isEqualTo(MaterialFileType.JPG);
        assertThat(validator.validate(file("anh.jpeg", JPG))).isEqualTo(MaterialFileType.JPG);
        assertThat(validator.validate(file("nghe.mp3", MP3_ID3))).isEqualTo(MaterialFileType.MP3);
        assertThat(validator.validate(file("nghe2.mp3", MP3_FRAME))).isEqualTo(MaterialFileType.MP3);
    }

    @Test
    void rejectsMissingOrEmptyFile() {
        assertThatThrownBy(() -> validator.validate(null)).isInstanceOf(ClassroomException.class);
        assertRejected(file("rong.pdf", new byte[0]), HttpStatus.BAD_REQUEST);
    }

    @Test
    void rejectsOversizedFile() {
        byte[] big = Arrays.copyOf(PDF, (int) MaterialFileValidator.MAX_SIZE_BYTES + 1);
        assertRejected(file("lon.pdf", big), HttpStatus.PAYLOAD_TOO_LARGE);
    }

    @Test
    void acceptsFileExactlyAtLimit() {
        byte[] exact = Arrays.copyOf(PDF, (int) MaterialFileValidator.MAX_SIZE_BYTES);
        assertThat(validator.validate(file("vua.pdf", exact))).isEqualTo(MaterialFileType.PDF);
    }

    @Test
    void rejectsUnsupportedExtension() {
        assertRejected(file("video.mp4", PDF), HttpStatus.BAD_REQUEST);
        assertRejected(file("khongduoi", PDF), HttpStatus.BAD_REQUEST);
        assertRejected(file("cham.", PDF), HttpStatus.BAD_REQUEST);
    }

    @Test
    void formatAndSizeErrorsUseDistinctMessages() {
        assertThatThrownBy(() -> validator.validate(file("gia.pdf", EXE)))
                .hasMessage(MaterialFileValidator.UNSUPPORTED_FORMAT_MESSAGE);
        byte[] big = Arrays.copyOf(PDF, (int) MaterialFileValidator.MAX_SIZE_BYTES + 1);
        assertThatThrownBy(() -> validator.validate(file("lon.pdf", big)))
                .hasMessage(MaterialFileValidator.INVALID_FILE_MESSAGE);
    }

    @Test
    void rejectsSpoofedContent() {
        assertRejected(file("gia.pdf", EXE), HttpStatus.BAD_REQUEST);
        assertRejected(file("anh.png", JPG), HttpStatus.BAD_REQUEST);
        assertRejected(file("tai.mp3", PDF), HttpStatus.BAD_REQUEST);
    }
}
