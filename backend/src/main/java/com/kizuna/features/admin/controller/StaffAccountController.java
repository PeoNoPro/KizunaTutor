package com.kizuna.features.admin.controller;
import com.kizuna.core.dto.ApiResponse;
import com.kizuna.features.admin.dto.StaffAccountRequests.*;
import com.kizuna.features.admin.service.StaffAccountService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.UUID;

@RestController @RequestMapping("/api/admin/operations") @RequiredArgsConstructor
public class StaffAccountController {
    private final StaffAccountService staff;
    @GetMapping("/staff") public ApiResponse<?> list(@RequestParam(defaultValue="")String q,@RequestParam(defaultValue="0")int page,@RequestParam(defaultValue="20")int size){return ApiResponse.ok(staff.list(q,page,size));}
    @GetMapping("/roles") public ApiResponse<?> roles(){return ApiResponse.ok(staff.roles());}
    @GetMapping("/staff/history") public ApiResponse<?> history(@RequestParam(defaultValue="0")int page,@RequestParam(defaultValue="20")int size){return ApiResponse.ok(staff.history(page,size));}
    @PostMapping("/staff") public ResponseEntity<?> create(@Valid @RequestBody Create request){var data=staff.create(request);return ResponseEntity.created(URI.create("/api/admin/operations/staff?created="+data.get("userId"))).body(ApiResponse.ok(data));}
    @PutMapping("/staff/{id}") public ApiResponse<?> update(@PathVariable UUID id,@Valid @RequestBody Update request){return ApiResponse.ok(staff.update(id,request));}
    @PostMapping("/staff/{id}/resend-credentials") public ApiResponse<?> resend(@PathVariable UUID id,@Valid @RequestBody Resend request){return ApiResponse.ok(staff.resendCredentials(id,request));}
    @PostMapping("/staff/{id}/delete") public ApiResponse<?> delete(@PathVariable UUID id,@Valid @RequestBody Delete request){return ApiResponse.ok(staff.delete(id,request));}
    @PutMapping("/staff/{id}/status") public ApiResponse<?> status(@PathVariable UUID id,@Valid @RequestBody Command request){return ApiResponse.ok(staff.status(id,request));}
}
