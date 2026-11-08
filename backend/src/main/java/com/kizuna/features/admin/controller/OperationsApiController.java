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
    private final TechnicalIncidentService incidents;
    private final TechnicalIncidentExportService incidentExport;
    private final OperationsIncidentReportService reports;
    private final OperationsActorResolver actors;
    private final OperationsComponentCatalog catalog;
    private final OperationsAccountSummaryService accountSummary;
    private final OperationsAuditLogService auditLog;
    @GetMapping("/me") public ApiResponse<?> currentAdministrator() {
        return ApiResponse.ok(actors.administratorProfile());
    }
    @GetMapping("/dashboard/accounts") public ApiResponse<?> accountSummary() { return ApiResponse.ok(accountSummary.read()); }
    @GetMapping("/dashboard/account-statistics") public ApiResponse<?> accountStatistics(@RequestParam(required=false) LocalDate startDate,@RequestParam(required=false) LocalDate endDate) { return ApiResponse.ok(accountSummary.statistics(startDate,endDate)); }
    @GetMapping("/csrf") public ApiResponse<?> csrf(org.springframework.security.web.csrf.CsrfToken token) {
        actors.requireAdmin(); return ApiResponse.ok(Map.of("token",token.getToken(),"headerName",token.getHeaderName()));
    }
    @GetMapping("/booking-policy") public ApiResponse<?> configuration() { return ApiResponse.ok(policy.read()); }
    @PutMapping("/booking-policy") public ApiResponse<?> configure(@jakarta.validation.Valid @RequestBody BookingPolicyUpdateRequest r) { return ApiResponse.ok(policy.update(r)); }
    @GetMapping("/booking-policy/history") public ApiResponse<?> configHistory(@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="20") int size) { return ApiResponse.ok(policy.history(page,size)); }
    @GetMapping("/assignees") public ApiResponse<?> assignees() { return ApiResponse.ok(actors.admins()); }
    @GetMapping("/components") public ApiResponse<?> components() { actors.requireAdmin(); return ApiResponse.ok(catalog.options()); }
    @GetMapping("/reports/incidents") public ApiResponse<?> report(@RequestParam(required=false) LocalDate startDate,@RequestParam(required=false) LocalDate endDate,
        @RequestParam(required=false) String metric,@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="20") int size,@RequestParam(required=false) OffsetDateTime cutoff) {
        return ApiResponse.ok(reports.report(startDate,endDate,metric,page,size,cutoff));
    }
    @GetMapping("/audit-log") public ApiResponse<?> auditLog(@RequestParam(required=false) String source,@RequestParam(required=false) UUID actor,@RequestParam(required=false) LocalDate fromDate,@RequestParam(required=false) LocalDate toDate,
        @RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="20") int size) {
        return ApiResponse.ok(auditLog.list(source,actor,fromDate,toDate,page,size));
    }
    @GetMapping("/incidents") public ApiResponse<?> list(@RequestParam(required=false) String status,@RequestParam(required=false) String severity,@RequestParam(required=false) String component,
        @RequestParam(required=false) UUID assignee,@RequestParam(required=false) LocalDate fromDate,@RequestParam(required=false) LocalDate toDate,
        @RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="20") int size) {
        return ApiResponse.ok(incidents.list(status,severity,component,assignee,fromDate,toDate,page,size));
    }
    @GetMapping("/incidents/export") public ResponseEntity<byte[]> export(@RequestParam(required=false) String status,@RequestParam(required=false) String severity,@RequestParam(required=false) String component,
        @RequestParam(required=false) UUID assignee,@RequestParam(required=false) LocalDate fromDate,@RequestParam(required=false) LocalDate toDate) {
        byte[] file=incidentExport.export(status,severity,component,assignee,fromDate,toDate);
        if(file==null) return ResponseEntity.noContent().build();
        return ResponseEntity.ok().contentType(org.springframework.http.MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
            .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION,"attachment; filename=\"su-co-ky-thuat.xlsx\"").body(file);
    }
    @PostMapping("/incidents") public ResponseEntity<?> create(@jakarta.validation.Valid @RequestBody IncidentCreateRequest r) {
        var result=incidents.create(r); return ResponseEntity.created(URI.create("/api/admin/operations/incidents/"+result.get("incidentId"))).body(ApiResponse.ok(result));
    }
    // Quản lý báo sự cố cho Admin và theo dõi sự cố mình đã báo (chỉ xem).
    @GetMapping("/reported-incidents") public ApiResponse<?> reported(@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="20") int size) { return ApiResponse.ok(incidents.reportedByMe(page,size)); }
    @GetMapping("/reported-incidents/components") public ApiResponse<?> reportComponents() { actors.requireManager(); return ApiResponse.ok(catalog.options()); }
    @GetMapping("/reported-incidents/csrf") public ApiResponse<?> reportCsrf(org.springframework.security.web.csrf.CsrfToken token) {
        actors.requireManager(); return ApiResponse.ok(Map.of("token",token.getToken(),"headerName",token.getHeaderName()));
    }
    @PostMapping("/reported-incidents") public ResponseEntity<?> report(@jakarta.validation.Valid @RequestBody IncidentCreateRequest r) {
        return ResponseEntity.status(201).body(ApiResponse.ok(incidents.report(r)));
    }
    @GetMapping("/incidents/{id}") public ApiResponse<?> detail(@PathVariable UUID id) { return ApiResponse.ok(incidents.detail(id)); }
    @GetMapping("/incidents/{id}/events") public ApiResponse<?> history(@PathVariable UUID id,@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="20") int size) { return ApiResponse.ok(incidents.history(id,page,size)); }
    @PostMapping("/incidents/{id}/claim") public ApiResponse<?> claim(@PathVariable UUID id,@RequestBody IncidentCommandRequest r) { return ApiResponse.ok(incidents.command(id,"CLAIM",r)); }
    @PostMapping("/incidents/{id}/release") public ApiResponse<?> release(@PathVariable UUID id,@RequestBody IncidentCommandRequest r) { return ApiResponse.ok(incidents.command(id,"RELEASE",r)); }
    @PostMapping("/incidents/{id}/progress") public ApiResponse<?> progress(@PathVariable UUID id,@RequestBody IncidentCommandRequest r) { return ApiResponse.ok(incidents.command(id,"PROGRESS",r)); }
    @PostMapping("/incidents/{id}/resolve") public ApiResponse<?> resolve(@PathVariable UUID id,@RequestBody IncidentCommandRequest r) { return ApiResponse.ok(incidents.command(id,"RESOLVE",r)); }
    @PostMapping("/incidents/{id}/resume") public ApiResponse<?> resume(@PathVariable UUID id,@RequestBody IncidentCommandRequest r) { return ApiResponse.ok(incidents.command(id,"RESUME",r)); }
    @PostMapping("/incidents/{id}/close") public ApiResponse<?> close(@PathVariable UUID id,@RequestBody IncidentCommandRequest r) { return ApiResponse.ok(incidents.command(id,"CLOSE",r)); }
}
