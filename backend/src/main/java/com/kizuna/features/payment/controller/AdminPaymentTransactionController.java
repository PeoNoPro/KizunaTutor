package com.kizuna.features.payment.controller;

import com.kizuna.core.dto.ApiResponse;
import com.kizuna.features.payment.dto.request.MembershipRequests;
import com.kizuna.features.payment.dto.response.MembershipViews;
import com.kizuna.features.payment.service.CurrentUserAccessor;
import com.kizuna.features.payment.service.PaymentIssueService;
import com.kizuna.features.payment.service.PaymentMessages;
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

/**
 * Xử lý sự cố thanh toán (contracts mục D). Phân quyền ROLE_ADMIN trong SecurityConfig.
 */
@RestController
@RequestMapping("/api/admin/payment-transactions")
@CrossOrigin(origins = {"http://localhost:3000", "http://127.0.0.1:3000"})
public class AdminPaymentTransactionController {

    private final PaymentIssueService service;
    private final CurrentUserAccessor currentUser;

    public AdminPaymentTransactionController(PaymentIssueService service, CurrentUserAccessor currentUser) {
        this.service = service;
        this.currentUser = currentUser;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<MembershipViews.PageView<MembershipViews.AdminTransactionRowView>>> list(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.ok(service.listTransactions(status, from, to, keyword, page, size)));
    }

    @GetMapping("/{transactionId}")
    public ResponseEntity<ApiResponse<MembershipViews.AdminTransactionDetailView>> detail(@PathVariable String transactionId) {
        return ResponseEntity.ok(ApiResponse.ok(service.getDetail(transactionId)));
    }

    @PostMapping("/{transactionId}/gateway-check")
    public ResponseEntity<ApiResponse<MembershipViews.GatewayCheckView>> gatewayCheck(
            @PathVariable String transactionId, HttpServletRequest http) {
        MembershipViews.GatewayCheckView view =
                service.checkWithGateway(transactionId, currentUser.requireUser(), ClientIp.of(http));
        return ResponseEntity.ok(ApiResponse.ok(view.gatewayMessage(), view));
    }

    @PostMapping("/{transactionId}/notes")
    public ResponseEntity<ApiResponse<MembershipViews.NoteView>> addNote(
            @PathVariable String transactionId, @Valid @RequestBody MembershipRequests.NoteRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(PaymentMessages.NOTE_SAVED,
                service.addNote(transactionId, currentUser.requireUser(), request.note())));
    }
}
