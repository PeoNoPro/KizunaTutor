package com.kizuna.features.booking.service.impl;

import com.kizuna.core.entity.Learner;
import com.kizuna.core.entity.Tutor;
import com.kizuna.core.entity.User;
import com.kizuna.core.exception.BadRequestException;
import com.kizuna.core.exception.ConflictException;
import com.kizuna.core.exception.ResourceNotFoundException;
import com.kizuna.core.repository.LearnerRepository;
import com.kizuna.core.repository.TutorRepository;
import com.kizuna.core.repository.UserRepository;
import com.kizuna.features.booking.dto.request.ConnectionSlotRequestDTO;
import com.kizuna.features.booking.dto.request.CreateConnectionRequestDTO;
import com.kizuna.features.booking.dto.request.RespondConnectionRequestDTO;
import com.kizuna.features.booking.dto.response.ConnectionRequestResponseDTO;
import com.kizuna.features.booking.entity.*;
import com.kizuna.features.booking.repository.ConnectionRequestRepository;
import com.kizuna.features.booking.repository.ConnectionRequestSlotRepository;
import com.kizuna.features.booking.repository.TrialBookingHistoryRepository;
import com.kizuna.features.booking.repository.TrialBookingRepository;
import com.kizuna.features.booking.service.BookingLimits;
import com.kizuna.features.booking.service.ConnectionRequestService;
import com.kizuna.features.tutor_search.service.TutorScheduleService;
import com.kizuna.features.verification.entity.TutorVerificationStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
public class ConnectionRequestServiceImpl implements ConnectionRequestService {

    /** Thời lượng một ca học chính thức được phép chọn (phút). */
    static final Set<Integer> ALLOWED_DURATIONS = Set.of(60, 90, 120);
    private static final UUID NO_REQUEST = new UUID(0L, 0L);
    private static final DateTimeFormatter HH_MM = DateTimeFormatter.ofPattern("HH:mm");

    private final UserRepository userRepository;
    private final LearnerRepository learnerRepository;
    private final TutorRepository tutorRepository;
    private final ConnectionRequestRepository connectionRequestRepository;
    private final ConnectionRequestSlotRepository slotRepository;
    private final TrialBookingRepository trialBookingRepository;
    private final TrialBookingHistoryRepository trialBookingHistoryRepository;
    private final TutorScheduleService tutorScheduleService;
    private final JdbcTemplate jdbcTemplate;

    public ConnectionRequestServiceImpl(UserRepository userRepository,
                                        LearnerRepository learnerRepository,
                                        TutorRepository tutorRepository,
                                        ConnectionRequestRepository connectionRequestRepository,
                                        ConnectionRequestSlotRepository slotRepository,
                                        TrialBookingRepository trialBookingRepository,
                                        TrialBookingHistoryRepository trialBookingHistoryRepository,
                                        @Lazy TutorScheduleService tutorScheduleService,
                                        JdbcTemplate jdbcTemplate) {
        this.userRepository = userRepository;
        this.learnerRepository = learnerRepository;
        this.tutorRepository = tutorRepository;
        this.connectionRequestRepository = connectionRequestRepository;
        this.slotRepository = slotRepository;
        this.trialBookingRepository = trialBookingRepository;
        this.trialBookingHistoryRepository = trialBookingHistoryRepository;
        this.tutorScheduleService = tutorScheduleService;
        this.jdbcTemplate = jdbcTemplate;
    }

    // ------------------------------------------------------------------ tạo yêu cầu

    @Override
    @Transactional
    public ConnectionRequestResponseDTO createDirectRequest(String learnerEmail, CreateConnectionRequestDTO request) {
        User learnerUser = userRepository.findByEmail(learnerEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy thông tin tài khoản người dùng"));
        if (!hasRole(learnerUser, "ROLE_LEARNER")) {
            throw new AccessDeniedException("Chỉ tài khoản học viên mới có thể gửi yêu cầu kết nối gia sư");
        }

        Tutor tutor = tutorRepository.findById(request.getTutorId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy gia sư với mã yêu cầu"));
        if (tutor.getVerificationStatus() != TutorVerificationStatus.APPROVED) {
            throw new BadRequestException("Gia sư hiện chưa sẵn sàng nhận học viên mới. Vui lòng chọn gia sư đã được phê duyệt.");
        }

        Learner learner = learnerRepository.findByUser(learnerUser)
                .orElseGet(() -> learnerRepository.save(Learner.builder().user(learnerUser).build()));
        String level = trimToNull(request.getCurrentLevel());
        String goals = trimToNull(request.getLearningGoals());
        if (level != null) learner.setCurrentLevel(level);
        if (goals != null) learner.setLearningGoals(goals);
        learner = learnerRepository.save(learner);

        lockTutor(tutor.getTutorId());
        expireStaleDirectRequests(); // yêu cầu cũ đã quá hạn không còn tính là "đang chờ"

        UUID learnerId = learner.getLearnerId();
        UUID tutorId = tutor.getTutorId();
        if (connectionRequestRepository.existsByLearnerLearnerIdAndTutorTutorIdAndRequestTypeAndStatusAndRespondedAtAfter(
                learnerId, tutorId, ConnectionRequestType.DIRECT, ConnectionRequestStatus.DECLINED,
                OffsetDateTime.now().minusHours(BookingLimits.DECLINE_COOLDOWN_HOURS))) {
            throw new ConflictException("Gia sư vừa từ chối yêu cầu kết nối của bạn. Bạn có thể gửi lại sau "
                    + BookingLimits.DECLINE_COOLDOWN_HOURS + " giờ, hoặc chọn gia sư khác.");
        }
        if (connectionRequestRepository.existsByLearnerLearnerIdAndTutorTutorIdAndStatus(learnerId, tutorId, ConnectionRequestStatus.ACCEPTED)) {
            throw new ConflictException("Bạn đã được kết nối với gia sư này. Hãy vào lớp học để trao đổi lịch học.");
        }
        if (connectionRequestRepository.existsByLearnerLearnerIdAndTutorTutorIdAndRequestTypeAndStatus(
                learnerId, tutorId, ConnectionRequestType.DIRECT, ConnectionRequestStatus.PENDING)) {
            throw new ConflictException("Bạn đã gửi yêu cầu kết nối tới gia sư này và đang chờ gia sư phản hồi.");
        }

        List<SlotRange> ranges = normalizeSlots(request.getSlots());
        for (SlotRange r : ranges) {
            if (!tutorScheduleService.isSlotAvailable(tutorId, r.dayOfWeek(), r.start(), r.end())) {
                throw new BadRequestException("Ca " + r.label() + " nằm ngoài lịch rảnh của gia sư. Vui lòng chọn ca khác.");
            }
            ensureSlotFree(tutorId, learnerId, NO_REQUEST, r,
                    "Ca " + r.label() + " đã có học viên khác nhận. Vui lòng chọn ca khác.",
                    "Ca " + r.label() + " trùng lịch dạy lớp chính thức của gia sư. Vui lòng chọn ca khác.",
                    "Ca " + r.label() + " trùng lịch học lớp chính thức của bạn. Vui lòng chọn ca khác.");
        }

        ConnectionRequest connection = ConnectionRequest.builder()
                .learner(learner)
                .tutor(tutor)
                .requestType(ConnectionRequestType.DIRECT)
                .status(ConnectionRequestStatus.PENDING)
                .initialMessage(composeInitialMessage(level, goals, trimToNull(request.getInitialMessage())))
                .build();
        for (SlotRange r : ranges) {
            connection.getSlots().add(ConnectionRequestSlot.builder()
                    .connectionRequest(connection)
                    .dayOfWeek(r.dayOfWeek())
                    .startTime(r.start())
                    .endTime(r.end())
                    .build());
        }
        connection = connectionRequestRepository.save(connection);

        log.info("Learner {} sent direct connection request {} to tutor {} ({} slot(s))",
                learnerUser.getEmail(), connection.getRequestId(), tutorId, ranges.size());
        return toDTO(connection, learnerUser);
    }

    // ------------------------------------------------------------------ danh sách

    @Override
    @Transactional
    public Page<ConnectionRequestResponseDTO> getMyDirectRequests(String userEmail, String status, int page, int size) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy thông tin tài khoản"));
        try {
            expireStaleDirectRequests();
        } catch (Exception e) {
            log.warn("Lỗi khi tự động hết hạn yêu cầu kết nối: {}", e.getMessage());
        }
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100),
                Sort.by(Sort.Direction.DESC, "createdAt"));

        ConnectionRequestStatus statusFilter = null;
        if (status != null && !status.isBlank() && !"ALL".equalsIgnoreCase(status)) {
            try {
                statusFilter = ConnectionRequestStatus.valueOf(status.trim().toUpperCase());
            } catch (IllegalArgumentException ignored) {
                // bộ lọc không hợp lệ: trả toàn bộ
            }
        }

        Page<ConnectionRequest> result;
        if (hasRole(user, "ROLE_TUTOR")) {
            Tutor tutor = tutorRepository.findByUser(user).orElse(null);
            if (tutor == null) return Page.empty(pageable);
            result = statusFilter == null
                    ? connectionRequestRepository.findByTutorTutorIdAndRequestType(tutor.getTutorId(), ConnectionRequestType.DIRECT, pageable)
                    : connectionRequestRepository.findByTutorTutorIdAndRequestTypeAndStatus(tutor.getTutorId(), ConnectionRequestType.DIRECT, statusFilter, pageable);
        } else {
            Learner learner = learnerRepository.findByUser(user).orElse(null);
            if (learner == null) return Page.empty(pageable);
            result = statusFilter == null
                    ? connectionRequestRepository.findByLearnerLearnerIdAndRequestType(learner.getLearnerId(), ConnectionRequestType.DIRECT, pageable)
                    : connectionRequestRepository.findByLearnerLearnerIdAndRequestTypeAndStatus(learner.getLearnerId(), ConnectionRequestType.DIRECT, statusFilter, pageable);
        }
        return result.map(cr -> toDTO(cr, user));
    }

    // ------------------------------------------------------------------ gia sư phản hồi

    @Override
    @Transactional
    public ConnectionRequestResponseDTO respond(String tutorEmail, UUID requestId, RespondConnectionRequestDTO request) {
        User tutorUser = userRepository.findByEmail(tutorEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy thông tin tài khoản"));
        Tutor tutor = tutorRepository.findByUser(tutorUser)
                .orElseThrow(() -> new AccessDeniedException("Tài khoản của bạn chưa được liên kết với hồ sơ gia sư"));

        ConnectionRequest connection = findDirectRequest(requestId);
        if (!connection.getTutor().getTutorId().equals(tutor.getTutorId())) {
            throw new AccessDeniedException("Bạn không có quyền thao tác trên yêu cầu kết nối này");
        }
        if (connection.getStatus() != ConnectionRequestStatus.PENDING) {
            throw new BadRequestException("Yêu cầu kết nối này đã được xử lý trước đó");
        }
        if (isStaleDirect(connection, OffsetDateTime.now())) {
            // Tác vụ nền sẽ chuyển yêu cầu này sang "Đã hết hạn"
            throw new BadRequestException("Yêu cầu kết nối này đã hết hạn phản hồi. Học viên có thể gửi lại yêu cầu mới.");
        }

        String action = request.getAction() == null ? "" : request.getAction().trim().toUpperCase();
        String message = trimToNull(request.getMessage());
        OffsetDateTime now = OffsetDateTime.now();

        if ("ACCEPT".equals(action)) {
            lockTutor(tutor.getTutorId());
            UUID learnerId = connection.getLearner().getLearnerId();
            for (ConnectionRequestSlot slot : connection.getSlots()) {
                SlotRange r = new SlotRange(slot.getDayOfWeek(), slot.getStartTime(), slot.getEndTime());
                ensureSlotFree(tutor.getTutorId(), learnerId, connection.getRequestId(), r,
                        "Ca " + r.label() + " đã được bạn nhận cho học viên khác. Hãy từ chối và đề nghị học viên chọn ca khác.",
                        "Ca " + r.label() + " trùng lịch dạy lớp chính thức của bạn. Hãy từ chối và đề nghị học viên chọn ca khác.",
                        "Ca " + r.label() + " trùng lịch học lớp chính thức hiện tại của học viên. Hãy từ chối và đề nghị học viên chọn ca khác.");
            }
            connection.setStatus(ConnectionRequestStatus.ACCEPTED);
            connection.setRespondedAt(now);
            connection.setResponseMessage(message);
            connection = connectionRequestRepository.save(connection);
            cancelOpenTrials(connection, tutorUser, now);
        } else if ("DECLINE".equals(action)) {
            connection.setStatus(ConnectionRequestStatus.DECLINED);
            connection.setRespondedAt(now);
            connection.setResponseMessage(message);
            connection = connectionRequestRepository.save(connection);
        } else {
            throw new BadRequestException("Hành động không hợp lệ. Vui lòng chọn đồng ý hoặc từ chối.");
        }

        log.info("Tutor {} responded {} to direct connection request {}", tutorUser.getEmail(), action, requestId);
        return toDTO(connection, tutorUser);
    }

    // ------------------------------------------------------------------ học viên hủy

    @Override
    @Transactional
    public ConnectionRequestResponseDTO cancel(String learnerEmail, UUID requestId) {
        User user = userRepository.findByEmail(learnerEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy thông tin tài khoản"));
        ConnectionRequest connection = findDirectRequest(requestId);
        if (!connection.getLearner().getUser().getUserId().equals(user.getUserId())) {
            throw new AccessDeniedException("Bạn không có quyền hủy yêu cầu kết nối này");
        }
        if (connection.getStatus() != ConnectionRequestStatus.PENDING) {
            throw new BadRequestException("Chỉ có thể hủy yêu cầu kết nối đang chờ gia sư phản hồi");
        }
        connection.setStatus(ConnectionRequestStatus.CANCELLED);
        connection.setRespondedAt(OffsetDateTime.now());
        connection = connectionRequestRepository.save(connection);
        log.info("Learner {} cancelled direct connection request {}", user.getEmail(), requestId);
        return toDTO(connection, user);
    }

    // ------------------------------------------------------------------ tự hết hạn

    private static boolean isStaleDirect(ConnectionRequest cr, OffsetDateTime now) {
        return cr.getStatus() == ConnectionRequestStatus.PENDING
                && cr.getCreatedAt() != null
                && cr.getCreatedAt().isBefore(now.minusDays(BookingLimits.DIRECT_RESPONSE_DAYS));
    }

    @Override
    @Transactional
    public int expireStaleDirectRequests() {
        OffsetDateTime now = OffsetDateTime.now();
        List<ConnectionRequest> stale = connectionRequestRepository.findByRequestTypeAndStatusAndCreatedAtBefore(
                ConnectionRequestType.DIRECT, ConnectionRequestStatus.PENDING, now.minusDays(BookingLimits.DIRECT_RESPONSE_DAYS));
        for (ConnectionRequest cr : stale) {
            cr.setStatus(ConnectionRequestStatus.EXPIRED);
            cr.setRespondedAt(now);
            cr.setResponseMessage("Tự động hết hạn vì gia sư chưa phản hồi trong " + BookingLimits.DIRECT_RESPONSE_DAYS + " ngày");
            connectionRequestRepository.save(cr);
        }
        if (!stale.isEmpty()) {
            log.info("Auto-expired {} pending direct connection request(s)", stale.size());
        }
        return stale.size();
    }

    // ------------------------------------------------------------------ hỗ trợ

    /** Chuẩn hóa và kiểm tra danh sách ca: thời lượng hợp lệ, mốc 30 phút, không qua nửa đêm, không chồng nhau. */
    List<SlotRange> normalizeSlots(List<ConnectionSlotRequestDTO> slots) {
        if (slots == null || slots.isEmpty()) {
            throw new BadRequestException("Vui lòng chọn ít nhất một ca học hằng tuần");
        }
        List<SlotRange> ranges = new ArrayList<>();
        for (ConnectionSlotRequestDTO s : slots) {
            if (s == null || s.getDayOfWeek() == null || s.getStartTime() == null || s.getDurationMinutes() == null) {
                throw new BadRequestException("Thông tin ca học chưa đầy đủ");
            }
            if (s.getDayOfWeek() < 1 || s.getDayOfWeek() > 7) {
                throw new BadRequestException("Thứ trong tuần của ca học không hợp lệ");
            }
            if (!ALLOWED_DURATIONS.contains(s.getDurationMinutes())) {
                throw new BadRequestException("Thời lượng mỗi ca học chỉ có thể là 60, 90 hoặc 120 phút");
            }
            LocalTime start = s.getStartTime().withSecond(0).withNano(0);
            if (start.getMinute() % 30 != 0) {
                throw new BadRequestException("Giờ bắt đầu ca học phải là giờ chẵn hoặc giờ rưỡi");
            }
            int startMinutes = start.getHour() * 60 + start.getMinute();
            if (startMinutes + s.getDurationMinutes() >= 24 * 60) {
                throw new BadRequestException("Ca học phải kết thúc trước 24:00");
            }
            ranges.add(new SlotRange(s.getDayOfWeek(), start, start.plusMinutes(s.getDurationMinutes())));
        }
        ranges.sort(Comparator.comparing(SlotRange::dayOfWeek).thenComparing(SlotRange::start));
        for (int i = 1; i < ranges.size(); i++) {
            SlotRange prev = ranges.get(i - 1);
            SlotRange cur = ranges.get(i);
            if (prev.dayOfWeek().equals(cur.dayOfWeek()) && cur.start().isBefore(prev.end())) {
                throw new BadRequestException("Ca " + prev.label() + " và ca " + cur.label() + " bị chồng giờ nhau");
            }
        }
        return ranges;
    }

    private void ensureSlotFree(UUID tutorId, UUID learnerId, UUID excludeRequestId, SlotRange r,
                                String lockedMessage, String tutorClassMessage, String learnerClassMessage) {
        if (slotRepository.existsLockedSlotOverlap(tutorId, excludeRequestId, r.dayOfWeek(), r.start(), r.end())) {
            throw new ConflictException(lockedMessage);
        }
        // class_schedules dùng quy ước 1 = Thứ Hai ... 7 = Chủ nhật
        int isoDow = r.dayOfWeek() == 1 ? 7 : r.dayOfWeek() - 1;
        trialBookingRepository.findClassScheduleClash(tutorId, learnerId, isoDow, r.start(), r.end())
                .ifPresent(who -> {
                    throw new ConflictException("TUTOR".equals(who) ? tutorClassMessage : learnerClassMessage);
                });
    }

    /** Đã kết nối trực tiếp: các buổi học thử còn mở giữa hai người không còn cần thiết → chuyển "Đã hủy". */
    private void cancelOpenTrials(ConnectionRequest accepted, User tutorUser, OffsetDateTime now) {
        List<TrialBooking> open = trialBookingRepository.findByTutorTutorIdAndLearnerLearnerIdAndBookingStatusIn(
                accepted.getTutor().getTutorId(), accepted.getLearner().getLearnerId(),
                List.of(TrialBookingStatus.PENDING, TrialBookingStatus.CONFIRMED, TrialBookingStatus.RESCHEDULED));
        for (TrialBooking trial : open) {
            TrialBookingStatus previous = trial.getBookingStatus();
            trial.setBookingStatus(TrialBookingStatus.CANCELLED);
            ConnectionRequest trialConnection = trial.getConnectionRequest();
            if (trialConnection != null && trialConnection.getStatus() == ConnectionRequestStatus.PENDING) {
                trialConnection.setStatus(ConnectionRequestStatus.CANCELLED);
                trialConnection.setRespondedAt(now);
            }
            trialBookingRepository.save(trial);
            trialBookingHistoryRepository.save(TrialBookingHistory.builder()
                    .trialBooking(trial)
                    .changedByUser(tutorUser)
                    .previousStatus(previous)
                    .newStatus(TrialBookingStatus.CANCELLED)
                    .changeReason("Tự động hủy vì gia sư đã đồng ý kết nối trực tiếp với học viên")
                    .build());
        }
        if (!open.isEmpty()) {
            log.info("Cancelled {} open trial(s) after direct connection {} was accepted", open.size(), accepted.getRequestId());
        }
    }

    private ConnectionRequest findDirectRequest(UUID requestId) {
        ConnectionRequest connection = connectionRequestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy yêu cầu kết nối"));
        if (connection.getRequestType() != ConnectionRequestType.DIRECT) {
            throw new ResourceNotFoundException("Không tìm thấy yêu cầu kết nối");
        }
        return connection;
    }

    private void lockTutor(UUID tutorId) {
        if (jdbcTemplate == null || tutorId == null) return;
        try {
            jdbcTemplate.execute("SELECT pg_advisory_xact_lock(" + Math.abs(tutorId.getMostSignificantBits()) + ")");
        } catch (Exception e) {
            log.warn("Không thể kích hoạt pg_advisory_xact_lock: {}", e.getMessage());
        }
    }

    private static boolean hasRole(User user, String role) {
        return user.getRole() != null && role.equalsIgnoreCase(user.getRole().getRoleName());
    }

    private static String trimToNull(String value) {
        if (value == null) return null;
        String t = value.trim();
        return t.isEmpty() ? null : t;
    }

    private static String composeInitialMessage(String level, String goals, String message) {
        StringBuilder sb = new StringBuilder("[Hình thức: Kết nối trực tiếp, không học thử]");
        if (level != null) sb.append("\n[Trình độ: ").append(level).append("]");
        if (goals != null) sb.append("\n[Mục tiêu: ").append(goals).append("]");
        if (message != null) sb.append("\n").append(message);
        return sb.toString();
    }

    private static String extractTag(String text, String tag) {
        if (text == null) return null;
        String marker = "[" + tag + ": ";
        int start = text.indexOf(marker);
        if (start < 0) return null;
        start += marker.length();
        int end = text.indexOf("]", start);
        return end > start ? text.substring(start, end).trim() : null;
    }

    private static String stripTags(String text) {
        if (text == null) return null;
        String clean = text.replaceAll("\\[(Hình thức|Trình độ|Mục tiêu):[^\\]]*\\]", "").trim();
        return clean.isEmpty() ? null : clean;
    }

    static String dayName(int dayOfWeek) {
        return switch (dayOfWeek) {
            case 1 -> "Chủ nhật";
            case 2 -> "Thứ hai";
            case 3 -> "Thứ ba";
            case 4 -> "Thứ tư";
            case 5 -> "Thứ năm";
            case 6 -> "Thứ sáu";
            case 7 -> "Thứ bảy";
            default -> "Thứ " + dayOfWeek;
        };
    }

    private ConnectionRequestResponseDTO toDTO(ConnectionRequest cr, User viewer) {
        boolean viewerIsLearner = cr.getLearner().getUser().getUserId().equals(viewer.getUserId());
        User partner = viewerIsLearner ? cr.getTutor().getUser() : cr.getLearner().getUser();

        String statusLabel = switch (cr.getStatus()) {
            case PENDING -> viewerIsLearner ? "Chờ gia sư xác nhận" : "Chờ bạn xác nhận";
            case ACCEPTED -> "Đã kết nối";
            case DECLINED -> viewerIsLearner ? "Gia sư đã từ chối" : "Đã từ chối";
            case CANCELLED -> "Đã hủy";
            case EXPIRED -> "Đã hết hạn";
        };

        List<ConnectionRequestResponseDTO.SlotView> slots = cr.getSlots().stream()
                .map(s -> ConnectionRequestResponseDTO.SlotView.builder()
                        .dayOfWeek(s.getDayOfWeek())
                        .dayName(dayName(s.getDayOfWeek()))
                        .startTime(s.getStartTime().format(HH_MM))
                        .endTime(s.getEndTime().format(HH_MM))
                        .durationMinutes((int) Duration.between(s.getStartTime(), s.getEndTime()).toMinutes())
                        .build())
                .toList();

        return ConnectionRequestResponseDTO.builder()
                .requestId(cr.getRequestId())
                .tutorId(cr.getTutor().getTutorId())
                .tutorName(cr.getTutor().getUser().getFullName())
                .learnerId(cr.getLearner().getLearnerId())
                .partnerId(partner.getUserId())
                .partnerName(partner.getFullName() != null ? partner.getFullName() : partner.getEmail())
                .partnerAvatarUrl(partner.getAvatarUrl())
                .partnerRole(viewerIsLearner ? "Gia sư" : "Học viên")
                .slots(slots)
                .currentLevel(extractTag(cr.getInitialMessage(), "Trình độ"))
                .learningGoals(extractTag(cr.getInitialMessage(), "Mục tiêu"))
                .initialMessage(stripTags(cr.getInitialMessage()))
                .responseMessage(cr.getResponseMessage())
                .status(cr.getStatus().name())
                .statusLabel(statusLabel)
                .createdAt(cr.getCreatedAt())
                .respondedAt(cr.getRespondedAt())
                .build();
    }

    /** Khoảng giờ của một ca hằng tuần (dayOfWeek: 1 = Chủ nhật ... 7 = Thứ Bảy). */
    record SlotRange(Integer dayOfWeek, LocalTime start, LocalTime end) {
        String label() {
            return dayName(dayOfWeek) + " " + start.format(HH_MM) + "–" + end.format(HH_MM);
        }
    }
}
