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
import com.kizuna.features.booking.dto.request.ConnectionPreviewRequestDTO;
import com.kizuna.features.booking.dto.request.CreateConnectionRequestDTO;
import com.kizuna.features.booking.dto.request.RespondConnectionRequestDTO;
import com.kizuna.features.booking.dto.response.ConnectionPreviewResponseDTO;
import com.kizuna.features.booking.dto.response.ConnectionRequestResponseDTO;
import com.kizuna.features.booking.entity.*;
import com.kizuna.features.booking.repository.ConnectionRequestRepository;
import com.kizuna.features.booking.repository.ConnectionRequestSlotRepository;
import com.kizuna.features.booking.repository.TrialBookingHistoryRepository;
import com.kizuna.features.booking.repository.TrialBookingRepository;
import com.kizuna.features.booking.service.BookingLimits;
import com.kizuna.features.booking.service.ConnectionBusyLoader;
import com.kizuna.features.booking.service.ConnectionRequestService;
import com.kizuna.features.booking.service.ConnectionSchedulePlanner;
import com.kizuna.features.booking.service.ConnectionSchedulePlanner.BusyIndex;
import com.kizuna.features.booking.service.ConnectionSchedulePlanner.BusyKind;
import com.kizuna.features.booking.service.ConnectionSchedulePlanner.PlannedSession;
import com.kizuna.features.booking.service.ConnectionSchedulePlanner.WeeklySlot;
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
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.TemporalAdjusters;
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
    private static final ZoneId VN_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
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
    private final ConnectionBusyLoader busyLoader;

    public ConnectionRequestServiceImpl(UserRepository userRepository,
                                        LearnerRepository learnerRepository,
                                        TutorRepository tutorRepository,
                                        ConnectionRequestRepository connectionRequestRepository,
                                        ConnectionRequestSlotRepository slotRepository,
                                        TrialBookingRepository trialBookingRepository,
                                        TrialBookingHistoryRepository trialBookingHistoryRepository,
                                        @Lazy TutorScheduleService tutorScheduleService,
                                        JdbcTemplate jdbcTemplate,
                                        ConnectionBusyLoader busyLoader) {
        this.userRepository = userRepository;
        this.learnerRepository = learnerRepository;
        this.tutorRepository = tutorRepository;
        this.connectionRequestRepository = connectionRequestRepository;
        this.slotRepository = slotRepository;
        this.trialBookingRepository = trialBookingRepository;
        this.trialBookingHistoryRepository = trialBookingHistoryRepository;
        this.tutorScheduleService = tutorScheduleService;
        this.jdbcTemplate = jdbcTemplate;
        this.busyLoader = busyLoader;
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

        LocalDate startDate = resolveStartDate(request.getStartDate());
        int total = resolveTotalSessions(request.getTotalSessions());
        List<SlotRange> ranges = normalizeSlots(request.getSlots());
        for (SlotRange r : ranges) {
            if (!tutorScheduleService.isSlotAvailable(tutorId, r.dayOfWeek(), r.start(), r.end())) {
                throw new BadRequestException("Ca " + r.label() + " nằm ngoài lịch rảnh của gia sư. Vui lòng chọn ca khác.");
            }
        }

        // Kiểm tra trùng lịch trên TỪNG buổi cụ thể (đã khóa theo gia sư ở trên nên hai người gửi cùng lúc không cùng lọt)
        LocalDateTime now = LocalDateTime.now(VN_ZONE);
        List<PlannedSession> sessions = ConnectionSchedulePlanner.plan(toWeeklySlots(ranges), startDate, total, now);
        if (sessions.size() < total) {
            throw new BadRequestException("Không xếp đủ " + total + " buổi học từ ngày bắt đầu đã chọn. Vui lòng chọn lại.");
        }
        BusyIndex busy = busyLoader.load(tutorId, learnerId, NO_REQUEST, today(),
                sessions.get(sessions.size() - 1).date(), ConnectionBusyLoader.Mode.LEARNER_REQUEST);
        for (PlannedSession s : sessions) {
            busy.conflictOf(s).ifPresent(kind -> {
                throw new ConflictException("Buổi " + sessionLabel(s) + " đã kín: " + kind.learnerText()
                        + ". Ca vừa có người đặt hoặc lịch gia sư vừa thay đổi, hãy xem lại lịch học dự kiến.");
            });
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
        for (PlannedSession s : sessions) {
            connection.getSessions().add(ConnectionRequestSession.builder()
                    .connectionRequest(connection)
                    .sessionDate(s.date())
                    .startTime(s.start())
                    .endTime(s.end())
                    .build());
        }
        connection = connectionRequestRepository.save(connection);

        log.info("Learner {} sent direct connection request {} to tutor {} ({} slot(s), {} session(s))",
                learnerUser.getEmail(), connection.getRequestId(), tutorId, ranges.size(), sessions.size());
        return toDTO(connection, learnerUser);
    }

    // ------------------------------------------------------------------ xem trước lịch học dự kiến

    @Override
    @Transactional(readOnly = true)
    public ConnectionPreviewResponseDTO preview(String learnerEmail, ConnectionPreviewRequestDTO request) {
        User learnerUser = userRepository.findByEmail(learnerEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy thông tin tài khoản người dùng"));
        UUID learnerId = learnerRepository.findByUser(learnerUser).map(Learner::getLearnerId).orElse(null);
        if (!tutorRepository.existsById(request.getTutorId())) {
            throw new ResourceNotFoundException("Không tìm thấy gia sư với mã yêu cầu");
        }

        LocalDate startDate = resolveStartDate(request.getStartDate());
        int total = resolveTotalSessions(request.getTotalSessions());
        List<WeeklySlot> slots = toWeeklySlots(normalizeSlots(request.getSlots()));
        LocalDateTime now = LocalDateTime.now(VN_ZONE);
        LocalDate lastWeekStart = lastStartWeek();

        List<PlannedSession> sessions = ConnectionSchedulePlanner.plan(slots, startDate, total, now);
        // Nạp lịch bận đủ rộng cho cả các tuần bắt đầu có thể gợi ý (tối đa `total` tuần sau tuần muộn nhất)
        LocalDate busyTo = lastWeekStart.plusWeeks(total + 1L);
        BusyIndex busy = busyLoader.load(request.getTutorId(), learnerId, NO_REQUEST, today(), busyTo,
                ConnectionBusyLoader.Mode.LEARNER_REQUEST);

        List<ConnectionPreviewResponseDTO.SessionCheck> checks = new ArrayList<>();
        int conflicts = 0;
        for (PlannedSession s : sessions) {
            var kind = busy.conflictOf(s);
            if (kind.isPresent()) conflicts++;
            checks.add(ConnectionPreviewResponseDTO.SessionCheck.builder()
                    .date(s.date())
                    .dayOfWeek(s.dayOfWeek())
                    .dayName(dayName(s.dayOfWeek()))
                    .startTime(s.start().format(HH_MM))
                    .endTime(s.end().format(HH_MM))
                    .available(kind.isEmpty())
                    .reason(kind.map(BusyKind::learnerText).orElse(null))
                    .build());
        }
        LocalDate suggested = conflicts == 0 ? null
                : ConnectionSchedulePlanner.suggestStartWeek(slots, startDate, total, now, lastWeekStart, busy).orElse(null);

        return ConnectionPreviewResponseDTO.builder()
                .startDate(sessions.isEmpty() ? startDate : sessions.get(0).date())
                .endDate(sessions.isEmpty() ? null : sessions.get(sessions.size() - 1).date())
                .totalSessions(sessions.size())
                .conflictCount(conflicts)
                .suggestedStartWeek(suggested)
                .maxStartWeek(lastWeekStart)
                .sessions(checks)
                .build();
    }

    // ------------------------------------------------------------------ danh sách

    @Override
    @Transactional
    public Page<ConnectionRequestResponseDTO> getMyDirectRequests(String userEmail, String status, int page, int size) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy thông tin tài khoản"));
        // Tự hết hạn yêu cầu quá hạn do tác vụ nền BookingExpiryScheduler đảm nhiệm (không chạy ở đây để mở nhanh hơn)
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
            if (!connection.getSessions().isEmpty()) {
                ensureSessionsFreeForAccept(connection, learnerId);
            } else {
                // Yêu cầu kiểu cũ: chỉ có ca hằng tuần
                for (ConnectionRequestSlot slot : connection.getSlots()) {
                    SlotRange r = new SlotRange(slot.getDayOfWeek(), slot.getStartTime(), slot.getEndTime());
                    ensureSlotFree(tutor.getTutorId(), learnerId, connection.getRequestId(), r, today(),
                            "Ca " + r.label() + " đã được bạn nhận cho học viên khác. Hãy từ chối và đề nghị học viên chọn ca khác.",
                            "Ca " + r.label() + " trùng lịch dạy lớp chính thức của bạn. Hãy từ chối và đề nghị học viên chọn ca khác.",
                            "Ca " + r.label() + " trùng lịch học lớp chính thức hiện tại của học viên. Hãy từ chối và đề nghị học viên chọn ca khác.",
                            "Ca " + r.label() + " trùng buổi học thử bạn đã nhận với học viên khác. Hãy từ chối hoặc dời buổi học thử đó.");
                }
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
                && cr.getCreatedAt().isBefore(now.minusHours(BookingLimits.DIRECT_RESPONSE_HOURS));
    }

    @Override
    @Transactional
    public int expireStaleDirectRequests() {
        OffsetDateTime now = OffsetDateTime.now();
        List<ConnectionRequest> stale = connectionRequestRepository.findByRequestTypeAndStatusAndCreatedAtBefore(
                ConnectionRequestType.DIRECT, ConnectionRequestStatus.PENDING, now.minusHours(BookingLimits.DIRECT_RESPONSE_HOURS));
        for (ConnectionRequest cr : stale) {
            cr.setStatus(ConnectionRequestStatus.EXPIRED);
            cr.setRespondedAt(now);
            cr.setResponseMessage("Tự động hết hạn vì gia sư chưa phản hồi trong " + BookingLimits.DIRECT_RESPONSE_HOURS + " giờ");
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

    private void ensureSlotFree(UUID tutorId, UUID learnerId, UUID excludeRequestId, SlotRange r, LocalDate startDate,
                                String lockedMessage, String tutorClassMessage, String learnerClassMessage,
                                String trialMessage) {
        if (slotRepository.existsLockedSlotOverlap(tutorId, excludeRequestId, r.dayOfWeek(), r.start(), r.end())) {
            throw new ConflictException(lockedMessage);
        }
        // class_schedules dùng quy ước 1 = Thứ Hai ... 7 = Chủ nhật
        int isoDow = r.dayOfWeek() == 1 ? 7 : r.dayOfWeek() - 1;
        trialBookingRepository.findClassScheduleClash(tutorId, learnerId, isoDow, r.start(), r.end())
                .ifPresent(who -> {
                    throw new ConflictException("TUTOR".equals(who) ? tutorClassMessage : learnerClassMessage);
                });
        // Ca lặp lại hằng tuần: mọi buổi học thử cùng thứ, trùng giờ, từ buổi đầu tiên trở đi đều bị đụng
        LocalDate first = firstOccurrence(startDate, r, LocalDateTime.now(VN_ZONE));
        if (trialBookingRepository.existsTrialClashFrom(tutorId, learnerId, first, r.dayOfWeek() - 1, r.start(), r.end())) {
            throw new ConflictException(trialMessage);
        }
    }

    private static LocalDate today() {
        return LocalDate.now(VN_ZONE);
    }

    /**
     * Ngày bắt đầu học: không sớm hơn hôm nay, không muộn hơn tuần cuối cùng học viên được xem lịch.
     * Bỏ trống = hôm nay.
     */
    static LocalDate resolveStartDate(LocalDate requested) {
        LocalDate today = today();
        if (requested == null || requested.isBefore(today)) return today;
        LocalDate lastViewable = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                .plusWeeks(BookingLimits.DIRECT_SCHEDULE_WEEKS).minusDays(1);
        if (requested.isAfter(lastViewable)) {
            throw new BadRequestException("Ngày bắt đầu học chỉ có thể trong " + BookingLimits.DIRECT_SCHEDULE_WEEKS
                    + " tuần tính từ tuần này");
        }
        return requested;
    }

    /** Ngày diễn ra buổi đầu tiên của ca hằng tuần, tính từ startDate và chưa qua thời điểm hiện tại. */
    static LocalDate firstOccurrence(LocalDate startDate, SlotRange r, LocalDateTime now) {
        // 1 = Chủ nhật ... 7 = Thứ Bảy  →  DayOfWeek (1 = Thứ Hai ... 7 = Chủ nhật)
        DayOfWeek target = DayOfWeek.of(r.dayOfWeek() == 1 ? 7 : r.dayOfWeek() - 1);
        LocalDate d = startDate.with(TemporalAdjusters.nextOrSame(target));
        if (!d.atTime(r.start()).isAfter(now)) d = d.plusWeeks(1);
        return d;
    }

    static int resolveTotalSessions(Integer requested) {
        if (requested == null) return BookingLimits.DEFAULT_CONNECTION_SESSIONS;
        if (requested < 1 || requested > BookingLimits.MAX_CONNECTION_SESSIONS) {
            throw new BadRequestException("Số buổi học phải từ 1 đến " + BookingLimits.MAX_CONNECTION_SESSIONS);
        }
        return requested;
    }

    /** Thứ Hai của tuần muộn nhất học viên được chọn làm tuần bắt đầu. */
    private static LocalDate lastStartWeek() {
        return ConnectionSchedulePlanner.mondayOf(today()).plusWeeks(BookingLimits.DIRECT_SCHEDULE_WEEKS - 1L);
    }

    private static List<WeeklySlot> toWeeklySlots(List<SlotRange> ranges) {
        return ranges.stream().map(r -> new WeeklySlot(r.dayOfWeek(), r.start(), r.end())).toList();
    }

    private static final DateTimeFormatter DD_MM = DateTimeFormatter.ofPattern("dd/MM");

    private static String sessionLabel(PlannedSession s) {
        return dayName(s.dayOfWeek()) + " " + s.date().format(DD_MM) + " " + s.start().format(HH_MM) + "–" + s.end().format(HH_MM);
    }

    /** Gia sư bấm đồng ý: các buổi CHƯA diễn ra không được trùng những gì gia sư đã xác nhận. */
    private void ensureSessionsFreeForAccept(ConnectionRequest connection, UUID learnerId) {
        LocalDateTime now = LocalDateTime.now(VN_ZONE);
        List<PlannedSession> upcoming = connection.getSessions().stream()
                .filter(s -> s.getSessionDate().atTime(s.getStartTime()).isAfter(now))
                .map(s -> new PlannedSession(s.getSessionDate(), ConnectionSchedulePlanner.dayOfWeekOf(s.getSessionDate()),
                        s.getStartTime(), s.getEndTime()))
                .toList();
        if (upcoming.isEmpty()) {
            throw new BadRequestException("Tất cả các buổi trong yêu cầu này đã qua. Hãy từ chối và đề nghị học viên gửi lại yêu cầu mới.");
        }
        BusyIndex busy = busyLoader.load(connection.getTutor().getTutorId(), learnerId, connection.getRequestId(),
                today(), upcoming.get(upcoming.size() - 1).date(), ConnectionBusyLoader.Mode.TUTOR_ACCEPT);
        for (PlannedSession s : upcoming) {
            busy.conflictOf(s).ifPresent(kind -> {
                throw new ConflictException("Buổi " + sessionLabel(s) + " " + kind.tutorText()
                        + ". Hãy từ chối và đề nghị học viên chọn tuần bắt đầu hoặc ca khác.");
            });
        }
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
                .sessions(cr.getSessions().stream()
                        .map(s -> ConnectionRequestResponseDTO.SessionView.builder()
                                .date(s.getSessionDate())
                                .dayOfWeek(ConnectionSchedulePlanner.dayOfWeekOf(s.getSessionDate()))
                                .dayName(dayName(ConnectionSchedulePlanner.dayOfWeekOf(s.getSessionDate())))
                                .startTime(s.getStartTime().format(HH_MM))
                                .endTime(s.getEndTime().format(HH_MM))
                                .build())
                        .toList())
                .totalSessions(cr.getSessions().isEmpty() ? null : cr.getSessions().size())
                .startDate(cr.getSessions().isEmpty() ? null : cr.getSessions().get(0).getSessionDate())
                .endDate(cr.getSessions().isEmpty() ? null : cr.getSessions().get(cr.getSessions().size() - 1).getSessionDate())
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
