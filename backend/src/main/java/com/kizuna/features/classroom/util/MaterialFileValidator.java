package com.kizuna.features.classroom.util;

import com.kizuna.features.classroom.exception.ClassroomException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;
import java.util.Map;

/**
 * Kiểm tra tệp tài liệu học tập (FE-06): PDF, PNG, JPG/JPEG, MP3; tối đa 10 MB.
 * Đuôi tệp và chữ ký nhị phân đầu tệp phải cùng loại; Content-Type của trình duyệt không được tin cậy.
 */
@Component
public class MaterialFileValidator {

    public static final long MAX_SIZE_BYTES = 10L * 1024 * 1024;
    /** Thông điệp tải tệp theo SRS (vượt dung lượng). */
    public static final String INVALID_FILE_MESSAGE =
            "Dung lượng tệp vượt quá giới hạn 10 MB. Định dạng cho phép: PDF, PNG, JPG, MP3.";
    public static final String UNSUPPORTED_FORMAT_MESSAGE =
            "Định dạng tệp không được hỗ trợ hoặc nội dung tệp không khớp phần mở rộng. Định dạng cho phép: PDF, PNG, JPG, MP3.";

    public enum MaterialFileType {
        PDF("pdf", "application/pdf"),
        PNG("png", "image/png"),
        JPG("jpg", "image/jpeg"),
        MP3("mp3", "audio/mpeg");

        public final String extension;
        public final String contentType;

        MaterialFileType(String extension, String contentType) {
            this.extension = extension;
            this.contentType = contentType;
        }
    }

    private static final Map<String, MaterialFileType> EXTENSIONS = Map.of(
            "pdf", MaterialFileType.PDF,
            "png", MaterialFileType.PNG,
            "jpg", MaterialFileType.JPG,
            "jpeg", MaterialFileType.JPG,
            "mp3", MaterialFileType.MP3);

    /** @return loại tệp đã xác minh; ném ClassroomException (400/413) nếu không hợp lệ. */
    public MaterialFileType validate(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw ClassroomException.field("file", "Vui lòng chọn tệp tài liệu.");
        }
        if (file.getSize() > MAX_SIZE_BYTES) {
            throw new ClassroomException(HttpStatus.PAYLOAD_TOO_LARGE, INVALID_FILE_MESSAGE,
                    Map.of("file", INVALID_FILE_MESSAGE));
        }

        MaterialFileType byExtension = EXTENSIONS.get(extensionOf(file.getOriginalFilename()));
        if (byExtension == null || !matchesSignature(byExtension, readHeader(file))) {
            throw ClassroomException.field("file", UNSUPPORTED_FORMAT_MESSAGE);
        }
        return byExtension;
    }

    static String extensionOf(String filename) {
        if (filename == null) return "";
        int dot = filename.lastIndexOf('.');
        if (dot < 0 || dot == filename.length() - 1) return "";
        return filename.substring(dot + 1).toLowerCase(Locale.ROOT);
    }

    private static byte[] readHeader(MultipartFile file) {
        try (InputStream in = file.getInputStream()) {
            return in.readNBytes(12);
        } catch (IOException e) {
            throw ClassroomException.field("file", "Không đọc được tệp. Vui lòng chọn lại tệp khác.");
        }
    }

    static boolean matchesSignature(MaterialFileType type, byte[] h) {
        return switch (type) {
            case PDF -> startsWith(h, 0x25, 0x50, 0x44, 0x46);                          // %PDF
            case PNG -> startsWith(h, 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A);
            case JPG -> startsWith(h, 0xFF, 0xD8, 0xFF);
            case MP3 -> startsWith(h, 0x49, 0x44, 0x33)                                 // ID3
                    || (h.length >= 2 && (h[0] & 0xFF) == 0xFF && ((h[1] & 0xE0) == 0xE0)); // MPEG frame sync
        };
    }

    private static boolean startsWith(byte[] data, int... prefix) {
        if (data.length < prefix.length) return false;
        for (int i = 0; i < prefix.length; i++) {
            if ((data[i] & 0xFF) != prefix[i]) return false;
        }
        return true;
    }
}
