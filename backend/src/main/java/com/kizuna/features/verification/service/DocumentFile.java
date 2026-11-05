package com.kizuna.features.verification.service;

import java.nio.file.Path;

/**
 * Tệp giấy tờ đã qua kiểm tra quyền, sẵn sàng trả về cho trình duyệt.
 */
public record DocumentFile(Path path, String contentType, String fileName) {
}
