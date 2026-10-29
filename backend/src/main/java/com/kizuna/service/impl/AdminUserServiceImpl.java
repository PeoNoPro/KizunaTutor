package com.kizuna.service.impl;

import com.kizuna.dto.admin.*;
import com.kizuna.entity.*;
import com.kizuna.exception.BadRequestException;
import com.kizuna.exception.ResourceNotFoundException;
import com.kizuna.repository.LearnerRepository;
import com.kizuna.repository.RoleRepository;
import com.kizuna.repository.TutorRepository;
import com.kizuna.repository.UserRepository;
import com.kizuna.service.IAdminUserService;
import com.kizuna.util.SecurityContextUtil;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Triển khai dịch vụ quản lý User & Role dành riêng cho Quản Trị Viên (Admin):
 * - Tìm kiếm, phân trang và bộ lọc đa tiêu chí (keyword, role, status).
 * - Khởi tạo tài khoản nhân sự nội bộ và phân quyền.
 * - Khóa / Mở khóa an toàn (chặn tự khóa bản thân).
 * - Xóa mềm (Soft Delete) bảo toàn tính toàn vẹn dữ liệu.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminUserServiceImpl implements IAdminUserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final TutorRepository tutorRepository;
    private final LearnerRepository learnerRepository;
    private final PasswordEncoder passwordEncoder;
    private final SecurityContextUtil securityUtil;
    private final com.kizuna.service.IEmailService emailService;

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    @Override
    @Transactional(readOnly = true)
    public UserPageResponse getUsers(int page, int size, String keyword, String role, String status, String sortField, String sortDir, User currentAdmin) {
        String safeSortField = (sortField != null && !sortField.isBlank()) ? sortField : "createdAt";
        Sort.Direction direction = "asc".equalsIgnoreCase(sortDir) ? Sort.Direction.ASC : Sort.Direction.DESC;
        Pageable pageable = PageRequest.of(Math.max(0, page), Math.max(1, size), Sort.by(direction, safeSortField));

        Specification<User> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // 1. Mặc định chỉ lấy người dùng chưa bị xóa mềm, trừ khi người dùng chủ đích lọc DELETED
            if ("DELETED".equalsIgnoreCase(status)) {
                predicates.add(cb.isNotNull(root.get("deletedAt")));
            } else {
                predicates.add(cb.isNull(root.get("deletedAt")));
            }

            // 2. Tìm kiếm từ khóa (Họ tên, Email, Số điện thoại)
            if (keyword != null && !keyword.trim().isEmpty()) {
                String kw = "%" + keyword.trim().toLowerCase() + "%";
                Predicate nameMatch = cb.like(cb.lower(root.get("fullName")), kw);
                Predicate emailMatch = cb.like(cb.lower(root.get("email")), kw);
                Predicate phoneMatch = cb.like(root.get("phone"), kw);
                predicates.add(cb.or(nameMatch, emailMatch, phoneMatch));
            }

            // 3. Lọc theo Vai trò (Role)
            if (role != null && !role.isBlank() && !"ALL".equalsIgnoreCase(role)) {
                predicates.add(cb.equal(root.get("role").get("roleName"), role.trim()));
            }

            // 4. Lọc theo Trạng thái (Status)
            if (status != null && !status.isBlank() && !"ALL".equalsIgnoreCase(status) && !"DELETED".equalsIgnoreCase(status)) {
                UserAccountStatus mappedStatus = mapStringToStatus(status);
                predicates.add(cb.equal(root.get("accountStatus"), mappedStatus));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<User> userPage = userRepository.findAll(spec, pageable);

        List<UserDTO> dtoList = userPage.getContent().stream()
                .map(u -> toDTO(u, currentAdmin))
                .toList();

        return UserPageResponse.builder()
                .content(dtoList)
                .totalElements(userPage.getTotalElements())
                .totalPages(userPage.getTotalPages())
                .currentPage(userPage.getNumber())
                .pageSize(userPage.getSize())
                .kpis(getKpiStats())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public UserDTO getUserById(UUID userId, User currentAdmin) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng với ID: " + userId));
        return toDTO(user, currentAdmin);
    }

    @Override
    @Transactional
    public UserDTO createUser(CreateUserRequest request, User currentAdmin) {
        String cleanEmail = request.getEmail().trim().toLowerCase();

        // 1. Kiểm tra trùng lặp email
        if (userRepository.findByEmail(cleanEmail).isPresent()) {
            throw new BadRequestException("Địa chỉ email '" + cleanEmail + "' đã tồn tại trong hệ thống.");
        }

        // 2. Tìm kiếm vai trò
        String targetRoleName = request.getRole().trim();
        Role role = roleRepository.findByRoleName(targetRoleName)
                .orElseThrow(() -> new BadRequestException("Vai trò không hợp lệ: " + targetRoleName));

        // 3. Khởi tạo thực thể User
        UserAccountStatus status = mapStringToStatus(request.getStatus());

        User user = User.builder()
                .email(cleanEmail)
                .fullName(request.getFullName().trim())
                .phone(request.getPhone() != null ? request.getPhone().trim() : null)
                .passwordHash(passwordEncoder.encode(request.getTemporaryPassword()))
                .role(role)
                .accountStatus(status)
                .mustChangePassword(true) // Bắt buộc đổi mật khẩu lần đầu đăng nhập
                .build();

        user = userRepository.save(user);

        // 4. Tạo thực thể bổ trợ nếu là Gia Sư hoặc Học Viên
        if (RoleEnum.ROLE_TUTOR.name().equals(role.getRoleName())) {
            Tutor tutor = Tutor.builder()
                    .user(user)
                    .headline("Gia sư tiếng Nhật Kizuna")
                    .bio("Gia sư được tạo bởi Quản Trị Viên hệ thống.")
                    .experienceYears((short) 1)
                    .hourlyRate(BigDecimal.valueOf(200000))
                    .highestJlptLevel("N2")
                    .avgRating(BigDecimal.valueOf(5.0))
                    .totalReviews(0)
                    .verificationStatus(TutorVerificationStatus.APPROVED)
                    .membershipStatus(TutorMembershipStatus.PREMIUM)
                    .isFeatured(false)
                    .updatedAt(OffsetDateTime.now())
                    .build();
            tutorRepository.save(tutor);
        } else if (RoleEnum.ROLE_LEARNER.name().equals(role.getRoleName())) {
            Learner learner = Learner.builder()
                    .user(user)
                    .currentLevel("N5")
                    .targetLevel("N3")
                    .learningGoals("Học tiếng Nhật & luyện chữ Kanji")
                    .updatedAt(OffsetDateTime.now())
                    .build();
            learnerRepository.save(learner);
        }

        // 5. Gửi email thông báo tài khoản kèm mật khẩu tạm qua Gmail bất đồng bộ (@Async)
        RoleEnum roleEnum = RoleEnum.fromString(role.getRoleName());
        String roleBadge = (roleEnum != null) ? roleEnum.getBadgeText() : role.getRoleName();
        emailService.sendStaffAccountCredentialsEmail(
                cleanEmail,
                user.getFullName(),
                request.getTemporaryPassword(),
                roleBadge
        );

        log.info("[AdminUser] Quản trị viên [{}] đã tạo tài khoản [{}] vai trò [{}], cờ mustChangePassword=true, đã gửi mail.",
                currentAdmin != null ? currentAdmin.getEmail() : "SYSTEM", cleanEmail, targetRoleName);

        return toDTO(user, currentAdmin);
    }

    @Override
    @Transactional
    public UserDTO updateUser(UUID userId, UpdateUserRoleRequest request, User currentAdmin) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng với ID: " + userId));

        // Không cho phép tự hạ quyền Admin của chính mình
        if (currentAdmin != null && user.getUserId().equals(currentAdmin.getUserId())) {
            if (!RoleEnum.ROLE_ADMIN.name().equals(request.getRole().trim())) {
                throw new BadRequestException("Bạn không thể tự hạ quyền Quản trị viên của chính mình.");
            }
        }

        // Cập nhật thông tin cơ bản
        user.setFullName(request.getFullName().trim());
        if (request.getPhone() != null) {
            user.setPhone(request.getPhone().trim());
        }

        // Cập nhật vai trò nếu có thay đổi
        String newRoleName = request.getRole().trim();
        if (!user.getRole().getRoleName().equals(newRoleName)) {
            Role newRole = roleRepository.findByRoleName(newRoleName)
                    .orElseThrow(() -> new BadRequestException("Vai trò không hợp lệ: " + newRoleName));
            user.setRole(newRole);

            // Đảm bảo có hồ sơ Gia sư / Học viên tương ứng nếu đổi sang các vai trò này
            if (RoleEnum.ROLE_TUTOR.name().equals(newRoleName) && tutorRepository.findByUser_UserId(user.getUserId()).isEmpty()) {
                Tutor tutor = Tutor.builder()
                        .user(user)
                        .headline("Gia sư tiếng Nhật Kizuna")
                        .experienceYears((short) 1)
                        .hourlyRate(BigDecimal.valueOf(200000))
                        .highestJlptLevel("N2")
                        .verificationStatus(TutorVerificationStatus.APPROVED)
                        .membershipStatus(TutorMembershipStatus.PREMIUM)
                        .updatedAt(OffsetDateTime.now())
                        .build();
                tutorRepository.save(tutor);
            } else if (RoleEnum.ROLE_LEARNER.name().equals(newRoleName) && learnerRepository.findByUser_UserId(user.getUserId()).isEmpty()) {
                Learner learner = Learner.builder()
                        .user(user)
                        .currentLevel("N5")
                        .targetLevel("N3")
                        .updatedAt(OffsetDateTime.now())
                        .build();
                learnerRepository.save(learner);
            }
        }

        // Cập nhật trạng thái nếu có
        if (request.getStatus() != null && !request.getStatus().isBlank()) {
            user.setAccountStatus(mapStringToStatus(request.getStatus()));
        }

        user = userRepository.save(user);
        log.info("[AdminUser] Quản trị viên cập nhật thành công tài khoản ID: {}", userId);

        return toDTO(user, currentAdmin);
    }

    @Override
    @Transactional
    public UserDTO updateStatus(UUID userId, String newStatus, User currentAdmin) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng với ID: " + userId));

        // Ngăn chặn tự khóa chính mình
        if (currentAdmin != null && user.getUserId().equals(currentAdmin.getUserId())) {
            throw new BadRequestException("Bạn không thể tự thay đổi trạng thái hoặc khóa tài khoản của chính mình.");
        }

        UserAccountStatus mappedStatus = mapStringToStatus(newStatus);
        user.setAccountStatus(mappedStatus);

        user = userRepository.save(user);
        log.info("[AdminUser] Đã chuyển trạng thái user [{}] sang [{}]", user.getEmail(), mappedStatus);

        return toDTO(user, currentAdmin);
    }

    @Override
    @Transactional
    public void deleteUser(UUID userId, User currentAdmin) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng với ID: " + userId));

        // Ngăn chặn tự xóa tài khoản của chính mình
        if (currentAdmin != null && user.getUserId().equals(currentAdmin.getUserId())) {
            throw new BadRequestException("Bạn không thể tự xóa tài khoản Quản trị viên của chính mình.");
        }

        // Thực hiện Xóa Mềm (Soft Delete)
        user.setDeletedAt(OffsetDateTime.now());
        user.setAccountStatus(UserAccountStatus.SUSPENDED);
        userRepository.save(user);

        log.info("[AdminUser] Đã xóa mềm tài khoản [{}] bởi [{}]",
                user.getEmail(), currentAdmin != null ? currentAdmin.getEmail() : "SYSTEM");
    }

    @Override
    @Transactional
    public UserDTO resendCredentials(UUID userId, User currentAdmin) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng với ID: " + userId));

        if (user.getDeletedAt() != null) {
            throw new BadRequestException("Không thể gửi thông tin cho tài khoản đã bị xóa.");
        }

        // 1. Sinh mật khẩu tạm thời ngẫu nhiên an toàn 10-12 ký tự
        String newTempPassword = generateSecureTemporaryPassword();

        // 2. Cập nhật mật khẩu băm mới và cờ must_change_password = true
        user.setPasswordHash(passwordEncoder.encode(newTempPassword));
        user.setMustChangePassword(true);
        userRepository.save(user);

        // 3. Gửi email thông báo qua Gmail (@Async)
        String roleName = user.getRole() != null ? user.getRole().getRoleName() : "ROLE_LEARNER";
        RoleEnum roleEnum = RoleEnum.fromString(roleName);
        String roleBadge = (roleEnum != null) ? roleEnum.getBadgeText() : roleName;

        emailService.sendStaffAccountCredentialsEmail(
                user.getEmail(),
                user.getFullName(),
                newTempPassword,
                roleBadge
        );

        log.info("[ADMIN] Quản trị viên [{}] đã kích hoạt gửi lại thông tin đăng nhập cho [{}]",
                currentAdmin != null ? currentAdmin.getEmail() : "SYSTEM", user.getEmail());

        return toDTO(user, currentAdmin);
    }

    private String generateSecureTemporaryPassword() {
        String upper = "ABCDEFGHJKLMNPQRSTUVWXYZ";
        String lower = "abcdefghijkmnopqrstuvwxyz";
        String digits = "23456789";
        String specials = "@!#$";
        String all = upper + lower + digits + specials;

        java.security.SecureRandom random = new java.security.SecureRandom();
        StringBuilder sb = new StringBuilder();
        sb.append(upper.charAt(random.nextInt(upper.length())));
        sb.append(lower.charAt(random.nextInt(lower.length())));
        sb.append(digits.charAt(random.nextInt(digits.length())));
        sb.append(specials.charAt(random.nextInt(specials.length())));

        for (int i = sb.length(); i < 11; i++) {
            sb.append(all.charAt(random.nextInt(all.length())));
        }

        char[] chars = sb.toString().toCharArray();
        for (int i = chars.length - 1; i > 0; i--) {
            int j = random.nextInt(i + 1);
            char temp = chars[i];
            chars[i] = chars[j];
            chars[j] = temp;
        }
        return new String(chars);
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Long> getKpiStats() {
        long totalUsers = userRepository.countByDeletedAtIsNull();
        long internalStaff = userRepository.countByRoleNames(List.of(
                RoleEnum.ROLE_ADMIN.name(),
                RoleEnum.ROLE_MANAGER.name(),
                RoleEnum.ROLE_CS_STAFF.name(),
                RoleEnum.ROLE_ACADEMIC_OFFICER.name()
        ));
        long activeTutors = userRepository.countByRoleNameAndStatus(
                RoleEnum.ROLE_TUTOR.name(),
                UserAccountStatus.ACTIVE
        );
        long lockedUsers = userRepository.countByAccountStatusIn(List.of(
                UserAccountStatus.SUSPENDED,
                UserAccountStatus.BANNED
        ));

        Map<String, Long> kpis = new LinkedHashMap<>();
        kpis.put("totalUsers", totalUsers);
        kpis.put("internalStaff", internalStaff);
        kpis.put("activeTutors", activeTutors);
        kpis.put("lockedUsers", lockedUsers);
        return kpis;
    }

    /**
     * Ánh xạ chuỗi trạng thái thành Enum PostgreSQL tương thích
     */
    private UserAccountStatus mapStringToStatus(String statusStr) {
        if (statusStr == null || statusStr.isBlank()) {
            return UserAccountStatus.ACTIVE;
        }
        String clean = statusStr.trim().toUpperCase();
        return switch (clean) {
            case "INACTIVE", "SUSPENDED" -> UserAccountStatus.SUSPENDED;
            case "BLOCKED", "BANNED" -> UserAccountStatus.BANNED;
            case "PENDING", "PENDING_VERIFICATION" -> UserAccountStatus.PENDING;
            default -> UserAccountStatus.ACTIVE;
        };
    }

    /**
     * Chuyển đổi User Entity sang UserDTO chuẩn
     */
    private UserDTO toDTO(User user, User currentAdmin) {
        String roleName = user.getRole() != null ? user.getRole().getRoleName() : "ROLE_LEARNER";
        RoleEnum roleEnum = RoleEnum.fromString(roleName);
        String roleBadge = (roleEnum != null) ? roleEnum.getBadgeText() : roleName;

        String statusStr = user.getAccountStatus() != null ? user.getAccountStatus().name() : "ACTIVE";
        String statusBadge = switch (statusStr) {
            case "SUSPENDED" -> "Tạm khóa";
            case "BANNED" -> "Đã cấm";
            case "PENDING" -> "Chờ kích hoạt";
            default -> "Đang hoạt động";
        };

        boolean isSelf = (currentAdmin != null && user.getUserId().equals(currentAdmin.getUserId()));

        return UserDTO.builder()
                .id(user.getUserId())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .phone(user.getPhone() != null && !user.getPhone().isBlank() ? user.getPhone() : "-")
                .role(roleName)
                .roleBadge(roleBadge)
                .status(statusStr)
                .statusBadge(statusBadge)
                .avatarUrl(user.getAvatarUrl())
                .avatarText(securityUtil.getCurrentUserAvatarText(user.getFullName()))
                .createdAt(user.getCreatedAt() != null ? user.getCreatedAt().format(DATE_TIME_FORMATTER) : "-")
                .updatedAt(user.getUpdatedAt() != null ? user.getUpdatedAt().format(DATE_TIME_FORMATTER) : "-")
                .isDeleted(user.getDeletedAt() != null)
                .isSelf(isSelf)
                .mustChangePassword(user.isMustChangePassword())
                .build();
    }
}
