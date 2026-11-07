package com.kizuna.features.classroom.controller;

import com.kizuna.features.classroom.dto.*;
import com.kizuna.features.classroom.exception.ClassroomExceptionHandler;
import com.kizuna.features.classroom.service.ClassScheduleService;
import com.kizuna.features.classroom.service.ClassroomService;
import com.kizuna.features.classroom.service.LearningMaterialService;
import com.kizuna.features.classroom.util.CurrentActor;
import com.kizuna.features.classroom.util.CurrentUserResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

/**
 * API FE-06 Classroom & Learning Materials Management.
 * Mọi endpoint yêu cầu header Authorization: Bearer &lt;token&gt;.
 */
@RestController
@RequestMapping("/api/classrooms")
@RequiredArgsConstructor
@CrossOrigin(origins = {"http://localhost:3000", "http://127.0.0.1:3000"})
public class ClassroomController extends ClassroomExceptionHandler {

    private final CurrentUserResolver currentUserResolver;
    private final ClassroomService classroomService;
    private final LearningMaterialService materialService;
    private final ClassScheduleService scheduleService;

    // ===== Lớp học (UC-93, UC-58) =====

    @GetMapping
    public ClassroomApiResponse<List<ClassroomSummaryResponse>> listMine(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String auth) {
        CurrentActor actor = currentUserResolver.resolve(auth);
        return ClassroomApiResponse.ok("Tải danh sách lớp học thành công.", classroomService.listMine(actor));
    }

    @GetMapping("/eligible-learners")
    public ClassroomApiResponse<List<EligibleLearnerResponse>> eligibleLearners(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String auth) {
        CurrentActor actor = currentUserResolver.resolve(auth);
        return ClassroomApiResponse.ok("Tải danh sách học viên đủ điều kiện thành công.",
                classroomService.listEligibleLearners(actor));
    }

    @PostMapping
    public ResponseEntity<ClassroomApiResponse<ClassroomSummaryResponse>> create(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String auth,
            @RequestBody ClassroomCreateRequest request) {
        CurrentActor actor = currentUserResolver.resolve(auth);
        return ResponseEntity.status(HttpStatus.CREATED).body(ClassroomApiResponse.ok(
                "Tạo lớp học thành công.", classroomService.create(actor, request)));
    }

    @GetMapping("/{spaceId}")
    public ClassroomApiResponse<ClassroomSummaryResponse> detail(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String auth,
            @PathVariable UUID spaceId) {
        CurrentActor actor = currentUserResolver.resolve(auth);
        return ClassroomApiResponse.ok("Tải thông tin lớp học thành công.", classroomService.getDetail(actor, spaceId));
    }

    // ===== Lịch học hằng tuần (thêm / sửa; không có xóa theo nghiệp vụ) =====

    @GetMapping("/{spaceId}/schedules")
    public ClassroomApiResponse<List<ClassScheduleResponse>> schedules(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String auth,
            @PathVariable UUID spaceId) {
        CurrentActor actor = currentUserResolver.resolve(auth);
        return ClassroomApiResponse.ok("Tải lịch học thành công.", scheduleService.list(actor, spaceId));
    }

    @PostMapping("/{spaceId}/schedules")
    public ResponseEntity<ClassroomApiResponse<ClassScheduleResponse>> addSchedule(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String auth,
            @PathVariable UUID spaceId,
            @RequestBody ClassScheduleRequest request) {
        CurrentActor actor = currentUserResolver.resolve(auth);
        return ResponseEntity.status(HttpStatus.CREATED).body(ClassroomApiResponse.ok(
                "Thêm buổi học thành công.", scheduleService.add(actor, spaceId, request)));
    }

    @PutMapping("/{spaceId}/schedules/{scheduleId}")
    public ClassroomApiResponse<ClassScheduleResponse> updateSchedule(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String auth,
            @PathVariable UUID spaceId, @PathVariable UUID scheduleId,
            @RequestBody ClassScheduleRequest request) {
        CurrentActor actor = currentUserResolver.resolve(auth);
        return ClassroomApiResponse.ok("Cập nhật lịch học thành công.",
                scheduleService.update(actor, spaceId, scheduleId, request));
    }

    // ===== Tài liệu học tập (UC-61, UC-65, UC-94, Delete Learning Material) =====

    @GetMapping("/{spaceId}/materials")
    public ClassroomApiResponse<List<LearningMaterialResponse>> materials(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String auth,
            @PathVariable UUID spaceId) {
        CurrentActor actor = currentUserResolver.resolve(auth);
        return ClassroomApiResponse.ok("Tải danh sách tài liệu thành công.", materialService.list(actor, spaceId));
    }

    /** Trả nội dung tệp (đọc từ DB) sau khi kiểm quyền; trình duyệt hiển thị trực tiếp hoặc tải về. */
    @GetMapping("/{spaceId}/materials/{materialId}/file")
    public ResponseEntity<byte[]> file(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String auth,
            @PathVariable UUID spaceId, @PathVariable UUID materialId) {
        CurrentActor actor = currentUserResolver.resolve(auth);
        MaterialFileContent content = materialService.getFile(actor, spaceId, materialId);
        ContentDisposition disposition = ContentDisposition.inline()
                .filename(content.fileName(), StandardCharsets.UTF_8)
                .build();
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(content.contentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .header(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS, HttpHeaders.CONTENT_DISPOSITION)
                .header("X-Content-Type-Options", "nosniff")
                .header(HttpHeaders.CACHE_CONTROL, "private, no-store")
                .body(content.data());
    }

    @PostMapping(value = "/{spaceId}/materials", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ClassroomApiResponse<LearningMaterialResponse>> upload(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String auth,
            @PathVariable UUID spaceId,
            @RequestParam(value = "title", required = false) String title,
            @RequestParam(value = "description", required = false) String description,
            @RequestPart(value = "file", required = false) MultipartFile file) {
        CurrentActor actor = currentUserResolver.resolve(auth);
        return ResponseEntity.status(HttpStatus.CREATED).body(ClassroomApiResponse.ok(
                "Tải lên tài liệu thành công.", materialService.upload(actor, spaceId, title, description, file)));
    }

    @PutMapping(value = "/{spaceId}/materials/{materialId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ClassroomApiResponse<LearningMaterialResponse> update(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String auth,
            @PathVariable UUID spaceId, @PathVariable UUID materialId,
            @RequestParam(value = "title", required = false) String title,
            @RequestParam(value = "description", required = false) String description,
            @RequestPart(value = "file", required = false) MultipartFile file) {
        CurrentActor actor = currentUserResolver.resolve(auth);
        return ClassroomApiResponse.ok("Cập nhật tài liệu thành công.",
                materialService.update(actor, spaceId, materialId, title, description, file));
    }

    @DeleteMapping("/{spaceId}/materials/{materialId}")
    public ClassroomApiResponse<Void> delete(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String auth,
            @PathVariable UUID spaceId, @PathVariable UUID materialId) {
        CurrentActor actor = currentUserResolver.resolve(auth);
        materialService.delete(actor, spaceId, materialId);
        return ClassroomApiResponse.ok("Xóa tài liệu thành công.", null);
    }
}
