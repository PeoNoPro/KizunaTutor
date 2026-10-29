package com.kizuna.core.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/api/test")
public class TestController {

    @GetMapping("/health")
    public Map<String, Object> healthCheck() {
        return Map.of(
            "status", "UP",
            "service", "KizunaTutor Backend",
            "timestamp", LocalDateTime.now().toString(),
            "message", "Cấu hình dự án KizunaTutor đã hoàn tất và sẵn sàng phát triển!"
        );
    }
}
