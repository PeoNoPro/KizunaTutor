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
    private final PersonalProfileService profiles;
    @GetMapping("/management-report") public ApiResponse<?> report(@RequestParam(required=false)LocalDate startDate,@RequestParam(required=false)LocalDate endDate){return ApiResponse.ok(reports.read(startDate,endDate));}
    @GetMapping("/profile") public ApiResponse<?> profile(){return ApiResponse.ok(actors.personalProfile());}
    /** CSRF chung (/csrf) chỉ dành cho Admin; Manager, CS, Cán bộ học vụ lấy mã ở đây để sửa hồ sơ và đổi mật khẩu của chính mình. */
    @GetMapping("/profile/csrf") public ApiResponse<?> profileCsrf(org.springframework.security.web.csrf.CsrfToken token){actors.requireStaff();return ApiResponse.ok(java.util.Map.of("token",token.getToken(),"headerName",token.getHeaderName()));}
    @PutMapping("/profile") public ApiResponse<?> updateProfile(@jakarta.validation.Valid @RequestBody PersonalProfileService.ProfileUpdate request){return ApiResponse.ok(profiles.update(request));}
    @PutMapping("/profile/password") public ApiResponse<?> changePassword(@jakarta.validation.Valid @RequestBody PersonalProfileService.PasswordChange request){return ApiResponse.ok(profiles.changePassword(request));}
}
