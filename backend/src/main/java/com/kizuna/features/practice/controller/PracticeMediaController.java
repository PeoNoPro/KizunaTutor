package com.kizuna.features.practice.controller;

import com.kizuna.core.exception.BusinessException;
import com.kizuna.features.practice.service.PracticeMediaTokens;
import com.kizuna.features.practice.storage.PracticeFileStorage;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.time.Duration;
import java.util.UUID;

/**
 * FE-13: phát file nghe / ảnh cho thẻ &lt;audio&gt;/&lt;img&gt; bằng đường dẫn đã ký (không cần token đăng nhập).
 * Có kho đám mây thì chuyển hướng (302) tới đường dẫn ký sẵn của kho; chưa có thì trả Resource từ máy chủ (Spring tự hỗ trợ tua / HTTP Range).
 */
@RestController
@RequiredArgsConstructor
public class PracticeMediaController {
    private final JdbcTemplate jdbc;
    private final PracticeFileStorage storage;
    private final PracticeMediaTokens tokens;

    @GetMapping("/api/practice/media/{fileId}")
    public ResponseEntity<Resource> media(@PathVariable UUID fileId, @RequestParam long exp, @RequestParam String sig) {
        if (!tokens.valid(fileId, exp, sig)) throw new BusinessException(403, "Đường dẫn tệp đã hết hạn. Vui lòng tải lại trang.");
        var rows = jdbc.queryForList("SELECT storage_key,content_type FROM practice_files WHERE file_id=?", fileId);
        if (rows.isEmpty()) throw BusinessException.missing();
        String key = String.valueOf(rows.get(0).get("storage_key"));
        // Có kho đám mây: chuyển hướng tới đường dẫn ký sẵn của kho, trình duyệt tải thẳng (vẫn tua được), backend không đẩy dữ liệu.
        var direct = storage.directUrl(key);
        if (direct.isPresent()) {
            return ResponseEntity.status(HttpStatus.FOUND).location(URI.create(direct.get()))
                    .cacheControl(CacheControl.maxAge(Duration.ofMinutes(30)).cachePrivate()).build();
        }
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(String.valueOf(rows.get(0).get("content_type"))))
                .cacheControl(CacheControl.maxAge(Duration.ofHours(1)).cachePrivate())
                .header("X-Content-Type-Options", "nosniff")
                .body(storage.load(key));
    }
}
