package com.kizuna.features.profile.mapper;

import com.kizuna.core.entity.Learner;
import com.kizuna.core.entity.RoleEnum;
import com.kizuna.core.entity.Tutor;
import com.kizuna.core.entity.User;
import com.kizuna.features.profile.dto.response.UserProfileDetailDto;
import com.kizuna.features.profile.dto.response.UserProfileListItemDto;
import com.kizuna.features.profile.support.ProfileSupport;

import java.util.List;

/**
 * Chuyển đổi User, Learner, Tutor sang DTO tra cứu và chi tiết người dùng.
 */
public final class UserProfileMapper {

    private UserProfileMapper() {
    }

    public static UserProfileListItemDto toListItem(User user) {
        String roleName = roleNameOf(user);
        return new UserProfileListItemDto(
                user.getUserId(),
                user.getEmail(),
                user.getFullName(),
                user.getPhone(),
                user.getAvatarUrl(),
                roleName,
                roleBadgeOf(roleName),
                user.getAccountStatus(),
                user.getCreatedAt()
        );
    }

    public static UserProfileDetailDto toDetail(User user,
                                                Learner learner,
                                                Tutor tutor,
                                                List<UserProfileDetailDto.StatusHistoryItem> history) {
        String roleName = roleNameOf(user);
        UserProfileDetailDto.LearnerDetails learnerDetails = learner == null ? null : new UserProfileDetailDto.LearnerDetails(
                learner.getCurrentLevel(),
                learner.getTargetLevel(),
                learner.getLearningGoals()
        );
        UserProfileDetailDto.TutorDetails tutorDetails = null;
        if (tutor != null) {
            ProfileSupport.ParsedAddress address = ProfileSupport.parseAddress(tutor.getAddress());
            tutorDetails = new UserProfileDetailDto.TutorDetails(
                    tutor.getTutorId(),
                    tutor.getTutorCode(),
                    tutor.getHeadline(),
                    tutor.getBio(),
                    tutor.getExperienceYears() == null ? null : tutor.getExperienceYears().intValue(),
                    tutor.getHourlyRate(),
                    tutor.getHighestJlptLevel(),
                    address.fullAddress(),
                    tutor.getAvgRating(),
                    tutor.getTotalReviews(),
                    tutor.getVerificationStatus() == null ? null : tutor.getVerificationStatus().name()
            );
        }
        return new UserProfileDetailDto(
                user.getUserId(),
                user.getEmail(),
                user.getFullName(),
                user.getPhone(),
                user.getAvatarUrl(),
                roleName,
                roleBadgeOf(roleName),
                user.getAccountStatus(),
                user.getCreatedAt(),
                user.getUpdatedAt(),
                learnerDetails,
                tutorDetails,
                history == null ? List.of() : history
        );
    }

    public static String roleNameOf(User user) {
        return user.getRole() != null ? user.getRole().getRoleName() : null;
    }

    public static String roleBadgeOf(String roleName) {
        if ("ROLE_TUTOR".equals(roleName)) {
            return "Gia sư";
        }
        if ("ROLE_LEARNER".equals(roleName)) {
            return "Học viên";
        }
        if (roleName == null) {
            return "Người dùng";
        }
        try {
            return RoleEnum.fromString(roleName).getBadgeText();
        } catch (IllegalArgumentException ignored) {
            return "Người dùng";
        }
    }
}
