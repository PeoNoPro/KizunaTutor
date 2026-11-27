package com.kizuna.features.payment.controller;

import com.kizuna.core.dto.ApiResponse;
import com.kizuna.features.payment.dto.response.MembershipViews;
import com.kizuna.features.payment.service.TutorMembershipMonitorService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Danh sách và chi tiết hội viên gia sư (contracts mục C5, C6). Phân quyền ROLE_MANAGER trong SecurityConfig.
 */
@RestController
@RequestMapping("/api/manager/tutor-memberships")
@CrossOrigin(origins = {"http://localhost:3000", "http://127.0.0.1:3000"})
public class ManagerTutorMembershipController {

    private final TutorMembershipMonitorService service;

    public ManagerTutorMembershipController(TutorMembershipMonitorService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<MembershipViews.PageView<MembershipViews.TutorMembershipRowView>>> list(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String planId,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.ok(service.listMemberships(keyword, planId, status, page, size)));
    }

    @GetMapping("/{tutorId}")
    public ResponseEntity<ApiResponse<MembershipViews.TutorMembershipDetailView>> detail(@PathVariable String tutorId) {
        return ResponseEntity.ok(ApiResponse.ok(service.getDetail(tutorId)));
    }
}
