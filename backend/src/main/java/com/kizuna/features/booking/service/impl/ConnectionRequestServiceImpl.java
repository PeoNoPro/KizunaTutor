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
import com.kizuna.features.booking.service.ConnectionBusyLoader;
import com.kizuna.features.booking.service.ConnectionRequestService;
import com.kizuna.features.booking.service.ConnectionSchedulePlanner;
import com.kizuna.features.booking.service.ConnectionSchedulePlanner.BusyIndex;
import com.kizuna.features.booking.service.ConnectionSchedulePlanner.BusyKind;
import com.kizuna.features.booking.service.ConnectionSchedulePlanner.PlannedSession;
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

    /** Thông báo tự động (FE-12). Không bắt buộc, nên test dựng service bằng tay vẫn chạy. */
    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private com.kizuna.features.notification.service.NotificationEvents notificationEvents;


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
        // Chỉ khóa kết nối (DIRECT) đã được nhận và còn buổi mới tính là "đã kết nối";
        // buổi học thử đã nhận / đã học xong không chặn việc gửi yêu cầu kết nối.
        if (connectionRequestRepository.existsActiveDirectConnection(learnerId, tutorId)) {
            throw new ConflictException("Bạn đang có khóa học kết nối với gia sư này. Hãy vào lớp học để trao đổi lịch học.");
        }
        if (connectionRequestRepository.existsByLearnerLearnerIdAndTutorTutorIdAndRequestTypeAndStatus(
                learnerId, tutorId, ConnectionRequestType.DIRECT, ConnectionRequestStatus.PENDING)) {
            throw new ConflictException("Bạn đã gửi yêu cầu kết nối tới gia sư này và đang chờ gia sư phản hồi.");
        }

        // Yêu cầu kết nối chỉ là ĐỀ NGHỊ: học viên gửi thứ + giờ bắt đầu mong muốn. KHÔNG tạo buổi học, KHÔNG giữ chỗ;
        // gia sư trao đổi với học viên rồi đồng ý (lúc đó ca mới "Đã có học viên") và tạo lớp học.
        List<SlotRange> ranges = normalizeSlots(request.getSlots());
        for (SlotRange r : ranges) {
            // Chỉ cần GIỜ BẮT ĐẦU nằm trong lịch rảnh (gia sư sắp xếp thời lượng khi nhận yêu cầu)
            SlotRange startOnly = new SlotRange(r.dayOfWeek(), r.start(), r.start().plusMinutes(30));
            if (!tutorScheduleService.isSlotAvailable(tutorId, r.dayOfWeek(), startOnly.start(), startOnly.end())) {
                throw new BadRequestException("Giờ " + dayName(r.dayOfWeek()) + " " + r.start().format(HH_MM)
                        + " nằm ngoài lịch rảnh của gia sư. Vui lòng chọn giờ khác.");
            }
            // Chỉ chặn khi giờ đó đã bị lịch ĐÃ CHẤP NHẬN chiếm (ca gia sư đã nhận cho học viên khác, lịch lớp)
            SlotConflict taken = slotConflict(tutorId, learnerId, NO_REQUEST, startOnly, today(), false);
            if (taken != null) {
                throw new ConflictException(taken.learnerText(startOnly));
            }
        }

        ConnectionRequest connection = ConnectionRequest.builder()
                .learner(learner)
                .tutor(tutor)
                .requestType(ConnectionRequestType.DIRECT)
                .status(ConnectionRequestStatus.PENDING)
                .initialMessage(composeInitialMessage(BookingLimits.levelLabel(learner.getCurrentLevel()), goals, trimToNull(request.getInitialMessage())))
                .build();
        // Học viên đồng ý gửi hồ sơ + email + số điện thoại cho gia sư
        com.kizuna.features.booking.service.ContactShare.attachLearnerContact(connection, learnerUser,
                request.getContactEmail(), request.getContactPhone(), request.getShareContact());
        for (SlotRange r : ranges) {
            connection.getSlots().add(ConnectionRequestSlot.builder()
                    .connectionRequest(connection)
                    .dayOfWeek(r.dayOfWeek())
                    .startTime(r.start())
                    .endTime(r.end())
                    .build());
        }
        connection = connectionRequestRepository.save(connection);

        notify("connection:" + connection.getRequestId() + ":created", "Có yêu cầu kết nối mới",
                learnerUser.getFullName() + " muốn kết nối học với bạn vào " + startSummary(connection) + " hằng tuần. "
                        + "Hãy liên hệ học viên để trao đổi, rồi đồng ý hoặc từ chối trong " + BookingLimits.DIRECT_RESPONSE_HOURS + " giờ.",
                TUTOR_DIRECT_PATH, tutor.getUser());
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
            cancelLearnerConflictingPending(connection, tutorUser, now);
        } else if ("DECLINE".equals(action)) {
            connection.setStatus(ConnectionRequestStatus.DECLINED);
            connection.setRespondedAt(now);
            connection.setResponseMessage(message);
            connection = connectionRequestRepository.save(connection);
        } else {
            throw new BadRequestException("Hành động không hợp lệ. Vui lòng chọn đồng ý hoặc từ chối.");
        }

        String tutorName = tutorUser.getFullName();
        if ("ACCEPT".equals(action)) {
            notify("connection:" + connection.getRequestId() + ":accepted", "Gia sư đã đồng ý kết nối",
                    tutorName + " đã đồng ý dạy bạn" + courseSummary(connection) + "."
                            + (message != null ? " Lời nhắn: " + message : ""),
                    LEARNER_DIRECT_PATH, connection.getLearner().getUser());
        } else {
            notify("connection:" + connection.getRequestId() + ":declined", "Gia sư đã từ chối yêu cầu kết nối",
                    tutorName + " đã từ chối yêu cầu kết nối của bạn."
                            + (message != null ? " Lý do: " + message : "") + " Bạn có thể chọn ca khác hoặc gia sư khác.",
                    LEARNER_DIRECT_PATH, connection.getLearner().getUser());
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
        notify("connection:" + connection.getRequestId() + ":cancelled", "Học viên đã hủy yêu cầu kết nối",
                user.getFullName() + " đã hủy yêu cầu kết nối" + courseSummary(connection) + " gửi tới bạn.",
                TUTOR_DIRECT_PATH, connection.getTutor().getUser());
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
        // 1) Gia sư chưa phản hồi quá DIRECT_RESPONSE_HOURS giờ
        java.util.Map<UUID, String> reasons = new java.util.LinkedHashMap<>();
        java.util.Map<UUID, ConnectionRequest> stale = new java.util.LinkedHashMap<>();
        for (ConnectionRequest cr : connectionRequestRepository.findByRequestTypeAndStatusAndCreatedAtBefore(
                ConnectionRequestType.DIRECT, ConnectionRequestStatus.PENDING, now.minusHours(BookingLimits.DIRECT_RESPONSE_HOURS))) {
            stale.put(cr.getRequestId(), cr);
            reasons.put(cr.getRequestId(), "Tự động hết hạn vì gia sư chưa phản hồi trong " + BookingLimits.DIRECT_RESPONSE_HOURS + " giờ");
        }
        // 2) Đã tới buổi học đầu tiên mà gia sư chưa phản hồi: không thể đồng ý nữa (thiếu buổi) → nhả chỗ các buổi còn lại ngay
        for (ConnectionRequest cr : connectionRequestRepository.findPendingDirectWithStartedSession()) {
            if (stale.putIfAbsent(cr.getRequestId(), cr) == null) {
                reasons.put(cr.getRequestId(), "Tự động hết hạn vì đã tới buổi học đầu tiên mà gia sư chưa phản hồi");
            }
        }
        for (ConnectionRequest cr : stale.values()) {
            cr.setStatus(ConnectionRequestStatus.EXPIRED);
            cr.setRespondedAt(now);
            cr.setResponseMessage(reasons.get(cr.getRequestId()));
            connectionRequestRepository.save(cr);
            notify("connection:" + cr.getRequestId() + ":expired:learner", "Yêu cầu kết nối đã hết hạn",
                    cr.getTutor().getUser().getFullName() + " chưa phản hồi kịp nên yêu cầu kết nối của bạn đã hết hạn. "
                            + "Bạn có thể gửi lại với tuần bắt đầu mới hoặc chọn gia sư khác.",
                    LEARNER_DIRECT_PATH, cr.getLearner().getUser());
            notify("connection:" + cr.getRequestId() + ":expired:tutor", "Yêu cầu kết nối đã hết hạn",
                    "Yêu cầu kết nối của " + cr.getLearner().getUser().getFullName() + " đã hết hạn vì chưa được phản hồi kịp.",
                    TUTOR_DIRECT_PATH, cr.getTutor().getUser());
        }
        if (!stale.isEmpty()) {
            log.info("Auto-expired {} pending direct connection request(s)", stale.size());
        }
        return stale.size();
    }

    @Override
    @Transactional(readOnly = true)
    public boolean hasUpcomingSessionsInSlot(UUID tutorId, Integer dayOfWeek, LocalTime startTime, LocalTime endTime) {
        if (tutorId == null || dayOfWeek == null || startTime == null || endTime == null) return false;
        return connectionRequestRepository.existsUpcomingSessionInWeeklySlot(tutorId, dayOfWeek, startTime, endTime);
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
        SlotConflict c = slotConflict(tutorId, learnerId, excludeRequestId, r, startDate, true);
        if (c == null) return;
        throw new ConflictException(switch (c) {
            case LOCKED -> lockedMessage;
            case TUTOR_CLASS -> tutorClassMessage;
            case LEARNER_CLASS -> learnerClassMessage;
            case TRIAL -> trialMessage;
        });
    }

    /** Ca hằng tuần bị lịch ĐÃ CHẤP NHẬN chiếm (yêu cầu đang chờ không tính). */
    enum SlotConflict {
        LOCKED, TUTOR_CLASS, LEARNER_CLASS, TRIAL;

        String learnerText(SlotRange r) {
            String when = dayName(r.dayOfWeek()) + " " + r.start().format(HH_MM);
            return switch (this) {
                case LOCKED -> "Giờ " + when + " gia sư đã nhận học viên khác. Vui lòng chọn giờ khác.";
                case TUTOR_CLASS -> "Giờ " + when + " gia sư đã có lớp học. Vui lòng chọn giờ khác.";
                case LEARNER_CLASS -> "Giờ " + when + " trùng lớp học hiện tại của bạn. Vui lòng chọn giờ khác.";
                case TRIAL -> "Giờ " + when + " gia sư đã có buổi học thử. Vui lòng chọn giờ khác.";
            };
        }

        String tutorText(SlotRange r) {
            String when = r.label();
            return switch (this) {
                case LOCKED -> "Ca " + when + " bạn đã nhận cho học viên khác";
                case TUTOR_CLASS -> "Ca " + when + " trùng lịch lớp học của bạn";
                case LEARNER_CLASS -> "Ca " + when + " trùng lớp học hiện tại của học viên";
                case TRIAL -> "Ca " + when + " trùng buổi học thử bạn đã nhận";
            };
        }
    }

    /**
     * Loại trùng của ca hằng tuần {@code r} với lịch đã chấp nhận, hoặc null nếu còn trống.
     * includeTrials: tính cả buổi học thử đã nhận (buổi lẻ) — khi học viên chỉ GỬI yêu cầu thì không tính,
     * vì đó là một buổi lẻ, ca hằng tuần vẫn có thể học các tuần khác.
     */
    private SlotConflict slotConflict(UUID tutorId, UUID learnerId, UUID excludeRequestId, SlotRange r, LocalDate startDate,
                                      boolean includeTrials) {
        if (slotRepository.existsLockedSlotOverlap(tutorId, excludeRequestId, r.dayOfWeek(), r.start(), r.end())) {
            return SlotConflict.LOCKED;
        }
        // Lịch lớp chính thức được kiểm theo NGÀY cụ thể (khóa học có số buổi, buổi dời / hủy riêng):
        // dùng buổi đầu tiên của ca hằng tuần này. class_schedules dùng quy ước 1 = Thứ Hai ... 7 = Chủ nhật.
        LocalDate first = firstOccurrence(startDate, r, LocalDateTime.now(VN_ZONE));
        int isoDow = r.dayOfWeek() == 1 ? 7 : r.dayOfWeek() - 1;
        var clash = trialBookingRepository.findClassScheduleClash(tutorId, learnerId, first, isoDow, r.start(), r.end());
        if (clash.isPresent()) {
            return "TUTOR".equals(clash.get()) ? SlotConflict.TUTOR_CLASS : SlotConflict.LEARNER_CLASS;
        }
        // Ca lặp lại hằng tuần: mọi buổi học thử cùng thứ, trùng giờ, từ buổi đầu tiên trở đi đều bị đụng
        if (includeTrials && trialBookingRepository.existsTrialClashFrom(tutorId, learnerId, first, r.dayOfWeek() - 1, r.start(), r.end())) {
            return SlotConflict.TRIAL;
        }
        return null;
    }

    /** Cảnh báo cho gia sư trên yêu cầu đang chờ: ca nào đã bị lịch đã chấp nhận chiếm (null nếu không có). */
    private String slotWarningForTutor(ConnectionRequest cr) {
        if (cr.getStatus() != ConnectionRequestStatus.PENDING || cr.getSlots().isEmpty()) return null;
        List<String> issues = new ArrayList<>();
        for (ConnectionRequestSlot slot : cr.getSlots()) {
            SlotRange r = new SlotRange(slot.getDayOfWeek(), slot.getStartTime(), slot.getEndTime());
            SlotConflict c = slotConflict(cr.getTutor().getTutorId(), cr.getLearner().getLearnerId(), cr.getRequestId(),
                    r, today(), true);
            if (c != null) issues.add(c.tutorText(r));
        }
        return issues.isEmpty() ? null : String.join("; ", issues) + ". Hãy trao đổi giờ khác với học viên hoặc từ chối.";
    }

    private static LocalDate today() {
        return LocalDate.now(VN_ZONE);
    }

    /** Ngày diễn ra buổi đầu tiên của ca hằng tuần, tính từ startDate và chưa qua thời điểm hiện tại. */
    static LocalDate firstOccurrence(LocalDate startDate, SlotRange r, LocalDateTime now) {
        // 1 = Chủ nhật ... 7 = Thứ Bảy  →  DayOfWeek (1 = Thứ Hai ... 7 = Chủ nhật)
        DayOfWeek target = DayOfWeek.of(r.dayOfWeek() == 1 ? 7 : r.dayOfWeek() - 1);
        LocalDate d = startDate.with(TemporalAdjusters.nextOrSame(target));
        if (!d.atTime(r.start()).isAfter(now)) d = d.plusWeeks(1);
        return d;
    }

    private static final DateTimeFormatter DD_MM = DateTimeFormatter.ofPattern("dd/MM");

    // ------------------------------------------------------------------ thông báo

    /** Trang mở từ thông báo: tab Kết nối trực tiếp của gia sư / của học viên. */
    static final String TUTOR_DIRECT_PATH = "/tutor/connections?tab=direct";
    static final String LEARNER_DIRECT_PATH = "/pages/learner-requests/?tab=direct";
    private static final DateTimeFormatter DMY = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private void notify(String eventKey, String title, String content, String path, User recipient) {
        notifyAs("CONNECTION", eventKey, title, content, path, recipient);
    }

    /** Gửi thông báo cho một người; lỗi tạo thông báo không làm hỏng nghiệp vụ (NotificationEvents tự ghi log). */
    private void notifyAs(String type, String eventKey, String title, String content, String path, User recipient) {
        if (notificationEvents == null || recipient == null || recipient.getUserId() == null) return;
        notificationEvents.publish(eventKey, type, title, content, path, List.of(recipient.getUserId()));
    }

    /** Ví dụ: "Thứ hai 19:00–20:30, Thứ năm 19:00–20:30" (theo ca hằng tuần của yêu cầu). */
    private static String slotSummary(ConnectionRequest cr) {
        return cr.getSlots().stream()
                .map(s -> dayName(s.getDayOfWeek()) + " " + s.getStartTime().format(HH_MM) + "–" + s.getEndTime().format(HH_MM))
                .collect(java.util.stream.Collectors.joining(", "));
    }

    /** Ví dụ: "Thứ hai 19:00, Thứ năm 19:00" (giờ bắt đầu mong muốn của học viên). */
    private static String startSummary(ConnectionRequest cr) {
        return cr.getSlots().stream()
                .map(s -> dayName(s.getDayOfWeek()) + " " + s.getStartTime().format(HH_MM))
                .collect(java.util.stream.Collectors.joining(", "));
    }

    /** Ví dụ: " 8 buổi, bắt đầu 17/11/2026"; rỗng với yêu cầu kiểu cũ chỉ có ca hằng tuần. */
    private static String courseSummary(ConnectionRequest cr) {
        if (cr.getSessions().isEmpty()) return "";
        return " " + cr.getSessions().size() + " buổi, bắt đầu " + cr.getSessions().get(0).getSessionDate().format(DMY);
    }

    private static String sessionLabel(PlannedSession s) {
        return dayName(s.dayOfWeek()) + " " + s.date().format(DD_MM) + " " + s.start().format(HH_MM) + "–" + s.end().format(HH_MM);
    }

    /** Gia sư bấm đồng ý: các buổi CHƯA diễn ra không được trùng những gì gia sư đã xác nhận. */
    private void ensureSessionsFreeForAccept(ConnectionRequest connection, UUID learnerId) {
        LocalDateTime now = LocalDateTime.now(VN_ZONE);
        // Đã có buổi bắt đầu trong lúc chờ duyệt → không cho đồng ý (học viên sẽ nhận ít buổi hơn số đã đăng ký).
        // Gia sư từ chối, học viên gửi lại yêu cầu với tuần bắt đầu mới.
        connection.getSessions().stream()
                .filter(s -> !s.getSessionDate().atTime(s.getStartTime()).isAfter(now))
                .findFirst()
                .ifPresent(started -> {
                    throw new BadRequestException("Buổi " + sessionLabel(new PlannedSession(started.getSessionDate(),
                            ConnectionSchedulePlanner.dayOfWeekOf(started.getSessionDate()), started.getStartTime(), started.getEndTime()))
                            + " trong yêu cầu này đã qua. Hãy từ chối để học viên gửi lại yêu cầu với tuần bắt đầu mới.");
                });
        List<PlannedSession> upcoming = connection.getSessions().stream()
                .map(s -> new PlannedSession(s.getSessionDate(), ConnectionSchedulePlanner.dayOfWeekOf(s.getSessionDate()),
                        s.getStartTime(), s.getEndTime()))
                .toList();
        BusyIndex busy = busyLoader.load(connection.getTutor().getTutorId(), learnerId, connection.getRequestId(),
                today(), upcoming.get(upcoming.size() - 1).date());
        for (PlannedSession s : upcoming) {
            busy.conflictOf(s).ifPresent(kind -> {
                throw new ConflictException("Buổi " + sessionLabel(s) + " " + kind.tutorText()
                        + ". Hãy từ chối và đề nghị học viên chọn tuần bắt đầu hoặc ca khác.");
            });
        }
    }

    /**
     * Học viên vừa được một gia sư nhận: các yêu cầu ĐANG CHỜ của học viên với GIA SƯ KHÁC mà trùng giờ với các buổi
     * vừa nhận không thể diễn ra nữa → tự chuyển "Đã hủy" (nhả chỗ cho gia sư kia, tránh gia sư kia bấm đồng ý vô ích).
     * Gồm yêu cầu kết nối trực tiếp (theo buổi cụ thể) và yêu cầu học thử đang chờ.
     */
    private void cancelLearnerConflictingPending(ConnectionRequest accepted, User actor, OffsetDateTime now) {
        List<ConnectionRequestSession> taken = accepted.getSessions();
        if (taken.isEmpty()) return; // yêu cầu kiểu cũ chỉ có ca hằng tuần: không có ngày cụ thể để so
        UUID learnerId = accepted.getLearner().getLearnerId();
        UUID tutorId = accepted.getTutor().getTutorId();

        int cancelledRequests = 0;
        for (ConnectionRequest other : connectionRequestRepository.findByLearnerLearnerIdAndRequestTypeAndStatus(
                learnerId, ConnectionRequestType.DIRECT, ConnectionRequestStatus.PENDING)) {
            if (other.getRequestId().equals(accepted.getRequestId()) || other.getTutor().getTutorId().equals(tutorId)) continue;
            boolean clash = other.getSessions().stream().anyMatch(s -> taken.stream().anyMatch(t ->
                    t.getSessionDate().equals(s.getSessionDate())
                            && ConnectionSchedulePlanner.overlapsTimes(s.getStartTime(), s.getEndTime(), t.getStartTime(), t.getEndTime())));
            if (!clash) continue;
            other.setStatus(ConnectionRequestStatus.CANCELLED);
            other.setRespondedAt(now);
            other.setResponseMessage("Tự động hủy vì học viên đã kết nối với gia sư khác vào cùng giờ");
            connectionRequestRepository.save(other);
            notify("connection:" + other.getRequestId() + ":auto-cancelled", "Yêu cầu kết nối đã tự hủy",
                    other.getLearner().getUser().getFullName() + " đã kết nối với gia sư khác vào cùng giờ nên yêu cầu gửi bạn đã tự hủy.",
                    TUTOR_DIRECT_PATH, other.getTutor().getUser());
            cancelledRequests++;
        }

        int cancelledTrials = 0;
        for (TrialBooking trial : trialBookingRepository.findByLearnerLearnerIdAndBookingStatusIn(
                learnerId, List.of(TrialBookingStatus.PENDING))) {
            if (trial.getTutor().getTutorId().equals(tutorId) || trial.getScheduledStartTime() == null) continue;
            var start = trial.getScheduledStartTime().atZoneSameInstant(VN_ZONE);
            var end = (trial.getScheduledEndTime() != null ? trial.getScheduledEndTime() : trial.getScheduledStartTime().plusMinutes(30))
                    .atZoneSameInstant(VN_ZONE);
            LocalTime endTime = end.toLocalDate().isAfter(start.toLocalDate()) ? LocalTime.MAX : end.toLocalTime();
            boolean clash = taken.stream().anyMatch(t -> t.getSessionDate().equals(start.toLocalDate())
                    && ConnectionSchedulePlanner.overlapsTimes(start.toLocalTime(), endTime, t.getStartTime(), t.getEndTime()));
            if (!clash) continue;
            trial.setBookingStatus(TrialBookingStatus.CANCELLED);
            ConnectionRequest trialConnection = trial.getConnectionRequest();
            if (trialConnection != null && trialConnection.getStatus() == ConnectionRequestStatus.PENDING) {
                trialConnection.setStatus(ConnectionRequestStatus.CANCELLED);
                trialConnection.setRespondedAt(now);
            }
            trialBookingRepository.save(trial);
            trialBookingHistoryRepository.save(TrialBookingHistory.builder()
                    .trialBooking(trial)
                    .changedByUser(actor)
                    .previousStatus(TrialBookingStatus.PENDING)
                    .newStatus(TrialBookingStatus.CANCELLED)
                    .changeReason("Tự động hủy vì học viên đã kết nối với gia sư khác vào cùng giờ")
                    .build());
            notifyAs("TRIAL", "trial:" + trial.getBookingId() + ":auto-cancelled", "Yêu cầu học thử đã tự hủy",
                    trial.getLearner().getUser().getFullName() + " đã kết nối với gia sư khác vào cùng giờ nên yêu cầu học thử lúc "
                            + trial.getScheduledStartTime().atZoneSameInstant(VN_ZONE).format(DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy"))
                            + " đã tự hủy.",
                    "/tutor/connections", trial.getTutor().getUser());
            cancelledTrials++;
        }
        if (cancelledRequests + cancelledTrials > 0) {
            log.info("Connection {} accepted: auto-cancelled {} pending request(s) and {} pending trial(s) of learner {} with other tutors",
                    accepted.getRequestId(), cancelledRequests, cancelledTrials, learnerId);
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
        // Liên hệ: gia sư xem của học viên (trừ khi đã từ chối / hủy / hết hạn); học viên xem của gia sư khi gia sư đã đồng ý
        var contact = com.kizuna.features.booking.service.ContactShare.Contact.NONE;
        boolean closed = cr.getStatus() == ConnectionRequestStatus.DECLINED || cr.getStatus() == ConnectionRequestStatus.CANCELLED
                || cr.getStatus() == ConnectionRequestStatus.EXPIRED;
        if (!closed && !viewerIsLearner) {
            contact = com.kizuna.features.booking.service.ContactShare.learnerContact(cr, cr.getLearner().getUser());
        } else if (!closed && cr.getStatus() == ConnectionRequestStatus.ACCEPTED) {
            contact = com.kizuna.features.booking.service.ContactShare.tutorContact(cr.getTutor());
        }

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
                .slotWarning(viewerIsLearner ? null : slotWarningForTutor(cr))
                .partnerEmail(contact.email())
                .partnerPhone(contact.phone())
                .build();
    }

    /** Khoảng giờ của một ca hằng tuần (dayOfWeek: 1 = Chủ nhật ... 7 = Thứ Bảy). */
    record SlotRange(Integer dayOfWeek, LocalTime start, LocalTime end) {
        String label() {
            return dayName(dayOfWeek) + " " + start.format(HH_MM) + "–" + end.format(HH_MM);
        }
    }
}
