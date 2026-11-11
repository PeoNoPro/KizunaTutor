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
import java.util.HashMap;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import com.kizuna.features.tutor_search.repository.TutorSearchRepository;
import com.kizuna.features.tutor_search.repository.TutorSearchSpecification;
import com.kizuna.features.tutor_search.service.TutorSearchService;
import com.kizuna.features.verification.entity.TutorVerificationStatus;
import com.kizuna.features.tutor_post.entity.TutorPost;
import com.kizuna.features.tutor_post.service.TutorPostService;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TutorSearchServiceImpl implements TutorSearchService {

    private static final ZoneId VN_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
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

        List<TutorSummaryResponseDTO> dtos = mapAll(pageResult.getContent(), false);

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
        List<Tutor> featured = tutorSearchRepository.findTop6ByIsFeaturedTrueOrderByAvgRatingDesc();
        Map<UUID, TutorPost> posts = tutorPostService.publishedPosts(featured.stream().map(Tutor::getTutorId).toList());
        // Chỉ gia sư có bài đăng đã duyệt và gói hội viên còn hạn
        List<Tutor> visible = featured.stream()
                .filter(t -> posts.containsKey(t.getTutorId())
                        && tutorPostService.hasActiveMembership(t.getTutorId(), today))
                .toList();
        return mapAll(visible, false, posts);
    }

    @Override
    public TutorSummaryResponseDTO getTutorDetail(UUID tutorId) {
        Tutor tutor = tutorSearchRepository.findById(tutorId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy thông tin gia sư với mã yêu cầu"));
        return mapAll(List.of(tutor), true).get(0);
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
        return mapAll(List.of(tutor), true).get(0);
    }

    // ------------------------------------------------------------------ nạp dữ liệu theo lô
    // DB ở xa (mỗi query ~110 ms) nên không query riêng cho từng gia sư: gom theo cả trang.

    /** Lịch của một gia sư: lịch rảnh (đã trừ lịch lớp), lịch lớp, buổi học thử đã đặt, ca đã khóa. */
    record TutorScheduleData(List<TutorAvailabilityDTO> availabilities,
                             List<TutorAvailabilityDTO> classSchedules,
                             List<TutorBookedSlotDTO> bookedSlots,
                             List<TutorAvailabilityDTO> lockedSlots) {
        static final TutorScheduleData EMPTY = new TutorScheduleData(List.of(), List.of(), List.of(), List.of());
    }

    record TutorTeachingStats(int studentCount, int lessonCount) {
        static final TutorTeachingStats EMPTY = new TutorTeachingStats(0, 0);
    }

    private List<TutorSummaryResponseDTO> mapAll(List<Tutor> tutors, boolean includeDetails) {
        return mapAll(tutors, includeDetails,
                tutorPostService.publishedPosts(tutors.stream().map(Tutor::getTutorId).toList()));
    }

    private List<TutorSummaryResponseDTO> mapAll(List<Tutor> tutors, boolean includeDetails, Map<UUID, TutorPost> posts) {
        if (tutors.isEmpty()) return new ArrayList<>();
        List<UUID> tutorIds = tutors.stream().map(Tutor::getTutorId).toList();
        Map<UUID, TutorScheduleData> schedules = loadScheduleData(tutorIds, includeDetails);
        Map<UUID, TutorTeachingStats> statsByTutor = loadTeachingStats(tutorIds);
        return tutors.stream()
                .map(t -> mapToSummaryDTO(t, includeDetails, schedules, statsByTutor, posts))
                .collect(Collectors.toList());
    }

    private static String dayName(int dow) {
        return switch (dow) {
            case 1 -> "Chủ nhật";
            case 2 -> "Thứ hai";
            case 3 -> "Thứ ba";
            case 4 -> "Thứ tư";
            case 5 -> "Thứ năm";
            case 6 -> "Thứ sáu";
            case 7 -> "Thứ bảy";
            default -> "Thứ " + dow;
        };
    }

    private static String hhmm(String time) {
        return time != null && time.length() >= 5 ? time.substring(0, 5) : time;
    }

    /**
     * Một query duy nhất (UNION ALL) lấy cho tất cả gia sư trong danh sách:
     *  AV = lịch rảnh đang bật, CS = lịch lớp chính thức đang mở (FE-06),
     *  và khi xem chi tiết: BK = buổi học thử đã đặt sắp tới, LK = ca hằng tuần đã khóa bởi kết nối trực tiếp.
     * Quy ước thứ: AV/LK dùng 1 = Chủ nhật ... 7 = Thứ Bảy; class_schedules dùng 1 = Thứ Hai ... 7 = Chủ nhật (đã quy đổi).
     */
    private Map<UUID, TutorScheduleData> loadScheduleData(List<UUID> tutorIds, boolean includeDetails) {
        Map<UUID, List<TutorAvailabilityDTO>> av = new HashMap<>();
        Map<UUID, List<TutorAvailabilityDTO>> cs = new HashMap<>();
        Map<UUID, List<TutorBookedSlotDTO>> bk = new HashMap<>();
        Map<UUID, List<TutorAvailabilityDTO>> lk = new HashMap<>();

        String in = String.join(",", Collections.nCopies(tutorIds.size(), "?"));
        StringBuilder sql = new StringBuilder()
            .append("SELECT 'AV' AS kind, a.tutor_id, a.availability_id::text AS item_id, a.day_of_week, ")
            .append("       a.start_time::text AS s, a.end_time::text AS e, NULL::text AS d, NULL::timestamptz AS ts ")
            .append("FROM tutor_availabilities a WHERE a.tutor_id IN (").append(in).append(") AND a.is_active = TRUE ")
            .append("UNION ALL ")
            .append("SELECT 'CS', ls.tutor_id, NULL, (c.day_of_week % 7) + 1, c.start_time::text, c.end_time::text, NULL, NULL ")
            .append("FROM class_schedules c JOIN learning_spaces ls ON ls.space_id = c.space_id ")
            .append("WHERE ls.tutor_id IN (").append(in).append(") AND ls.status IN ('ACTIVE', 'PAUSED') ");
        if (includeDetails) {
            sql.append("UNION ALL ")
               .append("SELECT 'BK', tb.tutor_id, NULL, NULL, ")
               .append("       to_char(tb.scheduled_start_time AT TIME ZONE 'Asia/Ho_Chi_Minh', 'HH24:MI'), ")
               .append("       to_char(tb.scheduled_end_time AT TIME ZONE 'Asia/Ho_Chi_Minh', 'HH24:MI'), ")
               .append("       to_char(tb.scheduled_start_time AT TIME ZONE 'Asia/Ho_Chi_Minh', 'DD/MM/YYYY'), tb.scheduled_start_time ")
               .append("FROM trial_bookings tb WHERE tb.tutor_id IN (").append(in).append(") ")
               .append("  AND tb.booking_status IN ('PENDING', 'CONFIRMED', 'RESCHEDULED') AND tb.scheduled_end_time > CURRENT_TIMESTAMP ")
               .append("UNION ALL ")
               // Ca được nhả khi cặp gia sư - học viên chỉ còn lớp đã kết thúc (cùng điều kiện với
               // ConnectionRequestSlotRepository.existsLockedSlotOverlap)
               .append("SELECT 'LK', cr.tutor_id, NULL, sl.day_of_week, sl.start_time::text, sl.end_time::text, NULL, NULL ")
               .append("FROM connection_request_slots sl JOIN connection_requests cr ON cr.request_id = sl.request_id ")
               .append("WHERE cr.tutor_id IN (").append(in).append(") AND cr.request_type = 'DIRECT' AND cr.status = 'ACCEPTED' ")
               .append("  AND (EXISTS (SELECT 1 FROM learning_spaces x WHERE x.tutor_id = cr.tutor_id AND x.learner_id = cr.learner_id ")
               .append("               AND x.status IN ('ACTIVE', 'PAUSED')) ")
               .append("       OR NOT EXISTS (SELECT 1 FROM learning_spaces x WHERE x.tutor_id = cr.tutor_id AND x.learner_id = cr.learner_id)) ");
        }
        sql.append("ORDER BY kind, tutor_id, ts NULLS FIRST, day_of_week, s");

        int branches = includeDetails ? 4 : 2;
        Object[] args = new Object[tutorIds.size() * branches];
        for (int b = 0; b < branches; b++) {
            for (int i = 0; i < tutorIds.size(); i++) {
                args[b * tutorIds.size() + i] = tutorIds.get(i);
            }
        }

        try {
            jdbcTemplate.query(sql.toString(), rs -> {
                UUID tutorId = rs.getObject("tutor_id", UUID.class);
                int dow = rs.getInt("day_of_week");
                String start = hhmm(rs.getString("s"));
                String end = hhmm(rs.getString("e"));
                switch (rs.getString("kind")) {
                    case "AV" -> av.computeIfAbsent(tutorId, k -> new ArrayList<>()).add(TutorAvailabilityDTO.builder()
                            .availabilityId(UUID.fromString(rs.getString("item_id")))
                            .dayOfWeek(dow).dayName(dayName(dow)).startTime(start).endTime(end).build());
                    case "CS" -> cs.computeIfAbsent(tutorId, k -> new ArrayList<>()).add(TutorAvailabilityDTO.builder()
                            .dayOfWeek(dow).dayName(dayName(dow)).startTime(start).endTime(end).isActive(true).build());
                    case "BK" -> bk.computeIfAbsent(tutorId, k -> new ArrayList<>()).add(TutorBookedSlotDTO.builder()
                            .date(rs.getString("d")).startTime(start).endTime(end).build());
                    case "LK" -> lk.computeIfAbsent(tutorId, k -> new ArrayList<>()).add(TutorAvailabilityDTO.builder()
                            .dayOfWeek(dow).startTime(start).endTime(end).isActive(true).build());
                    default -> { }
                }
            }, args);
        } catch (Exception e) {
            // giữ hành vi cũ: thiếu bảng / lỗi truy vấn thì coi như không có lịch
        }

        Map<UUID, TutorScheduleData> result = new HashMap<>();
        for (UUID id : tutorIds) {
            result.put(id, new TutorScheduleData(
                    av.getOrDefault(id, List.of()), cs.getOrDefault(id, List.of()),
                    bk.getOrDefault(id, List.of()), lk.getOrDefault(id, List.of())));
        }
        return result;
    }

    /**
     * Nạp theo lô cho cả trang:
     * - studentCount: số học viên duy nhất có lớp chính thức (learning_spaces: ACTIVE, PAUSED, COMPLETED).
     * - lessonCount: tổng buổi học thử đã hoàn thành (trial_bookings COMPLETED) cộng số buổi chính thức
     *   đã trôi qua theo lịch tuần (class_schedules) tính theo giờ Việt Nam.
     */
    private Map<UUID, TutorTeachingStats> loadTeachingStats(List<UUID> tutorIds) {
        Map<UUID, Integer> students = new HashMap<>();
        Map<UUID, Integer> lessons = new HashMap<>();
        String in = String.join(",", Collections.nCopies(tutorIds.size(), "?"));
        String sql = "SELECT 'ST' AS kind, ls.tutor_id, COUNT(DISTINCT ls.learner_id)::bigint AS cnt, "
                + "       NULL::timestamptz AS ls_created, NULL::timestamptz AS ls_updated, NULL::text AS ls_status, "
                + "       NULL::timestamptz AS cs_created, NULL::int AS dow, NULL::text AS st, NULL::text AS et "
                + "FROM learning_spaces ls "
                + "WHERE ls.tutor_id IN (" + in + ") AND ls.status IN ('ACTIVE', 'PAUSED', 'COMPLETED') "
                + "GROUP BY ls.tutor_id "
                + "UNION ALL "
                + "SELECT 'TR', tb.tutor_id, COUNT(*)::bigint, "
                + "       NULL::timestamptz, NULL::timestamptz, NULL::text, "
                + "       NULL::timestamptz, NULL::int, NULL::text, NULL::text "
                + "FROM trial_bookings tb "
                + "WHERE tb.tutor_id IN (" + in + ") AND tb.booking_status = 'COMPLETED' "
                + "GROUP BY tb.tutor_id "
                + "UNION ALL "
                + "SELECT 'CS', ls.tutor_id, 0::bigint, "
                + "       ls.created_at, ls.updated_at, ls.status::text, "
                + "       cs.created_at, cs.day_of_week::int, cs.start_time::text, cs.end_time::text "
                + "FROM class_schedules cs "
                + "JOIN learning_spaces ls ON ls.space_id = cs.space_id "
                + "WHERE ls.tutor_id IN (" + in + ") AND ls.status IN ('ACTIVE', 'PAUSED', 'COMPLETED')";

        Object[] args = new Object[tutorIds.size() * 3];
        for (int b = 0; b < 3; b++) {
            for (int i = 0; i < tutorIds.size(); i++) {
                args[b * tutorIds.size() + i] = tutorIds.get(i);
            }
        }

        ZonedDateTime now = ZonedDateTime.now(VN_ZONE);
        try {
            jdbcTemplate.query(sql, rs -> {
                UUID tutorId = rs.getObject("tutor_id", UUID.class);
                switch (rs.getString("kind")) {
                    case "ST" -> students.put(tutorId, Math.max(0, (int) rs.getLong("cnt")));
                    case "TR" -> lessons.merge(tutorId, Math.max(0, (int) rs.getLong("cnt")), Integer::sum);
                    case "CS" -> {
                        ZonedDateTime lsCreated = toVnZone(rs.getTimestamp("ls_created"));
                        ZonedDateTime csCreated = toVnZone(rs.getTimestamp("cs_created"));
                        ZonedDateTime lsUpdated = toVnZone(rs.getTimestamp("ls_updated"));
                        ZonedDateTime start = maxDateTime(lsCreated, csCreated);
                        String status = rs.getString("ls_status");
                        ZonedDateTime end = "ACTIVE".equals(status) || lsUpdated == null || lsUpdated.isAfter(now)
                                ? now : lsUpdated;
                        int count = countOccurredClassLessons(
                                rs.getInt("dow"),
                                parseLocalTime(rs.getString("st")),
                                parseLocalTime(rs.getString("et")),
                                start,
                                end);
                        if (count > 0) {
                            lessons.merge(tutorId, count, Integer::sum);
                        }
                    }
                    default -> { }
                }
            }, args);
        } catch (Exception e) {
            log.warn("Không thể tải thống kê học viên/buổi học: {}", e.getMessage(), e);
        }

        Map<UUID, TutorTeachingStats> result = new HashMap<>();
        for (UUID id : tutorIds) {
            result.put(id, new TutorTeachingStats(
                    students.getOrDefault(id, 0),
                    lessons.getOrDefault(id, 0)));
        }
        return result;
    }

    static int countOccurredClassLessons(int dayOfWeek, LocalTime startTime, LocalTime endTime,
                                         ZonedDateTime effectiveStart, ZonedDateTime effectiveEnd) {
        if (dayOfWeek < 1 || dayOfWeek > 7 || startTime == null || endTime == null
                || !endTime.isAfter(startTime) || effectiveStart == null || effectiveEnd == null) {
            return 0;
        }
        ZoneId zone = effectiveStart.getZone();
        ZonedDateTime endInZone = effectiveEnd.withZoneSameInstant(zone);
        if (!endInZone.isAfter(effectiveStart)) {
            return 0;
        }

        DayOfWeek targetDow = DayOfWeek.of(dayOfWeek);
        LocalDate firstDate = effectiveStart.toLocalDate().with(TemporalAdjusters.nextOrSame(targetDow));
        if (ZonedDateTime.of(firstDate, startTime, zone).isBefore(effectiveStart)) {
            firstDate = firstDate.plusWeeks(1);
        }

        LocalDate lastDate = endInZone.toLocalDate().with(TemporalAdjusters.previousOrSame(targetDow));
        if (ZonedDateTime.of(lastDate, endTime, zone).isAfter(endInZone)) {
            lastDate = lastDate.minusWeeks(1);
        }

        if (lastDate.isBefore(firstDate)) {
            return 0;
        }
        return (int) (ChronoUnit.WEEKS.between(firstDate, lastDate) + 1);
    }

    private static ZonedDateTime toVnZone(Timestamp ts) {
        return ts == null ? null : ts.toInstant().atZone(VN_ZONE);
    }

    private static ZonedDateTime maxDateTime(ZonedDateTime a, ZonedDateTime b) {
        if (a == null) return b;
        if (b == null) return a;
        return a.isAfter(b) ? a : b;
    }

    private static LocalTime parseLocalTime(String raw) {
        String formatted = hhmm(raw);
        if (formatted == null || formatted.length() < 5) {
            return null;
        }
        try {
            return LocalTime.parse(formatted);
        } catch (Exception e) {
            return null;
        }
    }

    private TutorSummaryResponseDTO mapToSummaryDTO(Tutor tutor, boolean includeDetails,
                                                    Map<UUID, TutorScheduleData> scheduleByTutor,
                                                    Map<UUID, TutorTeachingStats> statsByTutor,
                                                    Map<UUID, TutorPost> postsByTutor) {
        List<String> specialties = Collections.emptyList();
        if (tutor.getHeadline() != null && !tutor.getHeadline().trim().isEmpty()) {
            specialties = Arrays.stream(tutor.getHeadline().split("•|,"))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .collect(Collectors.toList());
        }

        List<TutorCredentialDTO> credentials = Collections.emptyList();
        // Lịch rảnh / lịch lớp / buổi học thử đã đặt / ca đã khóa: đã nạp theo lô cho cả trang (loadScheduleData)
        TutorScheduleData schedule = scheduleByTutor.getOrDefault(tutor.getTutorId(), TutorScheduleData.EMPTY);
        TutorTeachingStats teachingStats = statsByTutor.getOrDefault(tutor.getTutorId(), TutorTeachingStats.EMPTY);
        List<TutorAvailabilityDTO> availabilities = schedule.availabilities();
        List<TutorAvailabilityDTO> classSchedules = schedule.classSchedules();
        List<TutorBookedSlotDTO> bookedSlots = includeDetails ? schedule.bookedSlots() : Collections.emptyList();
        List<TutorAvailabilityDTO> lockedSlots = includeDetails ? schedule.lockedSlots() : Collections.emptyList();
        List<TutorReviewDTO> reviews = Collections.emptyList();

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
        }

        // Bài đăng đã duyệt (nếu có) thay cho phần giới thiệu, học phí, hình thức dạy mặc định
        TutorPost post = postsByTutor.get(tutor.getTutorId());
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
                .studentCount(teachingStats.studentCount())
                .lessonCount(teachingStats.lessonCount())
                .location(sanitizePublicLocation(tutor.getAddress()))
                .isFeatured(Boolean.TRUE.equals(tutor.getIsFeatured()))
                .isVerified(tutor.getVerificationStatus() == TutorVerificationStatus.APPROVED)
                .specialties(specialties)
                .teachingFormat(post != null ? TutorPostService.formatLabel(post.getTeachingFormat()) : "Online 1-1")
                .teachingFormatCode(post != null && post.getTeachingFormat() != null
                        ? post.getTeachingFormat().trim().toUpperCase() : null)
                .credentials(credentials)
                .availabilities(availabilities)
                .classSchedules(classSchedules)
                .bookedSlots(bookedSlots)
                .lockedSlots(lockedSlots)
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