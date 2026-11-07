package com.kizuna.features.ticket.controller;

import com.kizuna.core.dto.ApiResponse;
import com.kizuna.features.ticket.service.SupportShiftService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;
import java.util.UUID;

/** Lịch ca nhân viên hỗ trợ: Quản lý xếp/gỡ ca, nhân viên hỗ trợ xem. */
@RestController @RequestMapping("/api/v1/tickets/shifts") @RequiredArgsConstructor
public class SupportShiftController {
    private final SupportShiftService shifts;
    public record Assign(UUID staffId, LocalDate date, String shiftCode) { }

    @GetMapping public ApiResponse<?> week(@RequestParam(required = false) LocalDate weekStart) { return ApiResponse.ok(shifts.week(weekStart)); }
    @PostMapping public ApiResponse<?> assign(@RequestBody Assign request) { return ApiResponse.ok(shifts.assign(request.staffId(), request.date(), request.shiftCode())); }
    @DeleteMapping("/{shiftId}") public ApiResponse<?> unassign(@PathVariable UUID shiftId, @RequestParam(required = false) LocalDate weekStart) { return ApiResponse.ok(shifts.unassign(shiftId, weekStart)); }
}
