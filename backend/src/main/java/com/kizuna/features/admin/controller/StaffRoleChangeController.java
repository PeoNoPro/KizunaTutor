package com.kizuna.features.admin.controller;
import com.kizuna.core.dto.ApiResponse;
import com.kizuna.features.admin.dto.StaffRoleChangeRequests.*;
import com.kizuna.features.admin.service.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.*;

@RestController @RequestMapping("/api/admin/operations/role-changes") @RequiredArgsConstructor
public class StaffRoleChangeController {
    private final StaffRoleChangeService changes;
    private final OperationsActorResolver actors;
    @GetMapping("/csrf") public ApiResponse<?> csrf(org.springframework.security.web.csrf.CsrfToken token){actors.requireManager();return ApiResponse.ok(Map.of("token",token.getToken(),"headerName",token.getHeaderName()));}
    @GetMapping("/staff") public ApiResponse<?> staff(@RequestParam(defaultValue="")String q,@RequestParam(defaultValue="0")int page,@RequestParam(defaultValue="20")int size){return ApiResponse.ok(changes.eligible(q,page,size));}
    @PutMapping("/staff/{id}/role") public ApiResponse<?> move(@PathVariable UUID id,@Valid @RequestBody Move request){return ApiResponse.ok(changes.move(id,request));}
    @GetMapping("/promotions") public ApiResponse<?> promotions(@RequestParam(required=false)String status,@RequestParam(defaultValue="0")int page,@RequestParam(defaultValue="20")int size){return ApiResponse.ok(changes.list(status,page,size));}
    @PostMapping("/promotions") public ResponseEntity<?> propose(@Valid @RequestBody Propose request){var data=changes.propose(request);return ResponseEntity.created(URI.create("/api/admin/operations/role-changes/promotions/"+data.get("requestId"))).body(ApiResponse.ok(data));}
    @PostMapping("/promotions/{id}/cancel") public ApiResponse<?> cancel(@PathVariable UUID id){return ApiResponse.ok(changes.cancel(id));}
    @PostMapping("/promotions/{id}/approve") public ApiResponse<?> approve(@PathVariable UUID id,@Valid @RequestBody Decide request){return ApiResponse.ok(changes.approve(id,request));}
    @PostMapping("/promotions/{id}/reject") public ApiResponse<?> reject(@PathVariable UUID id,@Valid @RequestBody Decide request){return ApiResponse.ok(changes.reject(id,request));}
}
