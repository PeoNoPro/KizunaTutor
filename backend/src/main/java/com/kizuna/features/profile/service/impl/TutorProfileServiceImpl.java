package com.kizuna.features.profile.service.impl;

import com.kizuna.features.profile.dto.SaveTutorProfileRequest;
import com.kizuna.features.profile.dto.TutorProfileDto;
import com.kizuna.core.entity.*;
import com.kizuna.features.classroom.entity.*;
import com.kizuna.features.learning_support.entity.*;
import com.kizuna.features.payment.entity.*;
import com.kizuna.features.progress.entity.*;
import com.kizuna.features.verification.entity.*;
import com.kizuna.features.tutor_search.entity.*;
import com.kizuna.features.ticket.entity.*;
import com.kizuna.features.notification.entity.*;
import com.kizuna.features.booking.entity.*;
import com.kizuna.features.admin.entity.*;
import com.kizuna.core.exception.BadRequestException;
import com.kizuna.core.exception.ResourceNotFoundException;
import com.kizuna.core.repository.TutorRepository;
import com.kizuna.core.repository.UserRepository;
import com.kizuna.features.profile.service.ITutorProfileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Triển khai dịch vụ quản lý hồ sơ và phê duyệt gia sư (UC 9 - 16, UC 20).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TutorProfileServiceImpl implements ITutorProfileService {

    private final TutorRepository tutorRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public TutorProfileDto getProfileByEmail(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy tài khoản người dùng"));

        Tutor tutor = tutorRepository.findByUser_Email(email)
                .orElseGet(() -> {
                    // Tự động khởi tạo hồ sơ DRAFT nếu chưa có
                    Tutor newTutor = Tutor.builder()
                            .user(user)
                            .tutorState(TutorState.DRAFT_PROFILE)
                            .verificationStatus(TutorVerificationStatus.UNVERIFIED)
                            .hourlyRate(BigDecimal.valueOf(150000))
                            .experienceYears((short) 1)
                            .build();
                    return tutorRepository.save(newTutor);
                });

        return mapToDto(tutor);
    }

    @Override
    @Transactional
    public TutorProfileDto saveDraft(String email, SaveTutorProfileRequest request) {
        Tutor tutor = getOrCreateTutor(email);

        // Chỉ cho phép lưu nháp khi ở DRAFT_PROFILE hoặc REJECTED
        if (tutor.getTutorState() == TutorState.PENDING_VERIFICATION) {
            throw new BadRequestException("Hồ sơ đang trong quá trình thẩm định, không thể chỉnh sửa bản nháp.");
        }

        applyRequestToTutor(tutor, request);
        Tutor saved = tutorRepository.save(tutor);
        log.info("Lưu nháp hồ sơ gia sư thành công: {}", email);
        return mapToDto(saved);
    }

    @Override
    @Transactional
    public TutorProfileDto submitProfile(String email, SaveTutorProfileRequest request) {
        Tutor tutor = getOrCreateTutor(email);

        if (tutor.getTutorState() == TutorState.PENDING_VERIFICATION) {
            throw new BadRequestException("Hồ sơ đã được gửi trước đó và đang chờ thẩm định.");
        }

        applyRequestToTutor(tutor, request);

        // Kiểm tra hợp lệ tối thiểu khi nộp hồ sơ
        if (request.getPhone() == null || request.getPhone().trim().length() < 9) {
            throw new BadRequestException("Vui lòng cung cấp số điện thoại liên hệ hợp lệ.");
        }
        if (request.getHighestJlptLevel() == null || request.getHighestJlptLevel().isBlank()) {
            throw new BadRequestException("Vui lòng chọn trình độ JLPT cao nhất của bạn.");
        }

        tutor.setTutorState(TutorState.PENDING_VERIFICATION);
        tutor.setVerificationStatus(TutorVerificationStatus.PENDING);
        tutor.setSubmittedAt(OffsetDateTime.now());
        tutor.setVerificationFeedback(null); // Xóa lý do từ chối cũ nếu có

        Tutor saved = tutorRepository.save(tutor);
        log.info("Gia sư {} đã nộp hồ sơ phê duyệt thành công (UC 13). Trạng thái: PENDING_VERIFICATION", email);
        return mapToDto(saved);
    }

    @Override
    @Transactional
    public TutorProfileDto approveProfile(UUID tutorId) {
        Tutor tutor = tutorRepository.findById(tutorId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy gia sư với mã: " + tutorId));

        tutor.setTutorState(TutorState.APPROVED_UNPAID);
        tutor.setVerificationStatus(TutorVerificationStatus.APPROVED);
        tutor.setVerifiedAt(OffsetDateTime.now());
        tutor.setVerificationFeedback(null);

        Tutor saved = tutorRepository.save(tutor);
        log.info("Ban Học Vụ đã phê duyệt hồ sơ gia sư {} (UC 20). Trạng thái: APPROVED_UNPAID", tutor.getUser().getEmail());
        return mapToDto(saved);
    }

    @Override
    @Transactional
    public TutorProfileDto rejectProfile(UUID tutorId, String feedback) {
        Tutor tutor = tutorRepository.findById(tutorId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy gia sư với mã: " + tutorId));

        tutor.setTutorState(TutorState.REJECTED);
        tutor.setVerificationStatus(TutorVerificationStatus.REJECTED);
        tutor.setVerificationFeedback((feedback == null || feedback.isBlank())
                ? "Hồ sơ chưa đạt yêu cầu hoặc thiếu minh chứng bằng cấp. Vui lòng bổ sung."
                : feedback.trim());

        Tutor saved = tutorRepository.save(tutor);
        log.info("Ban Học Vụ đã từ chối hồ sơ gia sư {} (UC 15). Lý do: {}", tutor.getUser().getEmail(), feedback);
        return mapToDto(saved);
    }

    private Tutor getOrCreateTutor(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy tài khoản người dùng"));

        return tutorRepository.findByUser_Email(email)
                .orElseGet(() -> {
                    Tutor newTutor = Tutor.builder()
                            .user(user)
                            .tutorState(TutorState.DRAFT_PROFILE)
                            .verificationStatus(TutorVerificationStatus.UNVERIFIED)
                            .build();
                    return tutorRepository.save(newTutor);
                });
    }

    private void applyRequestToTutor(Tutor tutor, SaveTutorProfileRequest request) {
        if (request == null) return;

        if (request.getFullName() != null && !request.getFullName().isBlank()) {
            tutor.getUser().setFullName(request.getFullName().trim());
            userRepository.save(tutor.getUser());
        }

        if (request.getPhone() != null) tutor.setPhone(request.getPhone().trim());
        if (request.getDateOfBirth() != null) tutor.setDateOfBirth(request.getDateOfBirth());
        if (request.getAddress() != null) tutor.setAddress(request.getAddress().trim());
        if (request.getHeadline() != null) tutor.setHeadline(request.getHeadline().trim());
        if (request.getBio() != null) tutor.setBio(request.getBio().trim());
        if (request.getHighestJlptLevel() != null) tutor.setHighestJlptLevel(request.getHighestJlptLevel().trim());
        if (request.getExperienceYears() != null) tutor.setExperienceYears(request.getExperienceYears());
        if (request.getHourlyRate() != null) tutor.setHourlyRate(request.getHourlyRate());
        if (request.getCertificateUrls() != null) tutor.setCertificateUrls(request.getCertificateUrls().trim());
    }

    private TutorProfileDto mapToDto(Tutor tutor) {
        return TutorProfileDto.builder()
                .tutorId(tutor.getTutorId())
                .userId(tutor.getUser().getUserId())
                .fullName(tutor.getUser().getFullName())
                .email(tutor.getUser().getEmail())
                .phone(tutor.getPhone())
                .dateOfBirth(tutor.getDateOfBirth())
                .address(tutor.getAddress())
                .headline(tutor.getHeadline())
                .bio(tutor.getBio())
                .highestJlptLevel(tutor.getHighestJlptLevel())
                .experienceYears(tutor.getExperienceYears())
                .hourlyRate(tutor.getHourlyRate())
                .certificateUrls(tutor.getCertificateUrls())
                .tutorState(tutor.getTutorState())
                .verificationStatus(tutor.getVerificationStatus())
                .verificationFeedback(tutor.getVerificationFeedback())
                .submittedAt(tutor.getSubmittedAt())
                .verifiedAt(tutor.getVerifiedAt())
                .build();
    }
}
