package com.kizuna.features.admin.controller;


import com.kizuna.core.entity.*;
import com.kizuna.core.entity.RoleEnum;
import com.kizuna.features.classroom.repository.LearningSpaceRepository;
import com.kizuna.features.payment.repository.PaymentTransactionRepository;
import com.kizuna.core.security.SecurityContextUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * Controller phục vụ giao diện dành riêng cho Quản Lý Vận Hành (Manager).
 * Toàn bộ màn hình nằm trong Manager Master Shell Layout (Blue Theme).
 * Không sử dụng dữ liệu ảo hardcoded - phản ánh đúng dữ liệu thực tế từ hệ thống.
 */
@Controller
@RequestMapping("/manager")
@RequiredArgsConstructor
public class ManagerController {

    private final SecurityContextUtil securityUtil;
    private final LearningSpaceRepository learningSpaceRepository;
    private final PaymentTransactionRepository paymentTransactionRepository;

    private void populateManagerShell(Model model, String activeMenu, String pageTitle, String breadcrumbSection) {
        String userName = securityUtil.getCurrentUserName(RoleEnum.ROLE_MANAGER);
        String userEmail = securityUtil.getCurrentUserEmail(RoleEnum.ROLE_MANAGER);
        String avatarText = securityUtil.getCurrentUserAvatarText(userName);

        model.addAttribute("pageTitle", pageTitle + " - Quản Lý Vận Hành KizunaTutor");
        model.addAttribute("role", RoleEnum.ROLE_MANAGER);
        model.addAttribute("userName", userName);
        model.addAttribute("userEmail", userEmail);
        model.addAttribute("avatarText", avatarText);
        model.addAttribute("activeMenu", activeMenu);
        model.addAttribute("breadcrumbSection", breadcrumbSection);
    }

    /**
     * Màn hình 1: Dashboard Tổng Quan Quản Lý Vận Hành
     */
    @GetMapping({"", "/dashboard"})
    public String managerDashboard(Model model) {
        populateManagerShell(model, "dashboard", "Tổng Quan Vận Hành", "Tổng Quan");

        long activeSpaces = learningSpaceRepository.count();
        long txCount = paymentTransactionRepository.count();
        String revenueDisplay = (txCount > 0) ? (txCount + " Giao dịch") : "0 đ";

        model.addAttribute("activeClassrooms", activeSpaces);
        model.addAttribute("monthlyRevenue", revenueDisplay);
        model.addAttribute("avgAttendance", "Chưa có dữ liệu");

        return "manager/dashboard";
    }

    /**
     * Màn hình 2: Báo Cáo Hiệu Quả Đào Tạo & Phân Tích Chuyên Cần
     */
    @GetMapping("/learning-analytics")
    public String learningAnalytics(Model model) {
        populateManagerShell(model, "learning-analytics", "Phân Tích Vận Hành & Học Tập", "Báo Cáo Hiệu Quả");
        return "manager/learning-analytics";
    }
}
