package com.kizuna.features.learning_support.controller;

import com.kizuna.core.dto.ApiResponse;
import com.kizuna.core.entity.User;
import com.kizuna.features.learning_support.dto.LearningToolAccessResponse;
import com.kizuna.features.learning_support.service.LearningToolAccessService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Frontend hỏi trạng thái mở khóa công cụ học để khóa menu và hiện màn hình hướng dẫn. */
@RestController
@RequestMapping("/api/learner/learning-tools")
@RequiredArgsConstructor
public class LearningToolAccessController {

    private final LearningToolAccessService learningToolAccessService;

    @GetMapping("/access")
    public ApiResponse<LearningToolAccessResponse> getAccess(Authentication authentication) {
        User user = authentication != null && authentication.getPrincipal() instanceof User u ? u : null;
        return ApiResponse.ok(learningToolAccessService.getAccess(user));
    }
}
