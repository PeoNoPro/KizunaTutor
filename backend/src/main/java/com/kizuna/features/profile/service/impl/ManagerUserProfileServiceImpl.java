package com.kizuna.features.profile.service.impl;

import com.kizuna.core.entity.Learner;
import com.kizuna.core.entity.Role;
import com.kizuna.core.entity.Tutor;
import com.kizuna.core.entity.User;
import com.kizuna.core.entity.UserAccountStatus;
import com.kizuna.core.exception.BadRequestException;
import com.kizuna.core.exception.ResourceNotFoundException;
import com.kizuna.core.repository.LearnerRepository;
import com.kizuna.core.repository.TutorRepository;
import com.kizuna.core.repository.UserRepository;
import com.kizuna.features.profile.dto.request.UpdateUserStatusRequest;
import com.kizuna.features.profile.dto.response.PageResponse;
import com.kizuna.features.profile.dto.response.UserProfileDetailDto;
import com.kizuna.features.profile.dto.response.UserProfileListItemDto;
import com.kizuna.features.profile.dto.response.UserStatusUpdateResponseDto;
import com.kizuna.features.profile.mapper.UserProfileMapper;
import com.kizuna.features.profile.repository.ProfileAuditLogRepository;
import com.kizuna.features.profile.service.ManagerUserProfileService;
import com.kizuna.features.profile.support.ProfileSupport;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ManagerUserProfileServiceImpl implements ManagerUserProfileService {

    private static final String LEARNER_ROLE = "ROLE_LEARNER";
    private static final String TUTOR_ROLE = "ROLE_TUTOR";
    private static final Set<String> MANAGED_ROLES = Set.of(LEARNER_ROLE, TUTOR_ROLE);
    private static final Set<UserAccountStatus> ALLOWED_FILTER_STATUSES =
            EnumSet.of(UserAccountStatus.ACTIVE, UserAccountStatus.SUSPENDED, UserAccountStatus.BANNED);
    private static final Set<UserAccountStatus> ALLOWED_TARGET_STATUSES =
            EnumSet.of(UserAccountStatus.ACTIVE, UserAccountStatus.SUSPENDED, UserAccountStatus.BANNED);

    private final UserRepository userRepository;
    private final LearnerRepository learnerRepository;
    private final TutorRepository tutorRepository;
    private final ProfileAuditLogRepository profileAuditLogRepository;

    @Override
    @Transactional(readOnly = true)
    public PageResponse<UserProfileListItemDto> getUsers(String role, String status, String keyword, int page, int size) {
        List<String> targetRoles = resolveRoleFilter(role);
        UserAccountStatus statusFilter = resolveStatusFilter(status);
        String keywordFilter = ProfileSupport.blankToNull(keyword);

        int safePage = Math.max(0, page);
        int safeSize = Math.min(Math.max(size, 1), 50);
        Pageable pageable = PageRequest.of(safePage, safeSize, Sort.by(Sort.Direction.DESC, "createdAt"));

        Specification<User> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.isNull(root.get("deletedAt")));

            Join<User, Role> roleJoin = root.join("role");
            predicates.add(roleJoin.get("roleName").in(targetRoles));

            if (statusFilter != null) {
                predicates.add(cb.equal(root.get("accountStatus"), statusFilter));
            }
            if (keywordFilter != null) {
                String pattern = "%" + keywordFilter.toLowerCase(Locale.ROOT) + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("fullName")), pattern),
                        cb.like(cb.lower(root.get("email")), pattern)
                ));
            }
            return cb.and(predicates.toArray(Predicate[]::new));
        };

        Page<UserProfileListItemDto> result = userRepository.findAll(spec, pageable)
                .map(UserProfileMapper::toListItem);
        return PageResponse.from(result);
    }

    @Override
    @Transactional(readOnly = true)
    public UserProfileDetailDto getUserDetail(UUID userId) {
        User target = findManagedUser(userId);
        String roleName = UserProfileMapper.roleNameOf(target);

        Learner learner = LEARNER_ROLE.equals(roleName)
                ? learnerRepository.findByUser_UserId(target.getUserId()).orElse(null)
                : null;
        Tutor tutor = TUTOR_ROLE.equals(roleName)
                ? tutorRepository.findByUser_UserId(target.getUserId()).orElse(null)
                : null;

        List<UserProfileDetailDto.StatusHistoryItem> history =
                profileAuditLogRepository.findUserStatusHistory(target.getUserId());

        return UserProfileMapper.toDetail(target, learner, tutor, history);
    }

    @Override
    @Transactional
    public UserStatusUpdateResponseDto updateUserStatus(UUID userId,
                                                        UpdateUserStatusRequest request,
                                                        String actorEmail,
                                                        String ipAddress) {
        User target = findManagedUser(userId);

        if (request.status() == null || !ALLOWED_TARGET_STATUSES.contains(request.status())) {
            throw new BadRequestException("Chỉ cho phép chuyển sang trạng thái Hoạt động, Tạm khóa hoặc Cấm vĩnh viễn.");
        }

        String cleanReason = ProfileSupport.blankToNull(request.reason());
        if (cleanReason == null || cleanReason.length() < 5 || cleanReason.length() > 500) {
            throw new BadRequestException("Lý do thay đổi trạng thái phải từ 5 đến 500 ký tự.");
        }

        User actor = actorEmail == null ? null : userRepository.findByEmail(actorEmail).orElse(null);
        if (actor != null && actor.getUserId().equals(target.getUserId())) {
            throw new AccessDeniedException("Bạn không thể tự thay đổi trạng thái tài khoản của chính mình.");
        }

        UserAccountStatus previousStatus = target.getAccountStatus();
        target.setAccountStatus(request.status());
        User saved = userRepository.saveAndFlush(target);

        profileAuditLogRepository.recordStatusChange(
                actor == null ? null : actor.getUserId(),
                saved.getUserId(),
                previousStatus,
                saved.getAccountStatus(),
                cleanReason,
                ipAddress
        );

        OffsetDateTime updatedAt = saved.getUpdatedAt() != null ? saved.getUpdatedAt() : OffsetDateTime.now();
        return new UserStatusUpdateResponseDto(saved.getUserId(), saved.getAccountStatus(), cleanReason, updatedAt);
    }

    private User findManagedUser(UUID userId) {
        User user = userRepository.findById(userId)
                .filter(u -> u.getDeletedAt() == null)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy tài khoản người dùng."));
        String roleName = UserProfileMapper.roleNameOf(user);
        if (roleName == null || !MANAGED_ROLES.contains(roleName)) {
            throw new AccessDeniedException("Bạn chỉ có quyền quản lý tài khoản Học viên và Gia sư.");
        }
        return user;
    }

    private static List<String> resolveRoleFilter(String role) {
        String clean = ProfileSupport.blankToNull(role);
        if (clean == null || "ALL".equalsIgnoreCase(clean)) {
            return List.of(LEARNER_ROLE, TUTOR_ROLE);
        }
        String normalized = clean.toUpperCase(Locale.ROOT);
        if (MANAGED_ROLES.contains(normalized)) {
            return List.of(normalized);
        }
        throw new BadRequestException("Bộ lọc vai trò chỉ hỗ trợ Học viên hoặc Gia sư.");
    }

    private static UserAccountStatus resolveStatusFilter(String status) {
        String clean = ProfileSupport.blankToNull(status);
        if (clean == null || "ALL".equalsIgnoreCase(clean)) {
            return null;
        }
        try {
            UserAccountStatus parsed = UserAccountStatus.valueOf(clean.toUpperCase(Locale.ROOT));
            if (ALLOWED_FILTER_STATUSES.contains(parsed)) {
                return parsed;
            }
        } catch (IllegalArgumentException ignored) {
            // Ném thông báo tiếng Việt bên dưới.
        }
        throw new BadRequestException("Bộ lọc trạng thái tài khoản không hợp lệ.");
    }
}
