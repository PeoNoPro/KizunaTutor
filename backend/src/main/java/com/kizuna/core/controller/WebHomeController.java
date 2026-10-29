package com.kizuna.core.controller;

import com.kizuna.features.auth.dto.LearnerAccessState;
import com.kizuna.features.auth.dto.LoginResponse;
import com.kizuna.features.auth.dto.RegisterRequest;
import com.kizuna.features.learning_support.entity.KanjiDictionary;
import com.kizuna.features.learning_support.repository.KanjiDictionaryRepository;
import com.kizuna.features.auth.service.IAuthService;
import com.kizuna.features.classroom.service.ILearningSpaceService;
import com.kizuna.features.learning_support.service.kanji.HandwritingSearchService;
import com.kizuna.features.learning_support.util.SvgUtils;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Controller
@RequiredArgsConstructor
public class WebHomeController {

    private final KanjiDictionaryRepository kanjiRepository;
    private final HandwritingSearchService handwritingSearchService;
    private final ILearningSpaceService learningSpaceService;
    private final IAuthService authService;

    /**
     * Tự động bổ sung thông tin rào cản phân quyền cho toàn bộ các trang công khai & guest dashboard
     */
    @ModelAttribute
    public void addAccessControlAttributes(Model model) {
        LearnerAccessState accessState = learningSpaceService.checkCurrentUserAccessState();
        boolean isUnlocked = (accessState == LearnerAccessState.ACTIVE_OFFICIAL_LEARNER);

        model.addAttribute("accessState", accessState.name());
        model.addAttribute("isUnlocked", isUnlocked);

        if (accessState == LearnerAccessState.UNAUTHENTICATED) {
            model.addAttribute("lockedTitle", "Đặc Quyền Dành Cho Học Viên Chính Thức");
            model.addAttribute("lockedMessage", "Bạn cần đăng nhập và được Gia sư mở khóa để sử dụng chức năng này");
        } else if (accessState == LearnerAccessState.UNASSIGNED_LEARNER) {
            model.addAttribute("lockedTitle", "Chưa Được Gia Sư Thêm Vào Lớp Học");
            model.addAttribute("lockedMessage", "Chức năng này đang bị khóa. Bạn cần được Gia sư thêm vào Không gian học tập (Learning Space) để bắt đầu luyện tập!");
        } else {
            model.addAttribute("lockedTitle", "Không Gian Học Tập Đã Mở Khóa");
            model.addAttribute("lockedMessage", "Bạn đang tham gia học tập cùng Gia sư Kizuna chính thức.");
        }
    }

    @GetMapping({"/", "/index"})
    public String index(Model model) {
        model.addAttribute("pageTitle", "KizunaTutor - Nền Tảng Kết Nối Gia Sư Tiếng Nhật & Luyện Chữ Kanji");
        model.addAttribute("totalKanji", kanjiRepository.count());
        return "index";
    }

    @GetMapping("/kanji")
    public String kanjiPractice(
            @RequestParam(required = false, defaultValue = "愛") String charQuery,
            @RequestParam(required = false, defaultValue = "folders") String tab,
            @RequestParam(required = false) String folderId,
            Model model) {
        model.addAttribute("pageTitle", "Không Gian Học Tập Kanji - KizunaTutor");
        model.addAttribute("currentChar", charQuery);
        model.addAttribute("activeTab", tab);
        model.addAttribute("folderId", folderId);

        kanjiRepository.findByKanjiCharacter(charQuery).ifPresentOrElse(kanji -> {
            if (kanji.getSvgData() != null) {
                kanji.setSvgData(SvgUtils.cleanKanjiVgSvg(kanji.getSvgData()));
            }
            handwritingSearchService.enrichKanjiMetadata(kanji);
            model.addAttribute("kanjiData", kanji);
        }, () -> {
            handwritingSearchService.getOfflineKanji(charQuery).ifPresent(off -> {
                KanjiDictionary fallback = KanjiDictionary.builder()
                        .kanjiCharacter(charQuery)
                        .hanViet(off.han_viet())
                        .onyomi(off.onyomi())
                        .kunyomi(off.kunyomi())
                        .meaning(off.meaning())
                        .strokeCount((short) 1)
                        .build();
                model.addAttribute("kanjiData", fallback);
            });
        });
        return "kanji/practice";
    }

    @GetMapping("/tutors")
    public String tutorsSearch(Model model) {
        model.addAttribute("pageTitle", "Tìm Kiếm Gia Sư Tiếng Nhật - KizunaTutor");
        return "tutors/search";
    }

    /**
     * Đấu Trường Gõ Kana Siêu Tốc (Public Route mở rộng cho Guest và Learner)
     */
    @GetMapping("/kana-quiz")
    public String publicKanaQuiz(Model model) {
        model.addAttribute("pageTitle", "Đấu Trường Gõ Kana Siêu Tốc - KizunaTutor");
        model.addAttribute("isGuest", true);
        return "learner/kana-quiz";
    }

    @GetMapping("/login")
    public String login(Model model) {
        model.addAttribute("pageTitle", "Đăng Nhập - KizunaTutor");
        return "auth/login";
    }

    @GetMapping("/register")
    public String register(Model model) {
        model.addAttribute("pageTitle", "Đăng Ký Tài Khoản - KizunaTutor");
        return "auth/register";
    }

    @PostMapping("/register")
    public String handleRegisterForm(@RequestParam String fullName,
                                     @RequestParam String email,
                                     @RequestParam String password,
                                     @RequestParam(required = false, defaultValue = "N5") String targetJlpt,
                                     HttpServletResponse response,
                                     RedirectAttributes redirectAttributes) {
        try {
            RegisterRequest req = RegisterRequest.builder()
                    .fullName(fullName)
                    .email(email)
                    .password(password)
                    .targetJlpt(targetJlpt)
                    .role("ROLE_LEARNER")
                    .build();
            LoginResponse loginResponse = authService.register(req, response);
            return "redirect:" + loginResponse.getRedirectUrl();
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/register?error=" + URLEncoder.encode(e.getMessage(), StandardCharsets.UTF_8);
        }
    }

    @GetMapping("/oauth2/redirect")
    public String oauth2Redirect(Model model) {
        model.addAttribute("pageTitle", "Đang Chuyển Hướng Xác Thực - KizunaTutor");
        return "auth/oauth2-redirect";
    }
}
