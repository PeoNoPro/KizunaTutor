package com.kizuna.features.admin.controller;


import com.kizuna.core.entity.*;
import com.kizuna.core.entity.RoleEnum;
import com.kizuna.features.learning_support.repository.KanjiDictionaryRepository;
import com.kizuna.features.classroom.repository.LearningSpaceRepository;
import com.kizuna.core.repository.UserRepository;
import com.kizuna.core.security.SecurityContextUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * Controller phục vụ giao diện dành riêng cho Quản Trị Viên (Admin).
 * Toàn bộ màn hình nằm trong Admin Master Shell Layout (Navy Theme).
 * Không sử dụng dữ liệu ảo hardcoded - phản ánh đúng dữ liệu thực tế từ hệ thống.
 */
@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminController {

    private final SecurityContextUtil securityUtil;
    private final KanjiDictionaryRepository kanjiRepository;
    private final UserRepository userRepository;
    private final LearningSpaceRepository learningSpaceRepository;

    private void populateAdminShell(Model model, String activeMenu, String pageTitle, String breadcrumbSection) {
        String userName = securityUtil.getCurrentUserName(RoleEnum.ROLE_ADMIN);
        String userEmail = securityUtil.getCurrentUserEmail(RoleEnum.ROLE_ADMIN);
        String avatarText = securityUtil.getCurrentUserAvatarText(userName);

        model.addAttribute("pageTitle", pageTitle + " - Quản Trị Viên KizunaTutor");
        model.addAttribute("role", RoleEnum.ROLE_ADMIN);
        model.addAttribute("userName", userName);
        model.addAttribute("userEmail", userEmail);
        model.addAttribute("avatarText", avatarText);
        model.addAttribute("activeMenu", activeMenu);
        model.addAttribute("breadcrumbSection", breadcrumbSection);
    }

    /**
     * Màn hình 1: Dashboard Tổng Quan Quản Trị Viên
     */
    @GetMapping({"", "/dashboard"})
    public String adminDashboard(Model model) {
        populateAdminShell(model, "dashboard", "Tổng Quan Hệ Thống", "Tổng Quan");

        model.addAttribute("totalUsers", userRepository.count());
        model.addAttribute("totalKanji", kanjiRepository.count());
        model.addAttribute("activeClasses", learningSpaceRepository.count());
        model.addAttribute("serverStatus", "Đang hoạt động");

        return "admin/dashboard";
    }

    /**
     * Màn hình 2: Quản Trị Kho Dữ Liệu Gốc Kanji & Tọa Độ Nét SVG
     * Thẩm quyền Admin: Thêm mới, sửa trường Hán Việt / On / Kun, kích hoạt AI enrichment, sửa SVG.
     */
    @GetMapping("/kanji-management")
    public String kanjiManagement(Model model) {
        populateAdminShell(model, "kanji-management", "Quản Trị Dữ Liệu Kanji", "Từ Điển Kanji Gốc");

        model.addAttribute("totalKanjiCount", kanjiRepository.count());
        model.addAttribute("recentKanjiList", kanjiRepository.findAll().stream().limit(15).toList());

        return "admin/kanji-management";
    }

    /**
     * Màn hình 3: Quản Lý Toàn Bộ Vòng Đời User & Phân Quyền Role 6 Actor
     */
    @GetMapping("/users")
    public String userManagement(Model model) {
        populateAdminShell(model, "users", "Quản Lý User & Phân Quyền", "Người Dùng & Vai Trò");
        return "admin/user-management";
    }
}
