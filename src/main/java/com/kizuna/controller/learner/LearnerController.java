package com.kizuna.controller.learner;

import com.kizuna.entity.LearningSpace;
import com.kizuna.entity.RoleEnum;
import com.kizuna.repository.KanjiDictionaryRepository;
import com.kizuna.repository.LearningSpaceRepository;
import com.kizuna.util.SecurityContextUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

/**
 * Controller phục vụ giao diện dành riêng cho Học Viên (Learner).
 * Toàn bộ màn hình nằm trong Learner Master Shell Layout (Emerald Theme).
 * Không sử dụng dữ liệu ảo hardcoded - phản ánh đúng dữ liệu thực tế từ hệ thống.
 */
@Controller
@RequestMapping("/learner")
@RequiredArgsConstructor
public class LearnerController {

    private final SecurityContextUtil securityUtil;
    private final KanjiDictionaryRepository kanjiRepository;
    private final LearningSpaceRepository learningSpaceRepository;

    private void populateLearnerShell(Model model, String activeMenu, String pageTitle, String breadcrumbSection) {
        String userName = securityUtil.getCurrentUserName(RoleEnum.ROLE_LEARNER);
        String userEmail = securityUtil.getCurrentUserEmail(RoleEnum.ROLE_LEARNER);
        String avatarText = securityUtil.getCurrentUserAvatarText(userName);

        model.addAttribute("pageTitle", pageTitle + " - Không Gian Học Viên KizunaTutor");
        model.addAttribute("role", RoleEnum.ROLE_LEARNER);
        model.addAttribute("userName", userName);
        model.addAttribute("userEmail", userEmail);
        model.addAttribute("avatarText", avatarText);
        model.addAttribute("activeMenu", activeMenu);
        model.addAttribute("breadcrumbSection", breadcrumbSection);
    }

    /**
     * Màn hình 1: Dashboard Tổng Quan Học Tập
     */
    @GetMapping({"", "/dashboard"})
    public String learnerDashboard(Model model) {
        populateLearnerShell(model, "dashboard", "Tổng Quan Học Tập", "Tổng Quan");

        String userEmail = securityUtil.getCurrentUserEmail(RoleEnum.ROLE_LEARNER);
        List<LearningSpace> activeSpaces = learningSpaceRepository.findActiveSpacesByLearnerEmail(userEmail);
        String assignedTutorName = null;
        if (!activeSpaces.isEmpty()) {
            assignedTutorName = activeSpaces.get(0).getTutor().getUser().getFullName();
        }

        String nextLesson = (assignedTutorName != null) ? "Chờ xếp lịch kèm" : "Chưa có lịch học";

        model.addAttribute("learnedKanjiCount", 0L);
        model.addAttribute("accuracyRate", "Chưa có dữ liệu");
        model.addAttribute("nextLessonTime", nextLesson);
        model.addAttribute("assignedTutorName", assignedTutorName);
        model.addAttribute("studyStreak", 0L);

        return "learner/dashboard";
    }

    /**
     * Màn hình 2: Tra Cứu Từ Điển Kanji Cá Nhân Hóa
     * Thẩm quyền Learner: Tra cứu từ điển, xem nghĩa Hán Việt - On - Kun, thêm vào bộ thẻ cá nhân, bấm luyện viết ngay.
     */
    @GetMapping("/dictionary")
    public String learnerDictionary(
            @RequestParam(required = false, defaultValue = "") String keyword,
            Model model) {
        populateLearnerShell(model, "dictionary", "Từ Điển Kanji Học Tập", "Tra Cứu Kanji");

        model.addAttribute("keyword", keyword);
        model.addAttribute("totalDictionaryCount", kanjiRepository.count());

        return "learner/dictionary";
    }

    /**
     * Màn hình 3: Phòng Luyện Viết Shodo Canvas AI
     */
    @GetMapping("/practice-canvas")
    public String learnerPracticeCanvas(
            @RequestParam(required = false, defaultValue = "愛") String charQuery,
            Model model) {
        populateLearnerShell(model, "practice-canvas", "Luyện Viết Nét Chữ AI", "Luyện Viết Shodo");
        model.addAttribute("currentChar", charQuery);
        return "learner/practice-canvas";
    }

    /**
     * Màn hình 4: Nhật Ký Tiến Độ Cá Nhân
     */
    @GetMapping("/my-progress")
    public String learnerMyProgress(Model model) {
        populateLearnerShell(model, "my-progress", "Nhật Ký Tiến Độ Cá Nhân", "Tiến Độ Của Tôi");
        return "learner/my-progress";
    }
}
