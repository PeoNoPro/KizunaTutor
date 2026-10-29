package com.kizuna.features.classroom.service.impl;

import com.kizuna.features.auth.dto.LearnerAccessState;
import com.kizuna.core.entity.*;
import com.kizuna.features.classroom.entity.*;
import com.kizuna.features.learning_support.entity.*;
import com.kizuna.features.payment.entity.*;
import com.kizuna.features.progress.entity.*;
import com.kizuna.features.verification.entity.*;
import com.kizuna.features.tutor_search.entity.*;
import com.kizuna.features.ticket.entity.*;
import com.kizuna.features.notification.entity.*;
import com.kizuna.features.booking.entity.*;
import com.kizuna.features.admin.entity.*;
import com.kizuna.core.exception.BadRequestException;
import com.kizuna.core.repository.LearnerRepository;
import com.kizuna.features.classroom.repository.LearningSpaceRepository;
import com.kizuna.core.repository.TutorRepository;
import com.kizuna.core.repository.UserRepository;
import com.kizuna.features.classroom.service.ILearningSpaceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class LearningSpaceServiceImpl implements ILearningSpaceService {

    private final LearningSpaceRepository learningSpaceRepository;
    private final LearnerRepository learnerRepository;
    private final TutorRepository tutorRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public LearnerAccessState checkCurrentUserAccessState() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equalsIgnoreCase(auth.getName())) {
            return LearnerAccessState.UNAUTHENTICATED;
        }

        User user = null;
        if (auth.getPrincipal() instanceof User) {
            user = (User) auth.getPrincipal();
        } else {
            user = userRepository.findByEmail(auth.getName()).orElse(null);
        }

        if (user == null) {
            return LearnerAccessState.UNAUTHENTICATED;
        }

        return checkUserAccessState(user);
    }

    @Override
    @Transactional(readOnly = true)
    public LearnerAccessState checkUserAccessState(User user) {
        if (user == null) {
            return LearnerAccessState.UNAUTHENTICATED;
        }

        String roleName = (user.getRole() != null) ? user.getRole().getRoleName() : "ROLE_LEARNER";

        // Gia sư, Quản trị viên, Học vụ hoặc Quản lý được quyền truy cập xem xét / giảng dạy
        if (!"ROLE_LEARNER".equalsIgnoreCase(roleName)) {
            return LearnerAccessState.ACTIVE_OFFICIAL_LEARNER;
        }

        // Với Học Viên (ROLE_LEARNER): Kiểm tra có thuộc Learning Space nào đang ACTIVE không
        boolean isAssigned = isStudentAssignedToAnyTutor(user.getUserId());
        if (isAssigned) {
            return LearnerAccessState.ACTIVE_OFFICIAL_LEARNER;
        } else {
            return LearnerAccessState.UNASSIGNED_LEARNER;
        }
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isStudentAssignedToAnyTutor(UUID userId) {
        if (userId == null) return false;
        return learningSpaceRepository.isStudentAssignedToAnyTutor(userId);
    }

    @Override
    @Transactional
    public boolean joinSpaceByInviteCode(String inviteCode) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equalsIgnoreCase(auth.getName())) {
            throw new BadRequestException("Bạn cần đăng nhập trước khi nhập mã mời của Gia sư");
        }

        String email = (auth.getPrincipal() instanceof User) ? ((User) auth.getPrincipal()).getEmail() : auth.getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BadRequestException("Không tìm thấy thông tin tài khoản"));

        // Tìm hoặc tạo thực thể Learner cho User này
        Learner learner = learnerRepository.findByUser_UserId(user.getUserId()).orElseGet(() -> {
            Learner newLearner = Learner.builder()
                    .user(user)
                    .currentLevel("N5")
                    .targetLevel("N3")
                    .learningGoals("Luyện viết chữ Hán và giao tiếp cùng gia sư")
                    .build();
            return learnerRepository.save(newLearner);
        });

        // Tìm Gia sư mặc định (Yamada Kenji) để gán vào Learning Space
        User tutorUser = userRepository.findByEmail("tutor@kizuna.vn").orElse(null);
        Tutor tutor = null;
        if (tutorUser != null) {
            tutor = tutorRepository.findByUser_UserId(tutorUser.getUserId()).orElseGet(() -> {
                Tutor newTutor = Tutor.builder()
                        .user(tutorUser)
                        .headline("Gia sư tiếng Nhật JLPT N1 - 5 năm kinh nghiệm Tokyo")
                        .bio("Nhiệt tình, chú trọng phương pháp trực quan và luyện chữ Hán chuẩn nét.")
                        .build();
                return tutorRepository.save(newTutor);
            });
        }

        if (tutor == null) {
            throw new BadRequestException("Không tìm thấy thông tin Gia sư phù hợp để kết nối");
        }

        // Tạo mới Learning Space với trạng thái ACTIVE
        LearningSpace space = LearningSpace.builder()
                .tutor(tutor)
                .learner(learner)
                .title("Lớp Học Tiếng Nhật & Luyện Chữ Kanji (Mã: " + (inviteCode != null ? inviteCode.trim().toUpperCase() : "KIZUNA") + ")")
                .status(LearningSpaceStatus.ACTIVE)
                .build();

        learningSpaceRepository.save(space);
        log.info("Học viên [{}] đã tham gia Không Gian Học Tập [{}] thành công với Gia sư [{}] qua mã mời [{}]",
                user.getEmail(), space.getTitle(), tutor.getUser().getFullName(), inviteCode);

        return true;
    }
}
