package com.kizuna.features.verification.service;

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
import java.nio.file.StandardOpenOption;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Lưu giấy tờ xác minh trên máy chủ ứng dụng, ngoài mọi thư mục phục vụ công khai (FR-024).
 * Tên tệp là UUID ngẫu nhiên; DB chỉ giữ khóa này. Tệp mới bị xóa nếu transaction rollback,
 * tệp cũ chỉ bị xóa sau khi transaction commit.
 */
@Slf4j
@Component
public class VerificationDocumentStorage {

    public static final long MAX_FILE_SIZE = 10L * 1024 * 1024;

    private static final Pattern KEY_PATTERN =
            Pattern.compile("^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}\\.(pdf|png|jpg)$");
    private static final int PDF_TAIL_WINDOW = 1024;

    private final Path baseDir;

    public VerificationDocumentStorage(@Value("${app.verification.storage-dir}") String storageDir) {
        this.baseDir = Paths.get(storageDir).toAbsolutePath().normalize();
    }

    /**
     * Kiểm tra và lưu tệp; trả về khóa lưu trữ. Ném VerificationException.invalidFile() nếu tệp không hợp lệ.
     */
    public String store(MultipartFile file) {
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

        String key = UUID.randomUUID() + "." + extension;
        Path target = baseDir.resolve(key);
        try {
            Files.createDirectories(baseDir);
            Files.write(target, content, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);
        } catch (IOException e) {
            throw new IllegalStateException("Không thể ghi tệp giấy tờ xác minh vào kho lưu trữ", e);
        }
        registerRollbackCleanup(key);
        return key;
    }

    /**
     * Xóa tệp sau khi transaction hiện tại commit (hoặc ngay lập tức nếu không có transaction).
     */
    public void deleteAfterCommit(String key) {
        if (key == null) {
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
     * Đường dẫn tệp nếu khóa hợp lệ và tệp còn tồn tại.
     */
    public Optional<Path> resolveExisting(String key) {
        return resolve(key).filter(Files::isRegularFile);
    }

    public boolean exists(String key) {
        return resolveExisting(key).isPresent();
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
     * true nếu khóa do kho lưu trữ này sinh ra (UUID + đuôi hợp lệ).
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
        Path path = baseDir.resolve(key).normalize();
        if (!path.startsWith(baseDir)) {
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

    private void registerRollbackCleanup(String key) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                if (status != STATUS_COMMITTED) {
                    deleteQuietly(key);
                }
            }
        });
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
