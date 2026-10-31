package com.kizuna.seeder;

import com.kizuna.entity.*;
import com.kizuna.repository.LearnerRepository;
import com.kizuna.repository.LearningSpaceRepository;
import com.kizuna.repository.RoleRepository;
import com.kizuna.repository.TutorRepository;
import com.kizuna.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

/**
 * Seeder tự động nạp hoặc cập nhật dữ liệu 6 Vai trò và đúng 6 Tài khoản mẫu chuẩn:
 * 1. Quản trị viên (Admin)   : admin@kizuna.vn / admin123
 * 2. Quản lý vận hành        : manager@kizuna.vn / 123456
 * 3. Nhân viên CSKH          : cs@kizuna.vn / 123456
 * 4. Cán bộ học vụ           : academic@kizuna.vn / 123456
 * 5. Gia sư Kizuna           : tutor@kizuna.vn / 123456
 * 6. Học viên Kizuna         : learner@kizuna.vn / 123456
 */
@Slf4j
@Component
@Order(1)
@RequiredArgsConstructor
public class AuthDataSeeder implements CommandLineRunner {

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final TutorRepository tutorRepository;
    private final LearnerRepository learnerRepository;
    private final LearningSpaceRepository learningSpaceRepository;

    @Override
    @Transactional
    public void run(String... args) {
        log.info("--- Khởi chạy AuthDataSeeder: Đồng bộ đúng 6 Actor cốt lõi ---");

        // 1. Khởi tạo / Đồng bộ 6 Role
        for (RoleEnum roleEnum : RoleEnum.values()) {
            roleRepository.findByRoleName(roleEnum.getRoleName()).orElseGet(() -> {
                Role newRole = Role.builder()
                        .roleName(roleEnum.getRoleName())
                        .description(roleEnum.getRoleDescription())
                        .isActive(true)
                        .build();
                Role saved = roleRepository.save(newRole);
                log.info("Đã tạo mới Role: {}", saved.getRoleName());
                return saved;
            });
        }

        // 2. Khởi tạo / Cập nhật đúng 6 Tài khoản mẫu chuẩn ban đầu
        seedUser("admin@kizuna.vn", "admin123", "Quản Trị Viên Hệ Thống", "ROLE_ADMIN", "0901234567");
        seedUser("manager@kizuna.vn", "123456", "Trần Thị Quản Lý", "ROLE_MANAGER", "0902234567");
        seedUser("cs@kizuna.vn", "123456", "Lê Chăm Sóc Khách Hàng", "ROLE_CS_STAFF", "0903234567");
        seedUser("academic@kizuna.vn", "123456", "Phạm Cán Bộ Học Vụ", "ROLE_ACADEMIC_OFFICER", "0904234567");
        User tutorUser = seedUser("tutor@kizuna.vn", "123456", "Yamada Kenji (Gia Sư)", "ROLE_TUTOR", "0905234567");
        User learnerUser = seedUser("learner@kizuna.vn", "123456", "Nguyễn Văn An", "ROLE_LEARNER", "0906234567");

        // 3. Khởi tạo liên kết hồ sơ Gia Sư & Học Viên mẫu
        if (tutorUser != null) {
            tutorRepository.findByUser_UserId(tutorUser.getUserId()).orElseGet(() -> {
                Tutor newTutor = Tutor.builder()
                        .user(tutorUser)
                        .headline("Gia sư tiếng Nhật JLPT N1 - 5 năm kinh nghiệm Tokyo")
                        .bio("Nhiệt tình, chú trọng phương pháp trực quan và luyện chữ Hán chuẩn nét.")
                        .build();
                return tutorRepository.save(newTutor);
            });
        }

        if (learnerUser != null) {
            learnerRepository.findByUser_UserId(learnerUser.getUserId()).orElseGet(() -> {
                Learner newLearner = Learner.builder()
                        .user(learnerUser)
                        .currentLevel("N4")
                        .targetLevel("N2")
                        .learningGoals("Chinh phục JLPT N2 và luyện viết nét Shodo chuyên sâu")
                        .build();
                return learnerRepository.save(newLearner);
            });
        }

        // 4. Tự động liên kết Learning Space (Ghép lớp Gia sư - Học viên)
        Tutor tutor = (tutorUser != null) ? tutorRepository.findByUser_UserId(tutorUser.getUserId()).orElse(null) : null;
        Learner learner = (learnerUser != null) ? learnerRepository.findByUser_UserId(learnerUser.getUserId()).orElse(null) : null;
        if (tutor != null && learner != null) {
            learningSpaceRepository.findActiveSpacesByLearnerEmail(learnerUser.getEmail()).stream().findFirst().orElseGet(() -> {
                LearningSpace space = LearningSpace.builder()
                        .tutor(tutor)
                        .learner(learner)
                        .title("Lớp Kèm Tiếng Nhật & Shodo 1:1 (Sensei Yamada)")
                        .status(LearningSpaceStatus.ACTIVE)
                        .build();
                LearningSpace savedSpace = learningSpaceRepository.save(space);
                log.info("Đã tạo liên kết Learning Space thành công giữa Gia sư [{}] và Học viên [{}]", tutorUser.getFullName(), learnerUser.getFullName());
                return savedSpace;
            });
        }

        // 5. Tự động loại bỏ toàn bộ các tài khoản thừa không dùng, bảo toàn duy nhất 6 Actor mẫu
        userRepository.deleteNonCoreUsers();

        log.info("--- AuthDataSeeder hoàn tất thành công: Duy trì 6 Actor chuẩn ---");
    }

    private User seedUser(String email, String rawPassword, String fullName, String roleName, String phone) {
        Role role = roleRepository.findByRoleName(roleName).orElse(null);
        if (role == null) {
            log.warn("Không tìm thấy role {}", roleName);
            return null;
        }

        Optional<User> existingUserOpt = userRepository.findByEmail(email);
        if (existingUserOpt.isEmpty()) {
            User newUser = User.builder()
                    .userId(UUID.randomUUID())
                    .email(email)
                    .passwordHash(passwordEncoder.encode(rawPassword))
                    .fullName(fullName)
                    .phone(phone)
                    .role(role)
                    .accountStatus(UserAccountStatus.ACTIVE)
                    .build();
            User saved = userRepository.save(newUser);
            log.info("Đã tạo mới tài khoản mẫu [{} - {}]", email, roleName);
            return saved;
        } else {
            User existing = existingUserOpt.get();
            existing.setFullName(fullName);
            existing.setPhone(phone);
            existing.setRole(role);
            existing.setAccountStatus(UserAccountStatus.ACTIVE);
            existing.setDeletedAt(null);
            if (!passwordEncoder.matches(rawPassword, existing.getPasswordHash())) {
                existing.setPasswordHash(passwordEncoder.encode(rawPassword));
                log.info("Đã đồng bộ lại mật khẩu chuẩn cho tài khoản mẫu core [{}]", email);
            }
            return userRepository.save(existing);
        }
    }
}
