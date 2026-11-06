package com.kizuna.features.payment.controller;

import com.kizuna.core.dto.ApiResponse;
import com.kizuna.features.payment.dto.request.MembershipRequests;
import com.kizuna.features.payment.dto.response.MembershipViews;
import com.kizuna.features.payment.service.MembershipPlanAdminService;
import com.kizuna.features.payment.service.PaymentMessages;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Cấu hình gói hội viên (contracts mục C1–C4). Phân quyền ROLE_MANAGER trong SecurityConfig.
 */
@RestController
@RequestMapping("/api/manager/membership-plans")
@CrossOrigin(origins = {"http://localhost:3000", "http://127.0.0.1:3000"})
public class ManagerMembershipPlanController {

    private final MembershipPlanAdminService service;

    public ManagerMembershipPlanController(MembershipPlanAdminService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<MembershipViews.PlanAdminView>>> list() {
        return ResponseEntity.ok(ApiResponse.ok(service.listPlans()));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<MembershipViews.PlanAdminView>> create(
            @Valid @RequestBody MembershipRequests.CreatePlanRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(PaymentMessages.PLAN_CREATED, service.createPlan(request)));
    }

    @PutMapping("/{planId}")
    public ResponseEntity<ApiResponse<MembershipViews.PlanAdminView>> update(
            @PathVariable String planId, @Valid @RequestBody MembershipRequests.UpdatePlanRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(PaymentMessages.MSG04_UPDATED, service.updatePlan(planId, request)));
    }

    @PostMapping("/{planId}/activate")
    public ResponseEntity<ApiResponse<MembershipViews.PlanAdminView>> activate(@PathVariable String planId) {
        return ResponseEntity.ok(ApiResponse.ok(PaymentMessages.PLAN_OPENED, service.setActive(planId, true)));
    }

    @PostMapping("/{planId}/deactivate")
    public ResponseEntity<ApiResponse<MembershipViews.PlanAdminView>> deactivate(@PathVariable String planId) {
        return ResponseEntity.ok(ApiResponse.ok(PaymentMessages.PLAN_CLOSED, service.setActive(planId, false)));
    }
}
