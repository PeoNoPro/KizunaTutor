package com.kizuna.features.tutor_search.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.FileInputStream;
import java.math.BigDecimal;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.function.Predicate;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Integration test FE-05 Tutor Discovery & Search, chạy trên backend thật (mặc định http://localhost:8085)
 * và CSDL thật. CSDL chỉ được đọc (SELECT) để làm đáp án độc lập, không ghi gì.
 * Không chạy cùng "mvn test" (tên kết thúc bằng IT); chạy riêng:
 *   mvnw test -Dtest=TutorDiscoveryLiveIT -Dsurefire.failIfNoSpecifiedTests=false
 * Backend không chạy thì toàn bộ test được bỏ qua.
 */
class TutorDiscoveryLiveIT {

    private static final String BASE = System.getProperty("kizuna.baseUrl", "http://localhost:8085");
    private static final ZoneId VN = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final HttpClient HTTP = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();

    /** Gia sư được phép hiển thị theo SRS UC-05.01 bước 3, kèm bài đăng đang hiển thị (PUBLISHED duyệt gần nhất). */
    private static final String VISIBLE_SQL = """
            WITH v AS (
              SELECT t.tutor_id, t.highest_jlpt_level AS lvl, t.hourly_rate AS profile_rate, t.address,
                     (SELECT p.post_id FROM tutor_posts p WHERE p.tutor_id = t.tutor_id AND p.status::text = 'PUBLISHED'
                      ORDER BY p.reviewed_at DESC NULLS LAST LIMIT 1) AS post_id
              FROM tutors t JOIN users u ON u.user_id = t.user_id
              WHERE t.verification_status::text = 'APPROVED' AND u.account_status::text = 'ACTIVE' AND u.deleted_at IS NULL
                AND EXISTS (SELECT 1 FROM tutor_posts p WHERE p.tutor_id = t.tutor_id AND p.status::text = 'PUBLISHED')
                AND EXISTS (SELECT 1 FROM tutor_memberships m WHERE m.tutor_id = t.tutor_id AND m.status::text = 'ACTIVE'
                            AND m.end_date >= (now() AT TIME ZONE 'Asia/Ho_Chi_Minh')::date))
            SELECT v.tutor_id::text AS id, v.lvl, v.address, COALESCE(p.hourly_rate, v.profile_rate) AS price,
                   upper(COALESCE(p.teaching_format, '')) AS fmt, COALESCE(p.price_items::text, '') AS price_items
            FROM v LEFT JOIN tutor_posts p ON p.post_id = v.post_id""";

    private record Row(String id, String level, String address, BigDecimal price, String format, String priceItems) { }

    private static Connection db;
    private static List<Row> visible;
    private static List<JsonNode> allFromApi;

    @BeforeAll
    static void connect() throws Exception {
        assumeTrue(backendUp(), "Backend không chạy ở " + BASE + " - bỏ qua integration test");
        Properties p = new Properties();
        try (var in = new FileInputStream("src/main/resources/application.properties")) {
            p.load(in);
        }
        db = DriverManager.getConnection(p.getProperty("spring.datasource.url"),
                p.getProperty("spring.datasource.username"), p.getProperty("spring.datasource.password"));
        db.setReadOnly(true);
        visible = new ArrayList<>();
        try (ResultSet rs = db.createStatement().executeQuery(VISIBLE_SQL)) {
            while (rs.next()) {
                visible.add(new Row(rs.getString("id"), rs.getString("lvl"), rs.getString("address"),
                        rs.getBigDecimal("price"), rs.getString("fmt"), rs.getString("price_items")));
            }
        }
        allFromApi = search("");
        assumeTrue(!visible.isEmpty(), "CSDL không có gia sư hiển thị nào");
    }

    @AfterAll
    static void close() throws SQLException {
        if (db != null) db.close();
    }

    // ===== UC-05.01 Search Tutors by Keyword =====

    @Test @DisplayName("TC-05-001: Khách chưa đăng nhập tìm kiếm -> 200, có kết quả, không cần token")
    void tc05_001_guestSearch() throws Exception {
        String name = allFromApi.get(0).path("fullName").asText();
        String keyword = name.split("\\s+")[0];
        HttpResponse<String> res = get("/api/tutors/search?keyword=" + enc(keyword));
        assertEquals(200, res.statusCode());
        assertTrue(JSON.readTree(res.body()).path("totalElements").asLong() > 0);
    }

    @Test @DisplayName("TC-05-002: Từ khóa khớp họ tên / headline / trình độ JLPT / địa chỉ -> gia sư đó có trong kết quả")
    void tc05_002_keywordFields() throws Exception {
        JsonNode t = allFromApi.get(0);
        String id = t.path("tutorId").asText();
        Row row = row(id);
        List<String> keywords = new ArrayList<>(List.of(t.path("fullName").asText().split("\\s+")[0],
                t.path("headline").asText().trim().split("\\s+")[0]));
        if (row.level() != null) keywords.add(row.level());
        if (row.address() != null) keywords.add(row.address().split("\\s+-\\s+")[0]);
        for (String k : keywords) {
            assertTrue(ids(search("keyword=" + enc(k))).contains(id), "Từ khóa '" + k + "' không trả về gia sư " + id);
        }
    }

    @Test @DisplayName("TC-05-005: Gia sư chưa được duyệt hồ sơ -> không có trong kết quả")
    void tc05_005_notApprovedHidden() throws Exception {
        assertHidden("SELECT tutor_id::text FROM tutors WHERE verification_status::text <> 'APPROVED'");
    }

    @Test @DisplayName("TC-05-006: Tài khoản bị khóa hoặc đã xóa -> không có trong kết quả")
    void tc05_006_inactiveHidden() throws Exception {
        assertHidden("SELECT t.tutor_id::text FROM tutors t JOIN users u ON u.user_id = t.user_id "
                + "WHERE u.account_status::text <> 'ACTIVE' OR u.deleted_at IS NOT NULL");
    }

    @Test @DisplayName("TC-05-007: Gia sư không có gói hội viên còn hạn -> không có trong kết quả")
    void tc05_007_noMembershipHidden() throws Exception {
        assertHidden("SELECT t.tutor_id::text FROM tutors t WHERE NOT EXISTS (SELECT 1 FROM tutor_memberships m "
                + "WHERE m.tutor_id = t.tutor_id AND m.status::text = 'ACTIVE' "
                + "AND m.end_date >= (now() AT TIME ZONE 'Asia/Ho_Chi_Minh')::date)");
    }

    @Test @DisplayName("TC-05-008: Gia sư không có bài đăng đã duyệt -> không có trong kết quả")
    void tc05_008_noPostHidden() throws Exception {
        assertHidden("SELECT t.tutor_id::text FROM tutors t WHERE NOT EXISTS (SELECT 1 FROM tutor_posts p "
                + "WHERE p.tutor_id = t.tutor_id AND p.status::text = 'PUBLISHED')");
    }

    @Test @DisplayName("TC-05-010/020: Từ khóa trống / bỏ hết bộ lọc -> trả đúng toàn bộ gia sư được hiển thị")
    void tc05_010_emptyKeywordReturnsAllVisible() throws Exception {
        Set<String> expected = new HashSet<>();
        visible.forEach(r -> expected.add(r.id()));
        assertEquals(expected, ids(search("keyword=")));
        assertEquals(expected, ids(allFromApi));
    }

    // ===== UC-05.02 Filter Tutors =====

    @Test @DisplayName("TC-05-011: Bộ lọc khu vực chỉ liệt kê tỉnh đang có gia sư")
    void tc05_011_locationsOnlyWithTutors() throws Exception {
        JsonNode list = JSON.readTree(get("/api/tutors/locations").body());
        List<String> withoutTutor = new ArrayList<>();
        for (JsonNode n : list) {
            String province = n.asText();
            boolean has = visible.stream().anyMatch(r -> r.address() != null && r.address().startsWith(province));
            if (!has) withoutTutor.add(province);
        }
        assertTrue(withoutTutor.isEmpty(), withoutTutor.size() + "/" + list.size()
                + " tỉnh trong bộ lọc không có gia sư nào, ví dụ: " + withoutTutor.subList(0, Math.min(5, withoutTutor.size())));
    }

    @Test @DisplayName("TC-05-012: Lọc trình độ N3 -> gia sư N3, N2, N1; ẩn N4, N5")
    void tc05_012_levelAtLeast() throws Exception {
        assertFilter("jlptLevel=N3", r -> r.level() != null && List.of("N1", "N2", "N3").contains(r.level().toUpperCase()));
    }

    @Test @DisplayName("TC-05-013: Lọc mục tiêu Kaiwa -> chỉ gia sư có KAIWA trong bảng học phí của bài đăng đang hiển thị")
    void tc05_013_goalKaiwa() throws Exception {
        Set<String> got = ids(search("goal=kaiwa"));
        for (Row r : visible) {
            if (hasPriceTable(r)) {
                assertEquals(r.priceItems().contains("\"KAIWA\""), got.contains(r.id()), "Gia sư " + r.id());
            }
        }
    }

    @Test @DisplayName("TC-05-014: Mục tiêu JLPT N2 -> gia sư dưới N2 không được hiển thị")
    void tc05_014_goalJlptN2ChecksLevel() throws Exception {
        Set<String> got = ids(search("goal=jlpt_n2"));
        for (Row r : visible) {
            if (got.contains(r.id())) {
                assertTrue(List.of("N1", "N2").contains(String.valueOf(r.level()).toUpperCase()),
                        "Gia sư " + r.id() + " trình độ " + r.level() + " không được xuất hiện");
            }
        }
    }

    @Test @DisplayName("TC-05-015: Lọc hình thức Online / Offline theo bài đăng đang hiển thị")
    void tc05_015_teachingFormat() throws Exception {
        assertFilter("format=online", r -> List.of("ONLINE", "BOTH").contains(r.format()));
        assertFilter("format=offline", r -> List.of("OFFLINE", "BOTH").contains(r.format()));
    }

    @Test @DisplayName("TC-05-016/017: Khoảng giá 200.000-300.000 -> theo học phí bài đăng, không có thì theo hồ sơ")
    void tc05_016_priceRange() throws Exception {
        BigDecimal min = new BigDecimal("200000"), max = new BigDecimal("300000");
        assertFilter("minPrice=200000&maxPrice=300000",
                r -> r.price() != null && r.price().compareTo(min) >= 0 && r.price().compareTo(max) <= 0);
    }

    @Test @DisplayName("TC-05-018: Lọc khu vực theo tỉnh -> chỉ gia sư ở tỉnh đó")
    void tc05_018_area() throws Exception {
        String province = visible.stream().map(Row::address).filter(a -> a != null && !a.isBlank())
                .map(a -> a.split("\\s+-\\s+")[0]).findFirst().orElse(null);
        assumeTrue(province != null, "Không có gia sư nào có địa chỉ");
        assertFilter("location=" + enc(province),
                r -> r.address() != null && r.address().toLowerCase().contains(province.toLowerCase()));
    }

    @Test @DisplayName("TC-05-019: Kết hợp từ khóa + trình độ + hình thức -> kết quả thỏa mọi điều kiện")
    void tc05_019_combined() throws Exception {
        String keyword = allFromApi.get(0).path("fullName").asText().split("\\s+")[0];
        Set<String> expected = ids(search("keyword=" + enc(keyword)));
        expected.retainAll(ids(search("jlptLevel=N2")));
        expected.retainAll(ids(search("format=online")));
        assertEquals(expected, ids(search("keyword=" + enc(keyword) + "&jlptLevel=N2&format=online")));
    }

    @Test @DisplayName("TC-05-021: Không gia sư nào khớp bộ lọc -> danh sách rỗng, tổng 0")
    void tc05_021_noMatch() throws Exception {
        JsonNode page = JSON.readTree(get("/api/tutors/search?jlptLevel=N1&location=" + enc("Không tồn tại XYZ")).body());
        assertEquals(0, page.path("totalElements").asLong());
        assertEquals(0, page.path("content").size());
    }

    @Test @DisplayName("TC-05-022: Giá trị bộ lọc lạ format=ABC -> bị bỏ qua, kết quả như không lọc")
    void tc05_022_unknownFilterIgnored() throws Exception {
        assertEquals(ids(allFromApi), ids(search("format=ABC")));
    }

    // ===== UC-05.03 Sort =====

    @Test @DisplayName("TC-05-023: Không chọn sắp xếp -> theo điểm đánh giá giảm dần, rồi số lượt đánh giá")
    void tc05_023_defaultSortByRating() {
        for (int i = 1; i < allFromApi.size(); i++) {
            JsonNode a = allFromApi.get(i - 1), b = allFromApi.get(i);
            int cmp = Double.compare(a.path("avgRating").asDouble(), b.path("avgRating").asDouble());
            assertTrue(cmp > 0 || (cmp == 0 && a.path("totalReviews").asInt() >= b.path("totalReviews").asInt()),
                    "Sai thứ tự ở vị trí " + i);
        }
    }

    // ===== UC-05.07 Weekly schedule =====

    @Test @DisplayName("TC-05-046: Buổi học thử đang chờ (Pending) trong tuần -> khung giờ đó không còn trống")
    void tc05_046_pendingTrialRemoved() throws Exception {
        String sql = """
                SELECT tb.tutor_id::text AS tutor, tb.scheduled_start_time AS s, tb.scheduled_end_time AS e
                FROM trial_bookings tb
                WHERE tb.booking_status::text = 'PENDING' AND tb.scheduled_start_time > now() + interval '1 hour'
                  AND tb.scheduled_start_time < date_trunc('week', now() AT TIME ZONE 'Asia/Ho_Chi_Minh') + interval '14 days'
                  AND EXISTS (SELECT 1 FROM tutor_availabilities a WHERE a.tutor_id = tb.tutor_id AND a.is_active
                      AND a.day_of_week = EXTRACT(DOW FROM tb.scheduled_start_time AT TIME ZONE 'Asia/Ho_Chi_Minh') + 1
                      AND a.start_time <= (tb.scheduled_start_time AT TIME ZONE 'Asia/Ho_Chi_Minh')::time
                      AND a.end_time >= (tb.scheduled_end_time AT TIME ZONE 'Asia/Ho_Chi_Minh')::time)
                ORDER BY tb.scheduled_start_time LIMIT 1""";
        String tutor;
        ZonedDateTime start, end;
        try (ResultSet rs = db.createStatement().executeQuery(sql)) {
            assumeTrue(rs.next(), "CSDL không có buổi học thử Pending nào nằm trong ca rảnh của 2 tuần tới");
            tutor = rs.getString("tutor");
            start = rs.getTimestamp("s").toInstant().atZone(VN);
            end = rs.getTimestamp("e").toInstant().atZone(VN);
        }
        LocalDate monday = start.toLocalDate().minusDays(start.getDayOfWeek().getValue() - 1L);
        JsonNode week = JSON.readTree(get("/api/tutors/" + tutor + "/weekly-schedule?weekStart=" + monday
                + "&minDuration=30").body());
        for (JsonNode day : week.path("days")) {
            if (!start.toLocalDate().toString().equals(day.path("date").asText())) continue;
            for (JsonNode r : day.path("freeRanges")) {
                LocalTime fs = LocalTime.parse(r.path("startTime").asText()), fe = LocalTime.parse(r.path("endTime").asText());
                assertFalse(fs.isBefore(end.toLocalTime()) && fe.isAfter(start.toLocalTime()),
                        "Giờ " + start.toLocalTime() + "-" + end.toLocalTime() + " ngày " + start.toLocalDate()
                                + " đang có học thử Pending nhưng vẫn hiện trống (" + fs + "-" + fe + ")");
            }
        }
    }

    // ===== helpers =====

    private static boolean backendUp() {
        try {
            return get("/api/tutors/featured").statusCode() == 200;
        } catch (Exception e) {
            return false;
        }
    }

    private static HttpResponse<String> get(String path) throws Exception {
        return HTTP.send(HttpRequest.newBuilder(URI.create(BASE + path)).timeout(Duration.ofSeconds(30)).GET().build(),
                HttpResponse.BodyHandlers.ofString());
    }

    private static String enc(String s) {
        return URLEncoder.encode(s, StandardCharsets.UTF_8);
    }

    /** Lấy hết các trang kết quả. */
    private static List<JsonNode> search(String query) throws Exception {
        List<JsonNode> all = new ArrayList<>();
        for (int page = 0; ; page++) {
            HttpResponse<String> res = get("/api/tutors/search?size=50&page=" + page + (query.isEmpty() ? "" : "&" + query));
            assertEquals(200, res.statusCode(), res.body());
            JsonNode body = JSON.readTree(res.body());
            body.path("content").forEach(all::add);
            if (body.path("last").asBoolean(true) || page > 20) return all;
        }
    }

    private static Set<String> ids(List<JsonNode> tutors) {
        Set<String> s = new HashSet<>();
        tutors.forEach(t -> s.add(t.path("tutorId").asText()));
        return s;
    }

    private static Row row(String id) {
        return visible.stream().filter(r -> r.id().equals(id)).findFirst().orElseThrow();
    }

    private static boolean hasPriceTable(Row r) {
        return !r.priceItems().isBlank() && !"[]".equals(r.priceItems());
    }

    private static void assertFilter(String query, Predicate<Row> expectedRule) throws Exception {
        Set<String> expected = new HashSet<>();
        visible.stream().filter(expectedRule).forEach(r -> expected.add(r.id()));
        assertEquals(expected, ids(search(query)), "Bộ lọc " + query);
    }

    private static void assertHidden(String hiddenSql) throws Exception {
        Set<String> hidden = new HashSet<>();
        try (ResultSet rs = db.createStatement().executeQuery(hiddenSql)) {
            while (rs.next()) hidden.add(rs.getString(1));
        }
        assumeTrue(!hidden.isEmpty(), "CSDL không có gia sư thuộc trường hợp này");
        Set<String> shown = ids(allFromApi);
        shown.retainAll(hidden);
        assertTrue(shown.isEmpty(), "Gia sư lẽ ra bị ẩn vẫn hiển thị: " + shown);
    }
}
