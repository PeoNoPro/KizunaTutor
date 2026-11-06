package com.kizuna.features.admin.controller;
import com.kizuna.core.dto.ApiResponse;
import com.kizuna.features.admin.service.*;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;
@RestController @RequestMapping("/api/admin/operations") @RequiredArgsConstructor
public class ManagementReportController {
    private final ManagementReportService reports;
    private final OperationsActorResolver actors;
    @GetMapping("/management-report") public ApiResponse<?> report(@RequestParam(required=false)LocalDate startDate,@RequestParam(required=false)LocalDate endDate){return ApiResponse.ok(reports.read(startDate,endDate));}
    @GetMapping("/profile") public ApiResponse<?> profile(){return ApiResponse.ok(actors.personalProfile());}
}
