package com.kizuna.features.tutor_search.service.impl;

import com.kizuna.core.entity.Tutor;
import com.kizuna.features.tutor_search.dto.TutorSearchCriteriaDTO;
import com.kizuna.features.tutor_search.dto.TutorSearchPageResponse;
import com.kizuna.features.tutor_search.dto.TutorSummaryResponseDTO;
import com.kizuna.features.tutor_search.dto.TutorCredentialDTO;
import com.kizuna.features.tutor_search.dto.TutorAvailabilityDTO;
import com.kizuna.features.tutor_search.dto.TutorBookedSlotDTO;
import com.kizuna.features.tutor_search.dto.TutorReviewDTO;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import com.kizuna.features.tutor_search.repository.TutorSearchRepository;
import com.kizuna.features.tutor_search.repository.TutorSearchSpecification;
import com.kizuna.features.tutor_search.service.TutorSearchService;
import com.kizuna.features.verification.entity.TutorVerificationStatus;
import com.kizuna.features.tutor_post.entity.TutorPost;
import com.kizuna.features.tutor_post.service.TutorPostService;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Hiện thực nghiệp vụ tìm kiếm gia sư.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TutorSearchServiceImpl implements TutorSearchService {

    private static final Pattern PUBLIC_GPS_PATTERN =
            Pattern.compile("[-+]?\\d{1,3}\\.\\d{4,}");
    private static final Pattern PUBLIC_STREET_PATTERN =
            Pattern.compile("(?iu)(?:^|[\\s,])(?:\\d+[A-Za-z]?(?:/\\d+)*\\s+\\S+|(?:số\\s*nhà|ngõ|ngách|hẻm|kiệt|căn\\s*hộ|tòa\\s*nhà|đường)\\s+\\S+|số\\s+\\d+|(?<!thành\\s)phố\\s+\\S+)");

    private final TutorSearchRepository tutorSearchRepository;
    private final JdbcTemplate jdbcTemplate;
    private final TutorPostService tutorPostService;

    @Override
    public TutorSearchPageResponse searchTutors(TutorSearchCriteriaDTO criteria) {
        // 1. Xác định thứ tự sắp xếp theo quyết định đã chốt trong spec
        Sort sort;
        String sortBy = criteria.getSortBy() != null ? criteria.getSortBy() : "rating";
        switch (sortBy) {
            case "exp":
                sort = Sort.by(Sort.Direction.DESC, "experienceYears");
                break;
            case "price_asc":
                sort = Sort.by(Sort.Direction.ASC, "hourlyRate");
                break;
            case "price_desc":
                sort = Sort.by(Sort.Direction.DESC, "hourlyRate");
                break;
            case "newest":
                sort = Sort.by(Sort.Direction.DESC, "experienceYears");
                break;
            case "rating":
            default:
                sort = Sort.by(Sort.Direction.DESC, "avgRating")
                        .and(Sort.by(Sort.Direction.DESC, "totalReviews"));
                break;
        }

        Pageable pageable = PageRequest.of(Math.max(0, criteria.getPage()), Math.max(1, criteria.getSize()), sort);
        Specification<Tutor> spec = TutorSearchSpecification.build(criteria);
        Page<Tutor> pageResult = tutorSearchRepository.findAll(spec, pageable);

        List<TutorSummaryResponseDTO> dtos = pageResult.getContent().stream()
                .map(t -> mapToSummaryDTO(t, false))
                .collect(Collectors.toList());

        return TutorSearchPageResponse.builder()
                .content(dtos)
                .pageNumber(pageResult.getNumber())
                .pageSize(pageResult.getSize())
                .totalElements(pageResult.getTotalElements())
                .totalPages(pageResult.getTotalPages())
                .last(pageResult.isLast())
                .build();
    }

    @Override
    public List<TutorSummaryResponseDTO> getFeaturedTutors() {
        LocalDate today = LocalDate.now(TutorPostService.ZONE);
        return tutorSearchRepository.findTop6ByIsFeaturedTrueOrderByAvgRatingDesc().stream()
                // Chỉ gia sư có bài đăng đã duyệt và gói hội viên còn hạn
                .filter(t -> tutorPostService.publishedPost(t.getTutorId()).isPresent()
                        && tutorPostService.hasActiveMembership(t.getTutorId(), today))
                .map(t -> mapToSummaryDTO(t, false))
                .collect(Collectors.toList());
    }

    @Override
    public TutorSummaryResponseDTO getTutorDetail(UUID tutorId) {
        Tutor tutor = tutorSearchRepository.findById(tutorId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy thông tin gia sư với mã yêu cầu"));
        return mapToSummaryDTO(tutor, true);
    }

    private static final List<String> VIETNAM_34_PROVINCES = List.of(
        "An Giang", "Bắc Ninh", "Cà Mau", "Cao Bằng", "Cần Thơ", "Đà Nẵng",
        "Đắk Lắk", "Điện Biên", "Đồng Nai", "Đồng Tháp", "Gia Lai", "Hà Nội",
        "Hà Tĩnh", "Hải Phòng", "Huế", "Hưng Yên", "Khánh Hòa", "Lai Châu",
        "Lạng Sơn", "Lào Cai", "Lâm Đồng", "Nghệ An", "Ninh Bình", "Phú Thọ",
        "Quảng Ngãi", "Quảng Ninh", "Quảng Trị", "Sơn La", "Tây Ninh", "Thái Nguyên",
        "Thanh Hóa", "TP. Hồ Chí Minh", "Tuyên Quang", "Vĩnh Long"
    );

    @Override
    public List<String> getAvailableLocations() {
        return VIETNAM_34_PROVINCES;
    }

    private String sanitizePublicLocation(String address) {
        if (address == null || address.trim().isEmpty()) {
            return "Hà Nội, Việt Nam";
        }
        String clean = address.trim();
        if (clean.contains(" - ")) {
            String[] parts = clean.split("\\s+-\\s+", 2);
            String province = parts[0].trim();
            String district = parts.length > 1 ? parts[1].trim() : "";
            if (!district.isEmpty() && isCoarseAreaOnly(district)) {
                return province + " - " + district;
            }
            return province.isEmpty() ? "Hà Nội, Việt Nam" : province;
        }
        List<String> segments = Arrays.stream(clean.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty() && !s.equalsIgnoreCase("Việt Nam") && !s.equalsIgnoreCase("Vietnam"))
                .toList();
        if (segments.isEmpty()) {
            return "Hà Nội, Việt Nam";
        }
        String province = segments.get(segments.size() - 1);
        if (segments.size() >= 2) {
            String district = segments.get(segments.size() - 2);
            if (isCoarseAreaOnly(district)) {
                return province + " - " + district;
            }
        }
        return isCoarseAreaOnly(province) ? province : extractProvince(clean);
    }

    private boolean isCoarseAreaOnly(String segment) {
        return !PUBLIC_GPS_PATTERN.matcher(segment).find()
                && !PUBLIC_STREET_PATTERN.matcher(segment).find();
    }

    private String extractProvince(String address) {
        if (address == null || address.trim().isEmpty()) {
            return "Hà Nội";
        }
        String clean = address.trim();
        String[] parts = clean.split(",");
        if (parts.length >= 2) {
            String lastPart = parts[parts.length - 1].trim();
            if (lastPart.equalsIgnoreCase("Việt Nam") || lastPart.equalsIgnoreCase("Vietnam")) {
                return parts[parts.length - 2].trim();
            }
            return lastPart;
        }
        return clean;
    }

    @Override
    public TutorSummaryResponseDTO getTutorByCode(Long tutorCode) {
        Tutor tutor = tutorSearchRepository.findByTutorCode(tutorCode)
                .orElse(null);
        if (tutor == null) {
            return null;
        }
        return mapToSummaryDTO(tutor, true);
    }

    private TutorSummaryResponseDTO mapToSummaryDTO(Tutor tutor, boolean includeDetails) {
        List<String> specialties = Collections.emptyList();
        if (tutor.getHeadline() != null && !tutor.getHeadline().trim().isEmpty()) {
            specialties = Arrays.stream(tutor.getHeadline().split("•|,"))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .collect(Collectors.toList());
        }

        List<TutorCredentialDTO> credentials = Collections.emptyList();
        List<TutorAvailabilityDTO> availabilities = Collections.emptyList();
        List<TutorAvailabilityDTO> classSchedules = Collections.emptyList();
        List<TutorBookedSlotDTO> bookedSlots = Collections.emptyList();
        List<TutorReviewDTO> reviews = Collections.emptyList();

        try {
            availabilities = jdbcTemplate.query(
                "SELECT availability_id, day_of_week, start_time::text, end_time::text " +
                "FROM tutor_availabilities WHERE tutor_id = ? AND is_active = TRUE ORDER BY day_of_week, start_time",
                (rs, rowNum) -> {
                    int dow = rs.getInt("day_of_week");
                    String dayName = switch (dow) {
                        case 1 -> "Chủ nhật";
                        case 2 -> "Thứ hai";
                        case 3 -> "Thứ ba";
                        case 4 -> "Thứ tư";
                        case 5 -> "Thứ năm";
                        case 6 -> "Thứ sáu";
                        case 7 -> "Thứ bảy";
                        default -> "Thứ " + dow;
                    };
                    String start = rs.getString("start_time");
                    String end = rs.getString("end_time");
                    if (start != null && start.length() >= 5) start = start.substring(0, 5);
                    if (end != null && end.length() >= 5) end = end.substring(0, 5);
                    return TutorAvailabilityDTO.builder()
                        .availabilityId(rs.getObject("availability_id", UUID.class))
                        .dayOfWeek(dow)
                        .dayName(dayName)
                        .startTime(start)
                        .endTime(end)
                        .build();
                },
                tutor.getTutorId()
            );
        } catch (Exception e) {
            // ignore
        }

        // Truy vấn lịch học chính thức các lớp đang mở của gia sư (FE-06 class_schedules)
        try {
            classSchedules = jdbcTemplate.query(
                "SELECT cs.day_of_week, cs.start_time::text, cs.end_time::text " +
                "FROM class_schedules cs " +
                "JOIN learning_spaces ls ON ls.space_id = cs.space_id " +
                "WHERE ls.tutor_id = ? AND ls.status IN ('ACTIVE', 'PAUSED') " +
                "ORDER BY cs.day_of_week, cs.start_time",
                (rs, rowNum) -> {
                    int csDow = rs.getInt("day_of_week");
                    // Chuyển cs.day_of_week (1 = Thứ Hai ... 7 = Chủ nhật) sang định dạng tutor_availabilities (1 = Chủ nhật, 2 = Thứ Hai ... 7 = Thứ Bảy)
                    int dow = (csDow % 7) + 1;
                    String dayName = switch (dow) {
                        case 1 -> "Chủ nhật";
                        case 2 -> "Thứ hai";
                        case 3 -> "Thứ ba";
                        case 4 -> "Thứ tư";
                        case 5 -> "Thứ năm";
                        case 6 -> "Thứ sáu";
                        case 7 -> "Thứ bảy";
                        default -> "Thứ " + dow;
                    };
                    String start = rs.getString("start_time");
                    String end = rs.getString("end_time");
                    if (start != null && start.length() >= 5) start = start.substring(0, 5);
                    if (end != null && end.length() >= 5) end = end.substring(0, 5);
                    return TutorAvailabilityDTO.builder()
                        .dayOfWeek(dow)
                        .dayName(dayName)
                        .startTime(start)
                        .endTime(end)
                        .isActive(true)
                        .build();
                },
                tutor.getTutorId()
            );
        } catch (Exception e) {
            // ignore nếu bảng chưa tạo
        }

        // Tự động loại trừ các khung giờ bị trùng với lịch học lớp học chính thức
        if (!classSchedules.isEmpty() && !availabilities.isEmpty()) {
            Map<Integer, List<TimeInterval>> busyByDow = classSchedules.stream()
                .collect(Collectors.groupingBy(
                    TutorAvailabilityDTO::getDayOfWeek,
                    Collectors.mapping(cs -> new TimeInterval(parseMinutes(cs.getStartTime()), parseMinutes(cs.getEndTime())), Collectors.toList())
                ));

            List<TutorAvailabilityDTO> filtered = new ArrayList<>();
            for (TutorAvailabilityDTO av : availabilities) {
                List<TimeInterval> busyList = busyByDow.getOrDefault(av.getDayOfWeek(), Collections.emptyList());
                if (busyList.isEmpty()) {
                    filtered.add(av);
                } else {
                    List<TimeInterval> init = List.of(new TimeInterval(parseMinutes(av.getStartTime()), parseMinutes(av.getEndTime())));
                    List<TimeInterval> rem = subtractIntervals(init, busyList, 30);
                    for (TimeInterval interval : rem) {
                        filtered.add(TutorAvailabilityDTO.builder()
                            .availabilityId(av.getAvailabilityId())
                            .dayOfWeek(av.getDayOfWeek())
                            .dayName(av.getDayName())
                            .startTime(formatMinutes(interval.startMinutes()))
                            .endTime(formatMinutes(interval.endMinutes()))
                            .isActive(av.getIsActive())
                            .build());
                    }
                }
            }
            availabilities = filtered;
        }

        if (includeDetails) {
            try {
                credentials = jdbcTemplate.query(
                    "SELECT credential_id, credential_type, title, file_url, verified_status::text " +
                    "FROM tutor_credentials WHERE tutor_id = ? AND verified_status = 'VERIFIED' ORDER BY uploaded_at ASC",
                    (rs, rowNum) -> TutorCredentialDTO.builder()
                        .credentialId(rs.getObject("credential_id", UUID.class))
                        .credentialType(rs.getString("credential_type"))
                        .title(rs.getString("title"))
                        .fileUrl(rs.getString("file_url"))
                        .verifiedStatus(rs.getString("verified_status"))
                        .build(),
                    tutor.getTutorId()
                );
            } catch (Exception e) {
                // ignore if table not present or error
            }

            try {
                reviews = jdbcTemplate.query(
                    "SELECT tr.review_id, tr.rating, tr.comment, tr.created_at, " +
                    "COALESCE(u.full_name, 'Học viên ẩn danh') AS learner_name, " +
                    "u.avatar_url AS learner_avatar_url " +
                    "FROM tutor_reviews tr " +
                    "LEFT JOIN learners l ON tr.learner_id = l.learner_id " +
                    "LEFT JOIN users u ON l.user_id = u.user_id " +
                    "WHERE tr.tutor_id = ? " +
                    "ORDER BY tr.created_at DESC",
                    (rs, rowNum) -> {
                        Timestamp ts = rs.getTimestamp("created_at");
                        String dateStr = ts != null ? new SimpleDateFormat("dd/MM/yyyy").format(ts) : "Mới đây";
                        String comment = rs.getString("comment");
                        String course = "Học viên lớp Kaiwa N2 - 24 buổi học";
                        String tag = "all";
                        if (comment != null) {
                            if (comment.contains("Dokkai") || comment.contains("N1") || comment.contains("JLPT")) {
                                course = "Học viên JLPT N1 cấp tốc - 16 buổi học";
                                tag = "jlpt";
                            } else if (comment.contains("IT") || comment.contains("Doanh nghiệp")) {
                                course = "Học viên Tiếng Nhật Doanh nghiệp IT - 12 buổi học";
                                tag = "business";
                            } else {
                                course = "Học viên lớp Kaiwa N2 - 24 buổi học";
                                tag = "kaiwa";
                            }
                        }
                        return TutorReviewDTO.builder()
                            .reviewId(rs.getObject("review_id", UUID.class))
                            .learnerName(rs.getString("learner_name"))
                            .learnerAvatarUrl(rs.getString("learner_avatar_url"))
                            .courseName(course)
                            .rating(rs.getBigDecimal("rating"))
                            .comment(comment)
                            .createdAt(dateStr)
                            .tag(tag)
                            .build();
                    },
                    tutor.getTutorId()
                );
            } catch (Exception e) {
                // ignore
            }
            try {
                bookedSlots = jdbcTemplate.query(
                    "SELECT to_char(tb.scheduled_start_time AT TIME ZONE 'Asia/Ho_Chi_Minh', 'DD/MM/YYYY') AS date_str, " +
                    "to_char(tb.scheduled_start_time AT TIME ZONE 'Asia/Ho_Chi_Minh', 'HH24:MI') AS start_str, " +
                    "to_char(tb.scheduled_end_time AT TIME ZONE 'Asia/Ho_Chi_Minh', 'HH24:MI') AS end_str " +
                    "FROM trial_bookings tb " +
                    "WHERE tb.tutor_id = ? " +
                    "  AND tb.booking_status IN ('PENDING', 'CONFIRMED', 'RESCHEDULED') " +
                    "  AND tb.scheduled_end_time > CURRENT_TIMESTAMP " +
                    "ORDER BY tb.scheduled_start_time ASC",
                    (rs, rowNum) -> TutorBookedSlotDTO.builder()
                        .date(rs.getString("date_str"))
                        .startTime(rs.getString("start_str"))
                        .endTime(rs.getString("end_str"))
                        .build(),
                    tutor.getTutorId()
                );
            } catch (Exception e) {
                // ignore
            }
        }

        // Bài đăng đã duyệt (nếu có) thay cho phần giới thiệu, học phí, hình thức dạy mặc định
        TutorPost post = tutorPostService.publishedPost(tutor.getTutorId()).orElse(null);
        if (post != null && post.getSpecialties() != null && !post.getSpecialties().isBlank()) {
            specialties = Arrays.stream(post.getSpecialties().split("•|,"))
                    .map(String::trim).filter(s -> !s.isEmpty()).collect(Collectors.toList());
        }

        return TutorSummaryResponseDTO.builder()
                .postTitle(post != null ? post.getTitle() : null)
                .tutorId(tutor.getTutorId())
                .tutorCode(tutor.getTutorCode())
                .fullName(tutor.getUser() != null ? tutor.getUser().getFullName() : "Gia sư Kizuna")
                .avatarUrl(tutor.getUser() != null ? tutor.getUser().getAvatarUrl() : null)
                .headline(tutor.getHeadline())
                .bio(post != null ? post.getContent() : tutor.getBio())
                .highestJlptLevel(tutor.getHighestJlptLevel() != null ? tutor.getHighestJlptLevel() : "N1")
                .experienceYears(tutor.getExperienceYears())
                .hourlyRate(post != null ? post.getHourlyRate() : tutor.getHourlyRate())
                .avgRating(tutor.getAvgRating())
                .totalReviews(tutor.getTotalReviews())
                .location(sanitizePublicLocation(tutor.getAddress()))
                .isFeatured(Boolean.TRUE.equals(tutor.getIsFeatured()))
                .isVerified(tutor.getVerificationStatus() == TutorVerificationStatus.APPROVED)
                .specialties(specialties)
                .teachingFormat(post != null ? TutorPostService.formatLabel(post.getTeachingFormat()) : "Online 1-1")
                .credentials(credentials)
                .availabilities(availabilities)
                .classSchedules(classSchedules)
                .bookedSlots(bookedSlots)
                .reviews(reviews)
                .build();
    }

    public record TimeInterval(int startMinutes, int endMinutes) {
        public boolean isValid(int minDuration) {
            return (endMinutes - startMinutes) >= minDuration;
        }
    }

    public static int parseMinutes(String timeStr) {
        if (timeStr == null || !timeStr.contains(":")) return 0;
        String[] parts = timeStr.trim().split(":");
        return Integer.parseInt(parts[0]) * 60 + Integer.parseInt(parts[1]);
    }

    public static String formatMinutes(int minutes) {
        int h = minutes / 60;
        int m = minutes % 60;
        return String.format("%02d:%02d", h, m);
    }

    public static List<TimeInterval> subtractIntervals(List<TimeInterval> available, List<TimeInterval> busy, int minDuration) {
        List<TimeInterval> current = new ArrayList<>(available);
        for (TimeInterval b : busy) {
            List<TimeInterval> next = new ArrayList<>();
            for (TimeInterval a : current) {
                if (b.endMinutes <= a.startMinutes || b.startMinutes >= a.endMinutes) {
                    next.add(a);
                } else {
                    if (a.startMinutes < b.startMinutes) {
                        next.add(new TimeInterval(a.startMinutes, b.startMinutes));
                    }
                    if (a.endMinutes > b.endMinutes) {
                        next.add(new TimeInterval(b.endMinutes, a.endMinutes));
                    }
                }
            }
            current = next;
        }
        return current.stream().filter(i -> i.isValid(minDuration)).toList();
    }
}