package com.kizuna.features.admin.controller;
import com.kizuna.core.dto.ApiResponse;
import com.kizuna.features.admin.dto.*;
import com.kizuna.features.admin.service.*;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import java.net.URI;
import java.time.*;
import java.util.*;
@RestController @RequestMapping("/api/admin/operations") @RequiredArgsConstructor
public class OperationsApiController {
    private final BookingPolicyConfigurationService policy;
    private final OperationsActorResolver actors;
    private final OperationsAccountSummaryService accountSummary;
    private final OperationsAuditLogService auditLog;
    private final com.kizuna.features.admin.service.OperationsDashboardService dashboardOverview;
    @GetMapping("/me") public ApiResponse<?> currentAdministrator() {
        return ApiResponse.ok(actors.administratorProfile());
    }
    /** Tổng quan Admin: tài khoản mới so với kỳ trước (tỷ lệ tăng giảm trên thẻ). */
    @GetMapping("/dashboard/overview") public ApiResponse<?> overview(@RequestParam(required=false) LocalDate startDate,@RequestParam(required=false) LocalDate endDate) { return ApiResponse.ok(dashboardOverview.overview(startDate,endDate)); }
    @GetMapping("/dashboard/accounts") public ApiResponse<?> accountSummary() { return ApiResponse.ok(accountSummary.read()); }
    @GetMapping("/dashboard/account-statistics") public ApiResponse<?> accountStatistics(@RequestParam(required=false) LocalDate startDate,@RequestParam(required=false) LocalDate endDate) { return ApiResponse.ok(accountSummary.statistics(startDate,endDate)); }
    @GetMapping("/csrf") public ApiResponse<?> csrf(org.springframework.security.web.csrf.CsrfToken token) {
        actors.requireAdmin(); return ApiResponse.ok(Map.of("token",token.getToken(),"headerName",token.getHeaderName()));
    }
    @GetMapping("/booking-policy") public ApiResponse<?> configuration() { return ApiResponse.ok(policy.read()); }
    @PutMapping("/booking-policy") public ApiResponse<?> configure(@jakarta.validation.Valid @RequestBody BookingPolicyUpdateRequest r) { return ApiResponse.ok(policy.update(r)); }
    @GetMapping("/booking-policy/history") public ApiResponse<?> configHistory(@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="20") int size) { return ApiResponse.ok(policy.history(page,size)); }
    @GetMapping("/audit-log") public ApiResponse<?> auditLog(@RequestParam(required=false) String source,@RequestParam(required=false) UUID actor,@RequestParam(required=false) LocalDate fromDate,@RequestParam(required=false) LocalDate toDate,
        @RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="20") int size) {
        return ApiResponse.ok(auditLog.list(source,actor,fromDate,toDate,page,size));
    }
}
