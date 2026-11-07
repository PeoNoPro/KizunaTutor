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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class TrialBookingServiceImpl implements TrialBookingService {

    private final UserRepository userRepository;
    private final LearnerRepository learnerRepository;
    private final TutorRepository tutorRepository;
    private final ConnectionRequestRepository connectionRequestRepository;
    private final TrialBookingRepository trialBookingRepository;
    private final TrialBookingHistoryRepository trialBookingHistoryRepository;
    private final JdbcTemplate jdbcTemplate;

    @Override
    @Transactional
    public TrialBookingResponseDTO createTrialBooking(String userEmail, CreateTrialBookingRequestDTO request) {
        User learnerUser = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy thông tin tài khoản người dùng"));

        Learner learner = learnerRepository.findByUser(learnerUser)
                .orElseGet(() -> {
                    // Tự động tạo hồ sơ learner nếu chưa tồn tại
                    Learner newLearner = Learner.builder()
                            .user(learnerUser)
                            .currentLevel(request.getCurrentLevel())
                            .learningGoals(request.getLearningGoals())
                            .build();
                    return learnerRepository.save(newLearner);
                });

        Tutor tutor = tutorRepository.findById(request.getTutorId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy gia sư với mã yêu cầu"));

        OffsetDateTime startTime = request.getScheduledStartTime();
        if (startTime == null) {
            throw new BadRequestException("Thời gian bắt đầu học không được để trống");
        }
        if (startTime.isBefore(OffsetDateTime.now())) {
            throw new BadRequestException("Thời gian bắt đầu học phải ở tương lai");
        }

        int duration = (request.getDurationMinutes() != null && request.getDurationMinutes() > 0)
                ? request.getDurationMinutes() : 50;
        OffsetDateTime endTime = startTime.plusMinutes(duration);

        // Chống đặt trùng lịch (Race condition / Overlap check)
        boolean hasOverlap = trialBookingRepository.existsOverlappingBooking(tutor.getTutorId(), startTime, endTime);
        if (hasOverlap) {
            throw new ConflictException("Khung giờ này vừa có người đăng ký. Vui lòng chọn khung giờ khác.");
        }

        // Kiểm tra khung giờ có nằm trong lịch rảnh của gia sư không (UC-31)
        if (jdbcTemplate != null) {
            try {
                Integer totalAvail = jdbcTemplate.queryForObject(
                        "SELECT count(*) FROM tutor_availabilities WHERE tutor_id = ? AND is_active = TRUE",
                        Integer.class, tutor.getTutorId()
                );
                if (totalAvail != null && totalAvail > 0) {
                    int dbDow = (startTime.getDayOfWeek().getValue() % 7) + 1;
                    String startStr = startTime.toLocalTime().toString();
                    String endStr = endTime.toLocalTime().toString();
                    Integer matchCount = jdbcTemplate.queryForObject(
                            "SELECT count(*) FROM tutor_availabilities WHERE tutor_id = ? AND is_active = TRUE " +
                            "AND day_of_week = ? AND start_time <= ?::time AND end_time >= ?::time",
                            Integer.class, tutor.getTutorId(), dbDow, startStr, endStr
                    );
                    if (matchCount == null || matchCount == 0) {
                        throw new BadRequestException("Gia sư không có lịch rảnh vào khung giờ này. Vui lòng chọn khung giờ khác.");
                    }
                }
            } catch (BadRequestException bre) {
                throw bre;
            } catch (Exception e) {
                log.warn("Không thể xác thực lịch rảnh qua JdbcTemplate: {}", e.getMessage());
            }
        }

        // Tạo yêu cầu kết nối (ConnectionRequest)
        String modeText = "ONLINE".equalsIgnoreCase(request.getLearningMode()) ? "[Hình thức: Học trực tuyến]" :
                "[Hình thức: Học trực tiếp tại " + (request.getLocationDetails() != null ? request.getLocationDetails() : "địa chỉ hẹn") + "]";
        String goalsText = (request.getLearningGoals() != null && !request.getLearningGoals().isBlank())
                ? "\n[Mục tiêu: " + request.getLearningGoals() + "]" : "";
        String msgText = (request.getInitialMessage() != null && !request.getInitialMessage().isBlank())
                ? "\n" + request.getInitialMessage() : "";
        String fullInitialMessage = modeText + goalsText + msgText;

        ConnectionRequest connectionRequest = ConnectionRequest.builder()
                .learner(learner)
                .tutor(tutor)
                .initialMessage(fullInitialMessage)
                .status(ConnectionRequestStatus.PENDING)
                .build();
        connectionRequest = connectionRequestRepository.save(connectionRequest);

        // Tạo buổi học thử (TrialBooking)
        String meetingLink = "ONLINE".equalsIgnoreCase(request.getLearningMode()) ? null :
                (request.getLocationDetails() != null ? request.getLocationDetails() : "Địa điểm học thỏa thuận");

        TrialBooking trialBooking = TrialBooking.builder()
                .connectionRequest(connectionRequest)
                .learner(learner)
                .tutor(tutor)
                .scheduledStartTime(startTime)
                .scheduledEndTime(endTime)
                .meetingLink(meetingLink)
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
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy thông tin tài khoản gia sư"));

        Tutor tutor = tutorRepository.findByUser(tutorUser)
                .orElseThrow(() -> new AccessDeniedException("Chỉ tài khoản gia sư mới có quyền phê duyệt yêu cầu học thử"));

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

            if (request.getMeetingLink() != null && !request.getMeetingLink().isBlank()) {
                trialBooking.setMeetingLink(request.getMeetingLink().trim());
            }

            if (trialBooking.getConnectionRequest() != null) {
                trialBooking.getConnectionRequest().setStatus(ConnectionRequestStatus.ACCEPTED);
                trialBooking.getConnectionRequest().setRespondedAt(OffsetDateTime.now());
            }
            reason = request.getNote() != null ? request.getNote() : "Gia sư đã chấp nhận lịch học thử";
        } else if ("DECLINE".equalsIgnoreCase(request.getAction())) {
            newStatus = TrialBookingStatus.CANCELLED;
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

        // Kiểm tra quyền: phải là học viên hoặc gia sư của buổi học
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
            trialBooking.getBookingStatus() == TrialBookingStatus.COMPLETED) {
            throw new BadRequestException("Buổi học thử không thể hủy ở trạng thái hiện tại");
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
        };

        int duration = 50;
        if (tb.getScheduledStartTime() != null && tb.getScheduledEndTime() != null) {
            duration = (int) java.time.Duration.between(tb.getScheduledStartTime(), tb.getScheduledEndTime()).toMinutes();
        }

        String mode = (tb.getMeetingLink() != null && tb.getMeetingLink().startsWith("http")) ? "ONLINE" : "OFFLINE";
        String location = "OFFLINE".equals(mode) ? tb.getMeetingLink() : null;

        String goals = tb.getLearner().getLearningGoals();
        String initialMsg = tb.getConnectionRequest() != null ? tb.getConnectionRequest().getInitialMessage() : null;

        return TrialBookingResponseDTO.builder()
                .bookingId(tb.getBookingId())
                .requestId(tb.getConnectionRequest() != null ? tb.getConnectionRequest().getRequestId() : null)
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

