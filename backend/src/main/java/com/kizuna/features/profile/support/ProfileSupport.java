package com.kizuna.features.profile.support;

import com.kizuna.core.entity.User;
import com.kizuna.core.exception.BadRequestException;
import com.kizuna.core.repository.UserRepository;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.Authentication;

import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Tiện ích dùng chung trong phân hệ hồ sơ (xác định người dùng hiện tại, kiểm tra SĐT, địa chỉ GB-15).
 */
public final class ProfileSupport {

    private static final Pattern GPS_COORDINATE_PATTERN =
            Pattern.compile("[-+]?\\d{1,3}\\.\\d{4,}");
    private static final Pattern DETAILED_STREET_PATTERN =
            Pattern.compile("(?iu)(?:^|[\\s,])(?:\\d+[A-Za-z]?(?:/\\d+)*\\s+\\S+|(?:số\\s*nhà|ngõ|ngách|hẻm|kiệt|căn\\s*hộ|tòa\\s*nhà|đường)\\s+\\S+|số\\s+\\d+|(?<!thành\\s)phố\\s+\\S+)");

    public static final String PHONE_TAKEN_MESSAGE =
            "Số điện thoại này đã được liên kết với một tài khoản khác trong hệ thống.";
    public static final String COARSE_ADDRESS_MESSAGE =
            "Địa chỉ công khai chỉ nên ghi Quận/Huyện hoặc khu vực chung, không ghi số nhà, tên đường hay tọa độ chi tiết.";

    private ProfileSupport() {
    }

    public static String emailOf(Authentication authentication) {
        if (authentication.getPrincipal() instanceof User user) {
            return user.getEmail();
        }
        return authentication.getName();
    }

    public static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    public static OffsetDateTime latest(OffsetDateTime a, OffsetDateTime b) {
        if (a == null) {
            return b;
        }
        return b != null && b.isAfter(a) ? b : a;
    }

    public static boolean isPhoneUsedByOther(UserRepository userRepository, String phone, UUID userId) {
        if (phone == null) {
            return false;
        }
        Specification<User> spec = (root, query, cb) -> cb.and(
                cb.equal(root.get("phone"), phone),
                cb.notEqual(root.get("userId"), userId),
                cb.isNull(root.get("deletedAt")));
        return userRepository.exists(spec);
    }

    public static boolean containsDetailedStreetOrGps(String addressDetail) {
        String clean = blankToNull(addressDetail);
        if (clean == null) {
            return false;
        }
        return GPS_COORDINATE_PATTERN.matcher(clean).find()
                || DETAILED_STREET_PATTERN.matcher(clean).find();
    }

    public static void validateCoarseAddress(String addressDetail) {
        if (containsDetailedStreetOrGps(addressDetail)) {
            throw new BadRequestException(COARSE_ADDRESS_MESSAGE);
        }
    }

    public static String joinAddress(String province, String addressDetail) {
        String cleanProvince = blankToNull(province);
        String cleanDetail = blankToNull(addressDetail);
        if (cleanProvince == null) {
            return cleanDetail;
        }
        return cleanDetail == null ? cleanProvince : cleanProvince + " - " + cleanDetail;
    }

    public record ParsedAddress(String province, String addressDetail, String fullAddress) {
    }

    public static ParsedAddress parseAddress(String rawAddress) {
        String clean = blankToNull(rawAddress);
        if (clean == null) {
            return new ParsedAddress(null, null, null);
        }
        if (clean.contains(" - ")) {
            String[] parts = clean.split("\\s+-\\s+", 2);
            String province = blankToNull(parts[0]);
            String detail = parts.length > 1 ? sanitizeDistrictSegment(parts[1]) : null;
            return new ParsedAddress(province, detail, joinAddress(province, detail));
        }
        List<String> commaParts = Arrays.stream(clean.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty() && !s.equalsIgnoreCase("Việt Nam") && !s.equalsIgnoreCase("Vietnam"))
                .toList();
        if (commaParts.isEmpty()) {
            return new ParsedAddress(null, null, null);
        }
        String province = commaParts.get(commaParts.size() - 1);
        String detail = commaParts.size() >= 2 ? sanitizeDistrictSegment(commaParts.get(commaParts.size() - 2)) : null;
        return new ParsedAddress(province, detail, joinAddress(province, detail));
    }

    private static String sanitizeDistrictSegment(String segment) {
        String clean = blankToNull(segment);
        if (clean == null || containsDetailedStreetOrGps(clean)) {
            return null;
        }
        return clean;
    }
}
