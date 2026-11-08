package com.kizuna.features.tutor_search.service.impl;

import com.kizuna.core.entity.Tutor;
import com.kizuna.core.entity.User;
import com.kizuna.core.exception.BadRequestException;
import com.kizuna.core.exception.ConflictException;
import com.kizuna.core.exception.ResourceNotFoundException;
import com.kizuna.core.repository.TutorRepository;
import com.kizuna.core.repository.UserRepository;
import com.kizuna.features.booking.service.TrialBookingService;
import com.kizuna.features.tutor_search.dto.CreateAvailabilityRequestDTO;
import com.kizuna.features.tutor_search.dto.TutorAvailabilityDTO;
import com.kizuna.features.tutor_search.entity.TutorAvailability;
import com.kizuna.features.tutor_search.repository.TutorAvailabilityRepository;
import com.kizuna.features.tutor_search.service.TutorScheduleService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
public class TutorScheduleServiceImpl implements TutorScheduleService {

    private final UserRepository userRepository;
    private final TutorRepository tutorRepository;
    private final TutorAvailabilityRepository availabilityRepository;
    private final TrialBookingService trialBookingService;

    public TutorScheduleServiceImpl(
            UserRepository userRepository,
            TutorRepository tutorRepository,
            TutorAvailabilityRepository availabilityRepository,
            @Lazy TrialBookingService trialBookingService) {
        this.userRepository = userRepository;
        this.tutorRepository = tutorRepository;
        this.availabilityRepository = availabilityRepository;
        this.trialBookingService = trialBookingService;
    }

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    @Override
    @Transactional(readOnly = true)
    public List<TutorAvailabilityDTO> getMyAvailabilities(String userEmail) {
        Tutor tutor = getTutorByEmail(userEmail);
        List<TutorAvailability> list = availabilityRepository.findByTutor_TutorIdOrderByDayOfWeekAscStartTimeAsc(tutor.getTutorId());
        return list.stream().map(this::mapToDTO).toList();
    }

    @Override
    @Transactional
    public TutorAvailabilityDTO addAvailability(String userEmail, CreateAvailabilityRequestDTO request) {
        Tutor tutor = getTutorByEmail(userEmail);

        LocalTime startTime = parseTime(request.getStartTime(), "Giờ bắt đầu");
        LocalTime endTime = parseTime(request.getEndTime(), "Giờ kết thúc");

        if (!startTime.isBefore(endTime)) {
            throw new BadRequestException("Giờ bắt đầu (" + formatTime(startTime) + ") phải sớm hơn giờ kết thúc (" + formatTime(endTime) + ")");
        }

        long minutes = Duration.between(startTime, endTime).toMinutes();
        if (minutes < 30) {
            throw new BadRequestException("Khung giờ rảnh tối thiểu phải từ 30 phút trở lên (hiện tại: " + minutes + " phút)");
        }

        boolean hasOverlap = availabilityRepository.hasOverlappingSlot(
                tutor.getTutorId(), request.getDayOfWeek(), startTime, endTime
        );
        if (hasOverlap) {
            String dayName = getDayName(request.getDayOfWeek());
            throw new ConflictException("Khung giờ " + formatTime(startTime) + " - " + formatTime(endTime) +
                    " bị trùng với một ca rảnh đang mở trong ngày " + dayName);
        }

        TutorAvailability entity = TutorAvailability.builder()
                .tutor(tutor)
                .dayOfWeek(request.getDayOfWeek())
                .startTime(startTime)
                .endTime(endTime)
                .isActive(request.getIsActive() != null ? request.getIsActive() : true)
                .build();

        TutorAvailability saved = availabilityRepository.save(entity);
        log.info("Tutor {} added availability slot: Day {} {} - {}", tutor.getTutorId(), saved.getDayOfWeek(), saved.getStartTime(), saved.getEndTime());
        return mapToDTO(saved);
    }

    @Override
    @Transactional
    public void removeAvailability(String userEmail, UUID availabilityId) {
        Tutor tutor = getTutorByEmail(userEmail);
        TutorAvailability slot = availabilityRepository.findByAvailabilityIdAndTutor_TutorId(availabilityId, tutor.getTutorId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy ca rảnh cần xóa hoặc ca này không thuộc quyền sở hữu của bạn"));

        boolean hasActiveBookings = trialBookingService != null && trialBookingService.hasActiveBookingsInSlot(
                tutor.getTutorId(), slot.getDayOfWeek(), slot.getStartTime(), slot.getEndTime());
        if (hasActiveBookings) {
            throw new ConflictException("Không thể xóa ca rảnh này vì đang có lịch học thử của học viên đang chờ hoặc đã xác nhận. Vui lòng xử lý lịch học trước.");
        }

        availabilityRepository.delete(slot);
        log.info("Tutor {} deleted availability slot {}", tutor.getTutorId(), availabilityId);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isSlotAvailable(UUID tutorId, Integer dayOfWeek, LocalTime startTime, LocalTime endTime) {
        return availabilityRepository.isWithinActiveAvailability(tutorId, dayOfWeek, startTime, endTime);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean hasActiveAvailabilities(UUID tutorId) {
        return availabilityRepository.countByTutor_TutorIdAndIsActiveTrue(tutorId) > 0;
    }

    @Override
    @Transactional
    public TutorAvailabilityDTO toggleAvailability(String userEmail, UUID availabilityId, boolean active) {
        Tutor tutor = getTutorByEmail(userEmail);
        TutorAvailability slot = availabilityRepository.findByAvailabilityIdAndTutor_TutorId(availabilityId, tutor.getTutorId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy ca rảnh cần cập nhật"));

        slot.setIsActive(active);
        TutorAvailability saved = availabilityRepository.save(slot);
        log.info("Tutor {} toggled availability slot {} active={}", tutor.getTutorId(), availabilityId, active);
        return mapToDTO(saved);
    }

    private Tutor getTutorByEmail(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy thông tin tài khoản người dùng"));
        return tutorRepository.findByUser(user)
                .orElseThrow(() -> new ResourceNotFoundException("Tài khoản của bạn chưa được liên kết với hồ sơ gia sư"));
    }

    private LocalTime parseTime(String timeStr, String fieldName) {
        if (timeStr == null || timeStr.trim().isEmpty()) {
            throw new BadRequestException(fieldName + " không được để trống");
        }
        String clean = timeStr.trim();
        try {
            if (clean.length() == 5) {
                return LocalTime.parse(clean, TIME_FORMATTER);
            }
            return LocalTime.parse(clean);
        } catch (DateTimeParseException e) {
            throw new BadRequestException(fieldName + " không đúng định dạng HH:mm (Ví dụ: 18:30)");
        }
    }

    private String formatTime(LocalTime time) {
        return time != null ? time.format(TIME_FORMATTER) : "";
    }

    private TutorAvailabilityDTO mapToDTO(TutorAvailability entity) {
        return TutorAvailabilityDTO.builder()
                .availabilityId(entity.getAvailabilityId())
                .dayOfWeek(entity.getDayOfWeek())
                .dayName(getDayName(entity.getDayOfWeek()))
                .startTime(formatTime(entity.getStartTime()))
                .endTime(formatTime(entity.getEndTime()))
                .isActive(entity.getIsActive())
                .build();
    }

    private String getDayName(Integer dayOfWeek) {
        if (dayOfWeek == null) return "Chưa xác định";
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
}

