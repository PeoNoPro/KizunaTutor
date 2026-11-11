package com.kizuna.features.profile.mapper;

import com.kizuna.core.entity.Tutor;
import com.kizuna.core.entity.User;
import com.kizuna.features.profile.dto.request.UpdateTutorProfileRequest;
import com.kizuna.features.profile.dto.response.TutorProfileDetailDto;
import com.kizuna.features.profile.support.ProfileSupport;

/**
 * Chuyển đổi giữa User, Tutor và DTO hồ sơ giảng dạy của gia sư.
 */
public final class TutorProfileMapper {

    private TutorProfileMapper() {
    }

    public static TutorProfileDetailDto toResponse(User user, Tutor tutor) {
        ProfileSupport.ParsedAddress address = ProfileSupport.parseAddress(tutor == null ? null : tutor.getAddress());
        String phone = tutor != null && ProfileSupport.blankToNull(tutor.getPhone()) != null
                ? tutor.getPhone()
                : user.getPhone();
        return new TutorProfileDetailDto(
                user.getUserId(),
                tutor == null ? null : tutor.getTutorId(),
                tutor == null ? null : tutor.getTutorCode(),
                user.getEmail(),
                user.getFullName(),
                phone,
                user.getAvatarUrl(),
                tutor == null ? null : tutor.getHeadline(),
                tutor == null ? null : tutor.getBio(),
                tutor == null || tutor.getExperienceYears() == null ? null : tutor.getExperienceYears().intValue(),
                tutor == null ? null : tutor.getHourlyRate(),
                tutor == null ? null : tutor.getHighestJlptLevel(),
                address.province(),
                address.addressDetail(),
                address.fullAddress(),
                tutor == null ? null : tutor.getAvgRating(),
                tutor == null ? null : tutor.getTotalReviews(),
                tutor == null || tutor.getVerificationStatus() == null ? null : tutor.getVerificationStatus().name(),
                true,
                user.getCreatedAt(),
                ProfileSupport.latest(user.getUpdatedAt(), tutor == null ? null : tutor.getUpdatedAt())
        );
    }

    /** Ghi các trường được phép sửa đồng bộ trên users và tutors; email luôn chỉ đọc. */
    public static void apply(UpdateTutorProfileRequest request, User user, Tutor tutor) {
        String cleanPhone = ProfileSupport.blankToNull(request.phone());
        String cleanProvince = request.province().trim();
        String cleanDetail = ProfileSupport.blankToNull(request.addressDetail());

        user.setFullName(request.fullName().trim());
        user.setPhone(cleanPhone);
        user.setAvatarUrl(ProfileSupport.blankToNull(request.avatarUrl()));

        tutor.setPhone(cleanPhone);
        tutor.setHeadline(request.headline().trim());
        tutor.setBio(ProfileSupport.blankToNull(request.bio()));
        tutor.setExperienceYears(request.experienceYears() == null ? (short) 0 : request.experienceYears().shortValue());
        tutor.setHourlyRate(request.hourlyRate());
        tutor.setHighestJlptLevel(request.highestJlptLevel().trim());
        tutor.setAddress(ProfileSupport.joinAddress(cleanProvince, cleanDetail));
    }
}
