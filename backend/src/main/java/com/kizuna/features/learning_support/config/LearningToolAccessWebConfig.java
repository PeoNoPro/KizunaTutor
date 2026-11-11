package com.kizuna.features.learning_support.config;

import com.kizuna.features.learning_support.service.LearningToolAccessService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Đăng ký chặn quyền cho các công cụ học yêu cầu học viên đã vào lớp:
 * bộ thẻ (flashcard), ngữ pháp, từ vựng, chữa câu AI, minigame chữ Hán.
 * Từ điển Kanji (/api/learner/kanji, /api/kanji) luôn mở nên không nằm trong danh sách.
 * Mỗi đường dẫn phụ (alias) của controller phải có mặt ở đây, nếu không sẽ bị lách qua.
 */
@Configuration
@RequiredArgsConstructor
public class LearningToolAccessWebConfig implements WebMvcConfigurer {

    static final String[] LOCKED_TOOL_PATHS = {
            // FlashcardDeckController
            "/api/learner/decks", "/api/learner/decks/**",
            // GrammarController
            "/api/learner/grammar", "/api/learner/grammar/**",
            // AiWritingController
            "/api/learner/ai-writing/**",
            // KanjiGameController (kèm alias /api/minigame)
            "/api/learner/minigame/**", "/api/minigame/**",
            // VocabularyController (kèm alias /api/learner, /api/learner/topics, /api/learner/search)
            "/api/learner/vocabularies", "/api/learner/vocabularies/**",
            "/api/learner", "/api/learner/topics", "/api/learner/search"
    };

    private final ObjectProvider<LearningToolAccessService> accessServiceProvider;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new LearningToolAccessInterceptor(accessServiceProvider))
                .addPathPatterns(LOCKED_TOOL_PATHS);
    }
}
