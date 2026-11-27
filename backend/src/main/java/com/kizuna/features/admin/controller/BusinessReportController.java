package com.kizuna.features.admin.controller;
import com.kizuna.core.dto.ApiResponse;
import com.kizuna.features.admin.service.BusinessReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;

@RestController @RequestMapping("/api/admin/operations/reports/business") @RequiredArgsConstructor
public class BusinessReportController {
    private final BusinessReportService reports;
    private static ResponseEntity<byte[]> file(byte[] content,String name) {
        if(content==null)return ResponseEntity.noContent().build();
        return ResponseEntity.ok().contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
            .header(HttpHeaders.CONTENT_DISPOSITION,"attachment; filename=\""+name+"\"").body(content);
    }
    @GetMapping("/revenue") public ApiResponse<?> revenue(@RequestParam(required=false)LocalDate startDate,@RequestParam(required=false)LocalDate endDate){return ApiResponse.ok(reports.revenue(startDate,endDate));}
    @GetMapping("/revenue/rows") public ApiResponse<?> revenueRows(@RequestParam(required=false)LocalDate startDate,@RequestParam(required=false)LocalDate endDate){return ApiResponse.ok(reports.revenueRows(startDate,endDate));}
    @GetMapping("/revenue/export") public ResponseEntity<byte[]> revenueExport(@RequestParam(required=false)LocalDate startDate,@RequestParam(required=false)LocalDate endDate){return file(reports.revenueExport(startDate,endDate),"doanh-thu.xlsx");}
    @GetMapping("/tutors/rows") public ApiResponse<?> tutorRows(){return ApiResponse.ok(reports.tutorRows());}
    @GetMapping("/tutors/export") public ResponseEntity<byte[]> tutorExport(){return file(reports.tutorExport(),"gia-su-jlpt.xlsx");}
}
