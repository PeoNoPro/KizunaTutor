package com.kizuna.controller.tutor;

import com.kizuna.dto.tutor.TutorMembershipDto;
import com.kizuna.dto.tutor.TutorProfileDto;
import com.kizuna.entity.*;
import com.kizuna.repository.AssignmentSubmissionRepository;
import com.kizuna.repository.KanjiDictionaryRepository;
import com.kizuna.repository.LearningSpaceRepository;
import com.kizuna.repository.TutorRepository;
import com.kizuna.security.RequireActiveTutor;
import com.kizuna.service.tutor.ITutorMembershipService;
import com.kizuna.service.tutor.ITutorProfileService;
import com.kizuna.util.SecurityContextUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Controller phục vụ giao diện dành riêng cho Gia Sư Kizuna (Tutor).
 * Toàn bộ màn hình nằm trong Tutor Master Shell Layout (Warm Rose Theme).
 * Quản lý quy trình 3 giai đoạn: Onboarding (Module 2) -> Membership (Module 7) -> Full Access.
 */
@Controller
@RequestMapping("/tutor")
@RequiredArgsConstructor
@Slf4j
public class TutorController {

    private final SecurityContextUtil securityUtil;
    private final KanjiDictionaryRepository kanjiRepository;
    private final LearningSpaceRepository learningSpaceRepository;
    private final TutorRepository tutorRepository;
    private final AssignmentSubmissionRepository assignmentSubmissionRepository;
    private final ITutorProfileService profileService;
    private final ITutorMembershipService membershipService;

    private Tutor populateTutorShell(Model model, String activeMenu, String pageTitle, String breadcrumbSection) {
        String userName = securityUtil.getCurrentUserName(RoleEnum.ROLE_TUTOR);
        String userEmail = securityUtil.getCurrentUserEmail(RoleEnum.ROLE_TUTOR);
        String avatarText = securityUtil.getCurrentUserAvatarText(userName);

        Tutor tutor = tutorRepository.findByUser_Email(userEmail).orElse(null);
        TutorState tutorState = tutor != null ? tutor.getTutorState() : TutorState.DRAFT_PROFILE;

        boolean isDraft = tutorState == TutorState.DRAFT_PROFILE;
        boolean isPending = tutorState == TutorState.PENDING_VERIFICATION;
        boolean isRejected = tutorState == TutorState.REJECTED;
        boolean isApprovedUnpaid = tutorState == TutorState.APPROVED_UNPAID;
        boolean isActivePaid = tutorState == TutorState.ACTIVE_PAID;

        model.addAttribute("pageTitle", pageTitle + " - Gia Sư Kizuna");
        model.addAttribute("role", RoleEnum.ROLE_TUTOR);
        model.addAttribute("userName", userName);
        model.addAttribute("userEmail", userEmail);
        model.addAttribute("avatarText", avatarText);
        model.addAttribute("activeMenu", activeMenu);
        model.addAttribute("breadcrumbSection", breadcrumbSection);

        model.addAttribute("tutor", tutor);
        model.addAttribute("tutorState", tutorState.name());
        model.addAttribute("isDraft", isDraft);
        model.addAttribute("isPending", isPending);
        model.addAttribute("isRejected", isRejected);
        model.addAttribute("isApprovedUnpaid", isApprovedUnpaid);
        model.addAttribute("isActivePaid", isActivePaid);
        model.addAttribute("isFullyActive", isActivePaid);
        model.addAttribute("verificationFeedback", tutor != null ? tutor.getVerificationFeedback() : null);

        return tutor;
    }

    /**
     * Màn hình 1: Dashboard Tổng Quan Lớp Dạy Của Gia Sư (UC 28-37, UC 41)
     */
    @GetMapping({"", "/dashboard"})
    public String tutorDashboard(Model model) {
        Tutor tutor = populateTutorShell(model, "dashboard", "Tổng Quan Lớp Dạy", "Tổng Quan");

        // Nếu chưa hoàn tất giai đoạn thanh toán gói, chuyển hướng đến bước tương ứng
        if (tutor != null && tutor.getTutorState() != TutorState.ACTIVE_PAID) {
            if (tutor.getTutorState() == TutorState.APPROVED_UNPAID) {
                return "redirect:/tutor/membership-plans";
            }
            return "redirect:/tutor/profile";
        }

        String userEmail = securityUtil.getCurrentUserEmail(RoleEnum.ROLE_TUTOR);
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
     * Màn hình Cập nhật / Xác minh hồ sơ giảng dạy (UC 9 - 16).
     */
    @GetMapping("/profile")
    public String tutorProfile(Model model) {
        populateTutorShell(model, "profile", "Hồ Sơ Giảng Dạy", "Hồ Sơ Xác Minh");
        String userEmail = securityUtil.getCurrentUserEmail(RoleEnum.ROLE_TUTOR);
        TutorProfileDto profile = profileService.getProfileByEmail(userEmail);
        model.addAttribute("profile", profile);
        return "tutor/update-profile";
    }

    /**
     * Màn hình Gói Thành Viên & Kích Hoạt Quyền Gia Sư (UC 71, 72, 73, 76).
     */
    @GetMapping("/membership-plans")
    public String membershipPlans(Model model) {
        populateTutorShell(model, "membership-plans", "Gói Thành Viên & Kích Hoạt", "Gói Thành Viên");
        String userEmail = securityUtil.getCurrentUserEmail(RoleEnum.ROLE_TUTOR);

        List<MembershipPlan> plans = membershipService.getActivePlans();
        TutorMembershipDto currentMembership = membershipService.getCurrentMembership(userEmail);

        model.addAttribute("plans", plans);
        model.addAttribute("currentMembership", currentMembership);
        return "tutor/membership-plans";
    }

    /**
     * Endpoint tiếp nhận điều hướng trở về từ cổng thanh toán VNPay (UC 94).
     */
    @GetMapping("/membership-return")
    public String vnpayReturn(@RequestParam Map<String, String> allParams, Model model) {
        populateTutorShell(model, "membership-plans", "Kết Quả Thanh Toán", "Thanh Toán");
        log.info("VNPay Return callback: {}", allParams);

        boolean isSuccess = membershipService.processVnPayReturn(allParams);
        model.addAttribute("paymentSuccess", isSuccess);
        model.addAttribute("txnRef", allParams.get("vnp_TxnRef"));
        model.addAttribute("amount", allParams.get("vnp_Amount"));

        String userEmail = securityUtil.getCurrentUserEmail(RoleEnum.ROLE_TUTOR);
        List<MembershipPlan> plans = membershipService.getActivePlans();
        TutorMembershipDto currentMembership = membershipService.getCurrentMembership(userEmail);

        model.addAttribute("plans", plans);
        model.addAttribute("currentMembership", currentMembership);

        return "tutor/membership-plans";
    }

    /**
     * Màn hình Kho Kanji & Soạn Bài Tập (UC 43-45, 47-54).
     * Yêu cầu tài khoản ACTIVE_PAID.
     */
    @RequireActiveTutor
    @GetMapping("/kanji-library")
    public String tutorKanjiLibrary(Model model) {
        populateTutorShell(model, "kanji-library", "Kho Kanji & Soạn Bài Tập", "Soạn Bài Tập");
        model.addAttribute("totalKanjiCount", kanjiRepository.count());
        return "tutor/kanji-library";
    }

    /**
     * Màn hình Theo Dõi Tiến Độ Học Viên Trong Lớp (UC 57).
     * Yêu cầu tài khoản ACTIVE_PAID.
     */
    @RequireActiveTutor
    @GetMapping("/student-progress")
    public String tutorStudentProgress(Model model) {
        populateTutorShell(model, "student-progress", "Tiến Độ Học Viên", "Lớp Của Tôi");
        String userEmail = securityUtil.getCurrentUserEmail(RoleEnum.ROLE_TUTOR);
        List<LearningSpace> activeSpaces = learningSpaceRepository.findActiveSpacesByTutorEmail(userEmail);
        model.addAttribute("activeSpaces", activeSpaces);
        return "tutor/student-progress";
    }
}
