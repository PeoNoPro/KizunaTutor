package com.kizuna.features.verification.service;

/**
 * Tệp giấy tờ đã qua kiểm tra quyền, sẵn sàng trả về cho trình duyệt.
 */
public record DocumentFile(byte[] content, String contentType, String fileName) {
}
