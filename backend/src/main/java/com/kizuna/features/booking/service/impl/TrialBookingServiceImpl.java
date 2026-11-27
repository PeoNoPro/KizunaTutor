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
import com.kizuna.features.booking.service.BookingLimits;
import com.kizuna.features.booking.service.ConnectionBusyLoader;
import com.kizuna.features.booking.service.ConnectionSchedulePlanner;
import com.kizuna.features.booking.service.TrialBookingService;
import com.kizuna.features.verification.entity.TutorVerificationStatus;
import lombok.extern.slf4j.Slf4j;
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
    private final JdbcTemplate jdbcTemplate;
    private final ConnectionBusyLoader busyLoader;

    /**
     * FE-12 (thêm 2026-11-10): thông báo tự động trong ứng dụng. Không bắt
     * buộc, nên test dựng service bằng tay vẫn chạy.
     */
    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private com.kizuna.features.notification.service.NotificationEvents notificationEvents;

    /** Gói hội viên (FE-09): gia sư hết gói không nhận học viên mới. Không bắt buộc (test dựng tay) → bỏ qua kiểm tra. */
    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private com.kizuna.features.payment.service.TutorMembershipService membershipService;

    /** Cùng điều kiện với trang tìm kiếm (TutorSearchSpecification) để gia sư bị ẩn thì cũng không đặt được. */
    private void requireActiveMembership(Tutor tutor, String message) {
        if (membershipService != null
                && !membershipService.hasActiveMembership(tutor.getTutorId(), java.time.LocalDate.now(VN_ZONE))) {
            throw new BadRequestException(message);
        }
    }

    public TrialBookingServiceImpl(
            UserRepository userRepository,
            LearnerRepository learnerRepository,
            TutorRepository tutorRepository,
            ConnectionRequestRepository connectionRequestRepository,
            TrialBookingRepository trialBookingRepository,
            TrialBookingHistoryRepository trialBookingHistoryRepository,
            JdbcTemplate jdbcTemplate,
            ConnectionBusyLoader busyLoader) {
        this.busyLoader = busyLoader;
        this.userRepository = userRepository;
        this.learnerRepository = learnerRepository;
        this.tutorRepository = tutorRepository;
        this.connectionRequestRepository = connectionRequestRepository;
        this.trialBookingRepository = trialBookingRepository;
        this.trialBookingHistoryRepository = trialBookingHistoryRepository;
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
        requireActiveMembership(tutor, ConnectionRequestServiceImpl.TUTOR_NO_MEMBERSHIP_FOR_LEARNER);

        OffsetDateTime startTime = request.getScheduledStartTime();
        if (startTime == null) {
            throw new BadRequestException("Thời gian bắt đầu học không được để trống");
        }
        if (startTime.isBefore(OffsetDateTime.now())) {
            throw new BadRequestException("Thời gian bắt đầu học phải ở tương lai");
        }

        // Validate thời lượng (chỉ hỗ trợ 30 phút theo quy định đăng ký học thử)
        int duration = (request.getDurationMinutes() != null && request.getDurationMinutes() > 0)
                ? request.getDurationMinutes() : 30;
        if (duration != 30) {
            throw new BadRequestException("Thời lượng buổi học thử là 30 phút");
        }
        OffsetDateTime endTime = startTime.plusMinutes(duration);

        // Cách đặt chính: giờ trống trong lịch rảnh. Cách phụ "Đề xuất giờ khác": ngoài lịch rảnh, học viên phải chủ động
        // bật (request.offSchedule = true) và bị giới hạn số lần mỗi tuần. Trong cả hai trường hợp đều chặn trùng với giờ
        // gia sư / học viên đang bận (kể cả yêu cầu đang chờ của gia sư). Gia sư vẫn kiểm tra lại khi bấm "Đồng ý".
        // Chống spam: khóa theo gia sư để hai lần bấm gửi liên tiếp không cùng lọt qua các kiểm tra bên dưới
        lockTutor(tutor.getTutorId());
        // Yêu cầu cũ đã quá hạn của học viên này không còn tính là "đang mở" (giới hạn số yêu cầu chờ, trùng gia sư).
        // Chỉ xử lý của học viên này; phần còn lại do BookingExpiryScheduler quét định kỳ.
        OffsetDateTime checkedAt = OffsetDateTime.now();
        for (TrialBooking stale : trialBookingRepository.findStalePendingOfLearnerLocked(learner.getLearnerId(),
                TrialBookingStatus.PENDING, checkedAt.minusHours(BookingLimits.TRIAL_RESPONSE_HOURS), checkedAt)) {
            expireTrial(stale, checkedAt);
        }

        // Mỗi cặp học viên - gia sư chỉ có tối đa 1 buổi học thử đang mở
        if (trialBookingRepository.existsByLearnerLearnerIdAndTutorTutorIdAndBookingStatusIn(
                learner.getLearnerId(), tutor.getTutorId(), BookingLimits.OPEN_TRIAL_STATUSES)) {
            throw new ConflictException("Bạn đang có một yêu cầu học thử với gia sư này. "
                    + "Hãy chờ gia sư phản hồi, đổi lịch hoặc hủy yêu cầu cũ trước khi gửi yêu cầu mới.");
        }
        // Mỗi học viên chỉ chờ tối đa MAX_PENDING_TRIALS_PER_LEARNER gia sư phản hồi học thử cùng lúc
        if (trialBookingRepository.countByLearnerLearnerIdAndBookingStatus(learner.getLearnerId(), TrialBookingStatus.PENDING)
                >= BookingLimits.MAX_PENDING_TRIALS_PER_LEARNER) {
            throw new ConflictException("Bạn đang chờ " + BookingLimits.MAX_PENDING_TRIALS_PER_LEARNER
                    + " gia sư phản hồi yêu cầu học thử. Hãy chờ phản hồi hoặc hủy bớt một yêu cầu trước khi gửi thêm.");
        }
        // Vừa bị gia sư này từ chối thì phải chờ một thời gian mới được gửi lại
        if (trialBookingRepository.existsByLearnerLearnerIdAndTutorTutorIdAndBookingStatusAndUpdatedAtAfter(
                learner.getLearnerId(), tutor.getTutorId(), TrialBookingStatus.DECLINED,
                OffsetDateTime.now().minusHours(BookingLimits.DECLINE_COOLDOWN_HOURS))) {
            throw new ConflictException("Gia sư vừa từ chối yêu cầu học thử của bạn. Bạn có thể gửi lại sau "
                    + BookingLimits.DECLINE_COOLDOWN_HOURS + " giờ, hoặc chọn gia sư khác.");
        }
        // Giờ trong lịch rảnh = cách đặt chính; ngoài lịch rảnh = "Đề xuất giờ khác" (gia sư đồng ý hoặc từ chối)
        boolean offSchedule = !isWithinAvailability(tutor.getTutorId(), startTime, endTime);
        if (offSchedule && !Boolean.TRUE.equals(request.getOffSchedule())) {
            throw new BadRequestException("Giờ bạn chọn nằm ngoài lịch rảnh của gia sư. Hãy chọn một giờ trống trên lịch, "
                    + "hoặc dùng \"Đề xuất giờ khác\" để gửi đề xuất cho gia sư.");
        }
        if (offSchedule && trialBookingRepository.countOffScheduleSince(learner.getLearnerId(), tutor.getTutorId(),
                OffsetDateTime.now().minusDays(7)) >= BookingLimits.OFF_SCHEDULE_PROPOSALS_PER_WEEK) {
            throw new ConflictException("Bạn chỉ có thể đề xuất giờ ngoài lịch rảnh tối đa "
                    + BookingLimits.OFF_SCHEDULE_PROPOSALS_PER_WEEK + " lần mỗi tuần với gia sư này. "
                    + "Hãy chọn một giờ trống trên lịch của gia sư.");
        }
        // Dù trong hay ngoài lịch rảnh, không được đè lên giờ gia sư đang bận thật
        // (lịch lớp, buổi của khóa kết nối, buổi học thử khác đã nhận hoặc đang chờ duyệt)
        ensureTutorFreeAt(tutor.getTutorId(), learner.getLearnerId(), startTime, endTime);

        // Buổi học thử bắt buộc là học trực tuyến (ONLINE) theo quy định nền tảng
        String mode = "ONLINE";
        String location = null;

        // Tạo yêu cầu kết nối (ConnectionRequest)
        String modeText = "[Hình thức: Học trực tuyến 1-1]";
        // Trình độ lấy từ hồ sơ học viên (form không còn bắt nhập lại)
        String profileLevel = BookingLimits.levelLabel(learner.getCurrentLevel());
        String levelText = profileLevel != null ? "\n[Trình độ: " + profileLevel + "]" : "";
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
        // Học viên đồng ý gửi hồ sơ + email + số điện thoại cho gia sư
        com.kizuna.features.booking.service.ContactShare.attachLearnerContact(connectionRequest, learnerUser,
                request.getContactEmail(), request.getContactPhone(), request.getShareContact());
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
                .offSchedule(offSchedule)
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

        // Khóa theo gia sư TRƯỚC khi khóa dòng: hai lần bấm "Đồng ý" cho hai yêu cầu trùng giờ không cùng lọt qua,
        // và mọi thao tác của gia sư khóa cùng một thứ tự (tránh deadlock)
        lockTutor(tutor.getTutorId());
        TrialBooking trialBooking = trialBookingRepository.findLockedById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy thông tin buổi học thử"));

        if (!trialBooking.getTutor().getTutorId().equals(tutor.getTutorId())) {
            throw new AccessDeniedException("Bạn không có quyền thao tác trên yêu cầu học thử này");
        }

        if (trialBooking.getBookingStatus() != TrialBookingStatus.PENDING) {
            throw new BadRequestException("Buổi học thử này đã được xử lý trước đó");
        }
        if (isStalePending(trialBooking, OffsetDateTime.now())) {
            // Tác vụ nền sẽ chuyển yêu cầu này sang "Đã hết hạn"
            throw new BadRequestException("Yêu cầu học thử này đã hết hạn phản hồi. Học viên có thể gửi lại yêu cầu mới.");
        }

        TrialBookingStatus prevStatus = trialBooking.getBookingStatus();
        TrialBookingStatus newStatus;
        String reason;

        if ("ACCEPT".equalsIgnoreCase(request.getAction())) {
            requireActiveMembership(trialBooking.getTutor(), ConnectionRequestServiceImpl.TUTOR_NO_MEMBERSHIP_FOR_TUTOR);
            OffsetDateTime acceptEnd = trialBooking.getScheduledEndTime() != null ? trialBooking.getScheduledEndTime()
                    : trialBooking.getScheduledStartTime().plusMinutes(30);
            if (!trialBooking.getScheduledStartTime().isAfter(OffsetDateTime.now())) {
                throw new BadRequestException("Thời gian học thử học viên chọn đã qua. Hãy từ chối hoặc đề nghị học viên dời lịch.");
            }
            if (trialBookingRepository.existsConfirmedTutorOverlap(trialBooking.getTutor().getTutorId(), bookingId,
                    trialBooking.getScheduledStartTime(), acceptEnd)) {
                throw new ConflictException("Bạn đã nhận một buổi học thử khác trùng khung giờ này. Hãy đề nghị học viên dời lịch hoặc từ chối yêu cầu này.");
            }
            if (trialBookingRepository.existsConfirmedLearnerOverlap(trialBooking.getLearner().getLearnerId(), bookingId,
                    trialBooking.getScheduledStartTime(), acceptEnd)) {
                throw new ConflictException("Học viên đã có một buổi học thử khác được xác nhận vào khung giờ này. Hãy đề nghị học viên dời lịch hoặc từ chối yêu cầu này.");
            }
            ensureNoClassScheduleClash(trialBooking.getTutor().getTutorId(), trialBooking.getLearner().getLearnerId(),
                    trialBooking.getScheduledStartTime(), acceptEnd,
                    "Buổi học thử trùng với lịch dạy lớp chính thức của bạn. Hãy đề nghị học viên dời lịch hoặc từ chối yêu cầu này.",
                    "Học viên đã có lịch học lớp chính thức vào khung giờ này. Hãy đề nghị học viên dời lịch hoặc từ chối yêu cầu này.");
            ensureNoConnectionSessionClash(trialBooking.getTutor().getTutorId(), trialBooking.getScheduledStartTime(),
                    acceptEnd, false,
                    "Bạn đã có buổi học kết nối với học viên khác vào khung giờ này. Hãy đề nghị học viên dời lịch hoặc từ chối yêu cầu này.");
            ensureLearnerFreeForAccept(trialBooking.getTutor().getTutorId(), trialBooking.getLearner().getLearnerId(),
                    trialBooking.getScheduledStartTime(), acceptEnd);
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
            reason = request.getDeclineReason() != null ? request.getDeclineReason()
                    : (request.getNote() != null ? request.getNote() : "Gia sư đã từ chối lịch học thử");
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
                    // Đã nhận → buổi học nằm trên Lịch học; bị từ chối → xem lý do ở Yêu cầu của tôi
                    fe12Accepted ? LEARNER_SCHEDULE_PATH : LEARNER_TRIAL_REQUESTS_PATH,
                    java.util.List.of(trialBooking.getLearner().getUser().getUserId()));
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

        // Gia sư đã nhận học viên này: các yêu cầu học thử KHÁC đang chờ trùng giờ không thể diễn ra nữa → tự hủy, báo học viên
        if (newStatus == TrialBookingStatus.CONFIRMED) {
            cancelOverlappingPendingTrials(trialBooking, tutorUser);
        }

        log.info("Tutor {} responded to booking {}: {}", tutorUser.getEmail(), bookingId, request.getAction());
        return mapToDTO(trialBooking, tutorUser);
    }

    @Override
    @Transactional
    public Page<TrialBookingResponseDTO> getMyTrialBookings(String userEmail, String status, int page, int size) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy thông tin tài khoản"));

        // Việc tự hoàn thành buổi đã qua giờ và tự hết hạn yêu cầu quá hạn do tác vụ nền BookingExpiryScheduler
        // đảm nhiệm (mỗi 5 phút), không chạy ở đây nữa để mở danh sách nhanh hơn (DB ở xa, mỗi query ~110 ms).
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "scheduledStartTime"));

        TrialBookingStatus statusEnum = null;
        if (status != null && !status.isBlank() && !"ALL".equalsIgnoreCase(status)) {
            try {
                statusEnum = TrialBookingStatus.valueOf(status.trim().toUpperCase());
            } catch (IllegalArgumentException ignored) {
            }
        }

        boolean isTutor = user.getRole() != null && "ROLE_TUTOR".equalsIgnoreCase(user.getRole().getRoleName());
        boolean isLearner = user.getRole() != null && "ROLE_LEARNER".equalsIgnoreCase(user.getRole().getRoleName());

        // Chỉ tra hồ sơ đúng vai trò; vai trò khác mới tra cả hai (giữ hành vi cũ)
        Optional<Learner> learnerOpt = isTutor ? Optional.empty() : learnerRepository.findByUser(user);
        Optional<Tutor> tutorOpt = isLearner ? Optional.empty() : tutorRepository.findByUser(user);

        Page<TrialBooking> bookingsPage;

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

        TrialBooking trialBooking = trialBookingRepository.findLockedById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy buổi học thử"));

        boolean isLearner = trialBooking.getLearner().getUser().getUserId().equals(user.getUserId());
        boolean isTutor = trialBooking.getTutor().getUser().getUserId().equals(user.getUserId());

        if (!isLearner && !isTutor) {
            throw new AccessDeniedException("Bạn không có quyền hủy buổi học thử này");
        }

        if (trialBooking.getBookingStatus() == TrialBookingStatus.CANCELLED
                || trialBooking.getBookingStatus() == TrialBookingStatus.DECLINED
                || trialBooking.getBookingStatus() == TrialBookingStatus.EXPIRED
                || trialBooking.getBookingStatus() == TrialBookingStatus.COMPLETED) {
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
                    isLearner ? "/tutor/connections" : LEARNER_TRIAL_REQUESTS_PATH,
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

        TrialBooking trialBooking = trialBookingRepository.findLockedById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy buổi học thử"));

        boolean isLearner = trialBooking.getLearner().getUser().getUserId().equals(user.getUserId());
        boolean isTutor = trialBooking.getTutor().getUser().getUserId().equals(user.getUserId());

        if (!isLearner && !isTutor) {
            throw new AccessDeniedException("Bạn không có quyền dời lịch buổi học thử này");
        }

        // Chỉ cho đổi lịch khi buổi học thử còn đang mở (chờ xác nhận, đã xác nhận, đã dời lịch)
        if (!BookingLimits.OPEN_TRIAL_STATUSES.contains(trialBooking.getBookingStatus())) {
            throw new BadRequestException("Chỉ có thể đổi lịch cho buổi học thử đang chờ xác nhận hoặc đã xác nhận");
        }

        // Quy định đổi lịch: Buổi học đã xác nhận phải dời trước giờ học ít nhất 24 giờ (SRS 5.3 AF-1)
        if (trialBooking.getBookingStatus() != TrialBookingStatus.PENDING) {
            OffsetDateTime now = OffsetDateTime.now();
            if (now.plusHours(24).isAfter(trialBooking.getScheduledStartTime())) {
                throw new BadRequestException("Theo quy định, bạn chỉ có thể đổi lịch buổi học đã xác nhận trước giờ học ít nhất 24 giờ. Vui lòng hủy lịch nếu không thể tham dự.");
            }
        }

        OffsetDateTime newStartTime = request.getNewScheduledStartTime();
        if (newStartTime == null || newStartTime.isBefore(OffsetDateTime.now())) {
            throw new BadRequestException("Thời gian dời lịch học phải ở tương lai.");
        }

        int durationMinutes = 30;
        if (trialBooking.getScheduledStartTime() != null && trialBooking.getScheduledEndTime() != null) {
            long diff = Duration.between(trialBooking.getScheduledStartTime(), trialBooking.getScheduledEndTime()).toMinutes();
            if (diff > 0) {
                durationMinutes = (int) diff;
            }
        }
        OffsetDateTime newEndTime = newStartTime.plusMinutes(durationMinutes);

        // Giờ học thử do học viên tự chọn, không ràng buộc lịch rảnh. Vẫn chặn trùng với các buổi học thử
        // đã xác nhận và lịch lớp chính thức để học viên biết sớm.
        Tutor tutor = trialBooking.getTutor();
        lockTutor(tutor.getTutorId());

        // Kiểm tra trùng lịch gia sư (loại trừ chính booking này)
        boolean tutorOverlaps = trialBookingRepository.existsConfirmedTutorOverlap(
                tutor.getTutorId(), bookingId, newStartTime, newEndTime);
        if (tutorOverlaps) {
            throw new ConflictException("Gia sư đã có một lịch hẹn học thử khác vào khung giờ mới này. Vui lòng chọn khung giờ khác.");
        }

        // Kiểm tra trùng lịch học viên (loại trừ chính booking này)
        boolean learnerOverlaps = trialBookingRepository.existsConfirmedLearnerOverlap(
                trialBooking.getLearner().getLearnerId(), bookingId, newStartTime, newEndTime);
        if (learnerOverlaps) {
            throw new ConflictException("Bạn đã có một lịch hẹn học thử khác vào khung giờ mới này. Vui lòng chọn khung giờ khác.");
        }

        // Không trùng lịch học chính thức hằng tuần (lớp FE-06) của gia sư hoặc của học viên
        ensureNoClassScheduleClash(tutor.getTutorId(), trialBooking.getLearner().getLearnerId(), newStartTime, newEndTime,
                "Gia sư đã có lịch dạy lớp chính thức vào khung giờ mới này. Vui lòng chọn khung giờ khác.",
                "Học viên đã có lịch học lớp chính thức vào khung giờ mới này. Vui lòng chọn khung giờ khác.");
        ensureNoConnectionSessionClash(tutor.getTutorId(), newStartTime, newEndTime, false,
                "Gia sư đã có buổi học với học viên khác vào khung giờ mới này. Vui lòng chọn khung giờ khác.");

        boolean newOffSchedule = !isWithinAvailability(tutor.getTutorId(), newStartTime, newEndTime);
        UUID learnerId = trialBooking.getLearner().getLearnerId();
        if (isLearner && newOffSchedule) {
            // Đổi sang giờ ngoài lịch rảnh = một lần "Đề xuất giờ khác": tính chung giới hạn mỗi tuần với lúc tạo mới
            long proposals = trialBookingRepository.countOffScheduleSince(learnerId, tutor.getTutorId(), OffsetDateTime.now().minusDays(7))
                    + trialBookingRepository.countOffScheduleReschedulesSince(learnerId, tutor.getTutorId(), OffsetDateTime.now().minusDays(7));
            if (proposals >= BookingLimits.OFF_SCHEDULE_PROPOSALS_PER_WEEK) {
                throw new ConflictException("Bạn chỉ có thể đề xuất giờ ngoài lịch rảnh tối đa "
                        + BookingLimits.OFF_SCHEDULE_PROPOSALS_PER_WEEK + " lần mỗi tuần với gia sư này. "
                        + "Hãy chọn một giờ trống trên lịch của gia sư.");
            }
        }
        if (!isLearner) {
            // Gia sư tự dời lịch thì buổi được chốt luôn (RESCHEDULED), không ai duyệt lại:
            // giờ mới không được trùng lịch đã xác nhận của học viên với gia sư khác.
            ensureLearnerFreeForAccept(tutor.getTutorId(), learnerId, newStartTime, newEndTime);
        }

        TrialBookingStatus prevStatus = trialBooking.getBookingStatus();
        trialBooking.setOffSchedule(newOffSchedule);
        trialBooking.setScheduledStartTime(newStartTime);
        trialBooking.setScheduledEndTime(newEndTime);
        // Học viên đổi giờ → quay về "Chờ gia sư xác nhận" để gia sư đồng ý/từ chối giờ mới.
        // Gia sư tự đổi giờ thì coi như gia sư đã đồng ý giờ mới.
        TrialBookingStatus newStatus = isLearner ? TrialBookingStatus.PENDING : TrialBookingStatus.RESCHEDULED;
        trialBooking.setBookingStatus(newStatus);
        if (isLearner) {
            trialBooking.setTutorConfirmed(false);
        }

        trialBooking = trialBookingRepository.save(trialBooking);
        if (notificationEvents != null) {
            notificationEvents.publish("trial:" + trialBooking.getBookingId() + ":rescheduled:" + trialBooking.getScheduledStartTime(), "TRIAL", "Buổi học thử được đổi lịch",
                    user.getFullName() + " đã đổi buổi học thử sang " + fe12When(trialBooking.getScheduledStartTime()) + ".",
                    // Gia sư tự dời lịch → buổi đã chốt, nằm trên Lịch học của học viên
                    isLearner ? "/tutor/connections" : LEARNER_SCHEDULE_PATH,
                    java.util.List.of((isLearner ? trialBooking.getTutor().getUser() : trialBooking.getLearner().getUser()).getUserId()));
        }

        String reason = (request.getRescheduleReason() != null && !request.getRescheduleReason().isBlank())
                ? request.getRescheduleReason()
                : "Người dùng yêu cầu đổi lịch học";
        TrialBookingHistory history = TrialBookingHistory.builder()
                .trialBooking(trialBooking)
                .changedByUser(user)
                .previousStatus(prevStatus)
                .newStatus(newStatus)
                // Dấu OFF_SCHEDULE_RESCHEDULE_MARK ở đầu để đếm số lần đề xuất ngoài lịch rảnh (countOffScheduleReschedulesSince)
                .changeReason(limitReason((isLearner && newOffSchedule ? OFF_SCHEDULE_RESCHEDULE_MARK + " " : "")
                        + "Dời lịch học sang " + newStartTime + (isLearner ? " (chờ gia sư xác nhận lại)" : "") + ". Lý do: " + reason))
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
            if (tb.getScheduledStartTime() == null || tb.getScheduledEndTime() == null) {
                continue;
            }
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

        TrialBooking trialBooking = trialBookingRepository.findLockedById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy buổi học thử"));

        boolean isLearner = trialBooking.getLearner().getUser().getUserId().equals(user.getUserId());
        boolean isTutor = trialBooking.getTutor().getUser().getUserId().equals(user.getUserId());

        if (!isLearner && !isTutor) {
            throw new AccessDeniedException("Bạn không có quyền thao tác trên buổi học thử này");
        }

        if (trialBooking.getBookingStatus() == TrialBookingStatus.COMPLETED) {
            return mapToDTO(trialBooking, user);
        }

        if (trialBooking.getBookingStatus() != TrialBookingStatus.CONFIRMED
                && trialBooking.getBookingStatus() != TrialBookingStatus.RESCHEDULED) {
            throw new BadRequestException("Chỉ có thể hoàn thành buổi học thử đã được xác nhận");
        }

        OffsetDateTime now = OffsetDateTime.now();
        OffsetDateTime endTime = trialBooking.getScheduledEndTime();
        if (endTime == null && trialBooking.getScheduledStartTime() != null) {
            int duration = trialBooking.getPolicyDurationMinutes() != null ? trialBooking.getPolicyDurationMinutes() : 30;
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
        lockTutor(tutor.getTutorId()); // cùng thứ tự khóa với các thao tác khác của gia sư
        TrialBooking trialBooking = trialBookingRepository.findLockedById(bookingId)
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
        requireActiveMembership(tutor, ConnectionRequestServiceImpl.TUTOR_NO_MEMBERSHIP_FOR_TUTOR);

        OffsetDateTime now = OffsetDateTime.now();
        ConnectionRequest connection = trialBooking.getConnectionRequest();
        if (connection == null) {
            UUID learnerId = trialBooking.getLearner().getLearnerId();
            connection = connectionRequestRepository
                    .findFirstByLearnerLearnerIdAndTutorTutorIdAndStatus(learnerId, tutor.getTutorId(), ConnectionRequestStatus.ACCEPTED)
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
        if (notificationEvents != null) {
            notificationEvents.publish("trial:" + saved.getBookingId() + ":agreed", "CONNECTION", "Gia sư đã đồng ý kết nối",
                    tutorUser.getFullName() + " đã đồng ý dạy bạn chính thức sau buổi học thử. "
                    + "Lớp học sẽ xuất hiện trong Lịch học khi gia sư tạo lớp.",
                    LEARNER_TRIAL_REQUESTS_PATH, java.util.List.of(saved.getLearner().getUser().getUserId()));
        }

        log.info("Tutor {} agreed to connect with learner {} after trial {}", tutorUser.getEmail(),
                saved.getLearner().getLearnerId(), bookingId);
        return mapToDTO(saved, tutorUser);
    }

    /**
     * Gia sư nhận một buổi học thử: yêu cầu học thử đang chờ của học viên KHÁC với gia sư này mà trùng giờ
     * (kể cả trùng một phần) tự chuyển "Đã hủy" và học viên được báo để chọn giờ khác.
     * Yêu cầu đang chờ không giữ chỗ, nên nhiều học viên có thể cùng xin một giờ; gia sư nhận ai thì người đó được.
     */
    private void cancelOverlappingPendingTrials(TrialBooking accepted, User tutorUser) {
        OffsetDateTime start = accepted.getScheduledStartTime();
        OffsetDateTime end = accepted.getScheduledEndTime() != null ? accepted.getScheduledEndTime() : start.plusMinutes(30);
        OffsetDateTime now = OffsetDateTime.now();
        List<TrialBooking> pending = trialBookingRepository.findByTutorTutorIdAndBookingStatusIn(
                accepted.getTutor().getTutorId(), List.of(TrialBookingStatus.PENDING));
        int cancelled = 0;
        for (TrialBooking other : pending) {
            if (other.getBookingId().equals(accepted.getBookingId())) continue;
            OffsetDateTime os = other.getScheduledStartTime();
            OffsetDateTime oe = other.getScheduledEndTime() != null ? other.getScheduledEndTime() : os.plusMinutes(30);
            if (!(os.isBefore(end) && oe.isAfter(start))) continue;
            other.setBookingStatus(TrialBookingStatus.CANCELLED);
            if (other.getConnectionRequest() != null
                    && other.getConnectionRequest().getStatus() == ConnectionRequestStatus.PENDING) {
                other.getConnectionRequest().setStatus(ConnectionRequestStatus.CANCELLED);
                other.getConnectionRequest().setRespondedAt(now);
            }
            trialBookingRepository.save(other);
            trialBookingHistoryRepository.save(TrialBookingHistory.builder()
                    .trialBooking(other)
                    .changedByUser(tutorUser)
                    .previousStatus(TrialBookingStatus.PENDING)
                    .newStatus(TrialBookingStatus.CANCELLED)
                    .changeReason("Tự động hủy vì gia sư đã nhận học viên khác vào giờ này")
                    .build());
            if (notificationEvents != null && other.getLearner() != null && other.getLearner().getUser() != null) {
                notificationEvents.publish("trial:" + other.getBookingId() + ":taken", "TRIAL",
                        "Giờ học thử đã có người khác",
                        accepted.getTutor().getUser().getFullName() + " đã nhận học viên khác vào giờ học thử bạn xin ("
                                + fe12When(os) + "). Bạn hãy chọn giờ khác hoặc gia sư khác.",
                        LEARNER_TRIAL_REQUESTS_PATH, java.util.List.of(other.getLearner().getUser().getUserId()));
            }
            cancelled++;
        }
        if (cancelled > 0) {
            log.info("Auto-cancelled {} overlapping pending trial(s) after trial {} was accepted", cancelled, accepted.getBookingId());
        }
    }

    private void cancelOtherOpenTrials(TrialBooking agreed, User tutorUser, OffsetDateTime now) {
        List<TrialBooking> others = trialBookingRepository.findByTutorTutorIdAndLearnerLearnerIdAndBookingStatusIn(
                agreed.getTutor().getTutorId(), agreed.getLearner().getLearnerId(),
                List.of(TrialBookingStatus.PENDING, TrialBookingStatus.CONFIRMED, TrialBookingStatus.RESCHEDULED));
        for (TrialBooking other : others) {
            if (other.getBookingId().equals(agreed.getBookingId())) {
                continue;
            }
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
     * Buổi học thử không được trùng lịch học chính thức hằng tuần của gia sư
     * hoặc học viên (đọc lịch lớp FE-06). Khung giờ quy đổi sang giờ Việt Nam;
     * buổi kéo qua nửa đêm được tính tới 23:59:59 của ngày bắt đầu.
     */
    private void ensureNoClassScheduleClash(UUID tutorId, UUID learnerId, OffsetDateTime start, OffsetDateTime end,
            String tutorMessage, String learnerMessage) {
        if (start == null || end == null) {
            return;
        }
        ZonedDateTime vnStart = start.atZoneSameInstant(VN_ZONE);
        ZonedDateTime vnEnd = end.atZoneSameInstant(VN_ZONE);
        java.time.LocalTime endTime = vnEnd.toLocalDate().isAfter(vnStart.toLocalDate())
                ? java.time.LocalTime.of(23, 59, 59) : vnEnd.toLocalTime();
        trialBookingRepository.findClassScheduleClash(tutorId, learnerId, vnStart.toLocalDate(), vnStart.getDayOfWeek().getValue(),
                vnStart.toLocalTime(), endTime)
                .ifPresent(who -> {
                    throw new ConflictException("TUTOR".equals(who) ? tutorMessage : learnerMessage);
                });
    }

    /**
     * Buổi học thử không được đè lên buổi học của khóa kết nối trực tiếp (đã
     * xác nhận, hoặc đang chờ gia sư duyệt nếu includePending — yêu cầu đang
     * chờ giữ chỗ trong thời hạn phản hồi).
     */
    private void ensureNoConnectionSessionClash(UUID tutorId, OffsetDateTime start, OffsetDateTime end,
            boolean includePending, String message) {
        if (start == null || end == null) {
            return;
        }
        ZonedDateTime vnStart = start.atZoneSameInstant(VN_ZONE);
        ZonedDateTime vnEnd = end.atZoneSameInstant(VN_ZONE);
        java.time.LocalTime endTime = vnEnd.toLocalDate().isAfter(vnStart.toLocalDate())
                ? java.time.LocalTime.of(23, 59, 59) : vnEnd.toLocalTime();
        if (trialBookingRepository.existsConnectionSessionClash(tutorId, vnStart.toLocalDate(),
                vnStart.getDayOfWeek().getValue() % 7, vnStart.toLocalTime(), endTime,
                includePending, BookingLimits.DIRECT_RESPONSE_HOURS)) {
            throw new ConflictException(message);
        }
    }

    /**
     * Buổi học thử (giờ Việt Nam) nằm trọn trong một ca rảnh đang bật của gia
     * sư.
     */
    private boolean isWithinAvailability(UUID tutorId, OffsetDateTime start, OffsetDateTime end) {
        ZonedDateTime vnStart = start.atZoneSameInstant(VN_ZONE);
        ZonedDateTime vnEnd = end.atZoneSameInstant(VN_ZONE);
        if (vnEnd.toLocalDate().isAfter(vnStart.toLocalDate()) && !vnEnd.toLocalTime().equals(java.time.LocalTime.MIDNIGHT)) {
            return false; // kéo qua nửa đêm
        }
        int dow = vnStart.getDayOfWeek().getValue() % 7 + 1; // 1 = Chủ nhật ... 7 = Thứ Bảy
        return trialBookingRepository.isWithinAvailability(tutorId, dow, vnStart.toLocalTime(), vnEnd.toLocalTime());
    }

    /**
     * Giờ gia sư đang bận thật (kể cả yêu cầu đang chờ duyệt, vì chúng giữ chỗ)
     * thì không cho đặt / đề xuất.
     */
    private void ensureTutorFreeAt(UUID tutorId, UUID learnerId, OffsetDateTime start, OffsetDateTime end) {
        if (busyLoader == null) {
            return;
        }
        ZonedDateTime vnStart = start.atZoneSameInstant(VN_ZONE);
        ZonedDateTime vnEnd = end.atZoneSameInstant(VN_ZONE);
        java.time.LocalDate date = vnStart.toLocalDate();
        java.time.LocalTime endTime = vnEnd.toLocalDate().isAfter(date) ? java.time.LocalTime.MAX : vnEnd.toLocalTime();
        ConnectionSchedulePlanner.BusyIndex busy = busyLoader.load(tutorId, learnerId, null, date, date);
        busy.conflictOf(new ConnectionSchedulePlanner.PlannedSession(date, ConnectionSchedulePlanner.dayOfWeekOf(date),
                vnStart.toLocalTime(), endTime))
                .ifPresent(kind -> {
                    throw new ConflictException("Giờ này đã kín: " + kind.learnerText() + ". Vui lòng chọn giờ khác.");
                });
    }

    /**
     * Gia sư đồng ý học thử: học viên không được đang có lịch ĐÃ XÁC NHẬN với
     * gia sư khác vào giờ này (buổi của khóa học đã kết nối, buổi học thử đã
     * xác nhận). Yêu cầu đang chờ của học viên với gia sư khác không tính.
     */
    private void ensureLearnerFreeForAccept(UUID tutorId, UUID learnerId, OffsetDateTime start, OffsetDateTime end) {
        if (busyLoader == null) {
            return;
        }
        ZonedDateTime vnStart = start.atZoneSameInstant(VN_ZONE);
        ZonedDateTime vnEnd = end.atZoneSameInstant(VN_ZONE);
        java.time.LocalDate date = vnStart.toLocalDate();
        java.time.LocalTime endTime = vnEnd.toLocalDate().isAfter(date) ? java.time.LocalTime.MAX : vnEnd.toLocalTime();
        ConnectionSchedulePlanner.BusyIndex all = busyLoader.load(tutorId, learnerId, null, date, date);
        if (all == null) {
            return;
        }
        java.util.Set<ConnectionSchedulePlanner.BusyKind> learnerKinds = java.util.EnumSet.of(
                ConnectionSchedulePlanner.BusyKind.LEARNER_COURSE, ConnectionSchedulePlanner.BusyKind.LEARNER_TRIAL);
        ConnectionSchedulePlanner.BusyIndex learnerOnly = new ConnectionSchedulePlanner.BusyIndex(
                all.weekly().stream().filter(w -> learnerKinds.contains(w.kind())).toList(),
                all.dated().stream().filter(d -> learnerKinds.contains(d.kind())).toList());
        learnerOnly.conflictOf(new ConnectionSchedulePlanner.PlannedSession(date, ConnectionSchedulePlanner.dayOfWeekOf(date),
                vnStart.toLocalTime(), endTime))
                .ifPresent(kind -> {
                    throw new ConflictException("Không thể nhận buổi học thử này: " + kind.tutorText()
                            + ". Hãy đề nghị học viên dời lịch hoặc từ chối yêu cầu này.");
                });
    }

    /**
     * Trang học viên mở từ thông báo: Lịch học (buổi đã chốt) và Yêu cầu của
     * tôi - tab Học thử (từ chối / hủy).
     */
    static final String LEARNER_SCHEDULE_PATH = "/learner/connections";
    static final String LEARNER_TRIAL_REQUESTS_PATH = "/pages/learner-requests/?tab=trial";

    /**
     * Đánh dấu trong lịch sử: học viên đổi buổi học thử sang giờ ngoài lịch
     * rảnh (tính vào giới hạn đề xuất mỗi tuần).
     */
    public static final String OFF_SCHEDULE_RESCHEDULE_MARK = "[đề xuất ngoài lịch rảnh]";

    /**
     * Cột change_reason tối đa 255 ký tự.
     */
    private static String limitReason(String text) {
        return text == null || text.length() <= 255 ? text : text.substring(0, 254) + "…";
    }

    /**
     * Khóa giao dịch cấp cơ sở dữ liệu theo mã gia sư, giữ tới hết transaction.
     */
    private void lockTutor(UUID tutorId) {
        if (jdbcTemplate == null || tutorId == null) {
            return;
        }
        // Lỗi phải ném ra: PostgreSQL đã hủy transaction sau lệnh lỗi, chạy tiếp như đã khóa chỉ gây lỗi khó hiểu
        // ở lệnh sau và mất tác dụng chống trùng lịch.
        jdbcTemplate.execute("SELECT pg_advisory_xact_lock(" + Math.abs(tutorId.getMostSignificantBits()) + ")");
    }

    /**
     * Yêu cầu đang chờ đã quá hạn: gia sư chưa phản hồi sau
     * TRIAL_RESPONSE_HOURS kể từ lần gửi/đổi lịch gần nhất, hoặc đã tới giờ
     * học.
     */
    private static boolean isStalePending(TrialBooking tb, OffsetDateTime now) {
        if (tb.getBookingStatus() != TrialBookingStatus.PENDING) {
            return false;
        }
        boolean reachedStart = tb.getScheduledStartTime() != null && !tb.getScheduledStartTime().isAfter(now);
        boolean noResponse = tb.getUpdatedAt() != null
                && tb.getUpdatedAt().isBefore(now.minusHours(BookingLimits.TRIAL_RESPONSE_HOURS));
        return reachedStart || noResponse;
    }

    @Override
    @Transactional
    public int expireStalePendingTrials() {
        OffsetDateTime now = OffsetDateTime.now();
        // Query khóa dòng: yêu cầu vừa được gia sư nhận / học viên hủy thì không còn PENDING và bị bỏ qua.
        List<TrialBooking> stale = trialBookingRepository.findStalePending(
                TrialBookingStatus.PENDING, now.minusHours(BookingLimits.TRIAL_RESPONSE_HOURS), now);
        for (TrialBooking tb : stale) {
            expireTrial(tb, now);
        }
        if (!stale.isEmpty()) {
            log.info("Auto-expired {} pending trial booking(s)", stale.size());
        }
        return stale.size();
    }

    /** Chuyển một yêu cầu học thử đang chờ (đã được khóa dòng khi đọc) sang "Đã hết hạn", ghi lịch sử và báo hai bên. */
    private void expireTrial(TrialBooking tb, OffsetDateTime now) {
        boolean reachedStart = tb.getScheduledStartTime() != null && !tb.getScheduledStartTime().isAfter(now);
        tb.setBookingStatus(TrialBookingStatus.EXPIRED);
        ConnectionRequest cr = tb.getConnectionRequest();
        if (cr != null && cr.getStatus() == ConnectionRequestStatus.PENDING) {
            cr.setStatus(ConnectionRequestStatus.EXPIRED);
            cr.setRespondedAt(now);
        }
        trialBookingRepository.save(tb);
        trialBookingHistoryRepository.save(TrialBookingHistory.builder()
                .trialBooking(tb)
                .changedByUser(null)
                .previousStatus(TrialBookingStatus.PENDING)
                .newStatus(TrialBookingStatus.EXPIRED)
                .changeReason(reachedStart
                        ? "Tự động hết hạn vì đã tới giờ học mà gia sư chưa xác nhận"
                        : "Tự động hết hạn vì gia sư chưa phản hồi trong " + BookingLimits.TRIAL_RESPONSE_HOURS + " giờ")
                .build());
        if (notificationEvents != null) {
            String when = fe12When(tb.getScheduledStartTime());
            notificationEvents.publish("trial:" + tb.getBookingId() + ":expired:learner", "TRIAL", "Yêu cầu học thử đã hết hạn",
                    tb.getTutor().getUser().getFullName() + " chưa xác nhận kịp buổi học thử lúc " + when
                    + " nên yêu cầu đã hết hạn. Bạn có thể đặt lại giờ khác hoặc chọn gia sư khác.",
                    LEARNER_TRIAL_REQUESTS_PATH, java.util.List.of(tb.getLearner().getUser().getUserId()));
            notificationEvents.publish("trial:" + tb.getBookingId() + ":expired:tutor", "TRIAL", "Yêu cầu học thử đã hết hạn",
                    "Yêu cầu học thử lúc " + when + " của " + tb.getLearner().getUser().getFullName()
                    + " đã hết hạn vì chưa được xác nhận kịp.",
                    "/tutor/connections", java.util.List.of(tb.getTutor().getUser().getUserId()));
        }
    }

    @Override
    @Transactional
    public void completeEndedTrials() {
        autoCompleteEndedBookings();
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
            case PENDING ->
                "Chờ gia sư xác nhận";
            case CONFIRMED ->
                "Đã xác nhận";
            case COMPLETED ->
                "Đã hoàn thành";
            case CANCELLED ->
                "Đã hủy";
            case RESCHEDULED ->
                "Đã dời lịch";
            case DECLINED ->
                "Đã từ chối";
            case EXPIRED ->
                "Đã hết hạn";
        };

        int duration = 30;
        if (tb.getScheduledStartTime() != null && tb.getScheduledEndTime() != null) {
            duration = (int) Duration.between(tb.getScheduledStartTime(), tb.getScheduledEndTime()).toMinutes();
        }

        String mode = tb.getLearningMode() != null ? tb.getLearningMode() : "ONLINE";
        String location = tb.getLocationDetails();

        String goals = tb.getLearner() != null ? tb.getLearner().getLearningGoals() : null;
        String level = tb.getLearner() != null ? BookingLimits.levelLabel(tb.getLearner().getCurrentLevel()) : null;

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

        // Liên hệ: gia sư xem của học viên (trừ khi đã từ chối / hủy / hết hạn); học viên xem của gia sư khi buổi học thử đã được nhận
        var contact = com.kizuna.features.booking.service.ContactShare.Contact.NONE;
        TrialBookingStatus st = tb.getBookingStatus();
        boolean closed = st == TrialBookingStatus.DECLINED || st == TrialBookingStatus.CANCELLED || st == TrialBookingStatus.EXPIRED;
        if (!closed && !isCurrentUserLearner) {
            contact = com.kizuna.features.booking.service.ContactShare.learnerContact(tb.getConnectionRequest(), tb.getLearner().getUser());
        } else if (!closed && (st == TrialBookingStatus.CONFIRMED || st == TrialBookingStatus.RESCHEDULED || st == TrialBookingStatus.COMPLETED)) {
            contact = com.kizuna.features.booking.service.ContactShare.tutorContact(tb.getTutor());
        }

        return TrialBookingResponseDTO.builder()
                .bookingId(tb.getBookingId())
                .officialArrangementAgreed(Boolean.TRUE.equals(tb.getOfficialArrangementAgreed()))
                .offSchedule(Boolean.TRUE.equals(tb.getOffSchedule()))
                .requestId(tb.getConnectionRequest() != null ? tb.getConnectionRequest().getRequestId() : null)
                .tutorId(tb.getTutor().getTutorId())
                .tutorName(tb.getTutor().getUser().getFullName())
                .learnerId(tb.getLearner().getLearnerId())
                .partnerId(partnerUser.getUserId())
                .partnerName(partnerUser.getFullName() != null ? partnerUser.getFullName() : partnerUser.getEmail())
                .partnerAvatarUrl(partnerUser.getAvatarUrl())
                .partnerRole(partnerRole)
                .partnerEmail(contact.email())
                .partnerPhone(contact.phone())
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

    /**
     * FE-12: giờ hiển thị trong thông báo (giờ Việt Nam).
     */
    private static String fe12When(java.time.OffsetDateTime time) {
        return time == null ? "" : time.atZoneSameInstant(java.time.ZoneId.of("Asia/Ho_Chi_Minh"))
                .format(java.time.format.DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy"));
    }
}
