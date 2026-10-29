package com.kizuna.features.payment.controller;


import com.kizuna.core.entity.*;
import com.kizuna.features.payment.dto.CreatePaymentRequest;
import com.kizuna.features.payment.dto.TutorMembershipDto;
import com.kizuna.features.payment.entity.MembershipPlan;
import com.kizuna.core.entity.RoleEnum;
import com.kizuna.features.payment.service.ITutorMembershipService;
import com.kizuna.core.security.SecurityContextUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * REST API phục vụ Giai đoạn 2 & 3: Quản lý gói thành viên & thanh toán VNPay (UC 71-77, UC 94).
 */
@RestController
@RequestMapping("/api/tutor/memberships")
@PreAuthorize("hasAuthority('ROLE_TUTOR')")
@RequiredArgsConstructor
public class TutorMembershipApiController {

    private final ITutorMembershipService membershipService;
    private final SecurityContextUtil securityUtil;

    /**
     * Lấy danh sách các gói thành viên đang hoạt động (UC 71, 72).
     */
    @GetMapping("/plans")
    public ResponseEntity<Map<String, Object>> getActivePlans() {
        List<MembershipPlan> plans = membershipService.getActivePlans();
        return ResponseEntity.ok(Map.of(
                "success", true,
                "data", plans
        ));
    }

    /**
     * Lấy thông tin gói thành viên hiện tại của gia sư (UC 76).
     */
    @GetMapping("/current")
    public ResponseEntity<Map<String, Object>> getCurrentMembership() {
        String email = securityUtil.getCurrentUserEmail(RoleEnum.ROLE_TUTOR);
        TutorMembershipDto current = membershipService.getCurrentMembership(email);
        return ResponseEntity.ok(Map.of(
                "success", true,
                "data", current != null ? current : "Chưa có gói hoạt động"
        ));
    }

    /**
     * Tạo liên kết thanh toán sang cổng VNPay (UC 73).
     */
    @PostMapping("/payment-url")
    public ResponseEntity<Map<String, Object>> createPaymentUrl(@RequestBody @Valid CreatePaymentRequest request,
                                                                 HttpServletRequest servletRequest) {
        String email = securityUtil.getCurrentUserEmail(RoleEnum.ROLE_TUTOR);
        String paymentUrl = membershipService.createVnPayPaymentUrl(email, request.getPlanId(), servletRequest);

        return ResponseEntity.ok(Map.of(
                "success", true,
                "paymentUrl", paymentUrl
        ));
    }

    /**
     * Kích hoạt gói thành viên trực tiếp (Hỗ trợ thử nghiệm và demo quy trình).
     */
    @PostMapping("/direct-activate/{planId}")
    public ResponseEntity<Map<String, Object>> directActivate(@PathVariable UUID planId) {
        String email = securityUtil.getCurrentUserEmail(RoleEnum.ROLE_TUTOR);
        TutorMembershipDto result = membershipService.directActivateMembershipForTest(email, planId);

        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Kích hoạt gói thành viên thành công! Tài khoản gia sư đã được mở khóa toàn diện.",
                "data", result
        ));
    }
}
