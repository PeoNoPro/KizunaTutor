package com.kizuna.service.tutor.impl;

import com.kizuna.dto.tutor.TutorMembershipDto;
import com.kizuna.entity.*;
import com.kizuna.exception.BadRequestException;
import com.kizuna.exception.ResourceNotFoundException;
import com.kizuna.repository.MembershipPlanRepository;
import com.kizuna.repository.TutorMembershipRepository;
import com.kizuna.repository.TutorRepository;
import com.kizuna.service.payment.IVnPayService;
import com.kizuna.service.tutor.ITutorMembershipService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Triển khai dịch vụ quản lý gói thành viên và thanh toán kích hoạt gia sư (UC 71 - 77, UC 94).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TutorMembershipServiceImpl implements ITutorMembershipService {

    private final TutorMembershipRepository membershipRepository;
    private final MembershipPlanRepository planRepository;
    private final TutorRepository tutorRepository;
    private final IVnPayService vnPayService;

    @Override
    @Transactional(readOnly = true)
    public List<MembershipPlan> getActivePlans() {
        return planRepository.findByIsActiveTrueOrderByPriceAsc();
    }

    @Override
    @Transactional(readOnly = true)
    public TutorMembershipDto getCurrentMembership(String email) {
        return membershipRepository.findFirstByTutor_User_EmailAndStatusOrderByEndDateDesc(email, TutorMembershipState.ACTIVE)
                .map(this::mapToDto)
                .orElse(null);
    }

    @Override
    @Transactional
    public String createVnPayPaymentUrl(String email, UUID planId, HttpServletRequest request) {
        Tutor tutor = tutorRepository.findByUser_Email(email)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy thông tin gia sư"));

        // Chỉ cho phép mua gói khi hồ sơ đã duyệt hoặc gia sư đang gia hạn
        if (tutor.getTutorState() == TutorState.DRAFT_PROFILE || tutor.getTutorState() == TutorState.PENDING_VERIFICATION || tutor.getTutorState() == TutorState.REJECTED) {
            throw new BadRequestException("Hồ sơ giảng dạy của bạn chưa được duyệt. Vui lòng hoàn thành bước nộp hồ sơ trước khi mua gói thành viên.");
        }

        MembershipPlan plan = planRepository.findById(planId)
                .orElseThrow(() -> new ResourceNotFoundException("Gói thành viên không tồn tại hoặc đã ngừng cung cấp"));

        String txnRef = "KZ" + System.currentTimeMillis();
        String orderInfo = "KizunaTutor Thanh toan goi " + plan.getCode() + " cho " + tutor.getUser().getEmail();

        // Khởi tạo bản ghi giao dịch ở trạng thái PENDING
        TutorMembership pendingMembership = TutorMembership.builder()
                .tutor(tutor)
                .plan(plan)
                .startDate(LocalDate.now())
                .endDate(LocalDate.now().plusMonths(plan.getDurationMonths()))
                .status(TutorMembershipState.CANCELLED) // Tạm đặt CANCELLED cho đến khi thanh toán thành công
                .amount(plan.getPrice())
                .transactionRef(txnRef)
                .paymentStatus("PENDING")
                .paymentGateway("VNPAY")
                .build();
        membershipRepository.save(pendingMembership);

        String ipAddress = request != null ? request.getRemoteAddr() : "127.0.0.1";
        return vnPayService.createPaymentUrl(orderInfo, plan.getPrice(), txnRef, ipAddress);
    }

    @Override
    @Transactional
    public boolean processVnPayReturn(Map<String, String> vnpParams) {
        log.info("Nhận callback phản hồi từ VNPay: {}", vnpParams);
        String txnRef = vnpParams.get("vnp_TxnRef");
        String responseCode = vnpParams.get("vnp_ResponseCode");

        if (txnRef == null || txnRef.isBlank()) {
            log.warn("Giao dịch VNPay thiếu vnp_TxnRef");
            return false;
        }

        TutorMembership membership = membershipRepository.findByTransactionRef(txnRef).orElse(null);
        if (membership == null) {
            log.warn("Không tìm thấy giao dịch với mã tham chiếu: {}", txnRef);
            return false;
        }

        boolean isValidSignature = vnPayService.validateCallback(vnpParams);
        if (!isValidSignature) {
            log.warn("Chữ ký VNPay không hợp lệ cho giao dịch: {}", txnRef);
            membership.setPaymentStatus("FAILED_CHECKSUM");
            membershipRepository.save(membership);
            return false;
        }

        if ("00".equals(responseCode)) {
            // Thanh toán thành công (UC 94)
            membership.setStatus(TutorMembershipState.ACTIVE);
            membership.setPaymentStatus("SUCCESS");
            membership.setStartDate(LocalDate.now());
            membership.setEndDate(LocalDate.now().plusMonths(membership.getPlan().getDurationMonths()));
            membershipRepository.save(membership);

            // Mở khóa toàn diện tài khoản gia sư sang ACTIVE_PAID
            Tutor tutor = membership.getTutor();
            tutor.setTutorState(TutorState.ACTIVE_PAID);
            tutor.setMembershipStatus(TutorMembershipStatus.PREMIUM);
            tutorRepository.save(tutor);

            log.info("Kích hoạt thành công gói thành viên {} cho gia sư: {}. Chuyển trạng thái: ACTIVE_PAID",
                    membership.getPlan().getName(), tutor.getUser().getEmail());
            return true;
        } else {
            membership.setPaymentStatus("FAILED_" + responseCode);
            membershipRepository.save(membership);
            log.warn("Giao dịch VNPay thất bại với mã lỗi: {}", responseCode);
            return false;
        }
    }

    @Override
    @Transactional
    public TutorMembershipDto directActivateMembershipForTest(String email, UUID planId) {
        Tutor tutor = tutorRepository.findByUser_Email(email)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy thông tin gia sư"));

        MembershipPlan plan = planRepository.findById(planId)
                .orElseThrow(() -> new ResourceNotFoundException("Gói thành viên không tồn tại"));

        String txnRef = "TEST_" + System.currentTimeMillis();

        TutorMembership membership = TutorMembership.builder()
                .tutor(tutor)
                .plan(plan)
                .startDate(LocalDate.now())
                .endDate(LocalDate.now().plusMonths(plan.getDurationMonths()))
                .status(TutorMembershipState.ACTIVE)
                .amount(plan.getPrice())
                .transactionRef(txnRef)
                .paymentStatus("SUCCESS")
                .paymentGateway("DIRECT_TEST")
                .build();
        TutorMembership saved = membershipRepository.save(membership);

        tutor.setTutorState(TutorState.ACTIVE_PAID);
        tutor.setMembershipStatus(TutorMembershipStatus.PREMIUM);
        tutorRepository.save(tutor);

        log.info("Kích hoạt thử nghiệm thành công gói {} cho gia sư: {}. Trạng thái: ACTIVE_PAID",
                plan.getName(), email);

        return mapToDto(saved);
    }

    private TutorMembershipDto mapToDto(TutorMembership m) {
        return TutorMembershipDto.builder()
                .membershipId(m.getMembershipId())
                .planId(m.getPlan().getPlanId())
                .planName(m.getPlan().getName())
                .planCode(m.getPlan().getCode())
                .durationMonths(m.getPlan().getDurationMonths())
                .amount(m.getAmount())
                .startDate(m.getStartDate())
                .endDate(m.getEndDate())
                .status(m.getStatus().name())
                .paymentStatus(m.getPaymentStatus())
                .transactionRef(m.getTransactionRef())
                .build();
    }
}
