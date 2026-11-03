package com.kizuna.features.classroom.controller;

import com.kizuna.features.classroom.dto.*;
import com.kizuna.features.classroom.service.ClassroomService;
import com.kizuna.features.classroom.service.LearningMaterialService;
import com.kizuna.features.classroom.util.CurrentActor;
import com.kizuna.features.classroom.util.CurrentUserResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

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
public class ClassroomController {

    private final CurrentUserResolver currentUserResolver;
    private final ClassroomService classroomService;
    private final LearningMaterialService materialService;

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
                "Đã thêm học viên vào lớp học và kích hoạt quyền truy cập.", classroomService.create(actor, request)));
    }

    @GetMapping("/{spaceId}")
    public ClassroomApiResponse<ClassroomSummaryResponse> detail(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String auth,
            @PathVariable UUID spaceId) {
        CurrentActor actor = currentUserResolver.resolve(auth);
        return ClassroomApiResponse.ok("Tải thông tin lớp học thành công.", classroomService.getDetail(actor, spaceId));
    }

    // ===== Tài liệu học tập (UC-61, UC-65, UC-94) =====

    @GetMapping("/{spaceId}/materials")
    public ClassroomApiResponse<List<LearningMaterialResponse>> materials(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String auth,
            @PathVariable UUID spaceId) {
        CurrentActor actor = currentUserResolver.resolve(auth);
        return ClassroomApiResponse.ok("Tải danh sách tài liệu thành công.", materialService.list(actor, spaceId));
    }

    @GetMapping("/{spaceId}/materials/{materialId}/file")
    public ClassroomApiResponse<MaterialFileLinkResponse> fileLink(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String auth,
            @PathVariable UUID spaceId, @PathVariable UUID materialId) {
        CurrentActor actor = currentUserResolver.resolve(auth);
        return ClassroomApiResponse.ok("Đã tạo liên kết mở tài liệu.", materialService.getFileLink(actor, spaceId, materialId));
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
}
