package com.kizuna.features.admin.controller;
import com.kizuna.core.dto.ApiResponse;
import com.kizuna.features.admin.service.PolicyDocumentReadService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/policies") @RequiredArgsConstructor
public class PolicyDocumentController {
    private final PolicyDocumentReadService documents;
    @GetMapping public ApiResponse<?> list() { return ApiResponse.ok(documents.list()); }
    @GetMapping("/{type}") public ApiResponse<?> detail(@PathVariable String type) { return ApiResponse.ok(documents.detail(type)); }
}
