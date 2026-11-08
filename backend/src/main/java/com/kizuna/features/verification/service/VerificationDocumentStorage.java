package com.kizuna.features.verification.service;

import com.kizuna.features.verification.entity.TutorCredential;
import com.kizuna.features.verification.repository.TutorCredentialRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Lưu giấy tờ xác minh trong DB, cột tutor_credentials.file_data (BYTEA) — cùng cách FE-06 —
 * để mọi máy chạy backend dùng chung DB đều xem được tệp. Tệp không bao giờ nằm trong thư mục phục vụ công khai (FR-024).
 * Cột file_url giữ khóa tệp dạng UUID ngẫu nhiên + đuôi (pdf|png|jpg), dùng để xác định loại tệp.
 *
 * Chuyển tiếp: giấy tờ tải lên trước khi đổi cách lưu có file_data = NULL; khi đó đọc từ thư mục cũ
 * (app.verification.storage-dir) nếu máy hiện tại còn tệp.
 */
@Slf4j
@Component
public class VerificationDocumentStorage {

    public static final long MAX_FILE_SIZE = 10L * 1024 * 1024;

    private static final Pattern KEY_PATTERN =
            Pattern.compile("^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}\\.(pdf|png|jpg)$");
    private static final int PDF_TAIL_WINDOW = 1024;

    /** Tệp đã qua kiểm tra: khóa lưu ở file_url và nội dung ghi vào file_data. */
    public record ValidatedDocument(String key, byte[] content) {
    }

    private final Path legacyDir;
    private final TutorCredentialRepository credentialRepository;

    public VerificationDocumentStorage(@Value("${app.verification.storage-dir}") String storageDir,
                                       TutorCredentialRepository credentialRepository) {
        this.legacyDir = Paths.get(storageDir).toAbsolutePath().normalize();
        this.credentialRepository = credentialRepository;
    }

    /**
     * Kiểm tra tệp (dung lượng, đuôi, nội dung thật) và sinh khóa ngẫu nhiên.
     * Ném VerificationException.invalidFile() nếu tệp không hợp lệ.
     */
    public ValidatedDocument validate(MultipartFile file) {
        if (file == null || file.isEmpty() || file.getSize() > MAX_FILE_SIZE) {
            throw VerificationException.invalidFile();
        }
        String extension = normalizedExtension(file.getOriginalFilename());
        byte[] content;
        try {
            content = file.getBytes();
        } catch (IOException e) {
            throw VerificationException.invalidFile();
        }
        if (content.length == 0 || content.length > MAX_FILE_SIZE || !matchesFormat(extension, content)) {
            throw VerificationException.invalidFile();
        }
        return new ValidatedDocument(UUID.randomUUID() + "." + extension, content);
    }

    /**
     * Ghi nội dung tệp vào DB cho giấy tờ đã lưu; chạy trong cùng transaction với bản ghi giấy tờ.
     */
    public void saveContent(UUID credentialId, byte[] content) {
        if (credentialRepository.updateFileData(credentialId, content) != 1) {
            throw new IllegalStateException("Không ghi được nội dung giấy tờ xác minh " + credentialId);
        }
    }

    /**
     * Nội dung tệp của giấy tờ: ưu tiên DB, nếu chưa có thì tìm ở thư mục cũ trên máy hiện tại.
     */
    public Optional<byte[]> load(TutorCredential credential) {
        byte[] data = credentialRepository.findFileData(credential.getCredentialId());
        if (data != null && data.length > 0) {
            return Optional.of(data);
        }
        return resolveExisting(credential.getStorageKey()).flatMap(path -> {
            try {
                return Optional.of(Files.readAllBytes(path));
            } catch (IOException e) {
                log.warn("Không đọc được tệp giấy tờ xác minh {}: {}", credential.getStorageKey(), e.getMessage());
                return Optional.empty();
            }
        });
    }

    /**
     * Mã các giấy tờ xem được tệp: có nội dung trong DB, hoặc (chuyển tiếp) còn tệp ở thư mục cũ.
     * Một truy vấn cho cả danh sách, không tải dữ liệu nhị phân.
     */
    public Set<UUID> availableIds(Collection<TutorCredential> credentials) {
        List<UUID> ids = credentials.stream().map(TutorCredential::getCredentialId).filter(Objects::nonNull).toList();
        Set<UUID> available = new HashSet<>();
        if (ids.isEmpty()) {
            return available;
        }
        credentialRepository.findIdsWithFileData(ids).forEach(id -> available.add(UUID.fromString(id)));
        for (TutorCredential credential : credentials) {
            if (credential.getCredentialId() != null && !available.contains(credential.getCredentialId())
                    && resolveExisting(credential.getStorageKey()).isPresent()) {
                available.add(credential.getCredentialId());
            }
        }
        return available;
    }

    /**
     * Xóa tệp cũ ở thư mục chuyển tiếp (nếu máy này còn) sau khi transaction commit.
     * Nội dung trong DB tự mất cùng bản ghi hoặc bị ghi đè khi thay tệp.
     */
    public void deleteLegacyAfterCommit(String key) {
        if (key == null || resolveExisting(key).isEmpty()) {
            return;
        }
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    deleteQuietly(key);
                }
            });
        } else {
            deleteQuietly(key);
        }
    }

    /**
     * Đường dẫn tệp ở thư mục cũ nếu khóa hợp lệ và tệp còn tồn tại trên máy này.
     */
    public Optional<Path> resolveExisting(String key) {
        return resolve(key).filter(Files::isRegularFile);
    }

    public String contentType(String key) {
        String ext = isManagedKey(key) ? extensionOf(key) : "";
        return switch (ext) {
            case "pdf" -> "application/pdf";
            case "png" -> "image/png";
            case "jpg" -> "image/jpeg";
            default -> "application/octet-stream";
        };
    }

    /**
     * true nếu khóa do hệ thống sinh ra (UUID + đuôi hợp lệ).
     */
    public static boolean isManagedKey(String key) {
        return key != null && KEY_PATTERN.matcher(key).matches();
    }

    public static String extensionOf(String key) {
        if (key == null) {
            return "";
        }
        int dot = key.lastIndexOf('.');
        return dot < 0 ? "" : key.substring(dot + 1).toLowerCase(Locale.ROOT);
    }

    private Optional<Path> resolve(String key) {
        if (!isManagedKey(key)) {
            return Optional.empty();
        }
        Path path = legacyDir.resolve(key).normalize();
        if (!path.startsWith(legacyDir)) {
            return Optional.empty();
        }
        return Optional.of(path);
    }

    private String normalizedExtension(String originalFilename) {
        String ext = extensionOf(originalFilename);
        return switch (ext) {
            case "pdf" -> "pdf";
            case "png" -> "png";
            case "jpg", "jpeg" -> "jpg";
            default -> throw VerificationException.invalidFile();
        };
    }

    private boolean matchesFormat(String extension, byte[] content) {
        return switch (extension) {
            case "pdf" -> isPdf(content);
            case "png" -> startsWith(content, new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A})
                    && isDecodableImage(content);
            case "jpg" -> startsWith(content, new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF})
                    && isDecodableImage(content);
            default -> false;
        };
    }

    private boolean isPdf(byte[] content) {
        if (!startsWith(content, "%PDF-".getBytes(StandardCharsets.US_ASCII))) {
            return false;
        }
        int from = Math.max(0, content.length - PDF_TAIL_WINDOW);
        String tail = new String(content, from, content.length - from, StandardCharsets.ISO_8859_1);
        return tail.contains("%%EOF");
    }

    private boolean isDecodableImage(byte[] content) {
        try {
            return ImageIO.read(new ByteArrayInputStream(content)) != null;
        } catch (IOException | RuntimeException e) {
            return false;
        }
    }

    private static boolean startsWith(byte[] content, byte[] prefix) {
        if (content.length < prefix.length) {
            return false;
        }
        for (int i = 0; i < prefix.length; i++) {
            if (content[i] != prefix[i]) {
                return false;
            }
        }
        return true;
    }

    private void deleteQuietly(String key) {
        resolve(key).ifPresent(path -> {
            try {
                Files.deleteIfExists(path);
            } catch (IOException e) {
                log.warn("Không xóa được tệp giấy tờ xác minh {}: {}", key, e.getMessage());
            }
        });
    }
}
