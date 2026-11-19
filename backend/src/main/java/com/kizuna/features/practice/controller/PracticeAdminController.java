package com.kizuna.features.practice.controller;

import com.kizuna.features.payment.service.CurrentUserAccessor;
import com.kizuna.features.practice.service.PracticeAdminService;
import com.kizuna.features.practice.service.PracticeExcel;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.*;

/** FE-13: Ngân hàng đề của cán bộ học vụ. Chỉ ROLE_ACADEMIC_OFFICER (SecurityConfig: /api/academic/practice/**). */
@RestController
@RequestMapping("/api/academic/practice")
@RequiredArgsConstructor
public class PracticeAdminController {
    private final PracticeAdminService service;
    private final CurrentUserAccessor currentUser;

    @GetMapping("/exams")
    public List<Map<String, Object>> list(@RequestParam(required = false) String level, @RequestParam(required = false) String status) {
        return service.list(level, status);
    }

    @GetMapping("/exams/{id}")
    public Map<String, Object> detail(@PathVariable UUID id) { return service.detail(id); }

    @PostMapping(value = "/exams", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Map<String, Object> create(@RequestPart("file") MultipartFile file, @RequestPart(value = "images", required = false) List<MultipartFile> images) {
        return service.create(currentUser.requireUser().getUserId(), file, images);
    }

    @PostMapping(value = "/exams/{id}/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Map<String, Object> reimport(@PathVariable UUID id, @RequestPart("file") MultipartFile file,
                                        @RequestPart(value = "images", required = false) List<MultipartFile> images) {
        return service.reimport(currentUser.requireUser().getUserId(), id, file, images);
    }

    @PutMapping("/exams/{id}")
    public Map<String, Object> updateInfo(@PathVariable UUID id, @RequestBody Map<String, Object> body) { return service.updateInfo(id, body); }

    @PutMapping("/exams/{id}/questions/{questionId}")
    public Map<String, Object> updateQuestion(@PathVariable UUID id, @PathVariable UUID questionId, @RequestBody Map<String, Object> body) {
        return service.updateQuestion(id, questionId, body);
    }

    /** Đổi ảnh câu / ảnh đoạn đọc (target=question|group); gửi không kèm "file" là bỏ ảnh. */
    @PostMapping(value = "/exams/{id}/questions/{questionId}/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Map<String, Object> image(@PathVariable UUID id, @PathVariable UUID questionId, @RequestParam(defaultValue = "question") String target,
                                     @RequestPart(value = "file", required = false) MultipartFile file) {
        return service.setImage(currentUser.requireUser().getUserId(), id, questionId, target, file);
    }

    /** Thân yêu cầu là nội dung file nghe; tên file gửi ở tham số ?name= (header riêng bị CORS chặn). */
    @PutMapping("/exams/{id}/audio")
    public Map<String, Object> audio(@PathVariable UUID id, @RequestParam(required = false) String name, HttpServletRequest request) throws IOException {
        return service.uploadAudio(currentUser.requireUser().getUserId(), id, request.getContentType(), name, request.getInputStream());
    }

    @PostMapping("/exams/{id}/publish")
    public Map<String, Object> publish(@PathVariable UUID id) { return service.publish(id); }

    @PostMapping("/exams/{id}/hide")
    public Map<String, Object> hide(@PathVariable UUID id) { return service.hide(id); }

    @DeleteMapping("/exams/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) { service.delete(id); return ResponseEntity.noContent().build(); }

    @GetMapping("/template")
    public ResponseEntity<byte[]> template() {
        return ResponseEntity.ok().contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"mau-de-luyen-thi.xlsx\"")
                .body(PracticeExcel.template());
    }

    /** Tệp đề còn lỗi: trả danh sách lỗi theo dòng (trang hiện để học vụ sửa rồi tải lại). */
    @ExceptionHandler(PracticeAdminService.InvalidExam.class)
    public ResponseEntity<Map<String, Object>> invalid(PracticeAdminService.InvalidExam error) {
        var details = error.problems.stream().map(p -> Map.<String, Object>of("row", p.row(), "message", p.message())).toList();
        return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Tệp đề còn " + details.size() + " lỗi, chưa lưu gì. Hãy sửa rồi tải lại.",
                "details", details, "timestamp", OffsetDateTime.now()));
    }
}
