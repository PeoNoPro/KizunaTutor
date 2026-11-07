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

            if (trialBooking.getConnectionRequest() != null) {
                trialBooking.getConnectionRequest().setStatus(ConnectionRequestStatus.ACCEPTED);
                trialBooking.getConnectionRequest().setRespondedAt(OffsetDateTime.now());
            }
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
    @Transactional(readOnly = true)
    public Page<TrialBookingResponseDTO> getMyTrialBookings(String userEmail, String status, int page, int size) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy thông tin tài khoản"));

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

        String goals = tb.getLearner().getLearningGoals();
        String initialMsg = tb.getConnectionRequest() != null ? tb.getConnectionRequest().getInitialMessage() : null;

        return TrialBookingResponseDTO.builder()
                .bookingId(tb.getBookingId())
                .requestId(tb.getConnectionRequest() != null ? tb.getConnectionRequest().getRequestId() : null)
                .tutorId(tb.getTutor().getTutorId())
                .tutorName(tb.getTutor().getUser().getFullName())
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
                .currentLevel(tb.getLearner().getCurrentLevel())
                .learningGoals(goals)
                .initialMessage(initialMsg)
                .bookingStatus(tb.getBookingStatus().name())
                .statusLabel(statusLabel)
                .createdAt(tb.getCreatedAt())
                .updatedAt(tb.getUpdatedAt())
                .build();
    }
}
