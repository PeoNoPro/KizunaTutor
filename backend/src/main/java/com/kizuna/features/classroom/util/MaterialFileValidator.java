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
 * Kiểm tra tệp tài liệu / đề bài / bài làm (FE-06, FE-07): PDF, DOC, DOCX, PNG, JPG/JPEG, MP3; tối đa 25 MB mỗi tệp,
 * tối đa {@value #MAX_FILES} tệp mỗi lần tải lên.
 * Đuôi tệp và chữ ký nhị phân đầu tệp phải cùng loại; Content-Type của trình duyệt không được tin cậy.
 */
@Component
public class MaterialFileValidator {

    public static final long MAX_SIZE_BYTES = 25L * 1024 * 1024;
    public static final int MAX_FILES = 5;
    /** Thông điệp tải tệp theo SRS (vượt dung lượng). */
    public static final String INVALID_FILE_MESSAGE =
            "Dung lượng tệp vượt quá giới hạn 25 MB. Định dạng cho phép: PDF, DOC, DOCX, PNG, JPG, MP3.";
    public static final String UNSUPPORTED_FORMAT_MESSAGE =
            "Định dạng tệp không được hỗ trợ hoặc nội dung tệp không khớp phần mở rộng. Định dạng cho phép: PDF, DOC, DOCX, PNG, JPG, MP3.";
    public static final String TOO_MANY_FILES_MESSAGE = "Mỗi lần chỉ được tải lên tối đa " + MAX_FILES + " tệp.";

    public enum MaterialFileType {
        PDF("pdf", "application/pdf"),
        DOC("doc", "application/msword"),
        DOCX("docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document"),
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
            "doc", MaterialFileType.DOC,
            "docx", MaterialFileType.DOCX,
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

    /**
     * Kiểm tra một lần tải nhiều tệp: bỏ phần tử rỗng, tối đa {@value #MAX_FILES} tệp, từng tệp hợp lệ.
     * @return danh sách tệp không rỗng (có thể rỗng nếu không chọn tệp nào)
     */
    public java.util.List<MultipartFile> nonEmpty(java.util.List<MultipartFile> files) {
        java.util.List<MultipartFile> list = files == null ? java.util.List.of()
                : files.stream().filter(f -> f != null && !f.isEmpty()).toList();
        if (list.size() > MAX_FILES) {
            throw ClassroomException.field("file", TOO_MANY_FILES_MESSAGE);
        }
        return list;
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
            case DOC -> startsWith(h, 0xD0, 0xCF, 0x11, 0xE0, 0xA1, 0xB1, 0x1A, 0xE1);  // Word 97-2003 (OLE)
            case DOCX -> startsWith(h, 0x50, 0x4B, 0x03, 0x04);                         // Word 2007+ (ZIP)
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
