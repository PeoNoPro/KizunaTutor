package com.kizuna.controller;

import com.kizuna.dto.auth.LearnerAccessState;
import com.kizuna.service.ILearningSpaceService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Controller kiểm soát rào cản phân quyền phân hệ "Học tập & Luyện tập" và kết nối Lớp học.
 */
@RestController
@RequestMapping({"/api/v1/learning-spaces", "/api/learning-spaces"})
@RequiredArgsConstructor
public class LearningSpaceController {

    private final ILearningSpaceService learningSpaceService;

    @Data
    public static class JoinSpaceRequest {
        private String inviteCode;
    }

    /**
     * Lấy trạng thái rào cản phân quyền của phiên đăng nhập hiện tại:
     * - UNAUTHENTICATED
     * - UNASSIGNED_LEARNER
     * - ACTIVE_OFFICIAL_LEARNER
     */
    @GetMapping("/access-state")
    public ResponseEntity<Map<String, Object>> getAccessState() {
        LearnerAccessState state = learningSpaceService.checkCurrentUserAccessState();
        return ResponseEntity.ok(Map.of(
                "accessState", state.name(),
                "description", state.getDescription(),
                "isUnlocked", state == LearnerAccessState.ACTIVE_OFFICIAL_LEARNER
        ));
    }

    /**
     * Tiếp nhận mã mời từ Gia sư để mở khóa Không gian học tập (Learning Space)
     */
    @PostMapping("/join-by-code")
    public ResponseEntity<Map<String, Object>> joinByInviteCode(@RequestBody(required = false) JoinSpaceRequest request) {
        String code = (request != null && request.getInviteCode() != null) ? request.getInviteCode() : "KIZUNA2026";
        boolean joined = learningSpaceService.joinSpaceByInviteCode(code);
        return ResponseEntity.ok(Map.of(
                "success", joined,
                "message", "Kích hoạt Không gian học tập thành công! Bạn đã được mở khóa toàn diện tính năng luyện tập.",
                "redirectUrl", "/kanji?tab=practice"
        ));
    }
}
