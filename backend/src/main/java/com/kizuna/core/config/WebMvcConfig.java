package com.kizuna.core.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        Path frontendPath = Paths.get("..", "frontend").toAbsolutePath().normalize();
        String frontendUri = "file:" + frontendPath.toString().replace("\\", "/") + "/";

        registry.addResourceHandler("/**")
                .addResourceLocations(
                        frontendUri,
                        "file:c:/Users/Vuh26/Desktop/dev/KizunaTutor/frontend/",
                        "classpath:/static/",
                        "classpath:/public/"
                );
    }

    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        registry.addViewController("/").setViewName("forward:/pages/home/index.html");
        registry.addViewController("/home").setViewName("forward:/pages/home/index.html");
        registry.addViewController("/kanji-game").setViewName("forward:/pages/kanji-game/index.html");
        registry.addViewController("/game").setViewName("forward:/pages/kanji-game/index.html");
        registry.addViewController("/rain").setViewName("forward:/pages/kanji-game/index.html");
        registry.addViewController("/kanji-dictionary").setViewName("forward:/pages/kanji-dictionary/index.html");
        registry.addViewController("/dictionary").setViewName("forward:/pages/kanji-dictionary/index.html");
        registry.addViewController("/kanji").setViewName("forward:/pages/kanji-dictionary/index.html");
        registry.addViewController("/flashcards").setViewName("forward:/pages/flashcards/index.html");
        registry.addViewController("/decks").setViewName("forward:/pages/flashcards/index.html");
        registry.addViewController("/shodo").setViewName("forward:/pages/shodo-practice/index.html");
        registry.addViewController("/shodo-practice").setViewName("forward:/pages/shodo-practice/index.html");
        registry.addViewController("/grammar").setViewName("forward:/pages/grammar/index.html");
        registry.addViewController("/handwriting").setViewName("forward:/pages/handwriting-search/index.html");
        registry.addViewController("/login").setViewName("forward:/pages/login/index.html");
        registry.addViewController("/register").setViewName("forward:/pages/register/index.html");
        registry.addViewController("/tutors").setViewName("forward:/pages/tutor-discovery/index.html");
        registry.addViewController("/kana").setViewName("forward:/pages/kana-quiz/index.html");
        registry.addViewController("/learner/dashboard").setViewName("forward:/pages/learner-dashboard/index.html");
        registry.addViewController("/learner/dashboard/").setViewName("forward:/pages/learner-dashboard/index.html");
        registry.addViewController("/learner").setViewName("forward:/pages/learner-dashboard/index.html");
        registry.addViewController("/learner/").setViewName("forward:/pages/learner-dashboard/index.html");
        registry.addViewController("/dashboard").setViewName("forward:/pages/learner-dashboard/index.html");
        registry.addViewController("/dashboard/").setViewName("forward:/pages/learner-dashboard/index.html");
        // Khu vực gia sư: tổng quan và xác minh hồ sơ (FE-03)
        registry.addViewController("/tutor/dashboard").setViewName("forward:/pages/tutor-dashboard/index.html");
        registry.addViewController("/tutor/dashboard/").setViewName("forward:/pages/tutor-dashboard/index.html");
        registry.addViewController("/tutor").setViewName("forward:/pages/tutor-dashboard/index.html");
        registry.addViewController("/tutor/verification").setViewName("forward:/pages/tutor-verification/index.html");
        registry.addViewController("/tutor/verification/").setViewName("forward:/pages/tutor-verification/index.html");
        // Cổng học vụ: tổng quan & hàng đợi, chi tiết thẩm định hồ sơ (FE-03)
        registry.addViewController("/academic").setViewName("forward:/pages/academic-verifications/index.html");
        registry.addViewController("/academic/dashboard").setViewName("forward:/pages/academic-verifications/index.html");
        registry.addViewController("/academic/dashboard/").setViewName("forward:/pages/academic-verifications/index.html");
        registry.addViewController("/academic/verifications").setViewName("forward:/pages/academic-verifications/index.html");
        registry.addViewController("/academic/verifications/").setViewName("forward:/pages/academic-verifications/index.html");
        registry.addViewController("/academic/verifications/detail").setViewName("forward:/pages/academic-verification-detail/index.html");
        // FE-09: gói hội viên và thanh toán
        registry.addViewController("/tutor/membership").setViewName("forward:/pages/tutor-membership/index.html");
        registry.addViewController("/tutor/membership/").setViewName("forward:/pages/tutor-membership/index.html");
        registry.addViewController("/tutor/membership/result").setViewName("forward:/pages/tutor-membership-result/index.html");
        registry.addViewController("/manager/membership-plans").setViewName("forward:/pages/manager-membership-plans/index.html");
        registry.addViewController("/manager/tutor-memberships").setViewName("forward:/pages/manager-tutor-memberships/index.html");
        registry.addViewController("/admin/payments").setViewName("forward:/pages/admin-payments/index.html");
        // Trang trả về của VNPay mở ở cổng backend: các liên kết sidebar phải mở được ở đây (giống frontend/server.js)
        forwardWithSlash(registry, "/tutor/classrooms", "tutor-classrooms");
        forwardWithSlash(registry, "/tutor/post", "tutor-post");
        forwardWithSlash(registry, "/academic/posts", "academic-posts");
        forwardWithSlash(registry, "/academic/history", "academic-review-history");
        forwardWithSlash(registry, "/academic/profile", "academic-profile");
        forwardWithSlash(registry, "/academic/content-import", "academic-content-import");
        forwardWithSlash(registry, "/my-courses", "my-courses");
        forwardWithSlash(registry, "/learner/my-courses", "my-courses");
    }

    private static void forwardWithSlash(ViewControllerRegistry registry, String path, String page) {
        String view = "forward:/pages/" + page + "/index.html";
        registry.addViewController(path).setViewName(view);
        registry.addViewController(path + "/").setViewName(view);
    }
}
