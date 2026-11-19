package com.kizuna.features.practice.controller;

import com.kizuna.features.payment.service.CurrentUserAccessor;
import com.kizuna.features.practice.service.PracticeLearnerService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

/** FE-13: Luyện đề của học viên. Chỉ ROLE_LEARNER (SecurityConfig: /api/learner/practice/**); bài làm luôn kiểm đúng chủ. */
@RestController
@RequestMapping("/api/learner/practice")
@RequiredArgsConstructor
public class PracticeLearnerController {
    private final PracticeLearnerService service;
    private final CurrentUserAccessor currentUser;

    private UUID me() { return currentUser.requireUser().getUserId(); }

    @GetMapping("/options")
    public Map<String, Object> options() { return service.options(); }

    @PostMapping("/attempts")
    public Map<String, Object> start(@RequestBody Map<String, Object> body) { return service.start(me(), body); }

    @GetMapping("/attempts")
    public Map<String, Object> history(@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "10") int size) {
        return service.history(me(), page, size);
    }

    @GetMapping("/attempts/{id}")
    public Map<String, Object> attempt(@PathVariable UUID id) { return service.attempt(me(), id); }

    @PutMapping("/attempts/{id}/answers/{questionId}")
    public Map<String, Object> answer(@PathVariable UUID id, @PathVariable UUID questionId, @RequestBody Map<String, Object> body) {
        return service.answer(me(), id, questionId, body);
    }

    @PostMapping("/attempts/{id}/submit")
    public Map<String, Object> submit(@PathVariable UUID id) { return service.submit(me(), id); }
}
