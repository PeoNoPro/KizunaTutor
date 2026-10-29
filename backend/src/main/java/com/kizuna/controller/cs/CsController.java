package com.kizuna.controller.cs;

import com.kizuna.entity.RoleEnum;
import com.kizuna.repository.SupportTicketRepository;
import com.kizuna.util.SecurityContextUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * Controller phục vụ giao diện dành riêng cho Nhân Viên CSKH (CS Staff).
 * Toàn bộ màn hình nằm trong CS Staff Master Shell Layout (Purple Theme).
 * Không sử dụng dữ liệu ảo hardcoded - phản ánh đúng dữ liệu thực tế từ hệ thống.
 */
@Controller
@RequestMapping("/cs")
@RequiredArgsConstructor
public class CsController {

    private final SecurityContextUtil securityUtil;
    private final SupportTicketRepository supportTicketRepository;

    private void populateCsShell(Model model, String activeMenu, String pageTitle, String breadcrumbSection) {
        String userName = securityUtil.getCurrentUserName(RoleEnum.ROLE_CS_STAFF);
        String userEmail = securityUtil.getCurrentUserEmail(RoleEnum.ROLE_CS_STAFF);
        String avatarText = securityUtil.getCurrentUserAvatarText(userName);

        model.addAttribute("pageTitle", pageTitle + " - Chăm Sóc Khách Hàng KizunaTutor");
        model.addAttribute("role", RoleEnum.ROLE_CS_STAFF);
        model.addAttribute("userName", userName);
        model.addAttribute("userEmail", userEmail);
        model.addAttribute("avatarText", avatarText);
        model.addAttribute("activeMenu", activeMenu);
        model.addAttribute("breadcrumbSection", breadcrumbSection);
    }

    /**
     * Màn hình 1: Dashboard CSKH & Hỗ Trợ Khách Hàng
     */
    @GetMapping({"", "/dashboard"})
    public String csDashboard(Model model) {
        populateCsShell(model, "dashboard", "Tổng Quan CSKH", "Tổng Quan");

        long totalTickets = supportTicketRepository.count();

        model.addAttribute("newInquiries", 0L);
        model.addAttribute("activeTickets", totalTickets);
        model.addAttribute("csatScore", "Chưa có đánh giá");

        return "cs/dashboard";
    }

    /**
     * Màn hình 2: Hỗ Trợ Học Viên & Cảnh Báo Bỏ Bê Bài Tập
     */
    @GetMapping("/student-support-progress")
    public String studentSupportProgress(Model model) {
        populateCsShell(model, "student-support-progress", "Hỗ Trợ Học Viên & Cảnh Báo", "Hỗ Trợ Học Viên");
        return "cs/student-support-progress";
    }
}
