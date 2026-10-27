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
    private final com.kizuna.repository.TutorRepository tutorRepository;
    private final com.kizuna.repository.LearnerRepository learnerRepository;

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
        java.util.Optional<com.kizuna.entity.KanjiDictionary> kanjiOpt = kanjiRepository.findByKanjiCharacter(charQuery);
        model.addAttribute("kanjiInfo", kanjiOpt.orElse(null));
        return "learner/practice-canvas";
    }

    /**
     * Màn hình riêng: Tra Cứu Chữ Hán Bằng Nét Vẽ Viết Tay (Handwriting Recognition AI OHR)
     */
    @GetMapping("/handwriting-search")
    public String learnerHandwritingSearch(Model model) {
        populateLearnerShell(model, "handwriting-search", "Tra Cứu Viết Tay AI", "Nhận Diện Viết Tay");
        model.addAttribute("totalKanjiCount", kanjiRepository.count());
        return "learner/handwriting-search";
    }

    /**
     * Màn hình 4: Nhật Ký Tiến Độ Cá Nhân
     */
    @GetMapping({"/my-progress", "/progress"})
    public String learnerMyProgress(Model model) {
        populateLearnerShell(model, "progress", "Nhật Ký Tiến Độ Cá Nhân", "Tiến Độ Của Tôi");
        String userEmail = securityUtil.getCurrentUserEmail(RoleEnum.ROLE_LEARNER);
        com.kizuna.entity.Learner learner = learnerRepository.findByUser_Email(userEmail).orElse(null);
        String targetLevel = (learner != null && learner.getTargetLevel() != null) ? learner.getTargetLevel() : null;
        model.addAttribute("targetLevel", targetLevel);
        return "learner/progress";
    }

    /**
     * Màn hình 5: Bộ Thẻ Của Tôi (Chuẩn Master Shell Nhaitopik)
     */
    @GetMapping("/flashcards")
    public String learnerFlashcards(Model model) {
        populateLearnerShell(model, "flashcards", "Bộ Thẻ Của Tôi", "Bộ Thẻ Của Tôi");
        return "learner/flashcards";
    }

    @GetMapping("/find-tutor")
    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public String learnerFindTutor(Model model) {
        populateLearnerShell(model, "find-tutor", "Tìm Gia Sư Hợp Gu", "Gia Sư & Lớp Học");
        java.util.List<com.kizuna.entity.Tutor> allTutors = tutorRepository.findAllWithUser();
        java.util.List<com.kizuna.entity.Tutor> activeTutors = allTutors.stream()
                .filter(t -> t.getTutorState() == com.kizuna.entity.TutorState.ACTIVE_PAID
                        || t.getVerificationStatus() == com.kizuna.entity.TutorVerificationStatus.APPROVED)
                .toList();
        model.addAttribute("tutors", activeTutors.isEmpty() ? allTutors : activeTutors);
        return "learner/find-tutor";
    }

    @GetMapping("/trial-schedule")
    public String learnerTrialSchedule(Model model) {
        populateLearnerShell(model, "trial-schedule", "Lịch Học Thử & Ghép Lớp", "Gia Sư & Lớp Học");
        return "learner/trial-schedule";
    }

    @GetMapping("/my-classroom")
    public String learnerMyClassroom(Model model) {
        populateLearnerShell(model, "my-classroom", "Lớp Kèm Của Tôi", "Gia Sư & Lớp Học");
        String userEmail = securityUtil.getCurrentUserEmail(RoleEnum.ROLE_LEARNER);
        List<LearningSpace> activeSpaces = learningSpaceRepository.findActiveSpacesByLearnerEmail(userEmail);
        if (!activeSpaces.isEmpty()) {
            model.addAttribute("activeClassroom", activeSpaces.get(0));
            model.addAttribute("assignedTutorName", activeSpaces.get(0).getTutor().getUser().getFullName());
        } else {
            model.addAttribute("activeClassroom", null);
            model.addAttribute("assignedTutorName", null);
        }
        return "learner/my-classroom";
    }

    @GetMapping("/assignments")
    public String learnerAssignments(Model model) {
        populateLearnerShell(model, "assignments", "Bài Tập Sensei Giao", "Học Tập & Luyện Tập");
        return "learner/assignments";
    }
}
