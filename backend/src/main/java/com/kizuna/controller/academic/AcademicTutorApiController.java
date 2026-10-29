package com.kizuna.controller.academic;

import com.kizuna.dto.tutor.TutorProfileDto;
import com.kizuna.entity.Tutor;
import com.kizuna.entity.TutorState;
import com.kizuna.repository.TutorRepository;
import com.kizuna.service.tutor.ITutorProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * REST API dành cho Ban Học Vụ (Academic Officer) thẩm định hồ sơ gia sư (UC 13, 14, 15, 16, 20).
 */
@RestController
@RequestMapping("/api/academic/tutors")
@PreAuthorize("hasAuthority('ROLE_ACADEMIC_OFFICER')")
@RequiredArgsConstructor
public class AcademicTutorApiController {

    private final ITutorProfileService profileService;
    private final TutorRepository tutorRepository;

    /**
     * Lấy danh sách hồ sơ gia sư đang chờ thẩm định (PENDING_VERIFICATION)
     */
    @GetMapping("/pending")
    public ResponseEntity<Map<String, Object>> getPendingTutors() {
        List<Tutor> tutors = tutorRepository.findAll().stream()
                .filter(t -> t.getTutorState() == TutorState.PENDING_VERIFICATION)
                .toList();

        return ResponseEntity.ok(Map.of(
                "success", true,
                "count", tutors.size(),
                "data", tutors
        ));
    }

    /**
     * Phê duyệt hồ sơ gia sư (UC 20) -> Chuyển sang APPROVED_UNPAID
     */
    @PostMapping("/{tutorId}/approve")
    public ResponseEntity<Map<String, Object>> approveTutor(@PathVariable UUID tutorId) {
        TutorProfileDto updated = profileService.approveProfile(tutorId);
        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Phê duyệt hồ sơ gia sư thành công. Gia sư có thể tiến hành chọn gói thành viên.",
                "data", updated
        ));
    }

    /**
     * Từ chối hồ sơ gia sư kèm lý do góp ý (UC 15, 16) -> Chuyển sang REJECTED
     */
    @PostMapping("/{tutorId}/reject")
    public ResponseEntity<Map<String, Object>> rejectTutor(@PathVariable UUID tutorId,
                                                           @RequestBody(required = false) Map<String, String> body) {
        String feedback = body != null ? body.get("feedback") : null;
        TutorProfileDto updated = profileService.rejectProfile(tutorId, feedback);
        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Đã từ chối hồ sơ và gửi lý do phản hồi cho gia sư thành công.",
                "data", updated
        ));
    }
}
