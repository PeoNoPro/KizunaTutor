package com.kizuna.features.payment.controller;

import com.kizuna.core.dto.ApiResponse;
import com.kizuna.features.payment.dto.request.MembershipRequests;
import com.kizuna.features.payment.dto.response.MembershipViews;
import com.kizuna.features.payment.service.CurrentUserAccessor;
import com.kizuna.features.payment.service.PaymentMessages;
import com.kizuna.features.payment.service.TutorMembershipService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * API gói hội viên phía gia sư (contracts/membership-api.md mục A). Phân quyền ROLE_TUTOR trong SecurityConfig.
 */
@RestController
@RequestMapping("/api/tutor/memberships")
@CrossOrigin(origins = {"http://localhost:3000", "http://127.0.0.1:3000"})
public class TutorMembershipController {

    private final TutorMembershipService service;
    private final CurrentUserAccessor currentUser;

    public TutorMembershipController(TutorMembershipService service, CurrentUserAccessor currentUser) {
        this.service = service;
        this.currentUser = currentUser;
    }

    @GetMapping("/plans")
    public ResponseEntity<ApiResponse<List<MembershipViews.PlanView>>> plans() {
        return ResponseEntity.ok(ApiResponse.ok(service.listPlans()));
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<MembershipViews.MyMembershipView>> me() {
        return ResponseEntity.ok(ApiResponse.ok(service.getMyMembership(currentUser.requireUser())));
    }

    @PostMapping("/payments")
    public ResponseEntity<ApiResponse<MembershipViews.PaymentCreatedView>> createPayment(
            @Valid @RequestBody MembershipRequests.CreatePaymentRequest request, HttpServletRequest http) {
        MembershipViews.PaymentCreatedView created =
                service.createPayment(currentUser.requireUser(), request.planId(), ClientIp.of(http));
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(PaymentMessages.PAYMENT_CREATED, created));
    }

    @GetMapping("/payments")
    public ResponseEntity<ApiResponse<MembershipViews.PageView<MembershipViews.PaymentView>>> myPayments(
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.ok(service.listMyPayments(currentUser.requireUser(), page, size)));
    }

    @PostMapping("/payments/{transactionId}/check")
    public ResponseEntity<ApiResponse<MembershipViews.PaymentResultView>> checkPayment(
            @PathVariable String transactionId, HttpServletRequest http) {
        MembershipViews.PaymentResultView result =
                service.checkMyPayment(currentUser.requireUser(), transactionId, ClientIp.of(http));
        return ResponseEntity.ok(ApiResponse.ok(result.resultMessage(), result));
    }

    @PostMapping("/payments/{transactionId}/resume")
    public ResponseEntity<ApiResponse<MembershipViews.PaymentCreatedView>> resumePayment(
            @PathVariable String transactionId, HttpServletRequest http) {
        return ResponseEntity.ok(ApiResponse.ok(PaymentMessages.PAYMENT_RESUMED,
                service.resumePayment(currentUser.requireUser(), transactionId, ClientIp.of(http))));
    }

    @GetMapping("/payments/vnpay-return")
    public ResponseEntity<ApiResponse<MembershipViews.PaymentResultView>> vnpayReturn(
            @RequestParam Map<String, String> params, HttpServletRequest http) {
        MembershipViews.PaymentResultView result = service.handleReturn(currentUser.requireUser(), params, ClientIp.of(http));
        return ResponseEntity.ok(ApiResponse.ok(result.resultMessage(), result));
    }
}
