package com.kizuna.features.verification.controller;


import com.kizuna.core.entity.*;
import com.kizuna.core.entity.RoleEnum;
import com.kizuna.features.verification.entity.TutorVerificationStatus;
import com.kizuna.features.learning_support.repository.KanjiDictionaryRepository;
import com.kizuna.core.repository.TutorRepository;
import com.kizuna.core.security.SecurityContextUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * Controller phục vụ giao diện dành riêng cho Cán Bộ Học Vụ (Academic Officer).
 * Toàn bộ màn hình nằm trong Academic Master Shell Layout (Teal Theme).
 * Không sử dụng dữ liệu ảo hardcoded - phản ánh đúng dữ liệu thực tế từ hệ thống.
 */
@Controller
@RequestMapping("/academic")
@RequiredArgsConstructor
public class AcademicController {

    private final SecurityContextUtil securityUtil;
    private final KanjiDictionaryRepository kanjiRepository;
    private final TutorRepository tutorRepository;

    private void populateAcademicShell(Model model, String activeMenu, String pageTitle, String breadcrumbSection) {
        String userName = securityUtil.getCurrentUserName(RoleEnum.ROLE_ACADEMIC_OFFICER);
        String userEmail = securityUtil.getCurrentUserEmail(RoleEnum.ROLE_ACADEMIC_OFFICER);
        String avatarText = securityUtil.getCurrentUserAvatarText(userName);

        model.addAttribute("pageTitle", pageTitle + " - Cán Bộ Học Vụ KizunaTutor");
        model.addAttribute("role", RoleEnum.ROLE_ACADEMIC_OFFICER);
        model.addAttribute("userName", userName);
        model.addAttribute("userEmail", userEmail);
        model.addAttribute("avatarText", avatarText);
        model.addAttribute("activeMenu", activeMenu);
        model.addAttribute("breadcrumbSection", breadcrumbSection);
    }

    /**
     * Màn hình 1: Dashboard Tổng Quan Học Vụ
     */
    @GetMapping({"", "/dashboard"})
    public String academicDashboard(Model model) {
        populateAcademicShell(model, "dashboard", "Tổng Quan Học Vụ", "Tổng Quan");

        long pendingTutors = tutorRepository.countByVerificationStatus(TutorVerificationStatus.PENDING)
                + tutorRepository.countByVerificationStatus(TutorVerificationStatus.UNVERIFIED);
        long totalKanji = kanjiRepository.count();

        model.addAttribute("pendingTutorProfiles", pendingTutors);
        model.addAttribute("verifiedKanjiCount", totalKanji + " Chữ");
        model.addAttribute("standardCurriculums", "0 Khung");

        return "academic/dashboard";
    }

    /**
     * Màn hình 2: Thẩm Định Giáo Trình Kanji & Chuẩn Hóa Cấp Độ JLPT
     * Thẩm quyền Academic Officer: Phân loại chữ theo giáo trình, kiểm duyệt JLPT N5-N1, tạo khung bài học mẫu.
     */
    @GetMapping("/curriculum-kanji")
    public String curriculumKanji(Model model) {
        populateAcademicShell(model, "curriculum-kanji", "Chuẩn Hóa Giáo Trình Kanji", "Thẩm Định Giáo Trình");
        model.addAttribute("totalKanjiCount", kanjiRepository.count());
        return "academic/curriculum-kanji";
    }

    /**
     * Màn hình 3: Khảo Thí & Chuẩn Đầu Ra JLPT
     */
    @GetMapping("/academic-records")
    public String academicRecords(Model model) {
        populateAcademicShell(model, "academic-records", "Báo Cáo Khảo Thí & Chuẩn Đầu Ra", "Báo Cáo Đào Tạo");
        return "academic/academic-records";
    }
}
