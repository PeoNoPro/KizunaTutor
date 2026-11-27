package com.kizuna.features.admin.controller;
import com.kizuna.core.dto.ApiResponse;
import com.kizuna.features.admin.dto.StaffImportRequests.Confirm;
import com.kizuna.features.admin.service.StaffImportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import jakarta.validation.Valid;

@RestController @RequestMapping("/api/admin/operations/staff/import") @RequiredArgsConstructor
public class StaffImportController {
    private final StaffImportService imports;
    @GetMapping("/template") public ResponseEntity<byte[]> template(){
        return ResponseEntity.ok().contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
            .header(HttpHeaders.CONTENT_DISPOSITION,"attachment; filename=\"mau-nhap-nhan-vien.xlsx\"").body(imports.template());
    }
    @PostMapping("/preview") public ApiResponse<?> preview(@RequestParam(value="file",required=false) MultipartFile file){return ApiResponse.ok(imports.preview(file));}
    @PostMapping("/confirm") public ResponseEntity<?> confirm(@Valid @RequestBody Confirm request){return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(imports.confirm(request)));}
}
