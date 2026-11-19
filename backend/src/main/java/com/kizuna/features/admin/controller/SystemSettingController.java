package com.kizuna.features.admin.controller;
import com.kizuna.core.dto.ApiResponse;
import com.kizuna.features.admin.service.CloudStorageCopyService;
import com.kizuna.features.admin.service.CloudStorageSettingService;
import com.kizuna.features.admin.service.SystemSettingService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

@RestController @RequestMapping("/api/admin/operations/settings") @RequiredArgsConstructor
public class SystemSettingController {
    private final SystemSettingService settings;
    private final CloudStorageSettingService storage;
    private final CloudStorageCopyService storageCopy;
    @GetMapping("/cloud-storage") public ApiResponse<?> cloudStorage(){return ApiResponse.ok(storage.view());}
    @PutMapping("/cloud-storage") public ApiResponse<?> updateCloudStorage(@Valid @RequestBody CloudStorageSettingService.StorageUpdate request){return ApiResponse.ok(storage.update(request));}
    @PostMapping("/cloud-storage/test") public ApiResponse<?> testCloudStorage(@Valid @RequestBody CloudStorageSettingService.StorageTest request){return ApiResponse.ok(storage.test(request));}
    @PostMapping("/cloud-storage/reset") public ApiResponse<?> resetCloudStorage(@Valid @RequestBody CloudStorageSettingService.StorageReset request){return ApiResponse.ok(storage.resetToServer(request));}
    @GetMapping("/cloud-storage/copy") public ApiResponse<?> copyStatus(){return ApiResponse.ok(storageCopy.status());}
    @PostMapping("/cloud-storage/copy") public ApiResponse<?> startCopy(){return ApiResponse.ok(storageCopy.start());}
    @GetMapping("/attachment-limit") public ApiResponse<?> attachmentLimit(){return ApiResponse.ok(settings.attachmentLimit());}
    @PutMapping("/attachment-limit") public ApiResponse<?> update(@Valid @RequestBody SystemSettingService.AttachmentLimitUpdate request){return ApiResponse.ok(settings.updateAttachmentLimit(request));}
    @GetMapping("/mail-sender") public ApiResponse<?> mailSender(){return ApiResponse.ok(settings.mailSender());}
    @PutMapping("/mail-sender") public ApiResponse<?> updateMailSender(@Valid @RequestBody SystemSettingService.MailSenderUpdate request){return ApiResponse.ok(settings.updateMailSender(request));}
    @GetMapping("/ai") public ApiResponse<?> ai(){return ApiResponse.ok(settings.ai());}
    @PutMapping("/ai") public ApiResponse<?> updateAi(@Valid @RequestBody SystemSettingService.AiUpdate request){return ApiResponse.ok(settings.updateAi(request));}
    @PostMapping("/ai/test") public ApiResponse<?> testAi(@Valid @RequestBody SystemSettingService.AiTest request){return ApiResponse.ok(settings.testAi(request));}
    @GetMapping("/history") public ApiResponse<?> history(@RequestParam(defaultValue="0")int page,@RequestParam(defaultValue="20")int size){return ApiResponse.ok(settings.history(page,size));}
}
