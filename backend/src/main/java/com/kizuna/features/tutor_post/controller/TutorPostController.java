package com.kizuna.features.tutor_post.controller;

import com.kizuna.features.payment.service.CurrentUserAccessor;
import com.kizuna.features.tutor_post.dto.TutorPostDtos.MyPostView;
import com.kizuna.features.tutor_post.dto.TutorPostDtos.PostView;
import com.kizuna.features.tutor_post.dto.TutorPostDtos.SubmitPostRequest;
import com.kizuna.features.tutor_post.service.TutorPostService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;

/** Gia sư xem và gửi bài đăng của mình. Chỉ ROLE_TUTOR (SecurityConfig). */
@RestController
@CrossOrigin(origins = {"http://localhost:3000", "http://127.0.0.1:3000"})
@RequestMapping("/api/tutor/post")
@RequiredArgsConstructor
public class TutorPostController {

    private final TutorPostService service;
    private final CurrentUserAccessor currentUser;

    @GetMapping
    public ResponseEntity<MyPostView> getMine() {
        return ResponseEntity.ok(service.getMine(currentUser.requireUser()));
    }

    @PutMapping
    public ResponseEntity<Map<String, Object>> submit(@RequestBody(required = false) SubmitPostRequest request) {
        PostView post = service.submit(currentUser.requireUser(), request);
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("message", TutorPostService.submittedMessage(post));
        body.put("post", post);
        return ResponseEntity.ok(body);
    }
}
