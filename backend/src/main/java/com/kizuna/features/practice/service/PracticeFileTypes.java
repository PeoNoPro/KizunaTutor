package com.kizuna.features.practice.service;

import java.io.BufferedInputStream;
import java.io.IOException;

/**
 * Kiểm tra nội dung thật của file luyện đề bằng chữ ký đầu tệp (magic bytes), không chỉ tin Content-Type trình duyệt gửi.
 * Đọc trước tối đa 16 byte rồi trả luồng về vị trí đầu để lưu nguyên vẹn.
 */
final class PracticeFileTypes {
    private static final int HEAD = 16;

    private PracticeFileTypes() { }

    /** Đọc 16 byte đầu mà không làm mất dữ liệu của luồng. */
    static byte[] head(BufferedInputStream in) throws IOException {
        in.mark(HEAD);
        byte[] bytes = in.readNBytes(HEAD);
        in.reset();
        return bytes;
    }

    static boolean image(String ext, byte[] b) {
        return switch (ext) {
            case "png" -> starts(b, 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A);
            case "jpg" -> starts(b, 0xFF, 0xD8, 0xFF);
            case "webp" -> starts(b, 'R', 'I', 'F', 'F') && at(b, 8, 'W', 'E', 'B', 'P');
            default -> false;
        };
    }

    static boolean audio(String ext, byte[] b) {
        return switch (ext) {
            // MP3 có thẻ ID3 ở đầu, hoặc bắt đầu ngay bằng khung MPEG (11 bit đồng bộ).
            case "mp3" -> starts(b, 'I', 'D', '3') || mpegFrame(b);
            // M4A/MP4 có hộp "ftyp" ở byte 4; AAC thô (ADTS) cũng bắt đầu bằng 11 bit đồng bộ.
            case "m4a" -> at(b, 4, 'f', 't', 'y', 'p') || mpegFrame(b);
            case "wav" -> starts(b, 'R', 'I', 'F', 'F') && at(b, 8, 'W', 'A', 'V', 'E');
            case "ogg" -> starts(b, 'O', 'g', 'g', 'S');
            case "webm" -> starts(b, 0x1A, 0x45, 0xDF, 0xA3);
            default -> false;
        };
    }

    private static boolean mpegFrame(byte[] b) { return b.length >= 2 && (b[0] & 0xFF) == 0xFF && (b[1] & 0xE0) == 0xE0; }

    private static boolean starts(byte[] b, int... expected) { return at(b, 0, expected); }

    private static boolean at(byte[] b, int offset, int... expected) {
        if (b.length < offset + expected.length) return false;
        for (int i = 0; i < expected.length; i++) if ((b[offset + i] & 0xFF) != (expected[i] & 0xFF)) return false;
        return true;
    }
}
