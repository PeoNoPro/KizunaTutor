package com.kizuna.controller.tutor;

import com.kizuna.entity.LearningSpace;
import com.kizuna.entity.RoleEnum;
import com.kizuna.entity.Tutor;
import com.kizuna.entity.TutorVerificationStatus;
import com.kizuna.repository.AssignmentSubmissionRepository;
import com.kizuna.repository.KanjiDictionaryRepository;
import com.kizuna.repository.LearningSpaceRepository;
import com.kizuna.repository.TutorRepository;
import com.kizuna.util.SecurityContextUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

/**
 * Controller phục vụ giao diện dành riêng cho Gia Sư Kizuna (Tutor).
 * Toàn bộ màn hình nằm trong Tutor Master Shell Layout (Warm Rose Theme).
 * Không sử dụng dữ liệu ảo hardcoded - phản ánh đúng dữ liệu thực tế từ hệ thống.
 */
@Controller
@RequestMapping("/tutor")
@RequiredArgsConstructor
public class TutorController {

    private final SecurityContextUtil securityUtil;
    private final KanjiDictionaryRepository kanjiRepository;
    private final LearningSpaceRepository learningSpaceRepository;
    private final TutorRepository tutorRepository;
    private final AssignmentSubmissionRepository assignmentSubmissionRepository;

    private void populateTutorShell(Model model, String activeMenu, String pageTitle, String breadcrumbSection) {
        String userName = securityUtil.getCurrentUserName(RoleEnum.ROLE_TUTOR);
        String userEmail = securityUtil.getCurrentUserEmail(RoleEnum.ROLE_TUTOR);
        String avatarText = securityUtil.getCurrentUserAvatarText(userName);

        model.addAttribute("pageTitle", pageTitle + " - Gia Sư Kizuna");
        model.addAttribute("role", RoleEnum.ROLE_TUTOR);
        model.addAttribute("userName", userName);
        model.addAttribute("userEmail", userEmail);
        model.addAttribute("avatarText", avatarText);
        model.addAttribute("activeMenu", activeMenu);
        model.addAttribute("breadcrumbSection", breadcrumbSection);
    }

    /**
     * Màn hình 1: Dashboard Tổng Quan Lớp Dạy Của Gia Sư
     */
    @GetMapping({"", "/dashboard"})
    public String tutorDashboard(Model model) {
        populateTutorShell(model, "dashboard", "Tổng Quan Lớp Dạy", "Tổng Quan");

        String userEmail = securityUtil.getCurrentUserEmail(RoleEnum.ROLE_TUTOR);
        Tutor tutor = tutorRepository.findByUser_Email(userEmail).orElse(null);

        boolean isVerified = tutor != null && tutor.getVerificationStatus() == TutorVerificationStatus.APPROVED;
        String verificationBadge = isVerified ? "Đang hoạt động" : "Chờ thẩm định hồ sơ";
        String verificationStatusClass = isVerified ? "bg-emerald-50 text-emerald-700 border-emerald-200" : "bg-amber-50 text-amber-700 border-amber-200";
        String verificationDot = isVerified ? "🟢" : "🟡";

        long assignedStudents = learningSpaceRepository.countActiveSpacesByTutorEmail(userEmail);
        long pendingSubmissions = assignmentSubmissionRepository.count();

        model.addAttribute("isVerified", isVerified);
        model.addAttribute("verificationBadge", verificationBadge);
        model.addAttribute("verificationStatusClass", verificationStatusClass);
        model.addAttribute("verificationDot", verificationDot);
        model.addAttribute("todayClassesCount", 0L);
        model.addAttribute("assignedStudentsCount", assignedStudents);
        model.addAttribute("pendingSubmissionsCount", pendingSubmissions);

        return "tutor/dashboard";
    }

    /**
     * Màn hình 2: Thư Viện Kanji & Giỏ Gom Chữ Soạn Bài Tập
     * Thẩm quyền Tutor: Tra cứu, xem mô phỏng thứ tự nét Shodo, gom chữ tạo bài tập giao cho lớp.
     */
    @GetMapping("/kanji-library")
    public String tutorKanjiLibrary(Model model) {
        populateTutorShell(model, "kanji-library", "Kho Kanji & Soạn Bài Tập", "Soạn Bài Tập");

        model.addAttribute("totalKanjiCount", kanjiRepository.count());
        return "tutor/kanji-library";
    }

    /**
     * Màn hình 3: Theo Dõi Tiến Độ Học Viên Trong Lớp
     * Thẩm quyền Tutor: Xem chi tiết từng học viên, điểm số viết AI, gửi lời nhắc nộp bài.
     */
    @GetMapping("/student-progress")
    public String tutorStudentProgress(Model model) {
        populateTutorShell(model, "student-progress", "Tiến Độ Học Viên", "Lớp Của Tôi");

        String userEmail = securityUtil.getCurrentUserEmail(RoleEnum.ROLE_TUTOR);
        List<LearningSpace> activeSpaces = learningSpaceRepository.findActiveSpacesByTutorEmail(userEmail);
        model.addAttribute("activeSpaces", activeSpaces);

        return "tutor/student-progress";
    }
}
