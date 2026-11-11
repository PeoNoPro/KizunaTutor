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
import com.kizuna.features.booking.dto.request.CancelTrialBookingRequestDTO;
import com.kizuna.features.booking.dto.request.CreateTrialBookingRequestDTO;
import com.kizuna.features.booking.dto.request.RescheduleTrialBookingRequestDTO;
import com.kizuna.features.booking.dto.request.RespondTrialBookingRequestDTO;
import com.kizuna.features.booking.dto.response.TrialBookingResponseDTO;
import com.kizuna.features.booking.entity.*;
import com.kizuna.features.booking.repository.ConnectionRequestRepository;
import com.kizuna.features.booking.repository.TrialBookingHistoryRepository;
import com.kizuna.features.booking.repository.TrialBookingRepository;
import com.kizuna.features.booking.service.TrialBookingService;
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
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
public class TrialBookingServiceImpl implements TrialBookingService {

    private static final ZoneId VN_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    private final UserRepository userRepository;
    private final LearnerRepository learnerRepository;
    private final TutorRepository tutorRepository;
    private final ConnectionRequestRepository connectionRequestRepository;
    private final TrialBookingRepository trialBookingRepository;
    private final TrialBookingHistoryRepository trialBookingHistoryRepository;
    private final TutorScheduleService tutorScheduleService;
    private final JdbcTemplate jdbcTemplate;

    /** FE-12 (thêm 2026-11-10): thông báo tự động trong ứng dụng. Không bắt buộc, nên test dựng service bằng tay vẫn chạy. */
    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private com.kizuna.features.notification.service.NotificationEvents notificationEvents;

    public TrialBookingServiceImpl(
            UserRepository userRepository,
            LearnerRepository learnerRepository,
            TutorRepository tutorRepository,
            ConnectionRequestRepository connectionRequestRepository,
            TrialBookingRepository trialBookingRepository,
            TrialBookingHistoryRepository trialBookingHistoryRepository,
            @Lazy TutorScheduleService tutorScheduleService,
            JdbcTemplate jdbcTemplate) {
        this.userRepository = userRepository;
        this.learnerRepository = learnerRepository;
        this.tutorRepository = tutorRepository;
        this.connectionRequestRepository = connectionRequestRepository;
        this.trialBookingRepository = trialBookingRepository;
        this.trialBookingHistoryRepository = trialBookingHistoryRepository;
        this.tutorScheduleService = tutorScheduleService;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    @Transactional
    public TrialBookingResponseDTO createTrialBooking(String userEmail, CreateTrialBookingRequestDTO request) {
        User learnerUser = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy thông tin tài khoản người dùng"));

        // Kiểm tra phân quyền: chỉ học viên mới được đặt lịch
        if (learnerUser.getRole() == null || !"ROLE_LEARNER".equalsIgnoreCase(learnerUser.getRole().getRoleName())) {
            throw new AccessDeniedException("Chỉ tài khoản học viên mới có thể đặt lịch học thử");
        }

        Learner learner = learnerRepository.findByUser(learnerUser)
                .orElseGet(() -> {
                    Learner newLearner = Learner.builder()
                            .user(learnerUser)
                            .currentLevel(request.getCurrentLevel())
                            .learningGoals(request.getLearningGoals())
                            .build();
                    return learnerRepository.save(newLearner);
                });

        // Luôn cập nhật trình độ và mục tiêu của học viên theo những gì học viên đã nhập trong form đăng ký
        if (request.getCurrentLevel() != null && !request.getCurrentLevel().isBlank()) {
            learner.setCurrentLevel(request.getCurrentLevel().trim());
        }
        if (request.getLearningGoals() != null && !request.getLearningGoals().isBlank()) {
            learner.setLearningGoals(request.getLearningGoals().trim());
        }
        learner = learnerRepository.save(learner);

        Tutor tutor = tutorRepository.findById(request.getTutorId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy gia sư với mã yêu cầu"));

        // Kiểm tra gia sư đã được phê duyệt (Active tutor requirement)
        if (tutor.getVerificationStatus() != TutorVerificationStatus.APPROVED) {
            throw new BadRequestException("Gia sư hiện chưa sẵn sàng nhận học viên mới. Vui lòng chọn gia sư đã được phê duyệt.");
        }

        OffsetDateTime startTime = request.getScheduledStartTime();
        if (startTime == null) {
            throw new BadRequestException("Thời gian bắt đầu học không được để trống");
        }
        if (startTime.isBefore(OffsetDateTime.now())) {
            throw new BadRequestException("Thời gian bắt đầu học phải ở tương lai");
        }

        // Validate thời lượng (chỉ hỗ trợ 25 hoặc 50 phút)
        int duration = (request.getDurationMinutes() != null && request.getDurationMinutes() > 0)
                ? request.getDurationMinutes() : 50;
        if (duration != 25 && duration != 50) {
            throw new BadRequestException("Thời lượng buổi học thử chỉ hỗ trợ 25 phút hoặc 50 phút");
        }
        OffsetDateTime endTime = startTime.plusMinutes(duration);

        // Khóa giao dịch cấp cơ sở dữ liệu theo mã gia sư (Chống Race Condition khi 2 học viên đặt cùng lúc)
        if (jdbcTemplate != null) {
            try {
                long tutorLockKey = Math.abs(tutor.getTutorId().getMostSignificantBits());
                jdbcTemplate.execute("SELECT pg_advisory_xact_lock(" + tutorLockKey + ")");
            } catch (Exception e) {
                log.warn("Không thể kích hoạt pg_advisory_xact_lock: {}", e.getMessage());
            }
        }

        // Kiểm tra trùng lịch gia sư
        boolean hasOverlap = trialBookingRepository.existsOverlappingBooking(tutor.getTutorId(), startTime, endTime);
        if (hasOverlap) {
            throw new ConflictException("Khung giờ này vừa có người đăng ký. Vui lòng chọn khung giờ khác.");
        }

        // Kiểm tra học viên không được đặt 2 lịch trùng giờ
        boolean hasLearnerOverlap = trialBookingRepository.existsOverlappingLearnerBooking(learner.getLearnerId(), startTime, endTime);
        if (hasLearnerOverlap) {
            throw new ConflictException("Bạn đã có một lịch hẹn học thử khác vào khung giờ này. Vui lòng chọn khung giờ khác.");
        }

        // Không trùng lịch học chính thức hằng tuần (lớp FE-06) của gia sư hoặc của học viên
        ensureNoClassScheduleClash(tutor.getTutorId(), learner.getLearnerId(), startTime, endTime,
                "Gia sư đã có lịch dạy lớp chính thức vào khung giờ này. Vui lòng chọn khung giờ khác.",
                "Bạn đã có lịch học lớp chính thức vào khung giờ này. Vui lòng chọn khung giờ khác.");

        // Đổi sang múi giờ Việt Nam (+07:00) trước khi so sánh với lịch rảnh của gia sư
        ZonedDateTime vnStartTime = startTime.atZoneSameInstant(VN_ZONE);
        ZonedDateTime vnEndTime = endTime.atZoneSameInstant(VN_ZONE);
        int dayOfWeek = (vnStartTime.getDayOfWeek().getValue() % 7) + 1; // 1 = Chủ nhật, 2 = Thứ hai...
        LocalTime localStartTime = vnStartTime.toLocalTime();
        LocalTime localEndTime = vnEndTime.toLocalTime();

        // Kiểm tra ca rảnh của gia sư qua Service interface công khai (Tuân thủ AGENTS Luật 3)
        if (tutorScheduleService != null && tutorScheduleService.hasActiveAvailabilities(tutor.getTutorId())) {
            boolean isSlotAvailable = tutorScheduleService.isSlotAvailable(
                    tutor.getTutorId(), dayOfWeek, localStartTime, localEndTime);
            if (!isSlotAvailable) {
                throw new BadRequestException("Gia sư không có lịch rảnh vào khung giờ này. Vui lòng chọn khung giờ khác.");
            }
        }

        // Buổi học thử bắt buộc là học trực tuyến (ONLINE) theo quy định nền tảng
        String mode = "ONLINE";
        String location = null;

        // Tạo yêu cầu kết nối (ConnectionRequest)
        String modeText = "[Hình thức: Học trực tuyến 1-1]";
        String levelText = (request.getCurrentLevel() != null && !request.getCurrentLevel().isBlank())
                ? "\n[Trình độ: " + request.getCurrentLevel() + "]" : "";
        String goalsText = (request.getLearningGoals() != null && !request.getLearningGoals().isBlank())
                ? "\n[Mục tiêu: " + request.getLearningGoals() + "]" : "";
        String msgText = (request.getInitialMessage() != null && !request.getInitialMessage().isBlank())
                ? "\n" + request.getInitialMessage() : "";
        String fullInitialMessage = modeText + levelText + goalsText + msgText;

        ConnectionRequest connectionRequest = ConnectionRequest.builder()
                .learner(learner)
                .tutor(tutor)
                .initialMessage(fullInitialMessage)
                .status(ConnectionRequestStatus.PENDING)
                .build();
        connectionRequest = connectionRequestRepository.save(connectionRequest);

        // Tạo buổi học thử (TrialBooking)
        TrialBooking trialBooking = TrialBooking.builder()
                .connectionRequest(connectionRequest)
                .learner(learner)
                .tutor(tutor)
                .scheduledStartTime(startTime)
                .scheduledEndTime(endTime)
                .learningMode(mode)
                .locationDetails(location)
                .meetingLink(null) // Chưa có link khi ở trạng thái PENDING
                .bookingStatus(TrialBookingStatus.PENDING)
                .learnerConfirmed(true)
                .tutorConfirmed(false)
                .officialArrangementAgreed(false)
                .build();
        trialBooking = trialBookingRepository.save(trialBooking);

        // Lưu vết lịch sử (TrialBookingHistory)
        TrialBookingHistory history = TrialBookingHistory.builder()
                .trialBooking(trialBooking)
                .changedByUser(learnerUser)
                .previousStatus(null)
                .newStatus(TrialBookingStatus.PENDING)
                .changeReason("Học viên khởi tạo yêu cầu học thử 1-1")
                .build();
        trialBookingHistoryRepository.save(history);

        if (notificationEvents != null) {
            notificationEvents.publish("trial:" + trialBooking.getBookingId() + ":created", "TRIAL", "Có yêu cầu học thử mới",
                    learnerUser.getFullName() + " muốn học thử lúc " + fe12When(trialBooking.getScheduledStartTime()) + ". Vui lòng xác nhận hoặc từ chối.",
                    "/tutor/connections", java.util.List.of(tutor.getUser().getUserId()));
        }
        log.info("Learner {} created trial booking {} for tutor {}", learnerUser.getEmail(), trialBooking.getBookingId(), tutor.getTutorId());
        return mapToDTO(trialBooking, learnerUser);
    }

    @Override
    @Transactional
    public TrialBookingResponseDTO respondTrialBooking(String userEmail, UUID bookingId, RespondTrialBookingRequestDTO request) {
        User tutorUser = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy thông tin tài khoản"));

        if (tutorUser.getRole() == null || !"ROLE_TUTOR".equalsIgnoreCase(tutorUser.getRole().getRoleName())) {
            throw new AccessDeniedException("Chỉ tài khoản gia sư mới có quyền phê duyệt yêu cầu học thử");
        }

        Tutor tutor = tutorRepository.findByUser(tutorUser)
                .orElseThrow(() -> new AccessDeniedException("Tài khoản của bạn chưa được liên kết với hồ sơ gia sư"));

        TrialBooking trialBooking = trialBookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy thông tin buổi học thử"));

        if (!trialBooking.getTutor().getTutorId().equals(tutor.getTutorId())) {
            throw new AccessDeniedException("Bạn không có quyền thao tác trên yêu cầu học thử này");
        }

        if (trialBooking.getBookingStatus() != TrialBookingStatus.PENDING) {
            throw new BadRequestException("Buổi học thử này đã được xử lý trước đó");
        }

        TrialBookingStatus prevStatus = trialBooking.getBookingStatus();
        TrialBookingStatus newStatus;
        String reason;

        if ("ACCEPT".equalsIgnoreCase(request.getAction())) {
            OffsetDateTime acceptEnd = trialBooking.getScheduledEndTime() != null ? trialBooking.getScheduledEndTime()
                    : trialBooking.getScheduledStartTime().plusMinutes(50);
            ensureNoClassScheduleClash(trialBooking.getTutor().getTutorId(), trialBooking.getLearner().getLearnerId(),
                    trialBooking.getScheduledStartTime(), acceptEnd,
                    "Buổi học thử trùng với lịch dạy lớp chính thức của bạn. Hãy đề nghị học viên dời lịch hoặc từ chối yêu cầu này.",
                    "Học viên đã có lịch học lớp chính thức vào khung giờ này. Hãy đề nghị học viên dời lịch hoặc từ chối yêu cầu này.");
            newStatus = TrialBookingStatus.CONFIRMED;
            trialBooking.setBookingStatus(newStatus);
            trialBooking.setTutorConfirmed(true);

            // Bắt buộc link phòng học nếu là ONLINE
            String mode = trialBooking.getLearningMode() != null ? trialBooking.getLearningMode() : "ONLINE";
            if ("ONLINE".equalsIgnoreCase(mode)) {
                if (request.getMeetingLink() == null || request.getMeetingLink().isBlank() || !request.getMeetingLink().trim().startsWith("http")) {
                    throw new BadRequestException("Vui lòng cung cấp đường dẫn phòng học trực tuyến hợp lệ (ví dụ: Google Meet https://meet.google.com/...) để xác nhận");
                }
                trialBooking.setMeetingLink(request.getMeetingLink().trim());
            }

            // Chấp nhận lịch học thử KHÔNG đồng nghĩa đã kết nối: kết nối chỉ được xác lập khi buổi học thử
            // đã hoàn thành và gia sư bấm "Đồng ý kết nối" (agreeConnectionAfterTrial).
            reason = request.getNote() != null ? request.getNote() : "Gia sư đã chấp nhận lịch học thử";
        } else if ("DECLINE".equalsIgnoreCase(request.getAction())) {
            // Chuyển sang DECLINED (theo SRS 5.8 và spec)
            newStatus = TrialBookingStatus.DECLINED;
            trialBooking.setBookingStatus(newStatus);

            if (trialBooking.getConnectionRequest() != null) {
                trialBooking.getConnectionRequest().setStatus(ConnectionRequestStatus.DECLINED);
                trialBooking.getConnectionRequest().setRespondedAt(OffsetDateTime.now());
            }
            reason = request.getDeclineReason() != null ? request.getDeclineReason() :
                    (request.getNote() != null ? request.getNote() : "Gia sư đã từ chối lịch học thử");
        } else {
            throw new BadRequestException("Hành động không hợp lệ (chỉ chấp nhận ACCEPT hoặc DECLINE)");
        }

        trialBooking = trialBookingRepository.save(trialBooking);
        if (notificationEvents != null) {
            boolean fe12Accepted = "ACCEPT".equalsIgnoreCase(request.getAction());
            notificationEvents.publish("trial:" + trialBooking.getBookingId() + ":" + (fe12Accepted ? "accepted" : "declined"), "TRIAL",
                    fe12Accepted ? "Gia sư đã nhận lịch học thử" : "Gia sư đã từ chối lịch học thử",
                    trialBooking.getTutor().getUser().getFullName() + (fe12Accepted ? " đã nhận buổi học thử lúc " : " đã từ chối buổi học thử lúc ")
                            + fe12When(trialBooking.getScheduledStartTime()) + ".",
                    "/learner/connections", java.util.List.of(trialBooking.getLearner().getUser().getUserId()));
        }

        // Lưu vết lịch sử
        TrialBookingHistory history = TrialBookingHistory.builder()
                .trialBooking(trialBooking)
                .changedByUser(tutorUser)
                .previousStatus(prevStatus)
                .newStatus(newStatus)
                .changeReason(reason)
                .build();
        trialBookingHistoryRepository.save(history);

        log.info("Tutor {} responded to booking {}: {}", tutorUser.getEmail(), bookingId, request.getAction());
        return mapToDTO(trialBooking, tutorUser);
    }

    @Override
    @Transactional
    public Page<TrialBookingResponseDTO> getMyTrialBookings(String userEmail, String status, int page, int size) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy thông tin tài khoản"));

        // Tự động chuyển các buổi học đã quá giờ kết thúc sang COMPLETED
        try {
            autoCompleteEndedBookings();
        } catch (Exception e) {
            log.warn("Lỗi khi tự động hoàn thành buổi học quá giờ: {}", e.getMessage());
        }

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "scheduledStartTime"));

        TrialBookingStatus statusEnum = null;
        if (status != null && !status.isBlank() && !"ALL".equalsIgnoreCase(status)) {
            try {
                statusEnum = TrialBookingStatus.valueOf(status.trim().toUpperCase());
            } catch (IllegalArgumentException ignored) {}
        }

        Optional<Learner> learnerOpt = learnerRepository.findByUser(user);
        Optional<Tutor> tutorOpt = tutorRepository.findByUser(user);

        Page<TrialBooking> bookingsPage;

        boolean isTutor = user.getRole() != null && "ROLE_TUTOR".equalsIgnoreCase(user.getRole().getRoleName());
        boolean isLearner = user.getRole() != null && "ROLE_LEARNER".equalsIgnoreCase(user.getRole().getRoleName());

        if (isLearner && learnerOpt.isPresent()) {
            UUID learnerId = learnerOpt.get().getLearnerId();
            if (statusEnum != null) {
                bookingsPage = trialBookingRepository.findByLearnerLearnerIdAndBookingStatus(learnerId, statusEnum, pageable);
            } else {
                bookingsPage = trialBookingRepository.findByLearnerLearnerId(learnerId, pageable);
            }
        } else if (isTutor && tutorOpt.isPresent()) {
            UUID tutorId = tutorOpt.get().getTutorId();
            if (statusEnum != null) {
                bookingsPage = trialBookingRepository.findByTutorTutorIdAndBookingStatus(tutorId, statusEnum, pageable);
            } else {
                bookingsPage = trialBookingRepository.findByTutorTutorId(tutorId, pageable);
            }
        } else if (learnerOpt.isPresent()) {
            UUID learnerId = learnerOpt.get().getLearnerId();
            if (statusEnum != null) {
                bookingsPage = trialBookingRepository.findByLearnerLearnerIdAndBookingStatus(learnerId, statusEnum, pageable);
            } else {
                bookingsPage = trialBookingRepository.findByLearnerLearnerId(learnerId, pageable);
            }
        } else if (tutorOpt.isPresent()) {
            UUID tutorId = tutorOpt.get().getTutorId();
            if (statusEnum != null) {
                bookingsPage = trialBookingRepository.findByTutorTutorIdAndBookingStatus(tutorId, statusEnum, pageable);
            } else {
                bookingsPage = trialBookingRepository.findByTutorTutorId(tutorId, pageable);
            }
        } else {
            return Page.empty(pageable);
        }

        return bookingsPage.map(tb -> mapToDTO(tb, user));
    }

    @Override
    @Transactional(readOnly = true)
    public TrialBookingResponseDTO getTrialBookingDetail(String userEmail, UUID bookingId) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy thông tin tài khoản"));

        TrialBooking trialBooking = trialBookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy buổi học thử"));

        boolean isLearner = trialBooking.getLearner().getUser().getUserId().equals(user.getUserId());
        boolean isTutor = trialBooking.getTutor().getUser().getUserId().equals(user.getUserId());

        if (!isLearner && !isTutor) {
            throw new AccessDeniedException("Bạn không có quyền xem thông tin buổi học thử này");
        }

        return mapToDTO(trialBooking, user);
    }

    @Override
    @Transactional
    public TrialBookingResponseDTO cancelTrialBooking(String userEmail, UUID bookingId, CancelTrialBookingRequestDTO request) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy thông tin tài khoản"));

        TrialBooking trialBooking = trialBookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy buổi học thử"));

        boolean isLearner = trialBooking.getLearner().getUser().getUserId().equals(user.getUserId());
        boolean isTutor = trialBooking.getTutor().getUser().getUserId().equals(user.getUserId());

        if (!isLearner && !isTutor) {
            throw new AccessDeniedException("Bạn không có quyền hủy buổi học thử này");
        }

        if (trialBooking.getBookingStatus() == TrialBookingStatus.CANCELLED ||
            trialBooking.getBookingStatus() == TrialBookingStatus.DECLINED ||
            trialBooking.getBookingStatus() == TrialBookingStatus.EXPIRED ||
            trialBooking.getBookingStatus() == TrialBookingStatus.COMPLETED) {
            throw new BadRequestException("Buổi học thử không thể hủy ở trạng thái hiện tại");
        }

        // Quy định hủy lịch: Học viên hủy buổi học CONFIRMED phải trước giờ học ít nhất 24 giờ
        if (isLearner && trialBooking.getBookingStatus() == TrialBookingStatus.CONFIRMED) {
            OffsetDateTime now = OffsetDateTime.now();
            if (now.plusHours(24).isAfter(trialBooking.getScheduledStartTime())) {
                throw new BadRequestException("Theo quy định, bạn chỉ có thể hủy buổi học thử đã xác nhận trước giờ học ít nhất 24 giờ.");
            }
        }

        TrialBookingStatus prevStatus = trialBooking.getBookingStatus();
        trialBooking.setBookingStatus(TrialBookingStatus.CANCELLED);

        if (trialBooking.getConnectionRequest() != null) {
            trialBooking.getConnectionRequest().setStatus(ConnectionRequestStatus.CANCELLED);
            trialBooking.getConnectionRequest().setRespondedAt(OffsetDateTime.now());
        }

        trialBooking = trialBookingRepository.save(trialBooking);
        if (notificationEvents != null) {
            notificationEvents.publish("trial:" + trialBooking.getBookingId() + ":cancelled", "TRIAL", "Buổi học thử đã bị hủy",
                    user.getFullName() + " đã hủy buổi học thử lúc " + fe12When(trialBooking.getScheduledStartTime()) + ".",
                    isLearner ? "/tutor/connections" : "/learner/connections",
                    java.util.List.of((isLearner ? trialBooking.getTutor().getUser() : trialBooking.getLearner().getUser()).getUserId()));
        }

        // Lưu vết lịch sử
        TrialBookingHistory history = TrialBookingHistory.builder()
                .trialBooking(trialBooking)
                .changedByUser(user)
                .previousStatus(prevStatus)
                .newStatus(TrialBookingStatus.CANCELLED)
                .changeReason(request.getCancelReason() != null ? request.getCancelReason() : "Người dùng đã hủy lịch")
                .build();
        trialBookingHistoryRepository.save(history);

        log.info("User {} cancelled booking {}", user.getEmail(), bookingId);
        return mapToDTO(trialBooking, user);
    }

    @Override
    @Transactional
    public TrialBookingResponseDTO rescheduleTrialBooking(String userEmail, UUID bookingId, RescheduleTrialBookingRequestDTO request) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy thông tin tài khoản"));

        TrialBooking trialBooking = trialBookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy buổi học thử"));

        boolean isLearner = trialBooking.getLearner().getUser().getUserId().equals(user.getUserId());
        boolean isTutor = trialBooking.getTutor().getUser().getUserId().equals(user.getUserId());

        if (!isLearner && !isTutor) {
            throw new AccessDeniedException("Bạn không có quyền dời lịch buổi học thử này");
        }

        // Chỉ cho đổi lịch khi đang ở trạng thái PENDING hoặc CONFIRMED
        if (trialBooking.getBookingStatus() != TrialBookingStatus.PENDING &&
            trialBooking.getBookingStatus() != TrialBookingStatus.CONFIRMED) {
            throw new BadRequestException("Chỉ có thể đổi lịch cho buổi học thử đang chờ xác nhận hoặc đã xác nhận");
        }

        // Quy định đổi lịch: Buổi học CONFIRMED phải dời trước giờ học ít nhất 24 giờ (SRS 5.3 AF-1)
        if (trialBooking.getBookingStatus() == TrialBookingStatus.CONFIRMED) {
            OffsetDateTime now = OffsetDateTime.now();
            if (now.plusHours(24).isAfter(trialBooking.getScheduledStartTime())) {
                throw new BadRequestException("Theo quy định, bạn chỉ có thể đổi lịch buổi học đã xác nhận trước giờ học ít nhất 24 giờ. Vui lòng hủy lịch nếu không thể tham dự.");
            }
        }

        OffsetDateTime newStartTime = request.getNewScheduledStartTime();
        if (newStartTime == null || newStartTime.isBefore(OffsetDateTime.now())) {
            throw new BadRequestException("Thời gian dời lịch học phải ở tương lai.");
        }

        int durationMinutes = 25;
        if (trialBooking.getScheduledStartTime() != null && trialBooking.getScheduledEndTime() != null) {
            long diff = Duration.between(trialBooking.getScheduledStartTime(), trialBooking.getScheduledEndTime()).toMinutes();
            if (diff > 0) {
                durationMinutes = (int) diff;
            }
        }
        OffsetDateTime newEndTime = newStartTime.plusMinutes(durationMinutes);

        // Chuyển sang múi giờ Việt Nam (+07:00) để kiểm tra ca rảnh của gia sư
        ZonedDateTime vnStartTime = newStartTime.atZoneSameInstant(VN_ZONE);
        ZonedDateTime vnEndTime = newEndTime.atZoneSameInstant(VN_ZONE);
        int dayOfWeek = (vnStartTime.getDayOfWeek().getValue() % 7) + 1;
        LocalTime localStartTime = vnStartTime.toLocalTime();
        LocalTime localEndTime = vnEndTime.toLocalTime();

        Tutor tutor = trialBooking.getTutor();
        if (tutorScheduleService != null && tutorScheduleService.hasActiveAvailabilities(tutor.getTutorId())) {
            boolean isSlotAvailable = tutorScheduleService.isSlotAvailable(
                    tutor.getTutorId(), dayOfWeek, localStartTime, localEndTime);
            if (!isSlotAvailable) {
                throw new BadRequestException("Gia sư không có lịch rảnh vào khung giờ mới này. Vui lòng chọn khung giờ khác.");
            }
        }

        // Khóa đồng thời cấp database (Concurrency Lock)
        long tutorLockKey = Math.abs(tutor.getTutorId().getMostSignificantBits());
        jdbcTemplate.execute("SELECT pg_advisory_xact_lock(" + tutorLockKey + ")");

        // Kiểm tra trùng lịch gia sư (loại trừ chính booking này)
        boolean tutorOverlaps = trialBookingRepository.existsOverlappingBookingExcludingId(
                tutor.getTutorId(), bookingId, newStartTime, newEndTime);
        if (tutorOverlaps) {
            throw new ConflictException("Gia sư đã có một lịch hẹn học thử khác vào khung giờ mới này. Vui lòng chọn khung giờ khác.");
        }

        // Kiểm tra trùng lịch học viên (loại trừ chính booking này)
        boolean learnerOverlaps = trialBookingRepository.existsOverlappingLearnerBookingExcludingId(
                trialBooking.getLearner().getLearnerId(), bookingId, newStartTime, newEndTime);
        if (learnerOverlaps) {
            throw new ConflictException("Bạn đã có một lịch hẹn học thử khác vào khung giờ mới này. Vui lòng chọn khung giờ khác.");
        }

        // Không trùng lịch học chính thức hằng tuần (lớp FE-06) của gia sư hoặc của học viên
        ensureNoClassScheduleClash(tutor.getTutorId(), trialBooking.getLearner().getLearnerId(), newStartTime, newEndTime,
                "Gia sư đã có lịch dạy lớp chính thức vào khung giờ mới này. Vui lòng chọn khung giờ khác.",
                "Học viên đã có lịch học lớp chính thức vào khung giờ mới này. Vui lòng chọn khung giờ khác.");

        TrialBookingStatus prevStatus = trialBooking.getBookingStatus();
        trialBooking.setScheduledStartTime(newStartTime);
        trialBooking.setScheduledEndTime(newEndTime);
        trialBooking.setBookingStatus(TrialBookingStatus.RESCHEDULED);

        trialBooking = trialBookingRepository.save(trialBooking);
        if (notificationEvents != null) {
            notificationEvents.publish("trial:" + trialBooking.getBookingId() + ":rescheduled:" + trialBooking.getScheduledStartTime(), "TRIAL", "Buổi học thử được đổi lịch",
                    user.getFullName() + " đã đổi buổi học thử sang " + fe12When(trialBooking.getScheduledStartTime()) + ".",
                    isLearner ? "/tutor/connections" : "/learner/connections",
                    java.util.List.of((isLearner ? trialBooking.getTutor().getUser() : trialBooking.getLearner().getUser()).getUserId()));
        }

        String reason = (request.getRescheduleReason() != null && !request.getRescheduleReason().isBlank())
                ? request.getRescheduleReason()
                : "Người dùng yêu cầu đổi lịch học";
        TrialBookingHistory history = TrialBookingHistory.builder()
                .trialBooking(trialBooking)
                .changedByUser(user)
                .previousStatus(prevStatus)
                .newStatus(TrialBookingStatus.RESCHEDULED)
                .changeReason("Dời lịch học sang " + newStartTime + ". Lý do: " + reason)
                .build();
        trialBookingHistoryRepository.save(history);

        log.info("User {} rescheduled booking {} to {}", user.getEmail(), bookingId, newStartTime);
        return mapToDTO(trialBooking, user);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean hasActiveBookingsInSlot(UUID tutorId, Integer dayOfWeek, LocalTime startTime, LocalTime endTime) {
        List<TrialBooking> activeBookings = trialBookingRepository.findByTutorTutorIdAndBookingStatusIn(
                tutorId, List.of(TrialBookingStatus.PENDING, TrialBookingStatus.CONFIRMED));

        for (TrialBooking tb : activeBookings) {
            if (tb.getScheduledStartTime() == null || tb.getScheduledEndTime() == null) continue;
            ZonedDateTime st = tb.getScheduledStartTime().atZoneSameInstant(VN_ZONE);
            ZonedDateTime et = tb.getScheduledEndTime().atZoneSameInstant(VN_ZONE);
            int dow = (st.getDayOfWeek().getValue() % 7) + 1;
            if (dayOfWeek != null && dayOfWeek == dow) {
                LocalTime bStart = st.toLocalTime();
                LocalTime bEnd = et.toLocalTime();
                // Overlap: bStart < endTime && bEnd > startTime
                if (bStart.isBefore(endTime) && bEnd.isAfter(startTime)) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    @Transactional
    public TrialBookingResponseDTO completeTrialBooking(String userEmail, UUID bookingId) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy thông tin tài khoản"));

        TrialBooking trialBooking = trialBookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy buổi học thử"));

        boolean isLearner = trialBooking.getLearner().getUser().getUserId().equals(user.getUserId());
        boolean isTutor = trialBooking.getTutor().getUser().getUserId().equals(user.getUserId());

        if (!isLearner && !isTutor) {
            throw new AccessDeniedException("Bạn không có quyền thao tác trên buổi học thử này");
        }

        if (trialBooking.getBookingStatus() == TrialBookingStatus.COMPLETED) {
            return mapToDTO(trialBooking, user);
        }

        if (trialBooking.getBookingStatus() != TrialBookingStatus.CONFIRMED &&
            trialBooking.getBookingStatus() != TrialBookingStatus.RESCHEDULED) {
            throw new BadRequestException("Chỉ có thể hoàn thành buổi học thử đã được xác nhận");
        }

        OffsetDateTime now = OffsetDateTime.now();
        OffsetDateTime endTime = trialBooking.getScheduledEndTime();
        if (endTime == null && trialBooking.getScheduledStartTime() != null) {
            int duration = trialBooking.getPolicyDurationMinutes() != null ? trialBooking.getPolicyDurationMinutes() : 50;
            endTime = trialBooking.getScheduledStartTime().plusMinutes(duration);
        }

        if (endTime != null && now.isBefore(endTime)) {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm, 'ngày' dd/MM/yyyy");
            String formattedEndTime = endTime.atZoneSameInstant(VN_ZONE).format(formatter);
            throw new BadRequestException("Buổi học chưa kết thúc. Bạn chỉ có thể xác nhận hoàn thành sau " + formattedEndTime + ".");
        }

        TrialBookingStatus prevStatus = trialBooking.getBookingStatus();
        trialBooking.setBookingStatus(TrialBookingStatus.COMPLETED);
        if (isLearner) {
            trialBooking.setLearnerConfirmed(true);
        }
        if (isTutor) {
            trialBooking.setTutorConfirmed(true);
        }

        trialBooking = trialBookingRepository.save(trialBooking);

        TrialBookingHistory history = TrialBookingHistory.builder()
                .trialBooking(trialBooking)
                .changedByUser(user)
                .previousStatus(prevStatus)
                .newStatus(TrialBookingStatus.COMPLETED)
                .changeReason(isTutor ? "Gia sư đã xác nhận hoàn thành buổi học" : "Học viên đã xác nhận hoàn thành buổi học")
                .build();
        trialBookingHistoryRepository.save(history);

        log.info("User {} completed booking {}", user.getEmail(), bookingId);
        return mapToDTO(trialBooking, user);
    }

    @Override
    @Transactional
    public TrialBookingResponseDTO agreeConnectionAfterTrial(String userEmail, UUID bookingId) {
        User tutorUser = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy thông tin tài khoản gia sư"));
        Tutor tutor = tutorRepository.findByUser(tutorUser)
                .orElseThrow(() -> new AccessDeniedException("Chỉ gia sư mới có thể đồng ý kết nối với học viên"));
        TrialBooking trialBooking = trialBookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy buổi học thử"));

        if (!trialBooking.getTutor().getTutorId().equals(tutor.getTutorId())) {
            throw new AccessDeniedException("Bạn không có quyền thao tác trên buổi học thử này");
        }
        if (trialBooking.getBookingStatus() != TrialBookingStatus.COMPLETED) {
            throw new BadRequestException("Chỉ có thể đồng ý kết nối sau khi buổi học thử đã hoàn thành");
        }
        if (Boolean.TRUE.equals(trialBooking.getOfficialArrangementAgreed())) {
            return mapToDTO(trialBooking, tutorUser);
        }

        OffsetDateTime now = OffsetDateTime.now();
        ConnectionRequest connection = trialBooking.getConnectionRequest();
        if (connection == null) {
            UUID learnerId = trialBooking.getLearner().getLearnerId();
            connection = connectionRequestRepository
                    .findByLearnerLearnerIdAndTutorTutorIdAndStatus(learnerId, tutor.getTutorId(), ConnectionRequestStatus.ACCEPTED)
                    .orElseGet(() -> connectionRequestRepository.save(ConnectionRequest.builder()
                            .learner(trialBooking.getLearner())
                            .tutor(tutor)
                            .status(ConnectionRequestStatus.ACCEPTED)
                            .respondedAt(now)
                            .build()));
            trialBooking.setConnectionRequest(connection);
        } else {
            connection.setStatus(ConnectionRequestStatus.ACCEPTED);
            connection.setRespondedAt(now);
        }
        trialBooking.setOfficialArrangementAgreed(true);
        TrialBooking saved = trialBookingRepository.save(trialBooking);

        // Đã kết nối chính thức: các buổi học thử khác chưa diễn ra giữa hai người không còn cần thiết → chuyển "Đã hủy"
        // (giữ lịch sử, không xóa dữ liệu; lưới tuần và danh sách sắp diễn ra vốn ẩn buổi đã hủy).
        cancelOtherOpenTrials(saved, tutorUser, now);

        log.info("Tutor {} agreed to connect with learner {} after trial {}", tutorUser.getEmail(),
                saved.getLearner().getLearnerId(), bookingId);
        return mapToDTO(saved, tutorUser);
    }

    private void cancelOtherOpenTrials(TrialBooking agreed, User tutorUser, OffsetDateTime now) {
        List<TrialBooking> others = trialBookingRepository.findByTutorTutorIdAndLearnerLearnerIdAndBookingStatusIn(
                agreed.getTutor().getTutorId(), agreed.getLearner().getLearnerId(),
                List.of(TrialBookingStatus.PENDING, TrialBookingStatus.CONFIRMED, TrialBookingStatus.RESCHEDULED));
        for (TrialBooking other : others) {
            if (other.getBookingId().equals(agreed.getBookingId())) continue;
            TrialBookingStatus previous = other.getBookingStatus();
            other.setBookingStatus(TrialBookingStatus.CANCELLED);
            if (other.getConnectionRequest() != null
                    && other.getConnectionRequest() != agreed.getConnectionRequest()
                    && other.getConnectionRequest().getStatus() == ConnectionRequestStatus.PENDING) {
                other.getConnectionRequest().setStatus(ConnectionRequestStatus.CANCELLED);
                other.getConnectionRequest().setRespondedAt(now);
            }
            trialBookingRepository.save(other);
            trialBookingHistoryRepository.save(TrialBookingHistory.builder()
                    .trialBooking(other)
                    .changedByUser(tutorUser)
                    .previousStatus(previous)
                    .newStatus(TrialBookingStatus.CANCELLED)
                    .changeReason("Tự động hủy vì gia sư đã đồng ý kết nối chính thức với học viên sau buổi học thử khác")
                    .build());
        }
        if (!others.isEmpty()) {
            log.info("Cancelled {} other open trial(s) after tutor agreed connection on trial {}", others.size(), agreed.getBookingId());
        }
    }

    /**
     * Buổi học thử không được trùng lịch học chính thức hằng tuần của gia sư hoặc học viên (đọc lịch lớp FE-06).
     * Khung giờ quy đổi sang giờ Việt Nam; buổi kéo qua nửa đêm được tính tới 23:59:59 của ngày bắt đầu.
     */
    private void ensureNoClassScheduleClash(UUID tutorId, UUID learnerId, OffsetDateTime start, OffsetDateTime end,
                                            String tutorMessage, String learnerMessage) {
        if (start == null || end == null) return;
        ZonedDateTime vnStart = start.atZoneSameInstant(VN_ZONE);
        ZonedDateTime vnEnd = end.atZoneSameInstant(VN_ZONE);
        java.time.LocalTime endTime = vnEnd.toLocalDate().isAfter(vnStart.toLocalDate())
                ? java.time.LocalTime.of(23, 59, 59) : vnEnd.toLocalTime();
        trialBookingRepository.findClassScheduleClash(tutorId, learnerId, vnStart.getDayOfWeek().getValue(),
                        vnStart.toLocalTime(), endTime)
                .ifPresent(who -> {
                    throw new ConflictException("TUTOR".equals(who) ? tutorMessage : learnerMessage);
                });
    }

    private void autoCompleteEndedBookings() {
        OffsetDateTime now = OffsetDateTime.now();
        List<TrialBooking> endedBookings = trialBookingRepository.findByBookingStatusInAndScheduledEndTimeBefore(
                List.of(TrialBookingStatus.CONFIRMED, TrialBookingStatus.RESCHEDULED), now);
        if (!endedBookings.isEmpty()) {
            for (TrialBooking tb : endedBookings) {
                TrialBookingStatus prevStatus = tb.getBookingStatus();
                tb.setBookingStatus(TrialBookingStatus.COMPLETED);
                trialBookingRepository.save(tb);

                TrialBookingHistory history = TrialBookingHistory.builder()
                        .trialBooking(tb)
                        .changedByUser(tb.getTutor() != null ? tb.getTutor().getUser() : null)
                        .previousStatus(prevStatus)
                        .newStatus(TrialBookingStatus.COMPLETED)
                        .changeReason("Hệ thống tự động cập nhật hoàn thành khi hết giờ học thử")
                        .build();
                trialBookingHistoryRepository.save(history);
            }
            log.info("Auto-completed {} trial bookings that reached end time", endedBookings.size());
        }
    }

    private TrialBookingResponseDTO mapToDTO(TrialBooking tb, User currentUser) {
        boolean isCurrentUserLearner = tb.getLearner().getUser().getUserId().equals(currentUser.getUserId());
        User partnerUser = isCurrentUserLearner ? tb.getTutor().getUser() : tb.getLearner().getUser();
        String partnerRole = isCurrentUserLearner ? "Gia sư" : "Học viên";

        String statusLabel = switch (tb.getBookingStatus()) {
            case PENDING -> "Chờ gia sư xác nhận";
            case CONFIRMED -> "Đã xác nhận";
            case COMPLETED -> "Đã hoàn thành";
            case CANCELLED -> "Đã hủy";
            case RESCHEDULED -> "Đã dời lịch";
            case DECLINED -> "Đã từ chối";
            case EXPIRED -> "Đã hết hạn";
        };

        int duration = 50;
        if (tb.getScheduledStartTime() != null && tb.getScheduledEndTime() != null) {
            duration = (int) Duration.between(tb.getScheduledStartTime(), tb.getScheduledEndTime()).toMinutes();
        }

        String mode = tb.getLearningMode() != null ? tb.getLearningMode() : "ONLINE";
        String location = tb.getLocationDetails();

        String goals = tb.getLearner() != null ? tb.getLearner().getLearningGoals() : null;
        String level = tb.getLearner() != null ? tb.getLearner().getCurrentLevel() : null;

        // Ưu tiên trích xuất chính xác trình độ và mục tiêu mà học viên đã nhập cho buổi học thử này từ ConnectionRequest
        if (tb.getConnectionRequest() != null && tb.getConnectionRequest().getInitialMessage() != null) {
            String msg = tb.getConnectionRequest().getInitialMessage();
            if (msg.contains("[Trình độ: ")) {
                int start = msg.indexOf("[Trình độ: ") + 11;
                int end = msg.indexOf("]", start);
                if (end > start) {
                    level = msg.substring(start, end).trim();
                }
            }
            if (msg.contains("[Mục tiêu: ")) {
                int start = msg.indexOf("[Mục tiêu: ") + 11;
                int end = msg.indexOf("]", start);
                if (end > start) {
                    goals = msg.substring(start, end).trim();
                }
            } else {
                // Nếu buổi học thử cụ thể này học viên không điền mục tiêu, không hiển thị mục tiêu mặc định cũ từ hồ sơ
                goals = null;
            }
        } else if (tb.getConnectionRequest() == null && tb.getLocationDetails() != null && !tb.getLocationDetails().isBlank()) {
            goals = tb.getLocationDetails().trim();
        }

        String initialMsg = tb.getConnectionRequest() != null ? tb.getConnectionRequest().getInitialMessage() : null;
        String cleanInitialMsg = initialMsg;
        if (cleanInitialMsg != null) {
            cleanInitialMsg = cleanInitialMsg
                    .replaceAll("\\[Hình thức:[^\\]]*\\]", "")
                    .replaceAll("\\[Trình độ:[^\\]]*\\]", "")
                    .replaceAll("\\[Mục tiêu:[^\\]]*\\]", "")
                    .trim();
            if (cleanInitialMsg.isEmpty()) {
                cleanInitialMsg = null;
            }
        }

        return TrialBookingResponseDTO.builder()
                .bookingId(tb.getBookingId())
                .officialArrangementAgreed(Boolean.TRUE.equals(tb.getOfficialArrangementAgreed()))
                .requestId(tb.getConnectionRequest() != null ? tb.getConnectionRequest().getRequestId() : null)
                .tutorId(tb.getTutor().getTutorId())
                .tutorName(tb.getTutor().getUser().getFullName())
                .learnerId(tb.getLearner().getLearnerId())
                .partnerId(partnerUser.getUserId())
                .partnerName(partnerUser.getFullName() != null ? partnerUser.getFullName() : partnerUser.getEmail())
                .partnerAvatarUrl(partnerUser.getAvatarUrl())
                .partnerRole(partnerRole)
                .scheduledStartTime(tb.getScheduledStartTime())
                .scheduledEndTime(tb.getScheduledEndTime())
                .durationMinutes(duration)
                .learningMode(mode)
                .meetingLink(tb.getMeetingLink())
                .locationDetails(location)
                .currentLevel(level)
                .learningGoals(goals)
                .initialMessage(cleanInitialMsg)
                .bookingStatus(tb.getBookingStatus().name())
                .statusLabel(statusLabel)
                .createdAt(tb.getCreatedAt())
                .updatedAt(tb.getUpdatedAt())
                .build();
    }
    /** FE-12: giờ hiển thị trong thông báo (giờ Việt Nam). */
    private static String fe12When(java.time.OffsetDateTime time) {
        return time == null ? "" : time.atZoneSameInstant(java.time.ZoneId.of("Asia/Ho_Chi_Minh"))
                .format(java.time.format.DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy"));
    }
}
