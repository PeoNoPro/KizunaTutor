package com.kizuna.features.classroom.controller;

import com.kizuna.features.classroom.dto.*;
import com.kizuna.features.classroom.exception.ClassroomExceptionHandler;
import com.kizuna.features.classroom.service.ClassScheduleService;
import com.kizuna.features.classroom.service.ClassSessionService;
import com.kizuna.features.classroom.service.ClassroomService;
import com.kizuna.features.classroom.service.LearningMaterialService;
import com.kizuna.features.classroom.util.CurrentActor;
import com.kizuna.features.classroom.util.CurrentUserResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
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
    private final ClassSessionService sessionService;

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

    @PutMapping("/{spaceId}")
    public ClassroomApiResponse<ClassroomSummaryResponse> rename(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String auth,
            @PathVariable UUID spaceId,
            @RequestBody ClassroomRenameRequest request) {
        CurrentActor actor = currentUserResolver.resolve(auth);
        return ClassroomApiResponse.ok("Đổi tên lớp học thành công.",
                classroomService.rename(actor, spaceId, request == null ? null : request.title()));
    }

    // ===== Lịch học hằng tuần (thêm / sửa / xóa; lớp luôn còn ít nhất 1 buổi) =====

    /** Thời khóa biểu của người đang đăng nhập (dùng cho lưới tuần "Lịch giảng dạy" của FE-05). */
    @GetMapping("/schedules/me")
    public ClassroomApiResponse<List<TimetableEntryResponse>> myTimetable(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String auth) {
        CurrentActor actor = currentUserResolver.resolve(auth);
        return ClassroomApiResponse.ok("Tải thời khóa biểu thành công.", scheduleService.timetable(actor));
    }

    /** Gia sư sửa khóa học: tổng số buổi (không nhỏ hơn số buổi đã học) và ngày bắt đầu (khi chưa có buổi nào diễn ra). */
    @PutMapping("/{spaceId}/course")
    public ClassroomApiResponse<ClassroomSummaryResponse> updateCourse(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String auth,
            @PathVariable UUID spaceId,
            @RequestBody ClassroomCourseRequest request) {
        CurrentActor actor = currentUserResolver.resolve(auth);
        return ClassroomApiResponse.ok("Cập nhật khóa học thành công.", classroomService.updateCourse(actor, spaceId, request));
    }

    // ===== Buổi học cụ thể của khóa (lưới tuần): dời / hủy / thêm riêng trong một tuần =====

    /** Các buổi học cụ thể trong khoảng [from, to] (tối đa 62 ngày) của mọi lớp đang mở mà người dùng tham gia. */
    @GetMapping("/sessions/me")
    public ClassroomApiResponse<List<ClassSessionResponse>> mySessions(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String auth,
            @RequestParam("from") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam("to") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        CurrentActor actor = currentUserResolver.resolve(auth);
        return ClassroomApiResponse.ok("Tải lịch học thành công.", sessionService.sessions(actor, from, to));
    }

    /** Gia sư dời MỘT buổi (buổi của lịch {scheduleId} rơi vào ngày {originalDate}) sang ngày/giờ khác trong cùng tuần. */
    @PutMapping("/{spaceId}/schedules/{scheduleId}/sessions/{originalDate}")
    public ClassroomApiResponse<Void> moveSession(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String auth,
            @PathVariable UUID spaceId, @PathVariable UUID scheduleId,
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate originalDate,
            @RequestBody ClassSessionMoveRequest request) {
        CurrentActor actor = currentUserResolver.resolve(auth);
        sessionService.move(actor, spaceId, scheduleId, originalDate, request);
        return ClassroomApiResponse.ok("Dời buổi học thành công.", null);
    }

    /** Gia sư hủy riêng buổi của tuần đó (khóa tự bù thêm buổi ở cuối). */
    @PostMapping("/{spaceId}/schedules/{scheduleId}/sessions/{originalDate}/cancel")
    public ClassroomApiResponse<Void> cancelSession(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String auth,
            @PathVariable UUID spaceId, @PathVariable UUID scheduleId,
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate originalDate) {
        CurrentActor actor = currentUserResolver.resolve(auth);
        sessionService.cancel(actor, spaceId, scheduleId, originalDate);
        return ClassroomApiResponse.ok("Đã hủy buổi học của tuần này.", null);
    }

    /** Gia sư đưa buổi đã dời về lại lịch gốc. */
    @DeleteMapping("/{spaceId}/schedules/{scheduleId}/sessions/{originalDate}")
    public ClassroomApiResponse<Void> restoreSession(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String auth,
            @PathVariable UUID spaceId, @PathVariable UUID scheduleId,
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate originalDate) {
        CurrentActor actor = currentUserResolver.resolve(auth);
        sessionService.restore(actor, spaceId, scheduleId, originalDate);
        return ClassroomApiResponse.ok("Đã đưa buổi học về lịch gốc.", null);
    }

    /** Gia sư thêm một buổi riêng trong một tuần (không ảnh hưởng các tuần khác, tính vào tổng số buổi). */
    @PostMapping("/{spaceId}/extra-sessions")
    public ResponseEntity<ClassroomApiResponse<Void>> addExtraSession(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String auth,
            @PathVariable UUID spaceId,
            @RequestBody ClassSessionMoveRequest request) {
        CurrentActor actor = currentUserResolver.resolve(auth);
        sessionService.addExtra(actor, spaceId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ClassroomApiResponse.ok("Thêm buổi học thành công.", null));
    }

    @PutMapping("/{spaceId}/extra-sessions/{extraId}")
    public ClassroomApiResponse<Void> moveExtraSession(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String auth,
            @PathVariable UUID spaceId, @PathVariable UUID extraId,
            @RequestBody ClassSessionMoveRequest request) {
        CurrentActor actor = currentUserResolver.resolve(auth);
        sessionService.moveExtra(actor, spaceId, extraId, request);
        return ClassroomApiResponse.ok("Dời buổi học thành công.", null);
    }

    @DeleteMapping("/{spaceId}/extra-sessions/{extraId}")
    public ClassroomApiResponse<Void> cancelExtraSession(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String auth,
            @PathVariable UUID spaceId, @PathVariable UUID extraId) {
        CurrentActor actor = currentUserResolver.resolve(auth);
        sessionService.cancelExtra(actor, spaceId, extraId);
        return ClassroomApiResponse.ok("Đã hủy buổi học thêm.", null);
    }

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

    @DeleteMapping("/{spaceId}/schedules/{scheduleId}")
    public ClassroomApiResponse<Void> deleteSchedule(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String auth,
            @PathVariable UUID spaceId, @PathVariable UUID scheduleId) {
        CurrentActor actor = currentUserResolver.resolve(auth);
        scheduleService.delete(actor, spaceId, scheduleId);
        return ClassroomApiResponse.ok("Xóa buổi học thành công.", null);
    }

    // ===== Tài liệu học tập (UC-61, UC-65, UC-94, Delete Learning Material) =====

    @GetMapping("/{spaceId}/materials")
    public ClassroomApiResponse<List<LearningMaterialResponse>> materials(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String auth,
            @PathVariable UUID spaceId) {
        CurrentActor actor = currentUserResolver.resolve(auth);
        return ClassroomApiResponse.ok("Tải danh sách tài liệu thành công.", materialService.list(actor, spaceId));
    }

    /** Trả nội dung một tệp của tài liệu (đọc từ DB) sau khi kiểm quyền; trình duyệt hiển thị trực tiếp hoặc tải về. */
    @GetMapping("/{spaceId}/materials/{materialId}/files/{fileId}")
    public ResponseEntity<byte[]> file(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String auth,
            @PathVariable UUID spaceId, @PathVariable UUID materialId, @PathVariable UUID fileId) {
        CurrentActor actor = currentUserResolver.resolve(auth);
        MaterialFileContent content = materialService.getFile(actor, spaceId, materialId, fileId);
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

    /** Tạo một tài liệu gồm 1–10 tệp (nhiều phần "file"), tiêu đề bắt buộc. */
    @PostMapping(value = "/{spaceId}/materials", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ClassroomApiResponse<LearningMaterialResponse>> upload(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String auth,
            @PathVariable UUID spaceId,
            @RequestParam(value = "title", required = false) String title,
            @RequestParam(value = "description", required = false) String description,
            @RequestPart(value = "file", required = false) List<MultipartFile> files) {
        CurrentActor actor = currentUserResolver.resolve(auth);
        return ResponseEntity.status(HttpStatus.CREATED).body(ClassroomApiResponse.ok(
                "Tải lên tài liệu thành công.", materialService.upload(actor, spaceId, title, description, files)));
    }

    @PutMapping(value = "/{spaceId}/materials/{materialId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ClassroomApiResponse<LearningMaterialResponse> update(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String auth,
            @PathVariable UUID spaceId, @PathVariable UUID materialId,
            @RequestParam(value = "title", required = false) String title,
            @RequestParam(value = "description", required = false) String description,
            @RequestParam(value = "removeFileIds", required = false) List<UUID> removeFileIds,
            @RequestPart(value = "file", required = false) List<MultipartFile> files) {
        CurrentActor actor = currentUserResolver.resolve(auth);
        return ClassroomApiResponse.ok("Cập nhật tài liệu thành công.",
                materialService.update(actor, spaceId, materialId, title, description, files, removeFileIds));
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
